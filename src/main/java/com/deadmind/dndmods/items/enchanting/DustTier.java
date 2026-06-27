package com.deadmind.dndmods.items.enchanting;

public enum DustTier {
    COPPER(1, 1, "Copper"),
    IRON(2, 2, "Iron"),
    LAPIS(3, 3, "Lapis"),
    GOLD(4, 4, "Gold"),
    EMERALD(5, 5, "Emerald"),
    BLAZE(6, 6, "Blaze Powder"),
    GHAST(7, 7, "Ghast Tear"),
    DIAMOND(8, 8, "Diamond"),
    NETHERITE(9, 9, "Netherite Ingot"),
    NETHER_STAR(10, 10, "Nether Star");

    private final int tier;
    private final int requiredSpellLevel;
    private final String materialName;

    DustTier(int tier, int requiredSpellLevel, String materialName) {
        this.tier = tier;
        this.requiredSpellLevel = requiredSpellLevel;
        this.materialName = materialName;
    }

    public int getTier() {
        return tier;
    }

    public int getRequiredSpellLevel() {
        return requiredSpellLevel;
    }

    public String getMaterialName() {
        return materialName;
    }

    public float getUpcastingBonus() {
        return Math.min((tier - 1) * 0.15f, 0.75f);
    }
}
