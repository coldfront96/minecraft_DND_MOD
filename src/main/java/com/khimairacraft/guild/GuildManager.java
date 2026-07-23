package com.khimairacraft.guild;

import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin service layer over {@link GuildSavedData}. Every mutation goes through
 * here and calls {@link GuildSavedData#setDirty()} so persistence stays in sync.
 * All user-facing messaging (errors, broadcasts) is centralised here.
 */
public final class GuildManager {

    private static final String PREFIX = "§6[KhimairaCraft] ";
    private static final int MIN_FOUNDER_LEVEL = 5;
    private static final long INVITE_EXPIRY_MS = 60_000L;
    /** Guild hall teleport cooldown: 5 minutes. Session-only, never persisted. */
    private static final long HALL_COOLDOWN_MS = 5L * 60_000L;

    private static final Map<UUID, Long> guildHallTeleportCooldowns = new ConcurrentHashMap<>();

    private GuildManager() {}

    public static GuildSavedData getData(MinecraftServer server) {
        return GuildSavedData.get(server);
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    @Nullable
    public static Guild getGuildOf(UUID playerId, MinecraftServer server) {
        if (playerId == null || server == null) return null;
        GuildSavedData data = getData(server);
        UUID guildId = data.getPlayerToGuild().get(playerId);
        return guildId == null ? null : data.getGuilds().get(guildId);
    }

    @Nullable
    public static Guild getGuildAtChunk(ChunkPos pos, MinecraftServer server) {
        if (pos == null || server == null) return null;
        GuildSavedData data = getData(server);
        UUID guildId = data.getChunkToGuild().get(pos);
        return guildId == null ? null : data.getGuilds().get(guildId);
    }

    public static boolean isSameGuild(UUID playerA, UUID playerB, MinecraftServer server) {
        if (playerA == null || playerB == null || server == null) return false;
        GuildSavedData data = getData(server);
        UUID a = data.getPlayerToGuild().get(playerA);
        UUID b = data.getPlayerToGuild().get(playerB);
        return a != null && a.equals(b);
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Nullable
    public static Guild createGuild(ServerPlayer founder, String name, String tag, MinecraftServer server) {
        if (founder == null || server == null) return null;
        GuildSavedData data = getData(server);

        if (name == null || name.isBlank() || name.length() > Guild.MAX_NAME_LENGTH) {
            msg(founder, PREFIX + "§cGuild name must be 1-" + Guild.MAX_NAME_LENGTH + " characters.");
            return null;
        }
        if (tag == null || tag.isBlank() || tag.length() > Guild.MAX_TAG_LENGTH) {
            msg(founder, PREFIX + "§cGuild tag must be 1-" + Guild.MAX_TAG_LENGTH + " characters.");
            return null;
        }
        if (data.getPlayerToGuild().containsKey(founder.getUUID())) {
            msg(founder, PREFIX + "§cYou are already in a guild.");
            return null;
        }
        if (PlayerDataHelper.get(founder).getTotalLevel() < MIN_FOUNDER_LEVEL) {
            msg(founder, PREFIX + "§cYou must be level " + MIN_FOUNDER_LEVEL + " to found a guild.");
            return null;
        }

        Guild guild = new Guild(UUID.randomUUID(), name, tag);
        guild.addMember(founder.getUUID(), GuildRank.GUILD_MASTER);
        data.getGuilds().put(guild.getGuildId(), guild);
        data.getPlayerToGuild().put(founder.getUUID(), guild.getGuildId());
        data.setDirty();
        return guild;
    }

    public static boolean disbandGuild(UUID guildId, ServerPlayer requestor, MinecraftServer server) {
        if (requestor == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = data.getGuilds().get(guildId);
        if (guild == null) {
            msg(requestor, PREFIX + "§cThat guild no longer exists.");
            return false;
        }
        if (!isRank(guild, requestor.getUUID(), GuildRank.GUILD_MASTER)) {
            msg(requestor, PREFIX + "§cOnly the guild master can disband the guild.");
            return false;
        }

        broadcast(guild, server, PREFIX + "§cGuild '" + guild.getGuildName() + "' has been disbanded.");
        for (ChunkPos pos : guild.getClaimedChunks()) {
            data.getChunkToGuild().remove(pos);
        }
        for (UUID member : guild.getMembers().keySet()) {
            data.getPlayerToGuild().remove(member);
        }
        data.getGuilds().remove(guildId);
        data.setDirty();
        return true;
    }

    public static boolean leaveGuild(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (isRank(guild, player.getUUID(), GuildRank.GUILD_MASTER)) {
            msg(player, PREFIX + "§cThe guild master cannot leave. Transfer mastership or disband first.");
            return false;
        }

        guild.removeMember(player.getUUID());
        data.getPlayerToGuild().remove(player.getUUID());
        data.setDirty();
        msg(player, PREFIX + "§eYou have left the guild.");
        broadcast(guild, server, PREFIX + "§e" + player.getName().getString() + " has left the guild.");
        return true;
    }

    // ------------------------------------------------------------------
    // Invites
    // ------------------------------------------------------------------

    public static boolean inviteToGuild(ServerPlayer inviter, ServerPlayer target, MinecraftServer server) {
        if (inviter == null || target == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(inviter.getUUID(), server);
        if (guild == null) {
            msg(inviter, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, inviter.getUUID())) {
            msg(inviter, PREFIX + "§cOnly the guild master and officers may invite players.");
            return false;
        }
        if (data.getPlayerToGuild().containsKey(target.getUUID())) {
            msg(inviter, PREFIX + "§c" + target.getName().getString() + " is already in a guild.");
            return false;
        }
        if (data.hasInvite(target.getUUID())) {
            msg(inviter, PREFIX + "§c" + target.getName().getString() + " already has a pending guild invite.");
            return false;
        }

        data.putInvite(target.getUUID(), guild.getGuildId(), inviter.getUUID());
        msg(inviter, PREFIX + "§aInvited " + target.getName().getString() + " to the guild.");
        msg(target, PREFIX + "§e" + inviter.getName().getString() + " has invited you to join the guild '"
                + guild.getGuildName() + "'. Type /guild accept or /guild decline.");
        return true;
    }

    public static boolean acceptInvite(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return false;
        GuildSavedData data = getData(server);
        UUID guildId = data.getInviteGuild(player.getUUID());
        Long sentAt = data.getInviteTime(player.getUUID());
        if (guildId == null || sentAt == null) {
            msg(player, PREFIX + "§cYou have no pending guild invite.");
            return false;
        }
        if (System.currentTimeMillis() - sentAt > INVITE_EXPIRY_MS) {
            data.clearInvite(player.getUUID());
            msg(player, PREFIX + "§cThat guild invite has expired.");
            return false;
        }
        Guild guild = data.getGuilds().get(guildId);
        if (guild == null) {
            data.clearInvite(player.getUUID());
            msg(player, PREFIX + "§cThat guild no longer exists.");
            return false;
        }
        if (data.getPlayerToGuild().containsKey(player.getUUID())) {
            data.clearInvite(player.getUUID());
            msg(player, PREFIX + "§cYou are already in a guild.");
            return false;
        }

        guild.addMember(player.getUUID(), GuildRank.MEMBER);
        data.getPlayerToGuild().put(player.getUUID(), guild.getGuildId());
        data.clearInvite(player.getUUID());
        data.setDirty();
        broadcast(guild, server, PREFIX + "§a" + player.getName().getString() + " has joined the guild!");
        return true;
    }

    public static boolean declineInvite(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return false;
        GuildSavedData data = getData(server);
        if (!data.hasInvite(player.getUUID())) {
            msg(player, PREFIX + "§cYou have no pending guild invite.");
            return false;
        }
        UUID inviterId = data.getInviteInviter(player.getUUID());
        data.clearInvite(player.getUUID());
        msg(player, PREFIX + "§eYou declined the guild invite.");
        if (inviterId != null) {
            ServerPlayer inviter = server.getPlayerList().getPlayer(inviterId);
            if (inviter != null) {
                msg(inviter, PREFIX + "§e" + player.getName().getString() + " declined your guild invite.");
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Rank operations
    // ------------------------------------------------------------------

    public static boolean kickMember(ServerPlayer kicker, ServerPlayer target, MinecraftServer server) {
        if (kicker == null || target == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(kicker.getUUID(), server);
        if (guild == null) {
            msg(kicker, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!guild.canKick(kicker.getUUID(), target.getUUID())) {
            msg(kicker, PREFIX + "§cYou cannot kick that player.");
            return false;
        }
        guild.removeMember(target.getUUID());
        data.getPlayerToGuild().remove(target.getUUID());
        data.setDirty();
        msg(target, PREFIX + "§cYou have been kicked from the guild.");
        broadcast(guild, server, PREFIX + "§e" + target.getName().getString() + " has been kicked from the guild.");
        return true;
    }

    public static boolean promoteMember(ServerPlayer promoter, ServerPlayer target, MinecraftServer server) {
        if (promoter == null || target == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(promoter.getUUID(), server);
        if (guild == null) {
            msg(promoter, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!guild.canPromote(promoter.getUUID(), target.getUUID())) {
            msg(promoter, PREFIX + "§cYou cannot promote that player.");
            return false;
        }
        guild.setRank(target.getUUID(), GuildRank.OFFICER);
        data.setDirty();
        broadcast(guild, server, PREFIX + "§e" + target.getName().getString() + " has been promoted to Officer.");
        return true;
    }

    public static boolean demoteMember(ServerPlayer demoter, ServerPlayer target, MinecraftServer server) {
        if (demoter == null || target == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(demoter.getUUID(), server);
        if (guild == null) {
            msg(demoter, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        GuildRank demoterRank = guild.getRank(demoter.getUUID());
        GuildRank targetRank = guild.getRank(target.getUUID());
        if (demoterRank == null || targetRank == null) {
            msg(demoter, PREFIX + "§cThat player is not in your guild.");
            return false;
        }
        if (targetRank != GuildRank.OFFICER || !demoterRank.outranks(targetRank)) {
            msg(demoter, PREFIX + "§cYou cannot demote that player.");
            return false;
        }
        guild.setRank(target.getUUID(), GuildRank.MEMBER);
        data.setDirty();
        broadcast(guild, server, PREFIX + "§e" + target.getName().getString() + " has been demoted to Member.");
        return true;
    }

    public static boolean transferMastership(ServerPlayer master, ServerPlayer target, MinecraftServer server) {
        if (master == null || target == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(master.getUUID(), server);
        if (guild == null) {
            msg(master, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isRank(guild, master.getUUID(), GuildRank.GUILD_MASTER)) {
            msg(master, PREFIX + "§cOnly the guild master can transfer mastership.");
            return false;
        }
        if (!guild.isMember(target.getUUID())) {
            msg(master, PREFIX + "§cThat player is not in your guild.");
            return false;
        }
        if (master.getUUID().equals(target.getUUID())) {
            msg(master, PREFIX + "§cYou are already the guild master.");
            return false;
        }
        guild.transferMastership(target.getUUID());
        data.setDirty();
        broadcast(guild, server, PREFIX + "§e" + target.getName().getString() + " is now the guild master.");
        return true;
    }

    public static boolean setFriendlyFire(ServerPlayer actor, boolean enabled, MinecraftServer server) {
        if (actor == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(actor.getUUID(), server);
        if (guild == null) {
            msg(actor, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, actor.getUUID())) {
            msg(actor, PREFIX + "§cOnly the guild master and officers may change friendly fire.");
            return false;
        }
        guild.setFriendlyFireEnabled(enabled);
        data.setDirty();
        broadcast(guild, server, PREFIX + "§eGuild friendly fire is now "
                + (enabled ? "§cON" : "§aOFF") + "§e.");
        return true;
    }

    // ------------------------------------------------------------------
    // Land claims / zones / hall
    // ------------------------------------------------------------------

    public static boolean claimChunk(ServerPlayer player, ChunkPos pos, MinecraftServer server) {
        if (player == null || pos == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, player.getUUID())) {
            msg(player, PREFIX + "§cOnly the guild master and officers may claim land.");
            return false;
        }
        if (data.getChunkToGuild().containsKey(pos)) {
            msg(player, PREFIX + "§cThat chunk is already claimed.");
            return false;
        }
        if (!guild.canClaimMore()) {
            msg(player, PREFIX + "§cYour guild has reached its claim limit ("
                    + guild.getMaxChunks() + " chunks).");
            return false;
        }
        guild.claimChunk(pos, GuildZoneType.GENERAL);
        data.getChunkToGuild().put(pos, guild.getGuildId());
        data.setDirty();
        msg(player, PREFIX + "§aChunk claimed! ["
                + guild.getClaimedChunks().size() + "/" + guild.getMaxChunks() + "] chunks used.");
        return true;
    }

    public static boolean unclaimChunk(ServerPlayer player, ChunkPos pos, MinecraftServer server) {
        if (player == null || pos == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, player.getUUID())) {
            msg(player, PREFIX + "§cOnly the guild master and officers may unclaim land.");
            return false;
        }
        UUID owner = data.getChunkToGuild().get(pos);
        if (owner == null || !owner.equals(guild.getGuildId())) {
            msg(player, PREFIX + "§cThat chunk is not claimed by your guild.");
            return false;
        }
        guild.unclaimChunk(pos);
        data.getChunkToGuild().remove(pos);
        data.setDirty();
        msg(player, PREFIX + "§eChunk unclaimed.");
        return true;
    }

    public static boolean setZone(ServerPlayer player, ChunkPos pos, GuildZoneType zone, MinecraftServer server) {
        if (player == null || pos == null || zone == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, player.getUUID())) {
            msg(player, PREFIX + "§cOnly the guild master and officers may set zones.");
            return false;
        }
        if (!guild.isChunkClaimed(pos)) {
            msg(player, PREFIX + "§cThat chunk is not claimed by your guild.");
            return false;
        }
        guild.setZoneType(pos, zone);
        data.setDirty();
        msg(player, PREFIX + "§aZone set to " + zone.name() + " for this chunk.");
        return true;
    }

    public static boolean setGuildHall(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return false;
        GuildSavedData data = getData(server);
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        if (!isAtLeastOfficer(guild, player.getUUID())) {
            msg(player, PREFIX + "§cOnly the guild master and officers may set the guild hall.");
            return false;
        }
        ChunkPos here = new ChunkPos(player.blockPosition());
        guild.setGuildHallChunk(here);
        data.setDirty();
        msg(player, PREFIX + "§aGuild hall set to your current chunk.");
        return true;
    }

    public static boolean teleportToHall(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return false;
        Guild guild = getGuildOf(player.getUUID(), server);
        if (guild == null) {
            msg(player, PREFIX + "§cYou are not in a guild.");
            return false;
        }
        ChunkPos hall = guild.getGuildHallChunk();
        if (hall == null) {
            msg(player, PREFIX + "§cYour guild has not set a guild hall. Use /guild sethall first.");
            return false;
        }
        Long lastUse = guildHallTeleportCooldowns.get(player.getUUID());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < HALL_COOLDOWN_MS) {
            long remaining = (HALL_COOLDOWN_MS - (now - lastUse)) / 1000L;
            msg(player, PREFIX + "§cGuild hall teleport is on cooldown. " + remaining + " seconds remaining.");
            return false;
        }

        ServerLevel level = player.serverLevel();
        int x = hall.getMiddleBlockX();
        int z = hall.getMiddleBlockZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        guildHallTeleportCooldowns.put(player.getUUID(), now);
        msg(player, PREFIX + "§aTeleported to the guild hall.");
        return true;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isRank(Guild guild, UUID id, GuildRank rank) {
        return rank == guild.getRank(id);
    }

    private static boolean isAtLeastOfficer(Guild guild, UUID id) {
        GuildRank rank = guild.getRank(id);
        return rank == GuildRank.GUILD_MASTER || rank == GuildRank.OFFICER;
    }

    private static void broadcast(Guild guild, MinecraftServer server, String message) {
        if (guild == null || server == null) return;
        Component component = Component.literal(message);
        for (UUID member : guild.getMembers().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                player.sendSystemMessage(component);
            }
        }
    }

    private static void msg(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
}
