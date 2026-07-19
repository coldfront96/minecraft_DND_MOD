package com.khimairacraft.party;

import com.khimairacraft.DnDMods;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * All {@code /party} subcommands. The command layer only parses arguments,
 * resolves the caller's party where needed, and reports the not-in-a-party /
 * permission cases; every other validation and all broadcasts live in {@link
 * PartyManager} so messaging stays centralised.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class PartyCommand {

    private static final String PREFIX = "§6[DnDMods] ";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> createParty(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("accept")
                        .executes(ctx -> accept(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("decline")
                        .executes(ctx -> decline(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("leave")
                        .executes(ctx -> leave(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kick(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> promote(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> demote(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> transfer(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("leader")
                        .then(Commands.literal("claim")
                                .executes(ctx -> claim(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("friendlyfire")
                        .then(Commands.literal("on")
                                .executes(ctx -> friendlyFire(ctx.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> friendlyFire(ctx.getSource().getPlayerOrException(), false))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource()))));
    }

    private static int createParty(ServerPlayer player, String name) {
        Party party = PartyManager.getInstance().createParty(player, name);
        if (party == null) return 0;
        player.sendSystemMessage(Component.literal(PREFIX + "§aParty '" + party.getPartyName()
                + "' created! Use /party invite <player> to add members."));
        return 1;
    }

    private static int invite(ServerPlayer inviter, ServerPlayer target) {
        return PartyManager.getInstance().invitePlayer(inviter, target) ? 1 : 0;
    }

    private static int accept(ServerPlayer player) {
        return PartyManager.getInstance().acceptInvite(player) ? 1 : 0;
    }

    private static int decline(ServerPlayer player) {
        return PartyManager.getInstance().declineInvite(player) ? 1 : 0;
    }

    private static int leave(ServerPlayer player) {
        return PartyManager.getInstance().leaveParty(player) ? 1 : 0;
    }

    private static int kick(ServerPlayer kicker, ServerPlayer target) {
        PartyManager manager = PartyManager.getInstance();
        Party party = manager.getPartyOf(kicker.getUUID());
        if (party == null) {
            notInParty(kicker);
            return 0;
        }
        return manager.kickPlayer(party.getPartyId(), kicker, target) ? 1 : 0;
    }

    private static int promote(ServerPlayer promoter, ServerPlayer target) {
        PartyManager manager = PartyManager.getInstance();
        Party party = manager.getPartyOf(promoter.getUUID());
        if (party == null) {
            notInParty(promoter);
            return 0;
        }
        return manager.promotePlayer(party.getPartyId(), promoter, target) ? 1 : 0;
    }

    private static int demote(ServerPlayer demoter, ServerPlayer target) {
        PartyManager manager = PartyManager.getInstance();
        Party party = manager.getPartyOf(demoter.getUUID());
        if (party == null) {
            notInParty(demoter);
            return 0;
        }
        return manager.demotePlayer(party.getPartyId(), demoter, target) ? 1 : 0;
    }

    private static int transfer(ServerPlayer leader, ServerPlayer target) {
        return PartyManager.getInstance().transferLeadership(leader, target) ? 1 : 0;
    }

    private static int claim(ServerPlayer player) {
        return PartyManager.getInstance().claimLeadership(player) ? 1 : 0;
    }

    private static int disband(ServerPlayer player) {
        PartyManager manager = PartyManager.getInstance();
        Party party = manager.getPartyOf(player.getUUID());
        if (party == null) {
            notInParty(player);
            return 0;
        }
        return manager.disbandParty(party.getPartyId(), player) ? 1 : 0;
    }

    private static int friendlyFire(ServerPlayer player, boolean enabled) {
        return PartyManager.getInstance().setFriendlyFire(player, enabled) ? 1 : 0;
    }

    private static int info(ServerPlayer player) {
        PartyManager manager = PartyManager.getInstance();
        Party party = manager.getPartyOf(player.getUUID());
        if (party == null) {
            notInParty(player);
            return 0;
        }
        MinecraftServer server = player.getServer();

        player.sendSystemMessage(Component.literal("§6[DnDMods Party: " + party.getPartyName() + "]"));
        UUID leader = party.getLeader();
        player.sendSystemMessage(Component.literal("§eLeader: "
                + (leader != null ? manager.nameOf(leader, server) : "None")));
        player.sendSystemMessage(Component.literal("§eFirst: "
                + rankNames(party, PartyRank.FIRST_IN_COMMAND, manager, server)));
        player.sendSystemMessage(Component.literal("§eSecond: "
                + rankNames(party, PartyRank.SECOND_IN_COMMAND, manager, server)));
        player.sendSystemMessage(Component.literal("§eThird: "
                + rankNames(party, PartyRank.THIRD_IN_COMMAND, manager, server)));
        player.sendSystemMessage(Component.literal("§eMembers: "
                + rankNames(party, PartyRank.MEMBER, manager, server)));
        player.sendSystemMessage(Component.literal("§eFriendly Fire: "
                + (party.isFriendlyFireEnabled() ? "On" : "Off")));
        return 1;
    }

    private static int list(CommandSourceStack source) {
        PartyManager manager = PartyManager.getInstance();
        MinecraftServer server = source.getServer();
        ServerPlayer player = source.getPlayer();

        boolean allowed = source.hasPermission(2);
        if (!allowed && player != null) {
            Party own = manager.getPartyOf(player.getUUID());
            allowed = own != null && own.hasRank(player.getUUID(), PartyRank.LEADER);
        }
        if (!allowed) {
            source.sendFailure(Component.literal(PREFIX + "§cOnly party leaders or operators may list parties."));
            return 0;
        }

        var parties = manager.getAllParties().values();
        if (parties.isEmpty()) {
            source.sendSystemMessage(Component.literal(PREFIX + "§eThere are no active parties."));
            return 1;
        }
        source.sendSystemMessage(Component.literal("§6[DnDMods] §eActive parties (" + parties.size() + "):"));
        for (Party party : parties) {
            UUID leader = party.getLeader();
            String leaderName = leader != null ? manager.nameOf(leader, server) : "None";
            source.sendSystemMessage(Component.literal("§e- " + party.getPartyName()
                    + " §7(" + party.size() + "/" + Party.MAX_MEMBERS + ", leader " + leaderName + ")"));
        }
        return 1;
    }

    private static String rankNames(Party party, PartyRank rank, PartyManager manager, MinecraftServer server) {
        List<UUID> ids = party.getByRank(rank);
        if (ids.isEmpty()) return "None";
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            names.add(manager.nameOf(id, server));
        }
        return names.stream().collect(Collectors.joining(", "));
    }

    private static void notInParty(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(PREFIX + "§cYou are not in a party."));
    }
}
