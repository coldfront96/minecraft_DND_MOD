package com.khimairacraft.guild;

import com.khimairacraft.DnDMods;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * All {@code /guild} subcommands. The command layer parses arguments and the
 * current chunk, then delegates to {@link GuildManager}; messaging and rank
 * validation live in the manager.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class GuildCommand {

    private static final String PREFIX = "§6[DnDMods] ";

    private static final SuggestionProvider<CommandSourceStack> ZONE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    java.util.Arrays.stream(GuildZoneType.values()).map(Enum::name), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("guild")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("tag", StringArgumentType.word())
                                        .executes(ctx -> create(
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "tag"))))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("accept")
                        .executes(ctx -> simple(ctx.getSource().getPlayerOrException(),
                                p -> GuildManager.acceptInvite(p, p.getServer()))))
                .then(Commands.literal("decline")
                        .executes(ctx -> simple(ctx.getSource().getPlayerOrException(),
                                p -> GuildManager.declineInvite(p, p.getServer()))))
                .then(Commands.literal("leave")
                        .executes(ctx -> simple(ctx.getSource().getPlayerOrException(),
                                p -> GuildManager.leaveGuild(p, p.getServer()))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> target(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        GuildManager::kickMember))))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> target(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        GuildManager::promoteMember))))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> target(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        GuildManager::demoteMember))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> target(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        GuildManager::transferMastership))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("friendlyfire")
                        .then(Commands.literal("on")
                                .executes(ctx -> friendlyFire(ctx.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> friendlyFire(ctx.getSource().getPlayerOrException(), false))))
                .then(Commands.literal("claim")
                        .executes(ctx -> claim(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("unclaim")
                        .executes(ctx -> unclaim(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("zone")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(ZONE_SUGGESTIONS)
                                .executes(ctx -> zone(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "type")))))
                .then(Commands.literal("sethall")
                        .executes(ctx -> simple(ctx.getSource().getPlayerOrException(),
                                p -> GuildManager.setGuildHall(p, p.getServer()))))
                .then(Commands.literal("hall")
                        .executes(ctx -> simple(ctx.getSource().getPlayerOrException(),
                                p -> GuildManager.teleportToHall(p, p.getServer()))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("map")
                        .executes(ctx -> map(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource()))));
    }

    // --- Functional dispatch helpers ---

    private interface PlayerAction { boolean run(ServerPlayer player); }
    private interface TargetAction { boolean run(ServerPlayer actor, ServerPlayer target, MinecraftServer server); }

    private static int simple(ServerPlayer player, PlayerAction action) {
        return action.run(player) ? 1 : 0;
    }

    private static int target(ServerPlayer actor, ServerPlayer target, TargetAction action) {
        return action.run(actor, target, actor.getServer()) ? 1 : 0;
    }

    // --- Command bodies ---

    private static int create(ServerPlayer player, String name, String tag) {
        Guild guild = GuildManager.createGuild(player, name, tag, player.getServer());
        if (guild == null) return 0;
        player.sendSystemMessage(Component.literal(PREFIX + "§aGuild '" + guild.getGuildName()
                + "' [" + guild.getGuildTag() + "] founded!"));
        return 1;
    }

    private static int invite(ServerPlayer inviter, ServerPlayer target) {
        return GuildManager.inviteToGuild(inviter, target, inviter.getServer()) ? 1 : 0;
    }

    private static int disband(ServerPlayer player) {
        Guild guild = GuildManager.getGuildOf(player.getUUID(), player.getServer());
        if (guild == null) {
            notInGuild(player);
            return 0;
        }
        return GuildManager.disbandGuild(guild.getGuildId(), player, player.getServer()) ? 1 : 0;
    }

    private static int friendlyFire(ServerPlayer player, boolean enabled) {
        return GuildManager.setFriendlyFire(player, enabled, player.getServer()) ? 1 : 0;
    }

    private static int claim(ServerPlayer player) {
        return GuildManager.claimChunk(player, new ChunkPos(player.blockPosition()), player.getServer()) ? 1 : 0;
    }

    private static int unclaim(ServerPlayer player) {
        return GuildManager.unclaimChunk(player, new ChunkPos(player.blockPosition()), player.getServer()) ? 1 : 0;
    }

    private static int zone(ServerPlayer player, String typeName) {
        GuildZoneType zone;
        try {
            zone = GuildZoneType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal(PREFIX + "§cUnknown zone type: " + typeName));
            return 0;
        }
        return GuildManager.setZone(player, new ChunkPos(player.blockPosition()), zone, player.getServer()) ? 1 : 0;
    }

    private static int info(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Guild guild = GuildManager.getGuildOf(player.getUUID(), server);
        if (guild == null) {
            notInGuild(player);
            return 0;
        }
        UUID master = guild.getGuildMaster();
        player.sendSystemMessage(Component.literal("§6[Guild: " + guild.getGuildName()
                + " [" + guild.getGuildTag() + "]]"));
        player.sendSystemMessage(Component.literal("§eGuild Master: "
                + (master != null ? nameOf(master, server) : "None")));
        player.sendSystemMessage(Component.literal("§eOfficers: " + rankNames(guild, GuildRank.OFFICER, server)));
        player.sendSystemMessage(Component.literal("§eMembers: " + guild.size() + " total"));
        player.sendSystemMessage(Component.literal("§eClaimed Chunks: "
                + guild.getClaimedChunks().size() + "/" + guild.getMaxChunks()));
        player.sendSystemMessage(Component.literal("§eFriendly Fire: "
                + (guild.isFriendlyFireEnabled() ? "On" : "Off")));
        player.sendSystemMessage(Component.literal("§eGuild Hall: "
                + (guild.getGuildHallChunk() != null ? "Set" : "Not Set")));
        return 1;
    }

    private static int map(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ChunkPos center = new ChunkPos(player.blockPosition());
        Guild myGuild = GuildManager.getGuildOf(player.getUUID(), server);
        UUID myGuildId = myGuild != null ? myGuild.getGuildId() : null;

        player.sendSystemMessage(Component.literal("§6[DnDMods] §eChunk map around you (§a█§e=yours §c█§e=other §7░§e=free §e☆§e=here):"));
        for (int dz = -4; dz <= 4; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -4; dx <= 4; dx++) {
                ChunkPos cell = new ChunkPos(center.x + dx, center.z + dz);
                if (dx == 0 && dz == 0) {
                    row.append("§e☆");
                    continue;
                }
                Guild atCell = GuildManager.getGuildAtChunk(cell, server);
                if (atCell == null) {
                    row.append("§7░");
                } else if (myGuildId != null && atCell.getGuildId().equals(myGuildId)) {
                    row.append("§a█");
                } else {
                    row.append("§c█");
                }
            }
            row.append(" §8z=").append(center.z + dz);
            player.sendSystemMessage(Component.literal(row.toString()));
        }
        return 1;
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        GuildSavedData data = GuildManager.getData(server);
        var guilds = data.getGuilds().values();
        if (guilds.isEmpty()) {
            source.sendSystemMessage(Component.literal(PREFIX + "§eThere are no guilds yet."));
            return 1;
        }
        source.sendSystemMessage(Component.literal("§6[DnDMods] §eGuilds (" + guilds.size() + "):"));
        for (Guild guild : guilds) {
            source.sendSystemMessage(Component.literal("§e- " + guild.getGuildName()
                    + " §7[" + guild.getGuildTag() + "] (" + guild.size() + " members)"));
        }
        return 1;
    }

    // --- Helpers ---

    private static String rankNames(Guild guild, GuildRank rank, MinecraftServer server) {
        List<UUID> ids = guild.getByRank(rank);
        if (ids.isEmpty()) return "None";
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            names.add(nameOf(id, server));
        }
        return names.stream().collect(Collectors.joining(", "));
    }

    private static String nameOf(UUID id, MinecraftServer server) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) return player.getName().getString();
        }
        return id.toString().substring(0, 8);
    }

    private static void notInGuild(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(PREFIX + "§cYou are not in a guild."));
    }
}
