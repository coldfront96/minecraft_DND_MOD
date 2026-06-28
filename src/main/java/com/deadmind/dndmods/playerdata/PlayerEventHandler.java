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
            // Complete Scoundrel daily luck charges reset on login.
            if (data.getAchievementFlag("fortunate_one_unlocked")) {
                data.setLuckOfHeroesCharges(2);
            } else if (data.getAchievementFlag("luck_of_heroes_unlocked")) {
                data.setLuckOfHeroesCharges(1);
            }
            if (data.getAchievementFlag("fated_unlocked")) {
                data.setFatedCharges(1);
            }
            // Complete Champion daily charges reset on login.
            if (data.getAchievementFlag("greater_smite_unlocked")) {
                data.setSmiteCharges(3);
            } else if (data.getAchievementFlag("improved_smite_unlocked")) {
                data.setSmiteCharges(2);
            } else if (data.getAchievementFlag("smite_evil_unlocked")) {
                data.setSmiteCharges(1);
            }
            if (data.getAchievementFlag("divine_impetus_unlocked")) {
                data.setDivineImpetusCharges(1);
            }
            if (data.getAchievementFlag("zealous_surge_unlocked")) {
                data.setZealousSurgeCharges(1);
            }
            // Complete Mage daily charges reset on login.
            if (data.getAchievementFlag("eldritch_apex_unlocked")) {
                data.setEldritchApexCharges(1);
            }
            if (data.getAchievementFlag("automatic_metamagic_unlocked")) {
                data.setAutomaticMetamagicCharges(1);
            }
            if (data.getAchievementFlag("greater_warding_unlocked")) {
                data.setWardingGestureCharges(2);
            } else if (data.getAchievementFlag("warding_gesture_unlocked")) {
                data.setWardingGestureCharges(1);
            }
            if (data.getAchievementFlag("spell_reflection_unlocked")) {
                data.setSpellReflectionCharges(1);
            }
            if (data.getAchievementFlag("reactive_spell_unlocked")) {
                data.setReactiveSpellCharges(1);
            }
            if (data.getAchievementFlag("instant_metamagic_unlocked")) {
                data.setInstantMetamagicCharges(3);
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
