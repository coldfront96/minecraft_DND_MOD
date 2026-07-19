package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Tome of Magic — partial sourcebook pass.
 *
 * <p>Intentionally partial: the binder/vestige, shadowcasting, and truenaming
 * subsystems are skipped entirely. Only general combat-applicable feats are
 * registered. One new chain: the shadow line.
 *
 * <p>Active runtime effects (in {@link FeatEffectHandler}): {@code shadow_healing}
 * (doubled regen in darkness) and {@code embrace_the_dark} (a dynamic +STR melee
 * bonus and +4 max HP in full darkness). The remaining effects feed shared
 * save/initiative fields or are flag only.
 */
public class TomeOfMagicFeats {

    public static void register() {

        // --- SHADOW LINE (sequential) ---
        flagFeat("shadow_healing", FeatCategory.GENERAL, "Shadow Healing",
                "In dim light or darkness natural health regeneration is doubled.",
                "tome_shadow_line", 0,
                List.of(level(5), wis(13)));

        flagFeat("shadow_toughness", FeatCategory.GENERAL, "Shadow Toughness",
                "Gain temporary HP equal to WIS modifier when entering darkness. Refreshes each time you move from light to darkness.",
                "tome_shadow_line", 1,
                List.of(reqFeat("shadow_healing", "Shadow Healing"), level(8), con(13)));

        flagFeat("embrace_the_dark", FeatCategory.GENERAL, "Embrace the Dark",
                "In complete darkness gain +2 STR and +2 CON as shadow energy empowers you.",
                "tome_shadow_line", 2,
                List.of(reqFeat("shadow_toughness", "Shadow Toughness"), level(12), wis(15)));

        // --- GENERAL TOME OF MAGIC FEATS ---
        // Arcane Insight: split partial anti-magic save bonus (detection deferred).
        FeatRegistry.register(new Feat(
                "arcane_insight", "Arcane Insight",
                "+2 to saves vs spells and spell-like abilities. Detect active magical effects within 8 blocks.",
                FeatCategory.GENERAL, FeatSource.TOME_OF_MAGIC,
                null, 0,
                List.of(intel(15), level(5)),
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() + 1);
                },
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() - 1);
                }
        ));

        flagFeat("mystic_backlash", FeatCategory.COMBAT, "Mystic Backlash",
                "When an enemy spell targeting you fails they take 1d6 damage per spell level as backlash.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(7), intel(15)));

        flagFeat("power_of_shadow", FeatCategory.GENERAL, "Power of Shadow",
                "+1 to all spell save DCs when casting in dim light or darkness.",
                null, 0,
                List.of(level(6), wis(13)));

        flagFeat("shadow_cast", FeatCategory.GENERAL, "Shadow Cast",
                "Cast spells from shadows without being detected. Spell origin appears to come from a shadow within range instead of from you.",
                null, 0,
                List.of(cls(DnDClass.WIZARD), level(8), intel(15)));

        // Distinct ID "tome_awareness"; reuses the shared initiative field.
        FeatRegistry.register(new Feat(
                "tome_awareness", "Tome Awareness",
                "Sense the flow of combat magic. +2 to initiative and cannot be caught flat-footed by spell effects.",
                FeatCategory.GENERAL, FeatSource.TOME_OF_MAGIC,
                null, 0,
                List.of(wis(13), bab(3)),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() + 2),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() - 2)
        ));

        // Eldritch Insight grants a daily spell-save reroll charge (reset on login).
        FeatRegistry.register(new Feat(
                "eldritch_insight", "Eldritch Insight",
                "Once per day gain advantage on a spell save — reroll the die and take the higher result.",
                FeatCategory.GENERAL, FeatSource.TOME_OF_MAGIC,
                null, 0,
                List.of(intel(13), level(4)),
                data -> {
                    data.setAchievementFlag("eldritch_insight_unlocked", true);
                    data.setEldritchInsightCharges(1);
                },
                data -> {
                    data.setAchievementFlag("eldritch_insight_unlocked", false);
                    data.setEldritchInsightCharges(0);
                }
        ));

        // Mystic Resilience grants a +2 Fortitude bonus (no flag).
        FeatRegistry.register(new Feat(
                "mystic_resilience", "Mystic Resilience",
                "+2 to Fortitude saves vs poison and disease. Spells that would reduce your CON score have their effect halved.",
                FeatCategory.GENERAL, FeatSource.TOME_OF_MAGIC,
                null, 0,
                List.of(con(13), level(5)),
                data -> data.setFeatFortBonus(data.getFeatFortBonus() + 2),
                data -> data.setFeatFortBonus(data.getFeatFortBonus() - 2)
        ));
    }

    // --- Helpers ---

    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        String flag = id + "_unlocked";
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.TOME_OF_MAGIC,
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

    private static FeatPrerequisite con(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CONSTITUTION, minimum);
    }
}
