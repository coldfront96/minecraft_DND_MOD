package com.khimairacraft.xp;

import net.minecraft.world.entity.LivingEntity;

public class DnDXpValues {

    public static final int BASE_MULTIPLIER = 8;
    public static final int DUNGEON_CHEST_XP = 200;
    public static final int BOSS_KILL_XP = 1000;

    public static int convertVanillaXp(int vanillaXp) {
        return vanillaXp * BASE_MULTIPLIER;
    }

    public static int getDustCraftXp(int tier) {
        return 50 * tier;
    }
}
