package com.deadmind.dndmods.feat;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.ModDamageTypes;
import com.deadmind.dndmods.combat.SaveType;
import com.deadmind.dndmods.combat.SavingThrowSystem;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import com.deadmind.dndmods.race.DnDRace;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side event handler for general feats whose effects need an active
 * tick loop or event hook beyond simple stored flags. Feats are identified by
 * the unlock flags set in {@link DnDPlayerData} by the feat registrations in
 * {@code CoreFeats}.
 *
 * <p>Attribute-based feats (Run, Athletic) are reconciled every tick: the
 * modifier is added when the feat flag is set and removed when it is cleared,
 * so granting or revoking a feat takes effect on the next tick and the
 * modifiers survive respawns and relogs without ever stacking. Transient
 * modifiers are used so nothing is written to the entity's persistent NBT.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class FeatEffectHandler {

    private static final ResourceLocation RUN_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "run_feat_speed");
    private static final ResourceLocation ATHLETIC_JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "athletic_feat_jump");
    private static final ResourceLocation TOUGHNESS_HP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "toughness_feat_hp");
    private static final ResourceLocation DWARVEN_TOUGHNESS_HP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "dwarven_toughness_feat_hp");
    private static final ResourceLocation BATTLE_HARDENED_HP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "battle_hardened_feat");
    private static final ResourceLocation ARCANE_TOUGHNESS_HP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "arcane_toughness_feat");
    private static final ResourceLocation FAST_MOVEMENT_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "fast_movement_feat");

    /**
     * Fast Movement (Complete Adventurer): flat +10-feet movement boost. Applied
     * as a flat ADD_VALUE to MOVEMENT_SPEED (~+0.05 on the 0.1 base, roughly
     * +0.1 blocks/tick). Stacks with the Run feat's multiplied bonus.
     */
    private static final double FAST_MOVEMENT_SPEED_BONUS = 0.05;

    /** Evasion (Complete Adventurer): baseline area-effect Reflex DC. */
    private static final int EVASION_REFLEX_DC = 15;
    private static final ResourceLocation NATURAL_ATHLETE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "natural_athlete_speed");
    private static final ResourceLocation NATURAL_ATHLETE_JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "natural_athlete_jump");

    /** Toughness: +3 max HP per stack, applied to the vanilla health bar. */
    private static final double TOUGHNESS_HP_PER_STACK = 3.0;

    /** Natural Athlete (Goliath): +15% movement speed, +20% jump height. */
    private static final double NATURAL_ATHLETE_SPEED_BONUS = 0.15;
    private static final double NATURAL_ATHLETE_JUMP_BONUS = 0.20;

    /** Warforged Resilience: flat damage reduction per hit. */
    private static final float WARFORGED_DAMAGE_REDUCTION = 1.0f;
    /** Dwarven Resilience: proc when current HP drops below this fraction of max. */
    private static final float DWARVEN_RESILIENCE_HP_FRACTION = 0.25f;

    /** Run: move at 5x speed instead of 4x — modelled as +25% movement speed. */
    private static final double RUN_SPEED_BONUS = 0.25;
    /** Athletic: +10% jump height (flavor translation of the +2 Jump bonus). */
    private static final double ATHLETIC_JUMP_BONUS = 0.10;

    /** Acrobatic: negate the first 4 blocks of every fall. */
    private static final float ACROBATIC_FALL_NEGATE = 4.0f;

    /**
     * Self-Sufficient: ~25% faster natural regeneration. Vanilla heals about
     * 1 HP per 80 ticks while the hunger bar is high, so granting one extra HP
     * every 320 ticks under the same conditions approximates a 25% boost.
     */
    private static final int SELF_SUFFICIENT_INTERVAL = 320;
    private static final int REGEN_FOOD_THRESHOLD = 18;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;

        reconcileAttribute(player, Attributes.MOVEMENT_SPEED, RUN_SPEED_ID,
                RUN_SPEED_BONUS, data.getAchievementFlag("run_unlocked"));

        // JUMP_STRENGTH is a generic attribute in 1.21 but is not guaranteed to
        // be present on the player's attribute map; reconcileAttribute skips
        // gracefully (null instance) if it is absent, leaving the feat as a
        // flag-only effect to be revisited if the attribute becomes available.
        reconcileAttribute(player, Attributes.JUMP_STRENGTH, ATHLETIC_JUMP_ID,
                ATHLETIC_JUMP_BONUS, data.getAchievementFlag("athletic_unlocked"));

        reconcileToughness(player, data);

        // Natural Athlete (Goliath): movement speed and jump height boosts.
        boolean naturalAthlete = data.getAchievementFlag("natural_athlete_unlocked");
        reconcileAttribute(player, Attributes.MOVEMENT_SPEED, NATURAL_ATHLETE_SPEED_ID,
                NATURAL_ATHLETE_SPEED_BONUS, naturalAthlete);
        reconcileAttribute(player, Attributes.JUMP_STRENGTH, NATURAL_ATHLETE_JUMP_ID,
                NATURAL_ATHLETE_JUMP_BONUS, naturalAthlete);

        reconcileDwarvenToughness(player, data);

        reconcileBattleHardened(player, data);

        reconcileArcaneToughness(player, data);

        // Fast Movement (Complete Adventurer): flat speed bonus, reconciled each
        // tick like the Run feat but using a flat ADD_VALUE operation.
        reconcileAttribute(player, Attributes.MOVEMENT_SPEED, FAST_MOVEMENT_SPEED_ID,
                FAST_MOVEMENT_SPEED_BONUS, data.getAchievementFlag("fast_movement_unlocked"),
                AttributeModifier.Operation.ADD_VALUE);

        handleSelfSufficient(player, data);
    }

    /**
     * Arcane Toughness (Complete Arcane): +1 max HP per Wizard level, applied to
     * the vanilla health bar from the level-scaled arcaneToughnessHp value (kept
     * current by the level-up handler). Heals the player by the delta when it
     * grows. Mirrors {@link #reconcileDwarvenToughness} / {@link #reconcileBattleHardened}.
     */
    private static void reconcileArcaneToughness(ServerPlayer player, DnDPlayerData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        double desired = data.getArcaneToughnessHp();
        AttributeModifier existing = instance.getModifier(ARCANE_TOUGHNESS_HP_ID);
        double current = existing != null ? existing.amount() : 0.0;

        if (desired == current) return;

        if (existing != null) {
            instance.removeModifier(ARCANE_TOUGHNESS_HP_ID);
        }
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    ARCANE_TOUGHNESS_HP_ID, desired, AttributeModifier.Operation.ADD_VALUE));
        }
        if (desired > current) {
            player.heal((float) (desired - current));
        }
    }

    /**
     * Battle Hardened (Complete Warrior): +2 max HP per hit die, applied to the
     * vanilla health bar from the level-scaled battleHardenedHp value (kept
     * current by the level-up handler). Heals the player by the delta when it
     * grows. Mirrors {@link #reconcileDwarvenToughness}.
     */
    private static void reconcileBattleHardened(ServerPlayer player, DnDPlayerData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        double desired = data.getBattleHardenedHp();
        AttributeModifier existing = instance.getModifier(BATTLE_HARDENED_HP_ID);
        double current = existing != null ? existing.amount() : 0.0;

        if (desired == current) return;

        if (existing != null) {
            instance.removeModifier(BATTLE_HARDENED_HP_ID);
        }
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    BATTLE_HARDENED_HP_ID, desired, AttributeModifier.Operation.ADD_VALUE));
        }
        if (desired > current) {
            player.heal((float) (desired - current));
        }
    }

    /**
     * Dwarven Toughness: +1 max HP per character level, applied to the vanilla
     * health bar from the level-scaled dwarvenToughnessHp value (kept current
     * by the level-up handler). Heals the player by the delta when it grows.
     */
    private static void reconcileDwarvenToughness(ServerPlayer player, DnDPlayerData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        double desired = data.getDwarvenToughnessHp();
        AttributeModifier existing = instance.getModifier(DWARVEN_TOUGHNESS_HP_ID);
        double current = existing != null ? existing.amount() : 0.0;

        if (desired == current) return;

        if (existing != null) {
            instance.removeModifier(DWARVEN_TOUGHNESS_HP_ID);
        }
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    DWARVEN_TOUGHNESS_HP_ID, desired, AttributeModifier.Operation.ADD_VALUE));
        }
        if (desired > current) {
            player.heal((float) (desired - current));
        }
    }

    /**
     * Applies +3 max HP per Toughness stack to the player's vanilla MAX_HEALTH
     * attribute. A single ADD_VALUE modifier carries the full stacked amount
     * (count x 3) — functionally identical to many uniquely keyed +3 modifiers
     * but simpler to reconcile and remove. When the bonus grows the player is
     * healed by the delta so the new hit points are immediately usable.
     */
    private static void reconcileToughness(ServerPlayer player, DnDPlayerData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        double desired = data.getToughnessFeatCount() * TOUGHNESS_HP_PER_STACK;
        AttributeModifier existing = instance.getModifier(TOUGHNESS_HP_ID);
        double current = existing != null ? existing.amount() : 0.0;

        if (desired == current) return;

        if (existing != null) {
            instance.removeModifier(TOUGHNESS_HP_ID);
        }
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    TOUGHNESS_HP_ID, desired, AttributeModifier.Operation.ADD_VALUE));
        }
        if (desired > current) {
            player.heal((float) (desired - current));
        }
    }

    private static void reconcileAttribute(ServerPlayer player, Holder<Attribute> attribute,
                                           ResourceLocation id, double bonus, boolean shouldHave) {
        reconcileAttribute(player, attribute, id, bonus, shouldHave,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void reconcileAttribute(ServerPlayer player, Holder<Attribute> attribute,
                                           ResourceLocation id, double bonus, boolean shouldHave,
                                           AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        boolean has = instance.getModifier(id) != null;
        if (shouldHave && !has) {
            instance.addTransientModifier(new AttributeModifier(id, bonus, operation));
        } else if (!shouldHave && has) {
            instance.removeModifier(id);
        }
    }

    private static void handleSelfSufficient(ServerPlayer player, DnDPlayerData data) {
        if (!data.getAchievementFlag("self_sufficient_unlocked")) return;
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (player.getFoodData().getFoodLevel() < REGEN_FOOD_THRESHOLD) return;

        if (player.tickCount % SELF_SUFFICIENT_INTERVAL == 0) {
            player.heal(1.0f);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        if (!data.getAchievementFlag("acrobatic_unlocked")) return;

        float reduced = event.getDistance() - ACROBATIC_FALL_NEGATE;
        if (reduced <= 0.0f) {
            event.setCanceled(true);
        } else {
            event.setDistance(reduced);
        }
    }

    /**
     * Pre-damage hooks: Blade of Force (attacker-side flat force damage) and
     * Warforged Resilience (victim-side flat damage reduction).
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        // Attacker-side flat-damage feats.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DnDPlayerData attackerData = attacker.getData(ModAttachments.PLAYER_DATA);
            if (attackerData != null) {
                // Blade of Force (Complete Warrior): weapon strikes deal bonus
                // force damage equal to the attacker's INT modifier.
                if (attackerData.getAchievementFlag("blade_of_force_unlocked")
                        && attackerData.getBladeOfForceBonus() > 0) {
                    event.setNewDamage(event.getNewDamage() + attackerData.getBladeOfForceBonus());
                }

                // Holy Warrior (Complete Divine): a Cleric adds their WIS
                // modifier as flat bonus damage to all melee attacks. This is the
                // only active damage feat in the Complete Divine pass.
                if (attackerData.getAchievementFlag("holy_warrior_unlocked")
                        && attackerData.getHolyWarriorDamageBonus() > 0
                        && isCleric(attackerData)) {
                    event.setNewDamage(event.getNewDamage() + attackerData.getHolyWarriorDamageBonus());
                }
            }
        }

        // Victim-side damage feats.
        if (event.getEntity() instanceof ServerPlayer player) {
            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
            if (data == null) return;
            handleWarforgedResilience(data, event);
            handleEvasion(player, data, event);
        }
    }

    /**
     * Warforged Resilience: the chassis shrugs off minor hits, reducing all
     * incoming damage by a flat amount (minimum 0).
     */
    private static void handleWarforgedResilience(DnDPlayerData data, LivingDamageEvent.Pre event) {
        if (data.getRace() != DnDRace.WARFORGED) return;
        if (!data.getAchievementFlag("warforged_resilience_unlocked")) return;

        float reduced = Math.max(0.0f, event.getNewDamage() - WARFORGED_DAMAGE_REDUCTION);
        event.setNewDamage(reduced);
    }

    /**
     * Evasion (Complete Adventurer): against area-effect damage (explosions,
     * fireballs) a successful Reflex save negates the damage entirely. With
     * Improved Evasion a failed save still halves it.
     */
    private static void handleEvasion(ServerPlayer player, DnDPlayerData data, LivingDamageEvent.Pre event) {
        if (!data.isEvasionUnlocked()) return;
        if (!isAreaDamage(event.getSource())) return;

        boolean saved = SavingThrowSystem.rollSave(player, SaveType.REFLEX, EVASION_REFLEX_DC, null);
        if (saved) {
            event.setNewDamage(0.0f);
        } else if (data.getAchievementFlag("improved_evasion_unlocked")) {
            event.setNewDamage(event.getNewDamage() * 0.5f);
        }
    }

    /** True for explosion / fireball style area damage sources that Evasion can dodge. */
    private static boolean isAreaDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.FIREBALL)
                || source.is(DamageTypes.UNATTRIBUTED_FIREBALL);
    }

    /**
     * Post-damage hooks: Distracting Attack (attacker-side Glowing) and
     * Dwarven Resilience (victim-side daily resistance proc).
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();

        // Dwarven Resilience — victim side.
        if (victim instanceof ServerPlayer victimPlayer) {
            DnDPlayerData victimData = victimPlayer.getData(ModAttachments.PLAYER_DATA);
            if (victimData != null) {
                handleDwarvenResilience(victimPlayer, victimData);
            }
        }

        // Attacker-side post-hit feats.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DnDPlayerData data = attacker.getData(ModAttachments.PLAYER_DATA);
            if (data != null) {
                // Distracting Attack (Ranger): tag the target with Glowing.
                if (data.getAchievementFlag("distracting_attack_unlocked") && isRanger(data)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
                }

                // Weakening Strike (Complete Scoundrel): a Rogue's sneak attack —
                // represented in this mod by ability (Backstab) damage — saps the
                // victim's strength. The only active effect in this pass.
                if (data.getAchievementFlag("weakening_strike_unlocked")
                        && isRogue(data)
                        && event.getSource().is(ModDamageTypes.ABILITY_DAMAGE)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true));
                }
            }
        }
    }

    private static void handleDwarvenResilience(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.DWARF) return;
        if (!data.getAchievementFlag("dwarven_resilience_unlocked")) return;
        if (data.getAchievementFlag("dwarven_resilience_used")) return;
        if (player.getHealth() <= 0.0f) return;
        if (player.getHealth() >= player.getMaxHealth() * DWARVEN_RESILIENCE_HP_FRACTION) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, true));
        data.setAchievementFlag("dwarven_resilience_used", true);
    }

    /**
     * Half-Orc Ferocity: once per day, the first blow that would kill the
     * half-orc instead leaves them at 1 HP with Wither for a brief last stand.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        if (data.getRace() != DnDRace.HALF_ORC) return;
        if (!data.getAchievementFlag("half_orc_ferocity_unlocked")) return;
        if (!data.isHalfOrcFerocityAvailable()) return;

        event.setCanceled(true);
        player.setHealth(1.0f);
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true));
        data.setHalfOrcFerocityAvailable(false);
        player.sendSystemMessage(Component.literal("§6[DnDMods] §cFerocity — you fight on through death!"));
    }

    private static boolean isRanger(DnDPlayerData data) {
        if (data.getPrimary().getDnDClass() == DnDClass.RANGER) return true;
        return data.getSecondary() != null && data.getSecondary().getDnDClass() == DnDClass.RANGER;
    }

    private static boolean isCleric(DnDPlayerData data) {
        if (data.getPrimary().getDnDClass() == DnDClass.CLERIC) return true;
        return data.getSecondary() != null && data.getSecondary().getDnDClass() == DnDClass.CLERIC;
    }

    private static boolean isRogue(DnDPlayerData data) {
        if (data.getPrimary().getDnDClass() == DnDClass.ROGUE) return true;
        return data.getSecondary() != null && data.getSecondary().getDnDClass() == DnDClass.ROGUE;
    }
}
