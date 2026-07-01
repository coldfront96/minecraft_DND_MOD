package com.deadmind.dndmods.guild;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent store for all guilds, attached to the overworld's data storage.
 *
 * <p>Only the guild records themselves are written to NBT; the {@code playerToGuild}
 * and {@code chunkToGuild} indexes are rebuilt from those records on load to keep
 * the persisted form compact and self-consistent. Pending invites and their
 * timestamps are session-only and never serialized.
 */
public class GuildSavedData extends SavedData {

    private static final String DATA_ID = "dndmods_guilds";
    private static final String PREFIX = "§6[DnDMods] ";
    private static final long INVITE_EXPIRY_MS = 60_000L;

    public static GuildSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(GuildSavedData::new, GuildSavedData::load, null),
                DATA_ID);
    }

    // Persisted state.
    private final Map<UUID, Guild> guilds = new ConcurrentHashMap<>();
    // Derived indexes, rebuilt on load.
    private final Map<UUID, UUID> playerToGuild = new ConcurrentHashMap<>();
    private final Map<ChunkPos, UUID> chunkToGuild = new ConcurrentHashMap<>();

    // Session-only invite tracking (never serialized).
    private final Map<UUID, UUID> pendingGuildInvites = new ConcurrentHashMap<>();    // target -> guildId
    private final Map<UUID, Long> guildInviteTimestamps = new ConcurrentHashMap<>();  // target -> sent millis
    private final Map<UUID, UUID> guildInviteInviters = new ConcurrentHashMap<>();    // target -> inviter

    public GuildSavedData() {}

    // ------------------------------------------------------------------
    // Index accessors (live maps — callers mutate then call setDirty()).
    // ------------------------------------------------------------------

    public Map<UUID, Guild> getGuilds() { return guilds; }
    public Map<UUID, UUID> getPlayerToGuild() { return playerToGuild; }
    public Map<ChunkPos, UUID> getChunkToGuild() { return chunkToGuild; }

    // ------------------------------------------------------------------
    // Session-only invites.
    // ------------------------------------------------------------------

    public void putInvite(UUID target, UUID guildId, UUID inviter) {
        pendingGuildInvites.put(target, guildId);
        guildInviteTimestamps.put(target, System.currentTimeMillis());
        guildInviteInviters.put(target, inviter);
    }

    public boolean hasInvite(UUID target) { return pendingGuildInvites.containsKey(target); }

    public UUID getInviteGuild(UUID target) { return pendingGuildInvites.get(target); }
    public Long getInviteTime(UUID target) { return guildInviteTimestamps.get(target); }
    public UUID getInviteInviter(UUID target) { return guildInviteInviters.get(target); }

    public void clearInvite(UUID target) {
        pendingGuildInvites.remove(target);
        guildInviteTimestamps.remove(target);
        guildInviteInviters.remove(target);
    }

    /** Drops and notifies expired invites; pending invites never persist. */
    public void tickInviteExpiry(MinecraftServer server) {
        if (server == null) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : new LinkedHashMap<>(guildInviteTimestamps).entrySet()) {
            if (now - entry.getValue() > INVITE_EXPIRY_MS) {
                UUID target = entry.getKey();
                clearInvite(target);
                ServerPlayer player = server.getPlayerList().getPlayer(target);
                if (player != null) {
                    player.sendSystemMessage(Component.literal(PREFIX + "§eYour guild invite has expired."));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag guildList = new ListTag();
        for (Guild guild : guilds.values()) {
            guildList.add(saveGuild(guild));
        }
        tag.put("Guilds", guildList);
        return tag;
    }

    private static CompoundTag saveGuild(Guild guild) {
        CompoundTag g = new CompoundTag();
        g.putUUID("GuildId", guild.getGuildId());
        g.putString("GuildName", guild.getGuildName());
        g.putString("GuildTag", guild.getGuildTag());
        g.putBoolean("FriendlyFire", guild.isFriendlyFireEnabled());
        g.putLong("CreatedAt", guild.getCreatedAt());
        g.putInt("GuildLevel", guild.getGuildLevel());

        if (guild.getGuildHallChunk() != null) {
            g.putBoolean("HasHall", true);
            g.putLong("HallChunk", guild.getGuildHallChunk().toLong());
        } else {
            g.putBoolean("HasHall", false);
        }

        ListTag memberList = new ListTag();
        for (Map.Entry<UUID, GuildRank> entry : guild.getMembers().entrySet()) {
            CompoundTag m = new CompoundTag();
            m.putUUID("Id", entry.getKey());
            m.putString("Rank", entry.getValue().name());
            memberList.add(m);
        }
        g.put("Members", memberList);

        long[] claims = new long[guild.getClaimedChunks().size()];
        int idx = 0;
        for (ChunkPos pos : guild.getClaimedChunks()) {
            claims[idx++] = pos.toLong();
        }
        g.putLongArray("ClaimedChunks", claims);

        ListTag zoneList = new ListTag();
        for (Map.Entry<ChunkPos, GuildZoneType> entry : guild.getChunkZones().entrySet()) {
            CompoundTag z = new CompoundTag();
            z.putLong("Chunk", entry.getKey().toLong());
            z.putString("Zone", entry.getValue().name());
            zoneList.add(z);
        }
        g.put("ChunkZones", zoneList);

        return g;
    }

    public static GuildSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GuildSavedData data = new GuildSavedData();
        ListTag guildList = tag.getList("Guilds", Tag.TAG_COMPOUND);
        for (int i = 0; i < guildList.size(); i++) {
            Guild guild = loadGuild(guildList.getCompound(i));
            data.guilds.put(guild.getGuildId(), guild);
            // Rebuild derived indexes.
            for (UUID member : guild.getMembers().keySet()) {
                data.playerToGuild.put(member, guild.getGuildId());
            }
            for (ChunkPos pos : guild.getClaimedChunks()) {
                data.chunkToGuild.put(pos, guild.getGuildId());
            }
        }
        return data;
    }

    private static Guild loadGuild(CompoundTag g) {
        UUID guildId = g.getUUID("GuildId");
        Guild guild = new Guild(guildId, g.getString("GuildName"), g.getString("GuildTag"));
        guild.setFriendlyFireEnabled(g.getBoolean("FriendlyFire"));
        guild.setCreatedAt(g.getLong("CreatedAt"));
        guild.setGuildLevel(g.contains("GuildLevel") ? g.getInt("GuildLevel") : 1);

        ListTag memberList = g.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            CompoundTag m = memberList.getCompound(i);
            GuildRank rank;
            try {
                rank = GuildRank.valueOf(m.getString("Rank"));
            } catch (IllegalArgumentException e) {
                rank = GuildRank.MEMBER;
            }
            guild.addMember(m.getUUID("Id"), rank);
        }

        for (long packed : g.getLongArray("ClaimedChunks")) {
            guild.getClaimedChunks().add(new ChunkPos(packed));
        }

        ListTag zoneList = g.getList("ChunkZones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zoneList.size(); i++) {
            CompoundTag z = zoneList.getCompound(i);
            GuildZoneType zone;
            try {
                zone = GuildZoneType.valueOf(z.getString("Zone"));
            } catch (IllegalArgumentException e) {
                zone = GuildZoneType.GENERAL;
            }
            guild.getChunkZones().put(new ChunkPos(z.getLong("Chunk")), zone);
        }

        if (g.getBoolean("HasHall")) {
            guild.setGuildHallChunk(new ChunkPos(g.getLong("HallChunk")));
        }

        return guild;
    }
}
