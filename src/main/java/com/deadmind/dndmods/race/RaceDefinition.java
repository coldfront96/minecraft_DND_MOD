package com.deadmind.dndmods.race;

import java.util.List;

public class RaceDefinition {
    private final DnDRace race;
    private final String displayName;
    private final String description;
    private final AbilityScoreModifiers modifiers;
    private final RaceSource source;
    private final List<String> traitKeys;

    public RaceDefinition(DnDRace race, String displayName, String description,
                          AbilityScoreModifiers modifiers, RaceSource source, List<String> traitKeys) {
        this.race = race;
        this.displayName = displayName;
        this.description = description;
        this.modifiers = modifiers;
        this.source = source;
        this.traitKeys = traitKeys;
    }

    public DnDRace getRace() { return race; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public AbilityScoreModifiers getModifiers() { return modifiers; }
    public RaceSource getSource() { return source; }
    public List<String> getTraitKeys() { return traitKeys; }
}
