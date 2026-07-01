package com.deadmind.dndmods.race;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.combat.ModDamageTypes;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class RacialTraitHandler {

    private static boolean customRegenActive = false;
    private static boolean negativeEnergyHealActive = false;

    private static final long UNDYING_RESOLVE_COOLDOWN_MS = 3600_000L;

    private static final Set<DnDRace> DARKVISION_RACES = Set.of(
            DnDRace.ELF, DnDRace.DWARF, DnDRace.GNOME, DnDRace.HALF_ORC, DnDRace.HALF_ELF,
            DnDRace.DRAGONBORN, DnDRace.TIEFLING, DnDRace.AASIMAR, DnDRace.WARFORGED
    );

    private static boolean isUndead(DnDRace race) {
        return race == DnDRace.REVENANT || race == DnDRace.DHAMPIR
                || race == DnDRace.SHADAR_KAI || race == DnDRace.VAMPIRE_SPAWN
                || race == DnDRace.SKELETON_WARRIOR;
    }

    private static boolean hasHungerImmunity(DnDRace race) {
        return race == DnDRace.WARFORGED || isUndead(race);
    }

    private static boolean hasCustomSlowRegen(DnDRace race) {
        return race == DnDRace.WARFORGED || race == DnDRace.REVENANT;
    }

    // ---- PlayerTickEvent.Post ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();
        if (race == DnDRace.NONE) return;

        handleDarkvision(player, race);
        handleHungerImmunity(player, race);
        handleSlowRegen(player, data, race);
        handleGoliathPowerfulBuild(player, data, race);
        handleSunlightDamage(player, data, race);
        handleSpiderClimb(player, race);
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

    private static void handleHungerImmunity(ServerPlayer player, DnDRace race) {
        if (!hasHungerImmunity(race)) return;
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
        }
    }

    private static void handleSlowRegen(ServerPlayer player, DnDPlayerData data, DnDRace race) {
        if (!hasCustomSlowRegen(race)) return;

        if (player.getHealth() < player.getMaxHealth() && player.tickCount % 160 == 0) {
            customRegenActive = true;
            try {
                player.heal(1.0f);
            } finally {
                customRegenActive = false;
            }
        }
    }

    private static void handleGoliathPowerfulBuild(ServerPlayer player, DnDPlayerData data, DnDRace race) {
        if (race != DnDRace.GOLIATH) return;
        data.setAchievementFlag("powerful_build", true);
    }

    private static void handleSunlightDamage(ServerPlayer player, DnDPlayerData data, DnDRace race) {
        if (race != DnDRace.VAMPIRE_SPAWN && race != DnDRace.DHAMPIR) return;

        boolean inSunlight = player.level().isDay()
                && player.level().canSeeSky(player.blockPosition())
                && player.level().getBiome(player.blockPosition()).value().getBaseTemperature() > 0.15f;

        boolean isFullVampire = (race == DnDRace.VAMPIRE_SPAWN);
        int refreshInterval = isFullVampire ? 40 : 80;

        if (inSunlight) {
            if (player.tickCount % refreshInterval == 0) {
                int witherAmp = isFullVampire ? 1 : 0;
                int debuffAmp = isFullVampire ? 1 : 0;
                int duration = refreshInterval + 20;

                player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, witherAmp, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, debuffAmp, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, debuffAmp, false, false));
            }
        } else {
            removeSunlightEffect(player, MobEffects.WITHER, isFullVampire ? 1 : 0);
            removeSunlightEffect(player, MobEffects.WEAKNESS, isFullVampire ? 1 : 0);
            removeSunlightEffect(player, MobEffects.MOVEMENT_SLOWDOWN, isFullVampire ? 1 : 0);
        }
    }

    private static void removeSunlightEffect(ServerPlayer player, Holder<MobEffect> effect, int expectedAmplifier) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && active.getAmplifier() == expectedAmplifier && !active.isVisible()) {
            player.removeEffect(effect);
        }
    }

    private static void handleSpiderClimb(ServerPlayer player, DnDRace race) {
        if (race != DnDRace.VAMPIRE_SPAWN) return;
        if (player.onGround()) return;
        if (!player.isShiftKeyDown()) return;

        BlockPos facing = player.blockPosition().relative(player.getDirection());
        if (!player.level().getBlockState(facing).isSolid()) return;

        // Simplified wall cling: while sneaking against a wall, fall speed is near zero
        if (player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.05, 1.0));
            player.fallDistance = 0;
        }
    }

    // ---- CanPlayerSleepEvent ----

    @SubscribeEvent
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();

        if (race == DnDRace.ELF || race == DnDRace.HALF_ELF) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(
                    Component.literal("§6[DnDMods] §eElves do not sleep — you enter a meditative trance instead."),
                    false);
        }
    }

    // ---- LivingDeathEvent (Revenant Undying Resolve) ----

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        if (data.getRace() != DnDRace.REVENANT) return;

        long now = System.currentTimeMillis();
        long lastUsed = data.getUndyingResolveLastUsed();

        if (lastUsed >= 0 && (now - lastUsed) < UNDYING_RESOLVE_COOLDOWN_MS) return;

        event.setCanceled(true);
        player.setHealth(1.0f);
        data.setUndyingResolveLastUsed(now);
        player.displayClientMessage(
                Component.literal("§6[DnDMods] §cUndying Resolve — you refuse to fall."),
                false);
    }

    // ---- LivingDamageEvent.Pre (resistances/vulnerabilities) ----

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();
        if (race == DnDRace.NONE) return;

        // Dhampir: 50% poison damage reduction (processed BEFORE shared undead immunity)
        if (race == DnDRace.DHAMPIR && event.getSource().is(DamageTypes.MAGIC)) {
            if (player.hasEffect(MobEffects.POISON)) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
        }

        // Dwarf: 50% poison resistance
        if (race == DnDRace.DWARF && event.getSource().is(DamageTypes.MAGIC)) {
            if (player.hasEffect(MobEffects.POISON)) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
        }

        // Warforged + undead (except Dhampir): full poison immunity
        if ((race == DnDRace.WARFORGED || (isUndead(race) && race != DnDRace.DHAMPIR))
                && event.getSource().is(DamageTypes.MAGIC)) {
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
        if (race == DnDRace.AASIMAR) {
            if (event.getSource().is(DamageTypes.LIGHTNING_BOLT)
                    || event.getSource().is(DamageTypes.FREEZE)) {
                event.setNewDamage(event.getNewDamage() * 0.75f);
            }
        }

        // Shadar-Kai: 25% necrotic/ability damage resistance
        if (race == DnDRace.SHADAR_KAI) {
            if (event.getSource().is(ModDamageTypes.ABILITY_DAMAGE)) {
                event.setNewDamage(event.getNewDamage() * 0.75f);
            }
        }

        // Skeleton Warrior: 50% arrow resistance
        if (race == DnDRace.SKELETON_WARRIOR && event.getSource().is(DamageTypes.ARROW)) {
            event.setNewDamage(event.getNewDamage() * 0.5f);
        }

        // Skeleton Warrior: 25% blunt vulnerability (axes, shovels)
        if (race == DnDRace.SKELETON_WARRIOR && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            Entity attacker = event.getSource().getEntity();
            if (attacker instanceof Player attackerPlayer) {
                ItemStack weapon = attackerPlayer.getMainHandItem();
                if (weapon.is(ItemTags.AXES) || weapon.is(ItemTags.SHOVELS)) {
                    event.setNewDamage(event.getNewDamage() * 1.25f);
                }
            }
        }
    }

    // ---- LivingDamageEvent.Post (Dhampir Blood Drain) ----

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer attacker)) return;

        DnDPlayerData data = attacker.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        if (data.getRace() != DnDRace.DHAMPIR) return;

        LivingEntity victim = event.getEntity();

        if (victim instanceof ServerPlayer victimPlayer) {
            DnDPlayerData victimData = victimPlayer.getData(ModAttachments.PLAYER_DATA);
            if (victimData != null && isUndead(victimData.getRace())) return;
        } else {
            if (victim.getType().is(EntityTypeTags.UNDEAD)) return;
        }

        attacker.heal(1.0f);
    }

    // ---- LivingHealEvent (suppress vanilla regen for Warforged/Revenant/undead) ----

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (customRegenActive || negativeEnergyHealActive) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();

        if (race == DnDRace.WARFORGED || race == DnDRace.REVENANT) {
            event.setCanceled(true);
        }
    }

    // ---- MobEffectEvent.Applicable (immunities + negative energy healing) ----

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        DnDRace race = data.getRace();

        // Warforged + undead (except Dhampir for poison): poison immunity
        if (race == DnDRace.WARFORGED || (isUndead(race) && race != DnDRace.DHAMPIR)) {
            if (event.getEffectInstance().getEffect() == MobEffects.POISON) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                return;
            }
        }

        // Warforged + all undead: hunger effect immunity
        if (hasHungerImmunity(race)) {
            if (event.getEffectInstance().getEffect() == MobEffects.HUNGER) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                return;
            }
        }

        // All undead: nausea (disease) immunity
        if (isUndead(race)) {
            if (event.getEffectInstance().getEffect() == MobEffects.CONFUSION) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                return;
            }
        }

        // All undead: negative energy healing — external Wither (amplifier 0) heals instead
        // Sunlight system uses amplifier 1 for Vampire Spawn, amplifier 0 with invisible flag for Dhampir
        // We distinguish by checking the visible flag — sunlight effects are applied with visible=false
        if (isUndead(race)) {
            if (event.getEffectInstance().getEffect() == MobEffects.WITHER) {
                if (event.getEffectInstance().isVisible()) {
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                    int amplifier = event.getEffectInstance().getAmplifier();
                    int duration = event.getEffectInstance().getDuration();
                    int healPerTick = amplifier + 1;
                    int totalHeals = duration / 40;
                    float totalHeal = totalHeals * healPerTick;
                    if (totalHeal > 0) {
                        negativeEnergyHealActive = true;
                        try {
                            player.heal(totalHeal);
                        } finally {
                            negativeEnergyHealActive = false;
                        }
                        player.displayClientMessage(
                                Component.literal("§6[DnDMods] §aNecrotic energy courses through you, restoring " + (int) totalHeal + " health."),
                                true);
                    }
                }
            }
        }
    }
}
