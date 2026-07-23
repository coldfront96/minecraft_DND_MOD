package com.khimairacraft.classes;

public enum ResourceType {
    NONE("None", 0x888888),
    STAMINA("Stamina", 0xFFD700),
    MANA("Mana", 0x4488FF),
    FAITH("Faith", 0xFFFFCC),
    FOCUS("Focus", 0x44FF44),
    RAGE("Rage", 0xFF4444);

    private final String displayName;
    private final int color;

    ResourceType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
