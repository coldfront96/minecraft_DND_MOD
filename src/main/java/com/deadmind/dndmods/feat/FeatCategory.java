package com.deadmind.dndmods.feat;

public enum FeatCategory {
    GENERAL("General"),
    COMBAT("Combat"),
    METAMAGIC("Metamagic"),
    DIVINE("Divine"),
    RACIAL("Racial"),
    CLASS_SPECIFIC("Class Specific");

    private final String displayName;

    FeatCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
