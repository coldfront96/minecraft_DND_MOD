package com.khimairacraft.combat;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PerPlayerCombatState {

    private static final Map<UUID, CombatState> STATES = new ConcurrentHashMap<>();

    public static CombatState get(UUID playerUuid) {
        return STATES.computeIfAbsent(playerUuid, k -> new CombatState());
    }

    public static void remove(UUID playerUuid) {
        STATES.remove(playerUuid);
    }

    public static void tickAll() {
        STATES.values().forEach(CombatState::tick);
    }

    public static class CombatState {
        @Nullable
        private String pendingEnhancement;
        private boolean recklessActive;
        private int recklessTicksRemaining;
        @Nullable
        private UUID huntersMarkTarget;
        private int huntersMarkTicksRemaining;
        private boolean consumeNextAttack;
        private int consumeNextAttackTicks;

        // --- Fighter class ability content pass ---
        private int inCombatTicks;
        private int powerStrikeTicks;
        private int battleCryTicks;
        private int defensiveStanceTicks;
        private int unbreakableTicks;
        private int warlordsPresenceTicks;
        private boolean combatStanceActive; // stationary >= 1s (set by FighterPassives tick)

        /** In-combat state used by the Stamina regen rate selector. */
        public boolean isInCombat() { return inCombatTicks > 0; }
        public void markInCombat(int ticks) { this.inCombatTicks = Math.max(this.inCombatTicks, ticks); }

        /** Power Strike: the next melee hit gains bonus damage while pending. */
        public boolean isPowerStrikePending() { return powerStrikeTicks > 0; }
        public void setPowerStrike(int ticks) { this.powerStrikeTicks = ticks; }
        public void clearPowerStrike() { this.powerStrikeTicks = 0; }

        public boolean isBattleCryActive() { return battleCryTicks > 0; }
        public void setBattleCry(int ticks) { this.battleCryTicks = ticks; }

        public boolean isDefensiveStanceActive() { return defensiveStanceTicks > 0; }
        public void setDefensiveStance(int ticks) { this.defensiveStanceTicks = ticks; }

        public boolean isUnbreakableActive() { return unbreakableTicks > 0; }
        public void setUnbreakable(int ticks) { this.unbreakableTicks = ticks; }

        public boolean isWarlordsPresenceActive() { return warlordsPresenceTicks > 0; }
        public void setWarlordsPresence(int ticks) { this.warlordsPresenceTicks = ticks; }

        public boolean isCombatStanceActive() { return combatStanceActive; }
        public void setCombatStanceActive(boolean active) { this.combatStanceActive = active; }

        @Nullable
        public String getPendingEnhancement() { return pendingEnhancement; }

        public void setPendingEnhancement(@Nullable String abilityId) {
            this.pendingEnhancement = abilityId;
        }

        public String consumePendingEnhancement() {
            String enhancement = this.pendingEnhancement;
            this.pendingEnhancement = null;
            return enhancement;
        }

        public boolean isRecklessActive() { return recklessActive; }

        public void setReckless(int ticks) {
            this.recklessActive = true;
            this.recklessTicksRemaining = ticks;
        }

        @Nullable
        public UUID getHuntersMarkTarget() { return huntersMarkTarget; }

        public void setHuntersMark(UUID targetUuid, int ticks) {
            this.huntersMarkTarget = targetUuid;
            this.huntersMarkTicksRemaining = ticks;
        }

        public boolean isConsumeNextAttack() { return consumeNextAttack; }

        public void setConsumeNextAttack(boolean consume) {
            this.consumeNextAttack = consume;
            // Bound how long the flag survives. It exists to swallow the single
            // vanilla attack that immediately follows an ability use; without an
            // expiry an AOE ability (e.g. Ground Slam) that never produces an
            // AttackEntityEvent would leave it set and cancel a later real attack.
            this.consumeNextAttackTicks = consume ? 5 : 0;
        }

        public boolean consumeNextAttack() {
            boolean val = this.consumeNextAttack;
            this.consumeNextAttack = false;
            this.consumeNextAttackTicks = 0;
            return val;
        }

        public void tick() {
            if (recklessTicksRemaining > 0) {
                recklessTicksRemaining--;
                if (recklessTicksRemaining <= 0) {
                    recklessActive = false;
                }
            }
            if (huntersMarkTicksRemaining > 0) {
                huntersMarkTicksRemaining--;
                if (huntersMarkTicksRemaining <= 0) {
                    huntersMarkTarget = null;
                }
            }
            if (consumeNextAttack && consumeNextAttackTicks > 0) {
                consumeNextAttackTicks--;
                if (consumeNextAttackTicks <= 0) {
                    consumeNextAttack = false;
                }
            }
            if (inCombatTicks > 0) inCombatTicks--;
            if (powerStrikeTicks > 0) powerStrikeTicks--;
            if (battleCryTicks > 0) battleCryTicks--;
            if (defensiveStanceTicks > 0) defensiveStanceTicks--;
            if (unbreakableTicks > 0) unbreakableTicks--;
            if (warlordsPresenceTicks > 0) warlordsPresenceTicks--;
        }
    }
}
