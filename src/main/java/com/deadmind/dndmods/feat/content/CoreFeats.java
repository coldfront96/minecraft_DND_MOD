package com.deadmind.dndmods.feat.content;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.feat.*;

import java.util.List;

public class CoreFeats {

    public static void register() {
        FeatRegistry.register(new Feat(
                "power_attack", "Power Attack",
                "Trade attack bonus for bonus damage on melee attacks.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "power_attack_line", 0,
                List.of(
                        new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.STRENGTH, 13),
                        new FeatPrerequisite.BaseAttackBonusPrerequisite(1)
                ),
                data -> data.setAchievementFlag("power_attack_unlocked", true),
                data -> data.setAchievementFlag("power_attack_unlocked", false)
        ));

        FeatRegistry.register(new Feat(
                "cleave", "Cleave",
                "After dropping a foe, make one extra melee attack against an adjacent enemy.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "power_attack_line", 1,
                List.of(
                        new FeatPrerequisite.RequiredFeatPrerequisite("power_attack", "Power Attack"),
                        new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.STRENGTH, 13),
                        new FeatPrerequisite.BaseAttackBonusPrerequisite(1)
                ),
                data -> data.setAchievementFlag("cleave_unlocked", true),
                data -> data.setAchievementFlag("cleave_unlocked", false)
        ));

        FeatRegistry.register(new Feat(
                "great_cleave", "Great Cleave",
                "No limit on extra attacks from Cleave each round.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "power_attack_line", 2,
                List.of(
                        new FeatPrerequisite.RequiredFeatPrerequisite("cleave", "Cleave"),
                        new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.STRENGTH, 13),
                        new FeatPrerequisite.BaseAttackBonusPrerequisite(4)
                ),
                data -> data.setAchievementFlag("great_cleave_unlocked", true),
                data -> data.setAchievementFlag("great_cleave_unlocked", false)
        ));

        FeatRegistry.register(new Feat(
                "dodge", "Dodge",
                "+1 dodge bonus to AC.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "dodge_line", 0,
                List.of(
                        new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.DEXTERITY, 13)
                ),
                data -> data.setFeatAcBonus(data.getFeatAcBonus() + 1),
                data -> data.setFeatAcBonus(data.getFeatAcBonus() - 1)
        ));

        FeatRegistry.register(new Feat(
                "mobility", "Mobility",
                "+4 dodge bonus to AC against attacks of opportunity.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "dodge_line", 1,
                List.of(
                        new FeatPrerequisite.RequiredFeatPrerequisite("dodge", "Dodge"),
                        new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.DEXTERITY, 13)
                ),
                data -> data.setAchievementFlag("mobility_unlocked", true),
                data -> data.setAchievementFlag("mobility_unlocked", false)
        ));
    }
}
