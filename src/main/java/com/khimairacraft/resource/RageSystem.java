package com.khimairacraft.resource;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.network.SyncPlayerDataPayload;
import com.khimairacraft.playerdata.AbilityScores;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RageSystem {

    private RageSystem() {}

    private static final Map<UUID, RageState> ACTIVE_RAGES = new ConcurrentHashMap<>();
    // Fatigue score penalties actually applied ({str, dex} deltas), so they can
    // be reverted exactly when the fatigue timer expires or the player logs out.
    private static final Map<UUID, int[]> FATIGUE_PENALTIES = new ConcurrentHashMap<>();

    private static final ResourceLocation RAGE_MAX_HP_ID =
            ResourceLocation.fromNamespaceAndPath("dndmods", "rage_max_hp");

    public enum RageTier {
        BASE(4),
        GREATER(6),
        MIGHTY(8);

        private final int scoreBonus;

        RageTier(int scoreBonus) {
            this.scoreBonus = scoreBonus;
        }

        public int getScoreBonus() { return scoreBonus; }

        public static RageTier forLevel(int barbarianLevel) {
            if (barbarianLevel >= 20) return MIGHTY;
            if (barbarianLevel >= 11) return GREATER;
            return BASE;
        }
    }

    public static class RageState {
        private long endTime;
        private final RageTier tier;
        // Score deltas actually applied — may be smaller than the tier bonus if
        // AbilityScores clamped at 30. Reverting by these exact amounts is what
        // keeps activate/end symmetric.
        private final int appliedStr;
        private final int appliedCon;
        private boolean usedSecondWind;

        public RageState(long startTime, long durationMillis, RageTier tier,
                         int appliedStr, int appliedCon) {
            this.endTime = startTime + durationMillis;
            this.tier = tier;
            this.appliedStr = appliedStr;
            this.appliedCon = appliedCon;
            this.usedSecondWind = false;
        }

        public RageTier getTier() { return tier; }
        public int getAppliedStr() { return appliedStr; }
        public int getAppliedCon() { return appliedCon; }
        public boolean isUsedSecondWind() { return usedSecondWind; }
        public void setUsedSecondWind(boolean used) { this.usedSecondWind = used; }

        public boolean isExpired(long currentTime) {
            return currentTime >= endTime;
        }

        public void extend(long millis) {
            this.endTime += millis;
        }
    }

    public static boolean isRaging(UUID playerId) {
        RageState state = ACTIVE_RAGES.get(playerId);
        if (state == null) return false;
        return !state.isExpired(System.currentTimeMillis());
    }

    public static RageState getRageState(UUID playerId) {
        return ACTIVE_RAGES.get(playerId);
    }

    public static boolean activate(ServerPlayer player) {
        DnDPlayerData data = PlayerDataHelper.get(player);
        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);
        if (barbLevel <= 0) return false;

        UUID playerId = player.getUUID();

        if (isRaging(playerId)) {
            endRage(player, false);
            return true;
        }

        if (data.isOnCooldown("barbarian_rage_fatigue")) {
            player.displayClientMessage(
                    Component.literal("You are too fatigued to rage.")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        if (data.isOnCooldown("barbarian_rage")) {
            int remaining = data.getCooldownRemaining("barbarian_rage");
            int seconds = remaining / 20;
            player.displayClientMessage(
                    Component.literal("Rage is not ready. " + seconds + " seconds remaining.")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        RageTier tier = RageTier.forLevel(barbLevel);

        // Real STR/CON score bonuses — these cascade into every system that
        // reads ability scores. Record the deltas actually applied, since the
        // score setters clamp at 30.
        AbilityScores scores = data.getAbilityScores();
        int bonus = tier.getScoreBonus();
        int strBefore = scores.getStrength();
        int conBefore = scores.getConstitution();
        scores.setStrength(strBefore + bonus);
        scores.setConstitution(conBefore + bonus);
        int appliedStr = scores.getStrength() - strBefore;
        int appliedCon = scores.getConstitution() - conBefore;

        // Duration from the BOOSTED CON modifier, recalculated every
        // activation: (3 + conMod) x 120 ticks, +600 ticks with Extend Rage.
        int durationTicks = (3 + scores.getConMod()) * 120;
        if (data.getAchievementFlag("extend_rage_unlocked")) {
            durationTicks += 600;
        }
        durationTicks = Math.max(120, durationTicks);
        long durationMillis = durationTicks * 50L;

        ACTIVE_RAGES.put(playerId, new RageState(
                System.currentTimeMillis(), durationMillis, tier, appliedStr, appliedCon));

        applyRageHpBonus(player, barbLevel);

        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));

        player.displayClientMessage(
                Component.literal("You fly into a rage! (" + tier.name() + " - " + (durationMillis / 1000) + "s)")
                        .withStyle(s -> s.withColor(0xFF4444).withBold(true)), true);

        return true;
    }

    public static void endRage(ServerPlayer player, boolean fromDeath) {
        UUID playerId = player.getUUID();
        RageState state = ACTIVE_RAGES.remove(playerId);
        if (state == null) return;

        DnDPlayerData data = PlayerDataHelper.get(player);

        revertRageScores(data, state);
        removeRageHpBonus(player);

        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);

        if (!fromDeath) {
            player.displayClientMessage(
                    Component.literal("Rage ended.")
                            .withStyle(s -> s.withColor(0xAAAAAA)), true);
        }

        if (!fromDeath && barbLevel < 17) {
            data.startCooldown("barbarian_rage_fatigue", 30 * 20); // 30 seconds
            applyFatiguePenalty(playerId, data);
        }

        int cooldownTicks = (int) (getRageCooldownBaseTicks(barbLevel) * data.getRageCooldownMultiplier());
        data.startCooldown("barbarian_rage", cooldownTicks);

        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
    }

    /** Bloodlust Surge: extend an active rage (millis) on a killing blow. */
    public static void extendRage(ServerPlayer player, long millis) {
        RageState state = ACTIVE_RAGES.get(player.getUUID());
        if (state != null && !state.isExpired(System.currentTimeMillis())) {
            state.extend(millis);
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();

        RageState state = ACTIVE_RAGES.get(playerId);
        if (state != null && state.isExpired(System.currentTimeMillis())) {
            endRage(player, false);
        }

        // Fatigue expiry: the cooldown map already handles the timer; revert
        // the -2 STR/-2 DEX penalty at the moment the timer runs out.
        if (FATIGUE_PENALTIES.containsKey(playerId)) {
            DnDPlayerData data = PlayerDataHelper.get(player);
            if (!data.isOnCooldown("barbarian_rage_fatigue")) {
                revertFatiguePenalty(playerId, data);
                PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
            }
        }
    }

    /**
     * Logout cleanup: rage and fatigue score changes live in the persisted
     * AbilityScores, so they MUST be reverted before the player's data is
     * saved — otherwise a mid-rage logout would bake the bonus in permanently.
     * Stamps the rage cooldown (persisted) but skips fatigue and messages.
     */
    public static void handleLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        DnDPlayerData data = PlayerDataHelper.get(player);

        RageState state = ACTIVE_RAGES.remove(playerId);
        if (state != null) {
            revertRageScores(data, state);
            removeRageHpBonus(player);
            int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);
            int cooldownTicks = (int) (getRageCooldownBaseTicks(barbLevel) * data.getRageCooldownMultiplier());
            data.startCooldown("barbarian_rage", cooldownTicks);
        }

        revertFatiguePenalty(playerId, data);
    }

    public static boolean isFatigued(ServerPlayer player) {
        DnDPlayerData data = PlayerDataHelper.get(player);
        return data.isOnCooldown("barbarian_rage_fatigue");
    }

    private static void revertRageScores(DnDPlayerData data, RageState state) {
        AbilityScores scores = data.getAbilityScores();
        scores.setStrength(scores.getStrength() - state.getAppliedStr());
        scores.setConstitution(scores.getConstitution() - state.getAppliedCon());
        // CON removal lowers CON-scaled max HP; re-set current HP through the
        // clamping setter so it can't exceed the new max.
        data.setCurrentHp(data.getCurrentHp());
    }

    private static void applyFatiguePenalty(UUID playerId, DnDPlayerData data) {
        if (FATIGUE_PENALTIES.containsKey(playerId)) return;
        AbilityScores scores = data.getAbilityScores();
        int strBefore = scores.getStrength();
        int dexBefore = scores.getDexterity();
        scores.setStrength(strBefore - 2);
        scores.setDexterity(dexBefore - 2);
        // Deltas actually applied (negative), clamped at score 1 by the setter.
        FATIGUE_PENALTIES.put(playerId, new int[]{
                scores.getStrength() - strBefore,
                scores.getDexterity() - dexBefore});
    }

    private static void revertFatiguePenalty(UUID playerId, DnDPlayerData data) {
        int[] applied = FATIGUE_PENALTIES.remove(playerId);
        if (applied == null) return;
        AbilityScores scores = data.getAbilityScores();
        scores.setStrength(scores.getStrength() - applied[0]);
        scores.setDexterity(scores.getDexterity() - applied[1]);
    }

    // Rage HP bonus: +2 x Barbarian level as a transient MAX_HEALTH modifier.
    // Heal the delta on apply; on removal clamp current health down to the new
    // max so it never exceeds it (vanilla would clamp lazily on a later tick).
    private static void applyRageHpBonus(ServerPlayer player, int barbLevel) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        double bonus = 2.0 * barbLevel;
        maxHealth.removeModifier(RAGE_MAX_HP_ID);
        maxHealth.addTransientModifier(new AttributeModifier(
                RAGE_MAX_HP_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        player.heal((float) bonus);
    }

    private static void removeRageHpBonus(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        maxHealth.removeModifier(RAGE_MAX_HP_ID);
        float newMax = player.getMaxHealth();
        if (player.getHealth() > newMax) {
            player.setHealth(Math.max(1.0f, newMax));
        }
    }

    // Spec cooldown table (base, before the Extra Rage multiplier):
    // L1-3 5:00, L4-7 4:30, L8-11 4:00, L12-15 3:00, L16-19 2:00, L20 1:00.
    private static int getRageCooldownBaseTicks(int barbarianLevel) {
        if (barbarianLevel >= 20) return 60 * 20;
        if (barbarianLevel >= 16) return 120 * 20;
        if (barbarianLevel >= 12) return 180 * 20;
        if (barbarianLevel >= 8) return 240 * 20;
        if (barbarianLevel >= 4) return 270 * 20;
        return 300 * 20;
    }
}
