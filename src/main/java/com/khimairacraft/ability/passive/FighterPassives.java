package com.khimairacraft.ability.passive;

import com.khimairacraft.DnDMods;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Always-on Fighter passive effects. The numeric passive values (armor AC,
 * weapon attack/damage, damage reduction) are recomputed on level up in
 * {@code LevelUpPayload}; this handler applies the ones that have a hook in the
 * mod's vanilla-damage combat model:
 * <ul>
 *   <li>Armor Training — movement-speed bonus (attribute) while wearing armor;
 *       its AC bonus is a display value (no vanilla AC in combat).</li>
 *   <li>Weapon Training — flat melee <em>damage</em> bonus (the attack/to-hit
 *       bonus has no roll to modify and is stored for the sheet).</li>
 *   <li>Combat Stance — while stationary, +1 melee (the "to hit" reinterpreted
 *       as combat bonus since there is no attack roll).</li>
 *   <li>Armor Mastery — flat physical damage reduction while armored, stacking
 *       additively with Adamantine Body / Warforged Resilience.</li>
 * </ul>
 * Active-ability buffs (Power Strike, Battle Cry, Warlord's Presence, Unbreakable)
 * are read from {@link PerPlayerCombatState} here.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public final class FighterPassives {

    private static final ResourceLocation ARMOR_TRAINING_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "fighter_armor_training");

    private static final double MOVE_EPSILON_SQR = 0.0001; // (0.01)^2

    // Transient stationary-tracking for Combat Stance.
    private static final Map<UUID, double[]> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STATIONARY_TICKS = new ConcurrentHashMap<>();

    private FighterPassives() {}

    // ------------------------------------------------------------------
    // Per-tick passive reconciliation
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DnDPlayerData data = PlayerDataHelper.get(player);
        int fighterLevel = data.getClassLevel(DnDClass.FIGHTER);

        reconcileArmorTrainingSpeed(player, fighterLevel, hasArmor(player));
        reconcileCombatStance(player, fighterLevel);
    }

    /** Armor Training grants +5/10/15/20% movement speed at levels 3/7/11/15 while armored. */
    private static void reconcileArmorTrainingSpeed(ServerPlayer player, int fighterLevel, boolean armored) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;

        double desired = 0.0;
        if (armored) {
            if (fighterLevel >= 15) desired = 0.20;
            else if (fighterLevel >= 11) desired = 0.15;
            else if (fighterLevel >= 7) desired = 0.10;
            else if (fighterLevel >= 3) desired = 0.05;
        }

        AttributeModifier existing = instance.getModifier(ARMOR_TRAINING_SPEED_ID);
        double current = existing != null ? existing.amount() : 0.0;
        if (desired == current) return;
        if (existing != null) instance.removeModifier(ARMOR_TRAINING_SPEED_ID);
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    ARMOR_TRAINING_SPEED_ID, desired, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Combat Stance: stationary for >= 1s grants the Fighter its stance combat bonus. */
    private static void reconcileCombatStance(ServerPlayer player, int fighterLevel) {
        PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(player.getUUID());
        if (fighterLevel <= 0) {
            state.setCombatStanceActive(false);
            return;
        }
        UUID id = player.getUUID();
        double[] last = LAST_POS.get(id);
        double dx = last == null ? 999 : player.getX() - last[0];
        double dy = last == null ? 999 : player.getY() - last[1];
        double dz = last == null ? 999 : player.getZ() - last[2];
        LAST_POS.put(id, new double[]{player.getX(), player.getY(), player.getZ()});

        if (dx * dx + dy * dy + dz * dz >= MOVE_EPSILON_SQR) {
            STATIONARY_TICKS.put(id, 0);
            state.setCombatStanceActive(false);
        } else {
            int t = STATIONARY_TICKS.getOrDefault(id, 0) + 1;
            STATIONARY_TICKS.put(id, t);
            state.setCombatStanceActive(t >= 20);
        }
    }

    // ------------------------------------------------------------------
    // Damage hooks
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        // Attacker side: flat melee bonuses on physical player attacks.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DnDPlayerData data = PlayerDataHelper.get(attacker);
            if (data.getClassLevel(DnDClass.FIGHTER) > 0 && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(attacker.getUUID());
                int bonus = data.getFighterWeaponDamageBonus();
                if (state.isCombatStanceActive()) bonus += 1;
                if (state.isBattleCryActive()) bonus += 2;
                if (state.isWarlordsPresenceActive()) bonus += 3;
                if (state.isPowerStrikePending()) {
                    bonus += data.getAbilityScores().getStrMod();
                    state.clearPowerStrike();
                }
                if (bonus > 0) {
                    event.setNewDamage(event.getNewDamage() + bonus);
                }
            }
        }

        // Victim side: Defensive Stance / Unbreakable multipliers, then Armor
        // Mastery flat reduction.
        if (event.getEntity() instanceof ServerPlayer victim) {
            PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(victim.getUUID());
            if (state.isDefensiveStanceActive()) {
                // "+4 AC" has no vanilla effect; realised as 20% damage reduction.
                event.setNewDamage(event.getNewDamage() * 0.8f);
            }
            if (state.isUnbreakableActive()) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
            DnDPlayerData data = PlayerDataHelper.get(victim);
            if (data.getFighterDamageReduction() > 0 && isPhysicalMelee(event.getSource()) && hasArmor(victim)) {
                event.setNewDamage(Math.max(0.0f, event.getNewDamage() - data.getFighterDamageReduction()));
            }
        }
    }

    /** Unbreakable: immune to knockback while active. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PerPlayerCombatState.get(player.getUUID()).isUnbreakableActive()) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public static boolean hasArmor(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static boolean isPhysicalMelee(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK);
    }

    /** Drop per-player tracking state on logout so the static maps don't grow unbounded. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_POS.remove(id);
        STATIONARY_TICKS.remove(id);
    }
}
