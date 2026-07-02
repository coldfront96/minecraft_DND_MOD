package com.khimairacraft.classes;

import com.khimairacraft.DnDMods;
import com.khimairacraft.network.SyncPlayerDataPayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class ClassCommand {

    private static final SuggestionProvider<CommandSourceStack> CLASS_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(DnDClass.values())
                            .filter(c -> c != DnDClass.NONE)
                            .map(c -> c.name().toLowerCase()),
                    builder
            );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("dndclass")
                .then(Commands.argument("class", StringArgumentType.word())
                        .suggests(CLASS_SUGGESTIONS)
                        .executes(context -> {
                            String className = StringArgumentType.getString(context, "class");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return selectClass(player, className);
                        }))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return showClass(player);
                }));

        dispatcher.register(Commands.literal("dndstats")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return showStats(player);
                }));
    }

    private static int selectClass(ServerPlayer player, String className) {
        DnDClass dndClass;
        try {
            dndClass = DnDClass.valueOf(className.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("Unknown class: " + className));
            return 0;
        }

        if (dndClass == DnDClass.NONE) {
            player.sendSystemMessage(Component.literal("Cannot select NONE as a class."));
            return 0;
        }

        DnDPlayerData data = PlayerDataHelper.get(player);
        if (data.getDnDClass() != DnDClass.NONE) {
            player.sendSystemMessage(Component.literal("You have already chosen a class! Your class is " + data.getDnDClass().getDisplayName() + "."));
            return 0;
        }
        data.setDnDClass(dndClass);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));

        player.sendSystemMessage(Component.literal("You are now a " + dndClass.getDisplayName() + "!"));
        player.sendSystemMessage(Component.literal("Resource: " + dndClass.getResourceType().getDisplayName() +
                " (" + data.getMaxResource() + ")"));
        player.sendSystemMessage(Component.literal("Press V to open your ability bar."));
        return 1;
    }

    private static int showClass(ServerPlayer player) {
        DnDPlayerData data = PlayerDataHelper.get(player);
        if (data.getDnDClass() == DnDClass.NONE) {
            player.sendSystemMessage(Component.literal("You haven't chosen a class yet."));
            player.sendSystemMessage(Component.literal("Use /dndclass <class> to choose."));
            player.sendSystemMessage(Component.literal("Available: fighter, rogue, wizard, cleric, ranger, barbarian"));
        } else {
            player.sendSystemMessage(Component.literal("Class: " + data.getDnDClass().getDisplayName() +
                    " Level " + data.getLevel()));
        }
        return 1;
    }

    private static int showStats(ServerPlayer player) {
        DnDPlayerData data = PlayerDataHelper.get(player);
        player.sendSystemMessage(Component.literal("=== D&D Stats ==="));
        player.sendSystemMessage(Component.literal("Class: " + data.getDnDClass().getDisplayName() +
                " | Level: " + data.getLevel()));
        player.sendSystemMessage(Component.literal("XP: " + data.getXp() + "/" + data.getXpForNextLevel()));
        player.sendSystemMessage(Component.literal("HP: " + data.getCurrentHp() + "/" + data.getMaxHp()));
        if (data.getDnDClass() != DnDClass.NONE) {
            player.sendSystemMessage(Component.literal(
                    data.getDnDClass().getResourceType().getDisplayName() + ": " +
                            data.getCurrentResource() + "/" + data.getMaxResource()));
        }
        player.sendSystemMessage(Component.literal(
                "STR:" + data.getAbilityScores().getStrength() +
                        " DEX:" + data.getAbilityScores().getDexterity() +
                        " CON:" + data.getAbilityScores().getConstitution() +
                        " INT:" + data.getAbilityScores().getIntelligence() +
                        " WIS:" + data.getAbilityScores().getWisdom() +
                        " CHA:" + data.getAbilityScores().getCharisma()));
        return 1;
    }
}
