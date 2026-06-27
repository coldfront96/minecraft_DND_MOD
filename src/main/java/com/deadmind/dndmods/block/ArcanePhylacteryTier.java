package com.deadmind.dndmods.block;

public enum ArcanePhylacteryTier {
    TIER_1(1, 500, 10, 4),
    TIER_2(2, 1500, 6, 8),
    TIER_3(3, 3500, 4, 12),
    TIER_4(4, 8000, 2, 15);

    private final int tier;
    private final long maxXp;
    private final int tickInterval;
    private final int lightLevel;

    ArcanePhylacteryTier(int tier, long maxXp, int tickInterval, int lightLevel) {
        this.tier = tier;
        this.maxXp = maxXp;
        this.tickInterval = tickInterval;
        this.lightLevel = lightLevel;
    }

    public int getTier() { return tier; }
    public long getMaxXp() { return maxXp; }
    public int getTickInterval() { return tickInterval; }
    public int getLightLevel() { return lightLevel; }

    public static ArcanePhylacteryTier fromTier(int tier) {
        return switch (tier) {
            case 2 -> TIER_2;
            case 3 -> TIER_3;
            case 4 -> TIER_4;
            default -> TIER_1;
        };
    }
}
