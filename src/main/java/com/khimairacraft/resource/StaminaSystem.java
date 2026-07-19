package com.khimairacraft.resource;

import com.khimairacraft.DnDMods;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.network.SyncPlayerDataPayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fighter-specific Stamina resource: a dedicated pool (separate from the generic
 * class resource) that powers Fighter active abilities. Max scales with Fighter
 * level and CON; regen scales with Fighter level and whether the player is in
 * combat. This is the first class resource system and sets the pattern for the
 * per-class resources that follow — no Mana/Faith/Rage is built here.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public final class StaminaSystem {

    /** In-combat window (ticks) after dealing or taking damage. */
    private static final int COMBAT_WINDOW_TICKS = 100; // 5 seconds

    /** Fractional-regen carry, per player (transient, session-only). */
    private static final Map<UUID, Float> REGEN_ACCUMULATOR = new ConcurrentHashMap<>();

    private StaminaSystem() {}

    // ------------------------------------------------------------------
    // Max stamina + recalculation
    // ------------------------------------------------------------------

    /** maxStamina = 10 + fighterLevel*5 + CONmod*2, or 0 if the player has no Fighter levels. */
    public static int computeMaxStamina(DnDPlayerData data) {
        int fighterLevel = data.getClassLevel(DnDClass.FIGHTER);
        if (fighterLevel <= 0) return 0;
        return 10 + fighterLevel * 5 + data.getAbilityScores().getConMod() * 2;
    }

    /** Recomputes max stamina (call on Fighter level up and CON change). */
    public static void recalcMaxStamina(DnDPlayerData data) {
        data.setMaxStamina(computeMaxStamina(data));
    }

    // ------------------------------------------------------------------
    // Regen
    // ------------------------------------------------------------------

    /** Out-of-combat regen per second at the given Fighter level (highest breakpoint <= level). */
    private static float outOfCombatRegen(int level) {
        if (level >= 20) return 12.0f;
        if (level >= 18) return 11.0f;
        if (level >= 15) return 10.0f;
        if (level >= 12) return 9.0f;
        if (level >= 9) return 8.0f;
        if (level >= 6) return 7.0f;
        if (level >= 3) return 6.0f;
        return 5.0f;
    }

    /** In-combat regen per second at the given Fighter level. */
    private static float inCombatRegen(int level) {
        if (level >= 20) return 5.5f;
        if (level >= 18) return 5.0f;
        if (level >= 15) return 4.5f;
        if (level >= 12) return 4.0f;
        if (level >= 9) return 3.5f;
        if (level >= 6) return 3.0f;
        if (level >= 3) return 2.5f;
        return 2.0f;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return; // once per second

        DnDPlayerData data = PlayerDataHelper.get(player);
        int fighterLevel = data.getClassLevel(DnDClass.FIGHTER);
        if (fighterLevel <= 0) return;

        // Keep max in sync (covers CON changes that bypass level-up).
        int expectedMax = computeMaxStamina(data);
        if (data.getMaxStamina() != expectedMax) {
            data.setMaxStamina(expectedMax);
        }
        if (data.getCurrentStamina() >= data.getMaxStamina()) return;

        boolean inCombat = PerPlayerCombatState.get(player.getUUID()).isInCombat();
        float rate = inCombat ? inCombatRegen(fighterLevel) : outOfCombatRegen(fighterLevel);

        float acc = REGEN_ACCUMULATOR.getOrDefault(player.getUUID(), 0.0f) + rate;
        int whole = (int) acc;
        if (whole > 0) {
            data.setCurrentStamina(data.getCurrentStamina() + whole);
            REGEN_ACCUMULATOR.put(player.getUUID(), acc - whole);
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        } else {
            REGEN_ACCUMULATOR.put(player.getUUID(), acc);
        }
    }

    // ------------------------------------------------------------------
    // Combat-state marking
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            PerPlayerCombatState.get(victim.getUUID()).markInCombat(COMBAT_WINDOW_TICKS);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PerPlayerCombatState.get(attacker.getUUID()).markInCombat(COMBAT_WINDOW_TICKS);
        }
    }

    // ------------------------------------------------------------------
    // Sleep restore
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.players()) {
            if (!player.isSleeping()) continue;
            DnDPlayerData data = PlayerDataHelper.get(player);
            if (data.getClassLevel(DnDClass.FIGHTER) <= 0) continue;
            data.setCurrentStamina(data.getMaxStamina());
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        }
    }
}
