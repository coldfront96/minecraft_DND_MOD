package com.deadmind.dndmods.feat;

public enum FeatSource {
    PHB("Player's Handbook"),
    PHB2("Player's Handbook II"),
    DMG("Dungeon Master's Guide"),
    COMPLETE_WARRIOR("Complete Warrior"),
    COMPLETE_DIVINE("Complete Divine"),
    COMPLETE_ARCANE("Complete Arcane"),
    COMPLETE_ADVENTURER("Complete Adventurer"),
    COMPLETE_SCOUNDREL("Complete Scoundrel"),
    COMPLETE_CHAMPION("Complete Champion"),
    COMPLETE_MAGE("Complete Mage"),
    HEROES_OF_HORROR("Heroes of Horror"),
    TOME_OF_MAGIC("Tome of Magic"),
    TOME_OF_BATTLE("Tome of Battle");

    private final String displayName;

    FeatSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
