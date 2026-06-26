package com.deadmind.dndmods.combat;

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
        }

        public boolean consumeNextAttack() {
            boolean val = this.consumeNextAttack;
            this.consumeNextAttack = false;
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
        }
    }
}
