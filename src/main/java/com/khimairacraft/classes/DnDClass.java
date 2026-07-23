package com.khimairacraft.classes;

public enum DnDClass {
    NONE("None", ResourceType.NONE, 0),
    // Fighter has NO generic class resource: its Stamina lives in the dedicated
    // currentStamina/maxStamina pool on DnDPlayerData (level-scaled max + regen
    // breakpoints), not in the generic ClassEntry resource. See DnDHudOverlay /
    // CharacterSheetScreen for the Fighter-specific rendering of that pool.
    FIGHTER("Fighter", ResourceType.NONE, 0),
    ROGUE("Rogue", ResourceType.FOCUS, 80),
    WIZARD("Wizard", ResourceType.MANA, 100),
    CLERIC("Cleric", ResourceType.FAITH, 100),
    RANGER("Ranger", ResourceType.FOCUS, 80),
    BARBARIAN("Barbarian", ResourceType.RAGE, 50);

    private final String displayName;
    private final ResourceType resourceType;
    private final int baseMaxResource;

    DnDClass(String displayName, ResourceType resourceType, int baseMaxResource) {
        this.displayName = displayName;
        this.resourceType = resourceType;
        this.baseMaxResource = baseMaxResource;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public int getBaseMaxResource() {
        return baseMaxResource;
    }

    public int getMaxResourceAtLevel(int level) {
        if (resourceType == ResourceType.NONE) return 0;
        return baseMaxResource + (level - 1) * 5;
    }

    public int getBaseHp() {
        return switch (this) {
            case FIGHTER -> 12;
            case ROGUE -> 8;
            case WIZARD -> 6;
            case CLERIC -> 10;
            case RANGER -> 10;
            case BARBARIAN -> 14;
            case NONE -> 10;
        };
    }

    public int getHpPerLevel() {
        return switch (this) {
            case FIGHTER -> 7;
            case ROGUE -> 5;
            case WIZARD -> 4;
            case CLERIC -> 6;
            case RANGER -> 6;
            case BARBARIAN -> 8;
            case NONE -> 5;
        };
    }

    public int getMaxHpAtLevel(int level) {
        return getBaseHp() + getHpPerLevel() * (level - 1);
    }
}
