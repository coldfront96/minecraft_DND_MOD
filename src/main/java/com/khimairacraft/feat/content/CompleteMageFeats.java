package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Complete Mage feat content pass — the seventh and final full-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_MAGE}. Several extend existing
 * chains by exact string match: {@code "metamagic_line"} (PHB / Complete Arcane),
 * {@code "spell_focus_line"} and {@code "familiar_line"} (Complete Arcane). New
 * chains: arcane channeling, eldritch lore, mage armor, and ward.
 *
 * <p>No active damage effects this pass — every spell interaction is flag only
 * (deferred to the Wizard ability/spell passes). The effects that are live feed
 * real fields on {@link com.khimairacraft.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code eldritch_lore} / {@code eldritch_mastery} — INT-based caster-level
 *       bonus (x2 with Mastery), recalculated on INT change.</li>
 *   <li>{@code spell_power} / {@code greater_spell_power} — reuse
 *       schoolCasterLevelBonus from Complete Arcane.</li>
 *   <li>{@code armored_mage} / {@code arcane_ward} — reuse naturalArmorBonus.</li>
 *   <li>{@code arcane_defense_saves} — split +1 Will / +1 Reflex.</li>
 *   <li>Daily charge feats (eldritch_apex, automatic/instant metamagic, ward,
 *       spell_reflection, reactive_spell) reset on login.</li>
 *   <li>{@code arcane_reflexes} — INT-for-initiative, applied in CharacterSheetScreen.</li>
 * </ul>
 */
public class CompleteMageFeats {

    public static void register() {

        // --- ARCANE CHANNELING LINE (sequential) ---
        flagFeat("arcane_channeling", FeatCategory.COMBAT, "Arcane Channeling",
                "Deliver a touch spell through a melee weapon attack instead of a touch attack.",
                "arcane_channeling_line", 0,
                List.of(cls(DnDClass.WIZARD), bab(4), level(5)));

        flagFeat("greater_arcane_channeling", FeatCategory.COMBAT, "Greater Arcane Channeling",
                "Deliver any spell not just touch spells through melee weapon attacks.",
                "arcane_channeling_line", 1,
                List.of(reqFeat("arcane_channeling", "Arcane Channeling"), cls(DnDClass.WIZARD), bab(8), level(10)));

        flagFeat("arcane_fury", FeatCategory.COMBAT, "Arcane Fury",
                "Deliver a spell through every melee attack in a full attack action.",
                "arcane_channeling_line", 2,
                List.of(reqFeat("greater_arcane_channeling", "Greater Arcane Channeling"),
                        cls(DnDClass.WIZARD), bab(12), level(15)));

        // --- ELDRITCH LORE LINE (sequential) ---
        // Eldritch Lore: caster-level bonus equal to the INT modifier (field only).
        FeatRegistry.register(new Feat(
                "eldritch_lore", "Eldritch Lore",
                "Add INT modifier to caster level for determining spell duration and area.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "eldritch_lore_line", 0,
                List.of(cls(DnDClass.WIZARD), level(3), intel(15)),
                data -> data.setEldritchLoreCasterBonus(data.getAbilityScores().getIntMod()),
                data -> data.setEldritchLoreCasterBonus(0)
        ));

        // Eldritch Mastery overrides the bonus to INT modifier x2 (not increment).
        FeatRegistry.register(new Feat(
                "eldritch_mastery", "Eldritch Mastery",
                "Eldritch Lore bonus increases to INT modifier x 2 for caster level calculations.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "eldritch_lore_line", 1,
                List.of(reqFeat("eldritch_lore", "Eldritch Lore"), cls(DnDClass.WIZARD), level(8), intel(17)),
                data -> {
                    data.setAchievementFlag("eldritch_mastery_unlocked", true);
                    data.setEldritchLoreCasterBonus(data.getAbilityScores().getIntMod() * 2);
                },
                data -> {
                    data.setAchievementFlag("eldritch_mastery_unlocked", false);
                    data.setEldritchLoreCasterBonus(
                            data.hasFeat("eldritch_lore") ? data.getAbilityScores().getIntMod() : 0);
                }
        ));

        // Eldritch Apex grants a daily max-caster-level cast charge (reset on login).
        FeatRegistry.register(new Feat(
                "eldritch_apex", "Eldritch Apex",
                "Once per day cast any known spell at maximum caster level regardless of actual level.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "eldritch_lore_line", 2,
                List.of(reqFeat("eldritch_mastery", "Eldritch Mastery"), cls(DnDClass.WIZARD), level(15), intel(19)),
                data -> {
                    data.setAchievementFlag("eldritch_apex_unlocked", true);
                    data.setEldritchApexCharges(1);
                },
                data -> {
                    data.setAchievementFlag("eldritch_apex_unlocked", false);
                    data.setEldritchApexCharges(0);
                }
        ));

        // --- SPELL POWER LINE EXTENSIONS (Complete Arcane "spell_focus_line") ---
        // Reuse schoolCasterLevelBonus from Complete Arcane.
        FeatRegistry.register(new Feat(
                "spell_power", "Spell Power",
                "+1 to caster level for spells of chosen school. Stacks with school focus.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "spell_focus_line", 2,
                List.of(reqFeat("greater_spell_focus", "Greater Spell Focus"), cls(DnDClass.WIZARD), level(6)),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() + 1),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() - 1)
        ));

        FeatRegistry.register(new Feat(
                "greater_spell_power", "Greater Spell Power",
                "+2 additional caster levels for chosen school spells.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "spell_focus_line", 3,
                List.of(reqFeat("spell_power", "Spell Power"), cls(DnDClass.WIZARD), level(12)),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() + 2),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() - 2)
        ));

        // --- MAGE ARMOR LINE (sequential) ---
        flagFeatFlag("mage_armor_proficiency", "mage_armor_prof_unlocked", FeatCategory.GENERAL, "Mage Armor Proficiency",
                "Wear light armor without arcane spell failure chance.",
                "mage_armor_line", 0,
                List.of(cls(DnDClass.WIZARD), level(3)));

        // Armored Mage grants a small AC bonus from armor familiarity.
        FeatRegistry.register(new Feat(
                "armored_mage", "Armored Mage",
                "Wear medium armor without arcane spell failure chance.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "mage_armor_line", 1,
                List.of(reqFeat("mage_armor_proficiency", "Mage Armor Proficiency"), cls(DnDClass.WIZARD), level(6)),
                data -> {
                    data.setAchievementFlag("armored_mage_unlocked", true);
                    data.setNaturalArmorBonus(data.getNaturalArmorBonus() + 1);
                },
                data -> {
                    data.setAchievementFlag("armored_mage_unlocked", false);
                    data.setNaturalArmorBonus(data.getNaturalArmorBonus() - 1);
                }
        ));

        flagFeat("battle_mage", FeatCategory.COMBAT, "Battle Mage",
                "Cast spells in melee without provoking attacks of opportunity.",
                "mage_armor_line", 2,
                List.of(reqFeat("armored_mage", "Armored Mage"), cls(DnDClass.WIZARD), bab(4), level(8)));

        // --- METAMAGIC EXTENSIONS (PHB / Complete Arcane "metamagic_line") ---
        flagFeat("metamagic_effect", FeatCategory.METAMAGIC, "Metamagic Effect",
                "Apply a metamagic feat to an already-active ongoing spell effect.",
                "metamagic_line", 1,
                List.of(cls(DnDClass.WIZARD), level(5)));

        flagFeat("metamagic_specialist", FeatCategory.METAMAGIC, "Metamagic Specialist",
                "Reduce the slot level increase of all metamagic feats by 1 minimum 0 once per day per feat.",
                "metamagic_line", 2,
                List.of(cls(DnDClass.WIZARD), level(8), intel(15)));

        // Automatic Metamagic grants a daily free-metamagic charge (reset on login).
        FeatRegistry.register(new Feat(
                "automatic_metamagic", "Automatic Metamagic",
                "Apply one metamagic feat per day to a spell without increasing casting time or slot level.",
                FeatCategory.METAMAGIC, FeatSource.COMPLETE_MAGE,
                "metamagic_line", 3,
                List.of(cls(DnDClass.WIZARD), level(12), intel(17), reqFeat("rapid_metamagic", "Rapid Metamagic")),
                data -> {
                    data.setAchievementFlag("automatic_metamagic_unlocked", true);
                    data.setAutomaticMetamagicCharges(1);
                },
                data -> {
                    data.setAchievementFlag("automatic_metamagic_unlocked", false);
                    data.setAutomaticMetamagicCharges(0);
                }
        ));

        // --- WARD LINE (sequential) ---
        // Warding Gesture grants a daily defensive ward charge (reset on login).
        FeatRegistry.register(new Feat(
                "warding_gesture", "Warding Gesture",
                "Once per day as a free action gain +4 AC vs the next attack that would hit you.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "ward_line", 0,
                List.of(cls(DnDClass.WIZARD), intel(13)),
                data -> {
                    data.setAchievementFlag("warding_gesture_unlocked", true);
                    data.setWardingGestureCharges(1);
                },
                data -> {
                    data.setAchievementFlag("warding_gesture_unlocked", false);
                    data.setWardingGestureCharges(0);
                }
        ));

        // Greater Warding adds a second daily charge.
        FeatRegistry.register(new Feat(
                "greater_warding", "Greater Warding",
                "Warding Gesture now usable twice per day and grants +6 AC.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "ward_line", 1,
                List.of(reqFeat("warding_gesture", "Warding Gesture"), cls(DnDClass.WIZARD), level(7), intel(15)),
                data -> {
                    data.setAchievementFlag("greater_warding_unlocked", true);
                    data.setWardingGestureCharges(data.getWardingGestureCharges() + 1);
                },
                data -> {
                    data.setAchievementFlag("greater_warding_unlocked", false);
                    data.setWardingGestureCharges(data.getWardingGestureCharges() - 1);
                }
        ));

        // Arcane Ward grants permanent ward AC via naturalArmorBonus.
        FeatRegistry.register(new Feat(
                "arcane_ward", "Arcane Ward",
                "Permanent +2 AC from a persistent arcane ward that refreshes after 10 seconds out of combat.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                "ward_line", 2,
                List.of(reqFeat("greater_warding", "Greater Warding"), cls(DnDClass.WIZARD), level(12), intel(17)),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() + 2),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() - 2)
        ));

        // --- STANDALONE COMPLETE MAGE FEATS ---
        flagFeat("energy_aura", FeatCategory.GENERAL, "Energy Aura",
                "Surround yourself with an aura of chosen energy type dealing 1d6 damage per round to adjacent enemies.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(9), intel(15)));

        flagFeat("spell_cartouche", FeatCategory.GENERAL, "Spell Cartouche",
                "Store one spell per day in a cartouche item for later single use by anyone.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(5)));

        flagFeat("cooperative_metamagic", FeatCategory.METAMAGIC, "Cooperative Metamagic",
                "Two casters combine to apply a metamagic effect neither could apply alone.",
                "metamagic_line", 1,
                List.of(cls(DnDClass.WIZARD), level(6)));

        // Distinct ID from Complete Arcane's arcane_defense combat feat. Splits a
        // +2 anti-magic save bonus across Will and Reflex.
        FeatRegistry.register(new Feat(
                "arcane_defense_saves", "Arcane Defense (Saves)",
                "+2 to saves vs spells and spell-like abilities.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                null, 0,
                List.of(cls(DnDClass.WIZARD), intel(13), level(3)),
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() + 1);
                },
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() - 1);
                }
        ));

        // Extends Complete Arcane's familiar_line as a parallel branch at order 1.
        flagFeat("imbue_familiar", FeatCategory.GENERAL, "Imbue Familiar",
                "Grant your familiar one of your spell slots per day to use independently.",
                "familiar_line", 1,
                List.of(reqFeat("improved_familiar", "Improved Familiar"), cls(DnDClass.WIZARD), level(6)));

        // Spell Reflection grants a daily reflect charge (reset on login).
        FeatRegistry.register(new Feat(
                "spell_reflection", "Spell Reflection",
                "Once per day reflect a targeted spell back at its caster.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(14), intel(19)),
                data -> {
                    data.setAchievementFlag("spell_reflection_unlocked", true);
                    data.setSpellReflectionCharges(1);
                },
                data -> {
                    data.setAchievementFlag("spell_reflection_unlocked", false);
                    data.setSpellReflectionCharges(0);
                }
        ));

        // Reactive Spell grants a daily immediate-action cast charge (reset on login).
        FeatRegistry.register(new Feat(
                "reactive_spell", "Reactive Spell",
                "Cast a prepared spell as an immediate action once per day.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_MAGE,
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(8), intel(15),
                        reqFeat("reactive_counterspell", "Reactive Counterspell")),
                data -> {
                    data.setAchievementFlag("reactive_spell_unlocked", true);
                    data.setReactiveSpellCharges(1);
                },
                data -> {
                    data.setAchievementFlag("reactive_spell_unlocked", false);
                    data.setReactiveSpellCharges(0);
                }
        ));

        flagFeat("extraordinary_concentration", FeatCategory.GENERAL, "Extraordinary Concentration",
                "Maintain concentration on spells passively without using actions. +4 to all concentration checks.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), intel(17), level(10)));

        // Instant Metamagic grants three daily free-action metamagic casts
        // (reset on login). Parallel branch with automatic_metamagic at order 3.
        FeatRegistry.register(new Feat(
                "instant_metamagic", "Instant Metamagic",
                "Apply one metamagic feat to a spell as a free action three times per day.",
                FeatCategory.METAMAGIC, FeatSource.COMPLETE_MAGE,
                "metamagic_line", 3,
                List.of(cls(DnDClass.WIZARD), level(15), intel(19)),
                data -> {
                    data.setAchievementFlag("instant_metamagic_unlocked", true);
                    data.setInstantMetamagicCharges(3);
                },
                data -> {
                    data.setAchievementFlag("instant_metamagic_unlocked", false);
                    data.setInstantMetamagicCharges(0);
                }
        ));

        // Arcane Reflexes lets a Wizard use INT for initiative; applied in
        // CharacterSheetScreen (mirrors Insightful Reflexes for Reflex saves).
        flagFeat("arcane_reflexes", FeatCategory.COMBAT, "Arcane Reflexes",
                "Use INT modifier instead of DEX for initiative checks.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), intel(13), bab(2)));
    }

    // --- Helpers ---

    /** Registers a Complete Mage feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", category, name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Mage flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, FeatCategory category, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.COMPLETE_MAGE,
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
}
