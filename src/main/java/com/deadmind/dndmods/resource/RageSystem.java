package com.deadmind.dndmods.resource;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
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

    private static final ResourceLocation RAGE_MAX_HP_ID =
            ResourceLocation.fromNamespaceAndPath("dndmods", "rage_max_hp");

    public enum RageTier {
        BASE(1.3f, 1),
        GREATER(1.5f, 2),
        MIGHTY(1.75f, 3);

        private final float damageMultiplier;
        private final int damageReduction;

        RageTier(float damageMultiplier, int damageReduction) {
            this.damageMultiplier = damageMultiplier;
            this.damageReduction = damageReduction;
        }

        public float getDamageMultiplier() { return damageMultiplier; }
        public int getDamageReduction() { return damageReduction; }

        public static RageTier forLevel(int barbarianLevel) {
            if (barbarianLevel >= 20) return MIGHTY;
            if (barbarianLevel >= 11) return GREATER;
            return BASE;
        }
    }

    public static class RageState {
        private long endTime;
        private final RageTier tier;
        private boolean usedSecondWind;

        public RageState(long startTime, long durationMillis, RageTier tier) {
            this.endTime = startTime + durationMillis;
            this.tier = tier;
            this.usedSecondWind = false;
        }

        public RageTier getTier() { return tier; }
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
        long durationMillis = getRageDuration(barbLevel);

        ACTIVE_RAGES.put(playerId, new RageState(System.currentTimeMillis(), durationMillis, tier));

        applyRageHpBonus(player, barbLevel);

        player.displayClientMessage(
                Component.literal("You fly into a rage! (" + tier.name() + " - " + (durationMillis / 1000) + "s)")
                        .withStyle(s -> s.withColor(0xFF4444).withBold(true)), true);

        return true;
    }

    public static void endRage(ServerPlayer player, boolean fromDeath) {
        UUID playerId = player.getUUID();
        RageState state = ACTIVE_RAGES.remove(playerId);
        if (state == null) return;

        removeRageHpBonus(player);

        DnDPlayerData data = PlayerDataHelper.get(player);
        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);

        if (!fromDeath) {
            player.displayClientMessage(
                    Component.literal("Rage ended.")
                            .withStyle(s -> s.withColor(0xAAAAAA)), true);
        }

        if (!fromDeath && barbLevel < 17) {
            int fatigueTicks = 30 * 20; // 30 seconds
            data.startCooldown("barbarian_rage_fatigue", fatigueTicks);
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
        RageState state = ACTIVE_RAGES.get(player.getUUID());
        if (state == null) return;

        if (state.isExpired(System.currentTimeMillis())) {
            endRage(player, false);
        }
    }

    public static void remove(UUID playerId) {
        ACTIVE_RAGES.remove(playerId);
    }

    public static boolean isFatigued(ServerPlayer player) {
        DnDPlayerData data = PlayerDataHelper.get(player);
        return data.isOnCooldown("barbarian_rage_fatigue");
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

    private static long getRageDuration(int barbarianLevel) {
        if (barbarianLevel >= 20) return 30_000L;
        if (barbarianLevel >= 15) return 25_000L;
        if (barbarianLevel >= 11) return 20_000L;
        if (barbarianLevel >= 7) return 15_000L;
        return 10_000L;
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
