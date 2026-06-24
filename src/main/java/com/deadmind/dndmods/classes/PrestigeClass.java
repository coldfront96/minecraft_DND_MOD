package com.deadmind.dndmods.classes;

import com.deadmind.dndmods.playerdata.DnDPlayerData;
import org.jetbrains.annotations.Nullable;

public enum PrestigeClass {
    NONE("None", null, null,
            "No prestige class selected."),
    ARCHMAGE("Archmage", DnDClass.WIZARD, null,
            "A master of arcane magic who transcends normal spellcasting limits."),
    HIGH_PRIEST("High Priest", DnDClass.CLERIC, null,
            "A divine conduit whose faith can reshape reality itself."),
    SHADOWDANCER("Shadowdancer", DnDClass.ROGUE, null,
            "A shadow-wielding infiltrator who steps between planes of darkness."),
    ASSASSIN("Assassin", DnDClass.ROGUE, null,
            "A lethal specialist in death attacks and poison."),
    CHAMPION("Champion", DnDClass.FIGHTER, null,
            "A paragon of martial skill whose weapon mastery is unmatched."),
    WARLORD("Warlord", DnDClass.FIGHTER, null,
            "A battlefield commander who inspires allies and controls the flow of combat."),
    BERSERKER("Berserker", DnDClass.BARBARIAN, null,
            "A primal fury incarnate, trading all defense for overwhelming destruction."),
    BEAR_WARRIOR("Bear Warrior", DnDClass.BARBARIAN, null,
            "A shapeshifting warrior who channels the spirit of the bear in battle."),
    ARCANE_ARCHER("Arcane Archer", DnDClass.RANGER, DnDClass.WIZARD,
            "A mystical archer who infuses arrows with arcane energy."),
    DIVINE_CHAMPION("Divine Champion", DnDClass.FIGHTER, DnDClass.CLERIC,
            "A holy warrior combining martial prowess with divine power."),
    SPELLSWORD("Spellsword", DnDClass.FIGHTER, DnDClass.WIZARD,
            "A battle mage who seamlessly blends steel and sorcery."),
    NECROMANCER("Necromancer", DnDClass.WIZARD, DnDClass.CLERIC,
            "A master of death magic who commands undead and drains life force.");

    private final String displayName;
    @Nullable
    private final DnDClass requiredPrimary;
    @Nullable
    private final DnDClass requiredSecondary;
    private final boolean unifiesPools;
    private final String description;

    PrestigeClass(String displayName, @Nullable DnDClass requiredPrimary,
                  @Nullable DnDClass requiredSecondary, String description) {
        this.displayName = displayName;
        this.requiredPrimary = requiredPrimary;
        this.requiredSecondary = requiredSecondary;
        this.unifiesPools = requiredSecondary != null;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    @Nullable public DnDClass getRequiredPrimary() { return requiredPrimary; }
    @Nullable public DnDClass getRequiredSecondary() { return requiredSecondary; }
    public boolean unifiesPools() { return unifiesPools; }
    public String getDescription() { return description; }

    public boolean meetsRequirements(DnDPlayerData data) {
        // Stub — will be implemented per prestige class with specific level,
        // ability score, and achievement requirements
        return false;
    }
}
