package com.deadmind.dndmods.resource;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RageSystem {

    private RageSystem() {}

    private static final Map<UUID, RageState> ACTIVE_RAGES = new ConcurrentHashMap<>();

    public enum RageTier {
        BASE(1, 10, 1.3f, 1),
        GREATER(11, 19, 1.5f, 2),
        MIGHTY(20, 20, 1.75f, 3);

        private final int minLevel;
        private final int maxLevel;
        private final float damageMultiplier;
        private final int damageReduction;

        RageTier(int minLevel, int maxLevel, float damageMultiplier, int damageReduction) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
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
        private final long startTime;
        private final long endTime;
        private final RageTier tier;
        private boolean usedSecondWind;

        public RageState(long startTime, long durationMillis, RageTier tier) {
            this.startTime = startTime;
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

        public long getRemainingMillis(long currentTime) {
            return Math.max(0, endTime - currentTime);
        }
    }

    public static boolean isRaging(UUID playerId) {
        RageState state = ACTIVE_RAGES.get(playerId);
        if (state == null) return false;
        if (state.isExpired(System.currentTimeMillis())) {
            ACTIVE_RAGES.remove(playerId);
            return false;
        }
        return true;
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

        if (data.isOnCooldown("barbarian_rage")) {
            int remaining = data.getCooldownRemaining("barbarian_rage");
            int seconds = remaining / 20;
            player.displayClientMessage(
                    Component.literal("Rage is on cooldown! " + seconds + "s remaining.")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        RageTier tier = RageTier.forLevel(barbLevel);
        long durationMillis = getRageDuration(barbLevel);

        ACTIVE_RAGES.put(playerId, new RageState(System.currentTimeMillis(), durationMillis, tier));

        player.displayClientMessage(
                Component.literal("RAGE! (" + tier.name() + " - " + (durationMillis / 1000) + "s)")
                        .withStyle(s -> s.withColor(0xFF4444).withBold(true)), true);

        return true;
    }

    public static void endRage(ServerPlayer player, boolean fromDeath) {
        UUID playerId = player.getUUID();
        RageState state = ACTIVE_RAGES.remove(playerId);
        if (state == null) return;

        DnDPlayerData data = PlayerDataHelper.get(player);
        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);

        player.displayClientMessage(
                Component.literal("Rage ended.")
                        .withStyle(s -> s.withColor(0xAAAAAA)), true);

        if (!fromDeath && barbLevel < 17) {
            int fatigueTicks = 30 * 20; // 30 seconds
            data.startCooldown("barbarian_rage_fatigue", fatigueTicks);
        }

        int cooldownTicks = getRageCooldownTicks(barbLevel, data);
        data.startCooldown("barbarian_rage", cooldownTicks);

        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RageState state = ACTIVE_RAGES.get(playerId);
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

    private static long getRageDuration(int barbarianLevel) {
        if (barbarianLevel >= 20) return 30_000L;
        if (barbarianLevel >= 15) return 25_000L;
        if (barbarianLevel >= 11) return 20_000L;
        if (barbarianLevel >= 7) return 15_000L;
        return 10_000L;
    }

    private static int getRageCooldownTicks(int barbarianLevel, DnDPlayerData data) {
        int baseSeconds;
        if (barbarianLevel >= 17) baseSeconds = 60;
        else if (barbarianLevel >= 11) baseSeconds = 90;
        else if (barbarianLevel >= 5) baseSeconds = 120;
        else baseSeconds = 180;

        int extraRageCount = countFeatOccurrences(data, "extra_rage");
        float multiplier = 1.0f;
        for (int i = 0; i < extraRageCount; i++) {
            multiplier *= 0.8f;
        }

        return (int) (baseSeconds * 20 * multiplier);
    }

    private static int countFeatOccurrences(DnDPlayerData data, String featId) {
        int count = 0;
        for (String feat : data.getGrantedFeats()) {
            if (feat.equals(featId)) count++;
        }
        return count;
    }
}
