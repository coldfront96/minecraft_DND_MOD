package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Complete Scoundrel feat content pass — the fifth expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_SCOUNDREL}. Skill-rank-only
 * feats from the source are omitted; mixed feats keep their non-skill
 * prerequisites. New chains: luck, deception, sneak, positioning, and trickery.
 *
 * <p>Most feats are flag only — their active mechanics arrive in later combat /
 * Rogue / spell passes, gated on the {@code "<id>_unlocked"} flag. The effects
 * live this pass feed real fields on
 * {@link com.khimairacraft.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code weakening_strike} — the only active effect; a Rogue's sneak
 *       (ability) hit applies Weakness, handled in {@link FeatEffectHandler}.</li>
 *   <li>{@code luck_of_heroes} / {@code fortunate_one} / {@code fated} — daily
 *       reroll charges reset on login.</li>
 *   <li>{@code quick_thinking} — +2 initiative, reusing dangerSenseInitBonus so
 *       it stacks with Danger Sense.</li>
 * </ul>
 */
public class CompleteScoundrelFeats {

    public static void register() {

        // --- LUCK LINE (sequential 0->1->2->3) ---
        // Luck of Heroes grants a daily reroll charge (reset on login).
        FeatRegistry.register(new Feat(
                "luck_of_heroes", "Luck of Heroes",
                "Once per day reroll any d20 roll taking the better result.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_SCOUNDREL,
                "luck_line", 0,
                List.of(),
                data -> {
                    data.setAchievementFlag("luck_of_heroes_unlocked", true);
                    data.setLuckOfHeroesCharges(1);
                },
                data -> {
                    data.setAchievementFlag("luck_of_heroes_unlocked", false);
                    data.setLuckOfHeroesCharges(0);
                }
        ));

        // Fortunate One adds a second daily charge on top of Luck of Heroes.
        FeatRegistry.register(new Feat(
                "fortunate_one", "Fortunate One",
                "Luck of Heroes reroll may be used twice per day.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_SCOUNDREL,
                "luck_line", 1,
                List.of(reqFeat("luck_of_heroes", "Luck of Heroes"), level(5)),
                data -> {
                    data.setAchievementFlag("fortunate_one_unlocked", true);
                    data.setLuckOfHeroesCharges(data.getLuckOfHeroesCharges() + 1);
                },
                data -> {
                    data.setAchievementFlag("fortunate_one_unlocked", false);
                    data.setLuckOfHeroesCharges(data.getLuckOfHeroesCharges() - 1);
                }
        ));

        // Fated grants a daily auto-natural-20 charge (reset on login).
        FeatRegistry.register(new Feat(
                "fated", "Fated",
                "Once per day treat any d20 roll as a natural 20 before seeing the result.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_SCOUNDREL,
                "luck_line", 2,
                List.of(reqFeat("fortunate_one", "Fortunate One"), level(10)),
                data -> {
                    data.setAchievementFlag("fated_unlocked", true);
                    data.setFatedCharges(1);
                },
                data -> {
                    data.setAchievementFlag("fated_unlocked", false);
                    data.setFatedCharges(0);
                }
        ));

        flagFeat("auspicious_moment", FeatCategory.GENERAL, "Auspicious Moment",
                "Once per day grant an ally within 30 feet the benefit of Luck of Heroes as a free action.",
                "luck_line", 3,
                List.of(reqFeat("fated", "Fated"), level(15), cha(13)));

        // --- DECEPTION LINE (conceal -> obscure_object / false_theurgy) ---
        flagFeat("conceal_spellcasting", FeatCategory.GENERAL, "Conceal Spellcasting",
                "Cast spells without observers noticing. In Minecraft context: ability particles are suppressed for 5 seconds after casting.",
                "deception_line", 0,
                List.of(cls(DnDClass.WIZARD)));

        flagFeat("obscure_object", FeatCategory.GENERAL, "Obscure Object",
                "Hide magical auras on items from detection spells.",
                "deception_line", 1,
                List.of(reqFeat("conceal_spellcasting", "Conceal Spellcasting"), cls(DnDClass.WIZARD), level(5)));

        flagFeat("false_theurgy", FeatCategory.GENERAL, "False Theurgy",
                "Make your spells appear to be a different spell school to observers.",
                "deception_line", 1,
                List.of(reqFeat("conceal_spellcasting", "Conceal Spellcasting"), cls(DnDClass.WIZARD), level(5)));

        // --- SNEAK ATTACK LINE (staggering -> crippling / exploiting -> weakening) ---
        flagFeat("staggering_strike", FeatCategory.COMBAT, "Staggering Strike",
                "On a successful sneak attack target must make a Fortitude save or be staggered for one round losing their move action.",
                "sneak_line", 0,
                List.of(cls(DnDClass.ROGUE), bab(4)));

        flagFeat("crippling_strike", FeatCategory.COMBAT, "Crippling Strike",
                "On a sneak attack deal 2 STR damage to target in addition to normal sneak attack damage.",
                "sneak_line", 1,
                List.of(reqFeat("staggering_strike", "Staggering Strike"), cls(DnDClass.ROGUE), bab(8)));

        flagFeat("exploiting_strike", FeatCategory.COMBAT, "Exploiting Strike",
                "On a sneak attack reduce target AC by 2 for 3 seconds.",
                "sneak_line", 1,
                List.of(reqFeat("staggering_strike", "Staggering Strike"), cls(DnDClass.ROGUE), intel(13), bab(6)));

        // Weakening Strike is the only active effect this pass: a Rogue sneak
        // (ability) hit applies Weakness, handled in FeatEffectHandler#onLivingDamage.
        flagFeat("weakening_strike", FeatCategory.COMBAT, "Weakening Strike",
                "Crippling Strike now deals 4 STR damage and applies Weakness I for 10 seconds.",
                "sneak_line", 2,
                List.of(reqFeat("crippling_strike", "Crippling Strike"), cls(DnDClass.ROGUE), bab(12)));

        // --- POSITIONING LINE (sequential) ---
        flagFeat("positioning_attack", FeatCategory.COMBAT, "Positioning Attack",
                "After a successful attack move 5 feet without provoking attacks of opportunity.",
                "positioning_line", 0,
                List.of(cls(DnDClass.ROGUE), bab(3), dex(13)));

        flagFeat("flanking_maneuver", FeatCategory.COMBAT, "Flanking Maneuver",
                "When flanking with an ally your sneak attack deals +1d6 bonus damage.",
                "positioning_line", 1,
                List.of(reqFeat("positioning_attack", "Positioning Attack"), cls(DnDClass.ROGUE), bab(6)));

        flagFeat("superior_flanking", FeatCategory.COMBAT, "Superior Flanking",
                "You flank enemies even when attacking from the same side as your ally.",
                "positioning_line", 2,
                List.of(reqFeat("flanking_maneuver", "Flanking Maneuver"), cls(DnDClass.ROGUE), bab(10)));

        // --- TRICKERY LINE (sequential) ---
        flagFeat("feigned_opening", FeatCategory.COMBAT, "Feigned Opening",
                "Deliberately miss an attack to lure enemy into attacking you granting an attack of opportunity against them.",
                "trickery_line", 0,
                List.of(cls(DnDClass.ROGUE), intel(13), bab(4)));

        flagFeat("distracting_ember", FeatCategory.COMBAT, "Distracting Ember",
                "Throw a distracting object as a swift action. Target mob is distracted for 2 seconds losing targeting on you.",
                "trickery_line", 1,
                List.of(reqFeat("feigned_opening", "Feigned Opening"), cls(DnDClass.ROGUE), intel(13)));

        flagFeat("surprise_riposte", FeatCategory.COMBAT, "Surprise Riposte",
                "After a successful feint make an immediate sneak attack as a free action.",
                "trickery_line", 2,
                List.of(reqFeat("distracting_ember", "Distracting Ember"), cls(DnDClass.ROGUE), bab(8)));

        // --- STANDALONE COMPLETE SCOUNDREL FEATS (no chain) ---
        // Quick Thinking reuses the shared initiative field so it stacks with
        // Danger Sense (Complete Adventurer).
        FeatRegistry.register(new Feat(
                "quick_thinking", "Quick Thinking",
                "+2 to initiative and once per day act in the surprise round even when surprised.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_SCOUNDREL,
                null, 0,
                List.of(intel(13)),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() + 2),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() - 2)
        ));

        flagFeat("slippery_mind", FeatCategory.GENERAL, "Slippery Mind",
                "If you fail a Will save vs enchantment reroll it once on your next turn.",
                null, 0,
                List.of(cls(DnDClass.ROGUE), wis(13), level(8)));

        flagFeat("expeditious_dodge", FeatCategory.COMBAT, "Expeditious Dodge",
                "+2 dodge bonus to AC when moving at least 40 feet per round.",
                null, 0,
                List.of(reqFeat("dodge", "Dodge"), dex(13), bab(2)));

        flagFeat("minor_shapeshift", FeatCategory.GENERAL, "Minor Shapeshift",
                "Once per day assume a minor disguise granting +5 to Disguise for 1 hour. In Minecraft context apply a brief visual name tag change for 60 seconds.",
                null, 0,
                List.of(level(6), cha(13)));

        // Distinct ID from PHB improved_feint (different implementation).
        flagFeat("improved_feint_rogue", FeatCategory.COMBAT, "Improved Feint (Scoundrel)",
                "Feint as a free action once per round instead of a move action.",
                null, 0,
                List.of(cls(DnDClass.ROGUE), reqFeat("improved_feint", "Improved Feint"), intel(13)));

        flagFeat("swift_concentration", FeatCategory.GENERAL, "Swift Concentration",
                "Make concentration checks as a free action. Maintain spells while taking damage without full concentration action.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(5), intel(15)));

        flagFeat("dodge_trick", FeatCategory.COMBAT, "Dodge Trick",
                "Once per round when an enemy misses you gain a free 5-foot step without using your move action.",
                null, 0,
                List.of(reqFeat("dodge", "Dodge"), dex(15), bab(3)));

        flagFeat("confounding_opportunist", FeatCategory.COMBAT, "Confounding Opportunist",
                "Attacks of opportunity from you cause target to lose their next attack action on a failed Will save.",
                null, 0,
                List.of(cls(DnDClass.ROGUE), reqFeat("combat_reflexes", "Combat Reflexes"), bab(6)));

        flagFeat("timely_misdirection", FeatCategory.COMBAT, "Timely Misdirection",
                "After a successful feint redirect one enemy attack to another adjacent enemy.",
                null, 0,
                List.of(cls(DnDClass.ROGUE), cha(13), bab(4)));
    }

    // --- Helpers ---

    /** Registers a Complete Scoundrel feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        String flag = id + "_unlocked";
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.COMPLETE_SCOUNDREL,
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

    private static FeatPrerequisite dex(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.DEXTERITY, minimum);
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
}
