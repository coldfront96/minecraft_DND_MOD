package com.deadmind.dndmods.race;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSleepInBedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class RacialTraitHandler {

    private static boolean warforgedRegenActive = false;

    private static final Set<DnDRace> DARKVISION_RACES = Set.of(
            DnDRace.ELF, DnDRace.DWARF, DnDRace.GNOME, DnDRace.HALF_ORC, DnDRace.HALF_ELF,
            DnDRace.DRAGONBORN, DnDRace.TIEFLING, DnDRace.AASIMAR, DnDRace.WARFORGED
    );

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();
        if (race == DnDRace.NONE) return;

        handleDarkvision(player, race);
        handleWarforgedHunger(player, race);
        handleWarforgedRegen(player, data, race);
        handleGoliathPowerfulBuild(player, data, race);
    }

    private static void handleDarkvision(ServerPlayer player, DnDRace race) {
        if (!DARKVISION_RACES.contains(race)) return;

        BlockPos pos = player.blockPosition();
        int lightLevel = player.level().getMaxLocalRawBrightness(pos);
        if (lightLevel > 4) return;

        MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing != null && existing.getAmplifier() > 0) return;
        if (existing != null && existing.getDuration() > 200) return;

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
    }

    private static void handleWarforgedHunger(ServerPlayer player, DnDRace race) {
        if (race != DnDRace.WARFORGED) return;
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
        }
    }

    private static void handleWarforgedRegen(ServerPlayer player, DnDPlayerData data, DnDRace race) {
        if (race != DnDRace.WARFORGED) return;

        if (player.getHealth() < player.getMaxHealth() && player.tickCount % 160 == 0) {
            warforgedRegenActive = true;
            try {
                player.heal(1.0f);
            } finally {
                warforgedRegenActive = false;
            }
        }
    }

    private static void handleGoliathPowerfulBuild(ServerPlayer player, DnDPlayerData data, DnDRace race) {
        if (race != DnDRace.GOLIATH) return;
        // No vanilla carry weight attribute — flagged for future inventory expansion
        data.setAchievementFlag("powerful_build", true);
    }

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();

        if (race == DnDRace.ELF || race == DnDRace.HALF_ELF) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(
                    Component.literal("§6[DnDMods] §eElves do not sleep — you enter a meditative trance instead."),
                    false);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();
        if (race == DnDRace.NONE) return;

        // Dwarf: 50% poison resistance
        if (race == DnDRace.DWARF && event.getSource().is(DamageTypes.MAGIC)) {
            if (player.hasEffect(MobEffects.POISON)) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
        }

        // Warforged: full poison immunity
        if (race == DnDRace.WARFORGED && event.getSource().is(DamageTypes.MAGIC)) {
            if (player.hasEffect(MobEffects.POISON)) {
                event.setNewDamage(0.0f);
            }
        }

        // Goliath: 50% freeze/cold resistance
        if (race == DnDRace.GOLIATH && event.getSource().is(DamageTypes.FREEZE)) {
            event.setNewDamage(event.getNewDamage() * 0.5f);
        }

        // Tiefling: 50% fire resistance
        if (race == DnDRace.TIEFLING) {
            if (event.getSource().is(DamageTypes.IN_FIRE)
                    || event.getSource().is(DamageTypes.ON_FIRE)
                    || event.getSource().is(DamageTypes.LAVA)) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
        }

        // Aasimar: 25% lightning and freeze resistance
        // No direct acid damage type in vanilla — flagged for future custom damage type
        if (race == DnDRace.AASIMAR) {
            if (event.getSource().is(DamageTypes.LIGHTNING_BOLT)
                    || event.getSource().is(DamageTypes.FREEZE)) {
                event.setNewDamage(event.getNewDamage() * 0.75f);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (warforgedRegenActive) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;

        if (data.getRace() == DnDRace.WARFORGED) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();

        if (race == DnDRace.WARFORGED) {
            if (event.getEffectInstance().getEffect() == MobEffects.POISON
                    || event.getEffectInstance().getEffect() == MobEffects.HUNGER) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        }
    }
}
