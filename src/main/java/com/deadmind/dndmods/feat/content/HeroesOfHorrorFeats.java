package com.deadmind.dndmods.feat.content;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.feat.*;

import java.util.List;

/**
 * Heroes of Horror — partial sourcebook pass.
 *
 * <p>Intentionally partial: tainted/depravity/corruption subsystems are skipped
 * entirely. Only feats that translate to Minecraft are registered. New chains:
 * dread, horror, and dark knowledge.
 *
 * <p>The one active runtime effect is {@code shadow_mastery} (20% physical
 * damage reduction in darkness, in {@link FeatEffectHandler}). Several feats feed
 * shared save/initiative fields; {@code death_ward_feat} adds a daily charge.
 */
public class HeroesOfHorrorFeats {

    public static void register() {

        // --- DREAD LINE (sequential) ---
        // Dread of the Night: split partial save bonuses (full darkness detection
        // deferred to an environment pass).
        FeatRegistry.register(new Feat(
                "dread_of_the_night", "Dread of the Night",
                "In darkness or dim light gain +2 to all saves and +1 to attack rolls. Your presence unnerves enemies in darkness.",
                FeatCategory.GENERAL, FeatSource.HEROES_OF_HORROR,
                "dread_line", 0,
                List.of(wis(13)),
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                    data.setFeatFortBonus(data.getFeatFortBonus() + 1);
                },
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                    data.setFeatFortBonus(data.getFeatFortBonus() - 1);
                }
        ));

        flagFeat("child_of_night", FeatCategory.GENERAL, "Child of Night",
                "Darkvision range doubles and you cannot be blinded by magical darkness.",
                "dread_line", 1,
                List.of(reqFeat("dread_of_the_night", "Dread of the Night"), wis(15), level(5)));

        flagFeat("shadow_mastery", FeatCategory.GENERAL, "Shadow Mastery",
                "In darkness become partially incorporeal gaining 20% damage reduction from physical attacks.",
                "dread_line", 2,
                List.of(reqFeat("child_of_night", "Child of Night"), level(10), wis(17)));

        // --- HORROR LINE (fearsome_presence -> terrifying_strike / horrific_visage) ---
        flagFeat("fearsome_presence", FeatCategory.GENERAL, "Fearsome Presence",
                "Once per minute cause one enemy within 10 blocks to make a Will save DC 10 + CHA mod or flee for 3 seconds.",
                "horror_line", 0,
                List.of(cha(13), bab(3)));

        flagFeat("terrifying_strike", FeatCategory.COMBAT, "Terrifying Strike",
                "On a critical hit target must save vs fear or be panicked for 5 seconds.",
                "horror_line", 1,
                List.of(reqFeat("fearsome_presence", "Fearsome Presence"), cha(15), bab(6)));

        flagFeat("horrific_visage", FeatCategory.GENERAL, "Horrific Visage",
                "Once per day emit a burst of psychic horror causing all enemies within 15 blocks to make a Will save or be frightened for 8 seconds.",
                "horror_line", 1,
                List.of(reqFeat("fearsome_presence", "Fearsome Presence"), cha(13), level(7)));

        // --- DARK KNOWLEDGE LINE (sequential) ---
        flagFeat("dark_knowledge", FeatCategory.GENERAL, "Dark Knowledge",
                "Study an enemy type to gain +2 to attack and damage against that type for 1 minute once per day.",
                "dark_knowledge_line", 0,
                List.of(intel(13), level(3)));

        // Improved Dark Knowledge grants a partial save bonus alongside the flag.
        FeatRegistry.register(new Feat(
                "improved_dark_knowledge", "Improved Dark Knowledge",
                "Dark Knowledge bonus increases to +4 and also grants +2 to saves vs that enemy type's abilities.",
                FeatCategory.GENERAL, FeatSource.HEROES_OF_HORROR,
                "dark_knowledge_line", 1,
                List.of(reqFeat("dark_knowledge", "Dark Knowledge"), intel(15), level(7)),
                data -> {
                    data.setAchievementFlag("improved_dark_knowledge_unlocked", true);
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                },
                data -> {
                    data.setAchievementFlag("improved_dark_knowledge_unlocked", false);
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                }
        ));

        // --- STANDALONE HEROES OF HORROR FEATS ---
        flagFeat("necromantic_might", FeatCategory.DIVINE, "Necromantic Might",
                "Undead you animate gain +2 STR and +2 CON and last twice as long.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), level(5), wis(13)));

        flagFeat("enhanced_undead", FeatCategory.DIVINE, "Enhanced Undead",
                "Undead you control gain DR 5 and resistance to turning attempts.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("necromantic_might", "Necromantic Might"), level(9)));

        // Distinct ID from Complete Warrior's combat_awareness; reuses the shared
        // initiative field so it stacks.
        FeatRegistry.register(new Feat(
                "combat_awareness_horror", "Combat Awareness (Horror)",
                "Cannot be flanked and immune to surprise attacks. +2 to initiative.",
                FeatCategory.COMBAT, FeatSource.HEROES_OF_HORROR,
                null, 0,
                List.of(wis(15), bab(4)),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() + 2),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() - 2)
        ));

        flagFeat("spell_vulnerability", FeatCategory.GENERAL, "Spell Vulnerability",
                "Your spells reduce target magic resistance by 2 for 10 seconds on hit.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(6), intel(15)));

        // Grim Resolve grants a +2 Fortitude bonus alongside its flag.
        FeatRegistry.register(new Feat(
                "grim_resolve", "Grim Resolve",
                "+2 to Fortitude saves and once per day ignore death effects that would drop you below 1 HP.",
                FeatCategory.GENERAL, FeatSource.HEROES_OF_HORROR,
                null, 0,
                List.of(con(13), level(4)),
                data -> {
                    data.setAchievementFlag("grim_resolve_unlocked", true);
                    data.setFeatFortBonus(data.getFeatFortBonus() + 2);
                },
                data -> {
                    data.setAchievementFlag("grim_resolve_unlocked", false);
                    data.setFeatFortBonus(data.getFeatFortBonus() - 2);
                }
        ));

        // Death Ward grants a daily death-immunity charge (reset on login).
        FeatRegistry.register(new Feat(
                "death_ward_feat", "Death Ward",
                "Once per day become immune to death effects and negative energy damage for 10 seconds.",
                FeatCategory.GENERAL, FeatSource.HEROES_OF_HORROR,
                null, 0,
                List.of(level(7), con(15)),
                data -> {
                    data.setAchievementFlag("death_ward_unlocked", true);
                    data.setDeathWardCharges(1);
                },
                data -> {
                    data.setAchievementFlag("death_ward_unlocked", false);
                    data.setDeathWardCharges(0);
                }
        ));
    }

    // --- Helpers ---

    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        String flag = id + "_unlocked";
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.HEROES_OF_HORROR,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    private static FeatPrerequisite reqFeat(String id, String name) {
        return new FeatPrerequisite.RequiredFeatPrerequisite(id, name);
    }

    private static FeatPrerequisite cls(DnDClass requiredClass) {
        return new FeatPrerequisite.ClassPrerequisite(requiredClass);
    }

    private static FeatPrerequisite level(int minimum) {
        return new FeatPrerequisite.LevelPrerequisite(minimum);
    }

    private static FeatPrerequisite bab(int minimum) {
        return new FeatPrerequisite.BaseAttackBonusPrerequisite(minimum);
    }

    private static FeatPrerequisite intel(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.INTELLIGENCE, minimum);
    }

    private static FeatPrerequisite wis(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.WISDOM, minimum);
    }

    private static FeatPrerequisite cha(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CHARISMA, minimum);
    }

    private static FeatPrerequisite con(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CONSTITUTION, minimum);
    }
}
