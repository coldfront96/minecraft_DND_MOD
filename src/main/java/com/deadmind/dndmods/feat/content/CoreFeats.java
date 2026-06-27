package com.deadmind.dndmods.feat.content;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.classes.DnDClass;
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

        registerCombatFeats();
    }

    // ------------------------------------------------------------------
    // PHB combat feat content pass. Unlock flags only — active combat
    // effects are implemented in a later ability pass. Each flag feat
    // stores "<featId>_unlocked" in DnDPlayerData's achievement flags,
    // matching the convention used by the test feats above.
    // ------------------------------------------------------------------
    private static void registerCombatFeats() {

        // --- DODGE LINE (extends existing dodge/mobility chain) ---
        flagFeat("spring_attack", "Spring Attack",
                "Move before and after a melee attack without provoking attacks of opportunity.",
                "dodge_line", 2,
                List.of(reqFeat("mobility", "Mobility"), dex(13), bab(4)));

        flagFeat("whirlwind_attack", "Whirlwind Attack",
                "Make one melee attack against every enemy within reach as a full attack action.",
                "dodge_line", 3,
                List.of(reqFeat("spring_attack", "Spring Attack"), dex(13), intel(13), bab(4)));

        // --- TWO-WEAPON FIGHTING LINE ---
        flagFeat("two_weapon_fighting", "Two-Weapon Fighting",
                "Reduce penalties for fighting with two weapons.",
                "two_weapon_line", 0,
                List.of(dex(15)));

        flagFeat("improved_two_weapon_fighting", "Improved Two-Weapon Fighting",
                "Gain an additional attack with your off-hand weapon.",
                "two_weapon_line", 1,
                List.of(reqFeat("two_weapon_fighting", "Two-Weapon Fighting"), dex(17), bab(6)));

        flagFeat("greater_two_weapon_fighting", "Greater Two-Weapon Fighting",
                "Gain a third attack with your off-hand weapon.",
                "two_weapon_line", 2,
                List.of(reqFeat("improved_two_weapon_fighting", "Improved Two-Weapon Fighting"), dex(19), bab(11)));

        // Two-Weapon Defense grants a passive shield bonus to AC.
        FeatRegistry.register(new Feat(
                "two_weapon_defense", "Two-Weapon Defense",
                "+1 shield bonus to AC when wielding two weapons. +2 when fighting defensively.",
                FeatCategory.COMBAT, FeatSource.PHB,
                "two_weapon_line", 3,
                List.of(reqFeat("two_weapon_fighting", "Two-Weapon Fighting"), dex(15)),
                data -> data.setFeatShieldBonus(data.getFeatShieldBonus() + 1),
                data -> data.setFeatShieldBonus(data.getFeatShieldBonus() - 1)
        ));

        // --- ARCHERY LINE ---
        flagFeat("point_blank_shot", "Point Blank Shot",
                "+1 attack and damage with ranged weapons against targets within 30 feet.",
                "archery_line", 0,
                List.of());

        flagFeat("precise_shot", "Precise Shot",
                "Shoot into melee without penalty.",
                "archery_line", 1,
                List.of(reqFeat("point_blank_shot", "Point Blank Shot")));

        flagFeat("improved_precise_shot", "Improved Precise Shot",
                "Ignore cover and concealment bonuses on ranged attacks.",
                "archery_line", 2,
                List.of(reqFeat("precise_shot", "Precise Shot"), dex(19), bab(11)));

        flagFeat("rapid_shot", "Rapid Shot",
                "Fire one additional ranged attack per attack action at a -2 penalty to all attacks.",
                "archery_line", 1,
                List.of(reqFeat("point_blank_shot", "Point Blank Shot"), dex(13)));

        flagFeat("manyshot", "Manyshot",
                "Fire two arrows simultaneously at the same target.",
                "archery_line", 2,
                List.of(reqFeat("rapid_shot", "Rapid Shot"), dex(17), bab(6)));

        flagFeat("far_shot", "Far Shot",
                "Increase range increment of ranged weapons by 50%.",
                "archery_line", 1,
                List.of(reqFeat("point_blank_shot", "Point Blank Shot")));

        flagFeat("shot_on_the_run", "Shot on the Run",
                "Move before and after a ranged attack without penalty.",
                "archery_line", 3,
                List.of(reqFeat("point_blank_shot", "Point Blank Shot"),
                        reqFeat("rapid_shot", "Rapid Shot"), dex(13), bab(4)));

        // --- SHIELD LINE ---
        flagFeat("shield_proficiency", "Shield Proficiency",
                "Use a shield without armor check penalty applying to attack rolls.",
                "shield_line", 0,
                List.of());

        flagFeat("improved_shield_bash", "Improved Shield Bash",
                "Retain shield AC bonus when using shield as a weapon.",
                "shield_line", 1,
                List.of(reqFeat("shield_proficiency", "Shield Proficiency")));

        flagFeat("tower_shield_proficiency", "Tower Shield Proficiency",
                "Use a tower shield without the normal penalties.",
                "shield_line", 1,
                List.of(reqFeat("shield_proficiency", "Shield Proficiency")));

        // --- COMBAT EXPERTISE LINE ---
        flagFeat("combat_expertise", "Combat Expertise",
                "Trade attack bonus for dodge bonus to AC during combat.",
                "combat_expertise_line", 0,
                List.of(intel(13)));

        flagFeat("improved_disarm", "Improved Disarm",
                "Disarm opponents without provoking attacks of opportunity. +4 to disarm checks.",
                "combat_expertise_line", 1,
                List.of(reqFeat("combat_expertise", "Combat Expertise"), intel(13)));

        flagFeat("improved_trip", "Improved Trip",
                "Trip opponents without provoking attacks of opportunity. +4 to trip checks.",
                "combat_expertise_line", 1,
                List.of(reqFeat("combat_expertise", "Combat Expertise"), intel(13)));

        flagFeat("improved_feint", "Improved Feint",
                "Feint in combat as a move action instead of standard action.",
                "combat_expertise_line", 1,
                List.of(reqFeat("combat_expertise", "Combat Expertise"), intel(13)));

        // --- IMPROVED UNARMED LINE ---
        flagFeat("improved_unarmed_strike", "Improved Unarmed Strike",
                "Deal lethal damage with unarmed strikes and do not provoke attacks of opportunity.",
                "unarmed_line", 0,
                List.of());

        flagFeat("stunning_fist", "Stunning Fist",
                "Force an enemy to make a Fortitude save or be stunned for one round on a hit.",
                "unarmed_line", 1,
                List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), dex(13), wis(13), bab(8)));

        flagFeat("deflect_arrows", "Deflect Arrows",
                "Deflect one ranged attack per round as a free action.",
                "unarmed_line", 1,
                List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), dex(13)));

        flagFeat("snatch_arrows", "Snatch Arrows",
                "Catch deflected ranged weapons instead of simply deflecting them.",
                "unarmed_line", 2,
                List.of(reqFeat("deflect_arrows", "Deflect Arrows"), dex(15)));

        // --- MOUNTED COMBAT LINE ---
        flagFeat("mounted_combat", "Mounted Combat",
                "Negate hits against your mount once per round with a Ride check.",
                "mounted_line", 0,
                List.of());

        flagFeat("mounted_archery", "Mounted Archery",
                "Halve the penalty for ranged attacks while mounted.",
                "mounted_line", 1,
                List.of(reqFeat("mounted_combat", "Mounted Combat")));

        flagFeat("trample", "Trample",
                "Overrun opponents while mounted and deal damage automatically.",
                "mounted_line", 1,
                List.of(reqFeat("mounted_combat", "Mounted Combat")));

        flagFeat("ride_by_attack", "Ride-By Attack",
                "Move before and after a mounted charge attack.",
                "mounted_line", 1,
                List.of(reqFeat("mounted_combat", "Mounted Combat")));

        flagFeat("spirited_charge", "Spirited Charge",
                "Deal double damage on a mounted charge. Triple with a lance.",
                "mounted_line", 2,
                List.of(reqFeat("ride_by_attack", "Ride-By Attack"), reqFeat("mounted_combat", "Mounted Combat")));

        // --- WEAPON FOCUS LINE ---
        flagFeat("weapon_focus", "Weapon Focus",
                "+1 attack bonus with chosen weapon type.",
                "weapon_focus_line", 0,
                List.of(bab(1)));

        flagFeat("greater_weapon_focus", "Greater Weapon Focus",
                "+1 attack bonus with chosen weapon type (stacks with Weapon Focus for +2 total).",
                "weapon_focus_line", 1,
                List.of(reqFeat("weapon_focus", "Weapon Focus"), cls(DnDClass.FIGHTER), bab(8)));

        flagFeat("weapon_specialization", "Weapon Specialization",
                "+2 damage bonus with chosen weapon type.",
                "weapon_focus_line", 1,
                List.of(reqFeat("weapon_focus", "Weapon Focus"), cls(DnDClass.FIGHTER), bab(4)));

        flagFeat("greater_weapon_specialization", "Greater Weapon Specialization",
                "+2 damage bonus with chosen weapon type (stacks with Weapon Spec for +4 total).",
                "weapon_focus_line", 2,
                List.of(reqFeat("weapon_specialization", "Weapon Specialization"),
                        reqFeat("greater_weapon_focus", "Greater Weapon Focus"), cls(DnDClass.FIGHTER), bab(12)));

        // --- STANDALONE COMBAT FEATS (no chain) ---
        flagFeat("blind_fight", "Blind-Fight",
                "Reroll miss chance from concealment once. No penalty fighting invisible opponents.",
                null, 0, List.of());

        flagFeat("combat_reflexes", "Combat Reflexes",
                "Make additional attacks of opportunity per round equal to DEX modifier.",
                null, 0, List.of());

        flagFeat("exotic_weapon_proficiency", "Exotic Weapon Proficiency",
                "Use one exotic weapon without nonproficiency penalty.",
                null, 0, List.of(bab(1)));

        flagFeat("improved_bull_rush", "Improved Bull Rush",
                "Bull rush without provoking attacks of opportunity. +4 to bull rush checks.",
                null, 0, List.of(str(13), reqFeat("power_attack", "Power Attack")));

        flagFeat("improved_overrun", "Improved Overrun",
                "Overrun without provoking attacks of opportunity. +4 to overrun checks.",
                null, 0, List.of(str(13), reqFeat("power_attack", "Power Attack")));

        flagFeat("improved_sunder", "Improved Sunder",
                "Sunder without provoking attacks of opportunity. +4 to sunder checks.",
                null, 0, List.of(str(13), reqFeat("power_attack", "Power Attack")));

        // Improved Initiative grants a passive initiative bonus.
        FeatRegistry.register(new Feat(
                "improved_initiative", "Improved Initiative",
                "+4 bonus on initiative checks.",
                FeatCategory.COMBAT, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setFeatInitiativeBonus(data.getFeatInitiativeBonus() + 4),
                data -> data.setFeatInitiativeBonus(data.getFeatInitiativeBonus() - 4)
        ));

        flagFeat("weapon_finesse", "Weapon Finesse",
                "Use DEX modifier instead of STR on attack rolls with light weapons.",
                null, 0, List.of(bab(1)));

        flagFeat("endurance", "Endurance",
                "+4 bonus on checks to avoid nonlethal damage from harsh conditions.",
                null, 0, List.of());

        flagFeat("diehard", "Diehard",
                "Automatically stabilize when dying. Fight on at negative HP.",
                null, 0, List.of(reqFeat("endurance", "Endurance")));

        flagFeat("extra_rage", "Extra Rage",
                "Rage two additional times per day.",
                null, 0, List.of(cls(DnDClass.BARBARIAN)));

        flagFeat("extra_smiting", "Extra Smiting",
                "Two additional smite attempts per day.",
                null, 0, List.of(cls(DnDClass.CLERIC)));

        flagFeat("extra_turning", "Extra Turning",
                "Four additional turning attempts per day.",
                null, 0, List.of(cls(DnDClass.CLERIC)));

        flagFeat("improved_counterspell", "Improved Counterspell",
                "Counterspell with a spell of the same school rather than the same spell.",
                null, 0, List.of());

        flagFeat("improved_critical", "Improved Critical",
                "Double the threat range of your chosen weapon type.",
                null, 0, List.of(bab(8)));

        flagFeat("improved_grapple", "Improved Grapple",
                "Grapple without provoking attacks of opportunity. +4 to grapple checks.",
                null, 0, List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), dex(13)));

        flagFeat("quick_draw", "Quick Draw",
                "Draw a weapon as a free action.",
                null, 0, List.of(bab(1)));

        flagFeat("rapid_reload", "Rapid Reload",
                "Reload a crossbow faster than normal.",
                null, 0, List.of());

        flagFeat("track", "Track",
                "Follow tracks left by creatures in the wilderness.",
                null, 0, List.of());
    }

    // --- Helpers ---

    /** Registers a PHB combat feat whose only effect is setting an unlock flag. */
    private static void flagFeat(String id, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        String flag = id + "_unlocked";
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.COMBAT, FeatSource.PHB,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    private static FeatPrerequisite reqFeat(String id, String name) {
        return new FeatPrerequisite.RequiredFeatPrerequisite(id, name);
    }

    private static FeatPrerequisite bab(int minimum) {
        return new FeatPrerequisite.BaseAttackBonusPrerequisite(minimum);
    }

    private static FeatPrerequisite cls(DnDClass requiredClass) {
        return new FeatPrerequisite.ClassPrerequisite(requiredClass);
    }

    private static FeatPrerequisite str(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.STRENGTH, minimum);
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
}
