package com.deadmind.dndmods.race;

public enum DnDRace {
    NONE("None", RaceSource.CORE, new AbilityScoreModifiers()),

    // PHB races
    HUMAN("Human", RaceSource.CORE, new AbilityScoreModifiers()),
    ELF("Elf", RaceSource.CORE, new AbilityScoreModifiers(0, 2, 0, 0, 0, -2)),
    DWARF("Dwarf", RaceSource.CORE, new AbilityScoreModifiers(0, -2, 2, 0, 0, 0)),
    HALFLING("Halfling", RaceSource.CORE, new AbilityScoreModifiers(-2, 2, 0, 0, 0, 0)),
    GNOME("Gnome", RaceSource.CORE, new AbilityScoreModifiers(-2, 0, 2, 0, 0, 0)),
    HALF_ELF("Half-Elf", RaceSource.CORE, new AbilityScoreModifiers()),
    HALF_ORC("Half-Orc", RaceSource.CORE, new AbilityScoreModifiers(2, 0, 0, -2, 0, -2)),

    // Expanded races
    DRAGONBORN("Dragonborn", RaceSource.EXPANDED, new AbilityScoreModifiers(2, 0, 0, 0, 0, -2)),
    GOLIATH("Goliath", RaceSource.EXPANDED, new AbilityScoreModifiers(4, 0, 2, 0, -2, -2)),
    WARFORGED("Warforged", RaceSource.EXPANDED, new AbilityScoreModifiers(2, 0, 2, 0, -2, 0)),
    TIEFLING("Tiefling", RaceSource.EXPANDED, new AbilityScoreModifiers(0, 0, 0, 2, 0, -2)),
    AASIMAR("Aasimar", RaceSource.EXPANDED, new AbilityScoreModifiers(0, 0, 0, 0, 2, 2)),

    // Undead races
    REVENANT("Revenant", RaceSource.UNDEAD, new AbilityScoreModifiers(2, 0, 2, 0, 0, -2)),
    DHAMPIR("Dhampir", RaceSource.UNDEAD, new AbilityScoreModifiers(0, 2, 0, 0, 0, 2)),
    SHADAR_KAI("Shadar-Kai", RaceSource.UNDEAD, new AbilityScoreModifiers(0, 2, -2, 0, 0, 0)),
    VAMPIRE_SPAWN("Vampire Spawn", RaceSource.UNDEAD, new AbilityScoreModifiers(2, 0, 0, 0, 0, 2)),
    SKELETON_WARRIOR("Skeleton Warrior", RaceSource.UNDEAD, new AbilityScoreModifiers(2, 0, 2, -2, 0, -4));

    private final String displayName;
    private final RaceSource source;
    private final AbilityScoreModifiers modifiers;

    DnDRace(String displayName, RaceSource source, AbilityScoreModifiers modifiers) {
        this.displayName = displayName;
        this.source = source;
        this.modifiers = modifiers;
    }

    public String getDisplayName() { return displayName; }
    public RaceSource getSource() { return source; }
    public AbilityScoreModifiers getModifiers() { return modifiers; }
}
