package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Complete Arcane feat content pass — the third expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_ARCANE}. The metamagic feats
 * extend the existing PHB {@code "metamagic_line"} chain (exact string match
 * from {@link CoreFeats}); the rest introduce new chains: spell focus, spell
 * penetration, arcane combat, school, arcane preparation, and familiar.
 *
 * <p>Most feats are flag only — their spell-system interactions arrive in the
 * Wizard ability pass and are gated on the {@code "<id>_unlocked"} flag. The
 * handful with mechanical impact this pass feed real fields on
 * {@link com.khimairacraft.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code arcane_toughness} — the only active HP scaling effect; +1 HP per
 *       Wizard level via a MAX_HEALTH modifier in {@link FeatEffectHandler}.</li>
 *   <li>{@code spell_focus} / {@code greater_spell_focus} / {@code earth_power} —
 *       stacking spell-DC bonus (spellFocusBonus).</li>
 *   <li>{@code spell_penetration} / {@code greater_spell_penetration} — stacking
 *       spell-resistance bonus (spellPenetrationBonus).</li>
 *   <li>{@code school_focus} / {@code enhanced_school} — school caster-level bonus.</li>
 *   <li>{@code school_mastery} / {@code innate_spell} / {@code magical_training} —
 *       daily charges reset on login.</li>
 *   <li>{@code empathic_link} — small Will-save (awareness) bonus.</li>
 * </ul>
 * There are no active damage effects this pass.
 */
public class CompleteArcaneFeats {

    public static void register() {

        // --- METAMAGIC LINE EXTENSIONS (PHB "metamagic_line") ---
        flagFeat("arcane_thesis", FeatCategory.METAMAGIC, "Arcane Thesis",
                "Choose one spell — apply one metamagic feat to it at no slot cost increase.",
                "metamagic_line", 2,
                List.of(cls(DnDClass.WIZARD), level(6)));

        flagFeat("metamagic_school_focus", FeatCategory.METAMAGIC, "Metamagic School Focus",
                "Apply one metamagic feat to spells of your chosen school at one slot level lower than normal.",
                "metamagic_line", 1,
                List.of(cls(DnDClass.WIZARD), level(3)));

        flagFeat("rapid_metamagic", FeatCategory.METAMAGIC, "Rapid Metamagic",
                "Apply metamagic feats without increasing casting time.",
                "metamagic_line", 2,
                List.of(cls(DnDClass.WIZARD), bab(3)));

        flagFeat("residual_metamagic", FeatCategory.METAMAGIC, "Residual Metamagic",
                "After applying a metamagic feat the next spell of the same level costs one less slot level to metamagic.",
                "metamagic_line", 1,
                List.of(cls(DnDClass.WIZARD), level(5)));

        flagFeat("arcane_mastery", FeatCategory.METAMAGIC, "Arcane Mastery",
                "Apply two metamagic feats to your thesis spell at no slot cost increase.",
                "metamagic_line", 3,
                List.of(cls(DnDClass.WIZARD), level(10), reqFeat("arcane_thesis", "Arcane Thesis")));

        // --- SPELL FOCUS LINE (new; stacking save-DC bonus) ---
        FeatRegistry.register(new Feat(
                "spell_focus", "Spell Focus",
                "+1 to save DCs of spells from chosen school.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "spell_focus_line", 0,
                List.of(),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() + 1),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() - 1)
        ));

        FeatRegistry.register(new Feat(
                "greater_spell_focus", "Greater Spell Focus",
                "+1 additional to save DCs of spells from chosen school (stacks with Spell Focus for +2 total).",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "spell_focus_line", 1,
                List.of(reqFeat("spell_focus", "Spell Focus"), level(3)),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() + 1),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() - 1)
        ));

        // --- SPELL PENETRATION LINE (new; stacking SR bonus) ---
        FeatRegistry.register(new Feat(
                "spell_penetration", "Spell Penetration",
                "+2 bonus on caster level checks to overcome spell resistance.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "spell_penetration_line", 0,
                List.of(),
                data -> data.setSpellPenetrationBonus(data.getSpellPenetrationBonus() + 2),
                data -> data.setSpellPenetrationBonus(data.getSpellPenetrationBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "greater_spell_penetration", "Greater Spell Penetration",
                "+2 additional on caster level checks to overcome spell resistance (stacks with Spell Penetration for +4 total).",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "spell_penetration_line", 1,
                List.of(reqFeat("spell_penetration", "Spell Penetration")),
                data -> data.setSpellPenetrationBonus(data.getSpellPenetrationBonus() + 2),
                data -> data.setSpellPenetrationBonus(data.getSpellPenetrationBonus() - 2)
        ));

        // --- ARCANE COMBAT LINE (new; parallel branches at order 1) ---
        // arcane_strike declares arcaneStrikeBonus but only sets its flag this
        // pass; the active spell-slot toggle is deferred to the Wizard pass.
        flagFeat("arcane_strike", FeatCategory.COMBAT, "Arcane Strike",
                "Sacrifice a spell slot to deal bonus damage on melee attacks equal to spell level sacrificed for one round.",
                "arcane_combat_line", 0,
                List.of(cls(DnDClass.WIZARD), bab(4)));

        flagFeat("arcane_volley", FeatCategory.COMBAT, "Arcane Volley",
                "Fire two arcane bolts simultaneously each dealing spell level damage.",
                "arcane_combat_line", 1,
                List.of(reqFeat("arcane_strike", "Arcane Strike"), cls(DnDClass.WIZARD), bab(8)));

        flagFeat("arcane_defense", FeatCategory.COMBAT, "Arcane Defense",
                "+2 AC while you have an active spell effect on yourself.",
                "arcane_combat_line", 1,
                List.of(reqFeat("arcane_strike", "Arcane Strike"), cls(DnDClass.WIZARD), intel(15)));

        flagFeat("arcane_spellsurge", FeatCategory.COMBAT, "Arcane Spellsurge",
                "Once per day cast any spell as a free action by expending two spell slots of its level.",
                "arcane_combat_line", 2,
                List.of(reqFeat("arcane_volley", "Arcane Volley"), cls(DnDClass.WIZARD), level(12)));

        // --- SCHOOL SPECIALIZATION LINE (new) ---
        FeatRegistry.register(new Feat(
                "school_focus", "School Focus",
                "Treat your caster level as 2 higher for spells from your chosen school.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "school_line", 0,
                List.of(cls(DnDClass.WIZARD)),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() + 2),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "enhanced_school", "Enhanced School",
                "School Focus bonus increases to +4 caster levels for chosen school.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "school_line", 1,
                List.of(reqFeat("school_focus", "School Focus"), cls(DnDClass.WIZARD), level(6)),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() + 2),
                data -> data.setSchoolCasterLevelBonus(data.getSchoolCasterLevelBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "school_mastery", "School Mastery",
                "Once per day cast one spell from your chosen school without expending a spell slot.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "school_line", 2,
                List.of(reqFeat("enhanced_school", "Enhanced School"), cls(DnDClass.WIZARD), level(12)),
                data -> {
                    data.setAchievementFlag("school_mastery_unlocked", true);
                    data.setSchoolMasteryCharges(1);
                },
                data -> {
                    data.setAchievementFlag("school_mastery_unlocked", false);
                    data.setSchoolMasteryCharges(0);
                }
        ));

        // --- ARCANE PREPARATION LINE (new, sequential) ---
        flagFeat("arcane_preparation", FeatCategory.GENERAL, "Arcane Preparation",
                "Prepare arcane spells in advance as a full action rather than requiring 8 hours of rest.",
                "arcane_prep_line", 0,
                List.of(cls(DnDClass.WIZARD)));

        flagFeat("rapid_preparation", FeatCategory.GENERAL, "Rapid Preparation",
                "Prepare spells in half the normal time.",
                "arcane_prep_line", 1,
                List.of(reqFeat("arcane_preparation", "Arcane Preparation"), cls(DnDClass.WIZARD), level(5)));

        flagFeat("arcane_spontaneity", FeatCategory.GENERAL, "Arcane Spontaneity",
                "Cast one prepared spell per day spontaneously without prior preparation.",
                "arcane_prep_line", 2,
                List.of(reqFeat("rapid_preparation", "Rapid Preparation"), cls(DnDClass.WIZARD), level(10)));

        // --- FAMILIAR LINE (new; parallel roots improved_familiar + obtain_familiar) ---
        flagFeat("improved_familiar", FeatCategory.GENERAL, "Improved Familiar",
                "Gain access to more powerful familiars including magical beasts.",
                "familiar_line", 0,
                List.of(cls(DnDClass.WIZARD), level(3)));

        // Empathic Link grants a small Will-save (awareness) bonus alongside the flag.
        FeatRegistry.register(new Feat(
                "empathic_link", "Empathic Link",
                "Share senses with your familiar up to 1 mile. Gain +2 to Perception while linked.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                "familiar_line", 1,
                List.of(reqFeat("improved_familiar", "Improved Familiar"), cls(DnDClass.WIZARD)),
                data -> {
                    data.setAchievementFlag("empathic_link_unlocked", true);
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                },
                data -> {
                    data.setAchievementFlag("empathic_link_unlocked", false);
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                }
        ));

        flagFeat("familiar_spell", FeatCategory.GENERAL, "Familiar Spell",
                "Store one spell in your familiar. It can deliver touch spells on your behalf.",
                "familiar_line", 2,
                List.of(reqFeat("empathic_link", "Empathic Link"), cls(DnDClass.WIZARD), level(8)));

        // Parallel root with improved_familiar. Id keeps the source name; display
        // and flag use the "Obtain Familiar" rename to avoid Incarnum confusion.
        flagFeatFlag("shape_soulmeld", "obtain_familiar_unlocked", FeatCategory.GENERAL, "Obtain Familiar",
                "Gain a familiar companion that provides passive bonuses.",
                "familiar_line", 0,
                List.of(cls(DnDClass.WIZARD), level(3)));

        // --- STANDALONE METAMAGIC EXTENSIONS (PHB "metamagic_line") ---
        flagFeat("energy_admixture", FeatCategory.METAMAGIC, "Energy Admixture",
                "Add a second energy type to a spell dealing half damage of each type. Spell uses a slot four levels higher.",
                "metamagic_line", 2,
                List.of(reqFeat("energy_substitution", "Energy Substitution"), level(6)));

        flagFeat("invisible_spell", FeatCategory.METAMAGIC, "Invisible Spell",
                "Make a spell invisible so its effects are unseen. Spell uses a slot two levels higher.",
                "metamagic_line", 1,
                List.of(level(3)));

        flagFeat("fell_drain", FeatCategory.METAMAGIC, "Fell Drain",
                "Creatures damaged by spell gain one negative level. Spell uses a slot two levels higher.",
                "metamagic_line", 1,
                List.of(level(4)));

        flagFeat("fell_animate", FeatCategory.METAMAGIC, "Fell Animate",
                "Creatures killed by the spell rise as zombies under your control for one hour. Spell uses a slot three levels higher.",
                "metamagic_line", 2,
                List.of(reqFeat("fell_drain", "Fell Drain"), level(6)));

        // --- STANDALONE GENERAL FEATS (no chain) ---
        flagFeat("arcane_disciple", FeatCategory.GENERAL, "Arcane Disciple",
                "Add cleric domain spells to your Wizard spell list.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), wis(13)));

        flagFeat("earth_sense", FeatCategory.GENERAL, "Earth Sense",
                "Sense creatures within 30 feet through vibration in the ground while touching it. In Minecraft context: detect mobs within 8 blocks below you through solid ground.",
                null, 0,
                List.of(wis(13)));

        // Earth Power is treated as a general spell-focus bonus this pass; the
        // on-cast HP regen is deferred to the spell system pass.
        FeatRegistry.register(new Feat(
                "earth_power", "Earth Power",
                "While touching the ground gain +1 to spell save DCs and +2 HP per spell level of spells cast.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                null, 0,
                List.of(reqFeat("earth_sense", "Earth Sense"), wis(15)),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() + 1),
                data -> data.setSpellFocusBonus(data.getSpellFocusBonus() - 1)
        ));

        // Arcane Toughness is the only active HP scaling effect this pass: +1 HP
        // per Wizard level via a MAX_HEALTH modifier in FeatEffectHandler,
        // rescaled on level up in LevelUpPayload.
        FeatRegistry.register(new Feat(
                "arcane_toughness", "Arcane Toughness",
                "Gain +1 HP per Wizard level.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(5)),
                data -> data.setArcaneToughnessHp(data.getClassLevel(DnDClass.WIZARD)),
                data -> data.setArcaneToughnessHp(0)
        ));

        flagFeat("reactive_counterspell", FeatCategory.GENERAL, "Reactive Counterspell",
                "Counterspell as an immediate action once per round without readying an action.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), reqFeat("improved_counterspell", "Improved Counterspell"), bab(3)));

        flagFeat("spell_girding", FeatCategory.GENERAL, "Spell Girding",
                "Your spells last 50% longer when cast on yourself.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), intel(15)));

        // Innate Spell grants three daily slot-free casts, reset on login.
        FeatRegistry.register(new Feat(
                "innate_spell", "Innate Spell",
                "Cast one chosen spell three times per day without expending a spell slot.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(12), intel(17)),
                data -> {
                    data.setAchievementFlag("innate_spell_unlocked", true);
                    data.setInnateSpellCharges(3);
                },
                data -> {
                    data.setAchievementFlag("innate_spell_unlocked", false);
                    data.setInnateSpellCharges(0);
                }
        ));

        // Magical Training grants three daily cantrip-level uses, reset on login.
        FeatRegistry.register(new Feat(
                "magical_training", "Magical Training",
                "Gain three cantrip-level spell uses per day usable by any class.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ARCANE,
                null, 0,
                List.of(intel(10)),
                data -> {
                    data.setAchievementFlag("magical_training_unlocked", true);
                    data.setMagicalTrainingCharges(3);
                },
                data -> {
                    data.setAchievementFlag("magical_training_unlocked", false);
                    data.setMagicalTrainingCharges(0);
                }
        ));

        flagFeat("reserves_of_strength", FeatCategory.GENERAL, "Reserves of Strength",
                "Exceed normal spell level limits at the cost of 1d6 HP per level exceeded.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(6)));
    }

    // --- Helpers ---

    /** Registers a Complete Arcane feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", category, name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Arcane flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, FeatCategory category, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.COMPLETE_ARCANE,
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
}
