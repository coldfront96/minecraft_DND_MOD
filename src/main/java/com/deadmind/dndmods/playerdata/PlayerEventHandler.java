package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.OpenClassSelectionPayload;
import com.deadmind.dndmods.network.OpenRaceSelectionPayload;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);

            data.getAbilityHotbar().validateAndClean(data);

            // Daily racial-feat resources reset on login.
            data.setHalfOrcFerocityAvailable(true);
            data.setAchievementFlag("dwarven_resilience_used", false);
            if (data.hasFeat("halfling_luck")) {
                data.setHalflingLuckCharges(1);
            }
            // Pious Defiance (Complete Divine): daily auto-success charge.
            if (data.getAchievementFlag("pious_defiance_unlocked") && data.getPiousDefianceCharges() < 1) {
                data.setPiousDefianceCharges(1);
            }
            // Complete Arcane daily charges reset on login.
            if (data.getAchievementFlag("school_mastery_unlocked")) {
                data.setSchoolMasteryCharges(1);
            }
            if (data.getAchievementFlag("innate_spell_unlocked")) {
                data.setInnateSpellCharges(3);
            }
            if (data.getAchievementFlag("magical_training_unlocked")) {
                data.setMagicalTrainingCharges(3);
            }

            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));

            if (data.getRace() == DnDRace.NONE) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenRaceSelectionPayload());
            } else if (data.getDnDClass() == DnDClass.NONE) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenClassSelectionPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            data.setCurrentHp(data.getMaxHp());
            data.setCurrentResource(data.getMaxResource());
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
        }
    }
}
