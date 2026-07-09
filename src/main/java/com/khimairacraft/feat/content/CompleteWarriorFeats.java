package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Complete Warrior feat content pass — the first expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_WARRIOR}. Several extend
 * existing PHB chain groups by reusing the exact {@code chainGroup} string from
 * the PHB registration in {@link CoreFeats} (e.g. {@code "power_attack_line"},
 * {@code "dodge_line"}, {@code "unarmed_line"}, {@code "weapon_focus_line"},
 * {@code "two_weapon_line"}, {@code "shield_line"}, {@code "archery_line"}); the
 * feat UI groups by that string regardless of which file registered the feat.
 * Two brand-new chains are introduced here: {@code "combat_brute_line"} and
 * {@code "melee_evasion_line"}.
 *
 * <p>Most feats are flag only — their active combat mechanics arrive in later
 * ability passes and are gated on the {@code "<id>_unlocked"} achievement flag.
 * The handful with mechanical impact this pass feed real fields:
 * <ul>
 *   <li>{@code blade_of_force} — the only active damage effect; stores an
 *       INT-derived bonus applied in {@link FeatEffectHandler}.</li>
 *   <li>{@code battle_hardened} — the only active HP scaling effect; stores a
 *       level-scaled HP value applied via a MAX_HEALTH modifier.</li>
 *   <li>{@code improved_toughness} — reuses the shared toughness stacking
 *       system so the existing MAX_HEALTH reconcile handles it.</li>
 *   <li>{@code melee_evasion} / {@code combat_awareness} — grant partial save
 *       bonuses now; their full positional effects are deferred.</li>
 * </ul>
 */
public class CompleteWarriorFeats {

    public static void register() {

        // --- POWER ATTACK LINE EXTENSIONS (PHB "power_attack_line") ---
        flagFeat("leap_attack", "Leap Attack",
                "Combine a charge with Power Attack for double the bonus damage. Triple with a two-handed weapon.",
                "power_attack_line", 3,
                List.of(reqFeat("power_attack", "Power Attack"), bab(1)));

        flagFeat("shock_trooper", "Shock Trooper",
                "Redirect Power Attack penalty from attack roll to AC instead of damage.",
                "power_attack_line", 4,
                List.of(reqFeat("leap_attack", "Leap Attack"),
                        reqFeat("power_attack", "Power Attack"), bab(6)));

        flagFeat("headlong_rush", "Headlong Rush",
                "On a charge deal double STR modifier to damage.",
                "power_attack_line", 5,
                List.of(reqFeat("shock_trooper", "Shock Trooper"), str(17), bab(9)));

        // --- DODGE LINE EXTENSIONS (PHB "dodge_line") ---
        flagFeat("karmic_strike", "Karmic Strike",
                "When an enemy hits you in melee you may make an attack of opportunity against them at -4 penalty.",
                "dodge_line", 4,
                List.of(reqFeat("combat_expertise", "Combat Expertise"),
                        reqFeat("dodge", "Dodge"), dex(13), intel(13)));

        flagFeat("robilars_gambit", "Robilar's Gambit",
                "All enemies provoke attacks of opportunity from you when they attack you but gain +4 to their attack rolls.",
                "dodge_line", 5,
                List.of(reqFeat("karmic_strike", "Karmic Strike"), dex(13), intel(13), bab(12)));

        // --- UNARMED LINE EXTENSIONS (PHB "unarmed_line") ---
        flagFeat("rapid_stunning", "Rapid Stunning",
                "Spend two Stunning Fist attempts to stun as a swift action.",
                "unarmed_line", 2,
                List.of(reqFeat("stunning_fist", "Stunning Fist"), dex(13), bab(8)));

        // Parallel branch with rapid_stunning at chainOrder 2; explicit unlock flag.
        flagFeatFlag("superior_unarmed_strike", "superior_unarmed_unlocked", "Superior Unarmed Strike",
                "Unarmed strikes deal damage as if one size category larger.",
                "unarmed_line", 2,
                List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), bab(4)));

        flagFeat("snap_kick", "Snap Kick",
                "Make one additional unarmed strike per attack action at -2 to all attack rolls.",
                "unarmed_line", 2,
                List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), bab(6)));

        flagFeat("pain_touch", "Pain Touch",
                "Spend a Stunning Fist attempt to sicken an enemy for one round on hit.",
                "unarmed_line", 3,
                List.of(reqFeat("stunning_fist", "Stunning Fist"), bab(8)));

        // --- WEAPON FOCUS LINE EXTENSIONS (PHB "weapon_focus_line") ---
        // Blade of Force is the only active damage effect this pass: it stores a
        // flat force-damage bonus equal to the INT modifier, applied in
        // FeatEffectHandler and recalculated on INT change (LevelUpPayload).
        FeatRegistry.register(new Feat(
                "blade_of_force", "Blade of Force",
                "Your weapon strikes deal bonus force damage equal to your INT modifier.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
                "weapon_focus_line", 2,
                List.of(reqFeat("weapon_focus", "Weapon Focus"), intel(13), bab(5)),
                data -> {
                    data.setAchievementFlag("blade_of_force_unlocked", true);
                    data.setBladeOfForceBonus(data.getAbilityScores().getIntMod());
                },
                data -> {
                    data.setAchievementFlag("blade_of_force_unlocked", false);
                    data.setBladeOfForceBonus(0);
                }
        ));

        flagFeat("penetrating_strike", "Penetrating Strike",
                "Your weapon attacks ignore damage reduction up to your BAB value.",
                "weapon_focus_line", 3,
                List.of(reqFeat("blade_of_force", "Blade of Force"),
                        reqFeat("weapon_focus", "Weapon Focus"), bab(10)));

        // --- TWO WEAPON LINE EXTENSIONS (PHB "two_weapon_line") ---
        flagFeat("dual_strike", "Dual Strike",
                "Once per round make both weapon attacks as a single standard action.",
                "two_weapon_line", 3,
                List.of(reqFeat("two_weapon_fighting", "Two-Weapon Fighting"), dex(15), bab(4)));

        flagFeat("two_weapon_rend", "Two-Weapon Rend",
                "Deal bonus STR damage when hitting with both weapons in the same round.",
                "two_weapon_line", 4,
                List.of(reqFeat("greater_two_weapon_fighting", "Greater Two-Weapon Fighting"), dex(17), bab(11)));

        // --- COMBAT BRUTE LINE (new chain) ---
        flagFeat("combat_brute", "Combat Brute",
                "Deal extra damage after a successful bull rush equal to distance pushed.",
                "combat_brute_line", 0,
                List.of(reqFeat("improved_bull_rush", "Improved Bull Rush"),
                        reqFeat("power_attack", "Power Attack"), str(15)));

        flagFeat("brutal_strike", "Brutal Strike",
                "Sunder attempts deal bonus damage equal to your STR modifier.",
                "combat_brute_line", 1,
                List.of(reqFeat("combat_brute", "Combat Brute"), str(17), bab(6)));

        // Parallel branch with brutal_strike at chainOrder 1.
        flagFeat("powerful_charge", "Powerful Charge",
                "Deal bonus damage on a charge attack equal to one extra weapon damage die.",
                "combat_brute_line", 1,
                List.of(reqFeat("combat_brute", "Combat Brute"), str(13), bab(1)));

        flagFeat("overwhelming_assault", "Overwhelming Assault",
                "When you hit a single target twice in one round deal bonus damage equal to your STR modifier on the third hit.",
                "combat_brute_line", 2,
                List.of(reqFeat("brutal_strike", "Brutal Strike"), str(19), bab(12)));

        // Parallel branch with overwhelming_assault at chainOrder 2.
        flagFeat("greater_powerful_charge", "Greater Powerful Charge",
                "Deal bonus damage on a charge equal to two extra weapon damage dice.",
                "combat_brute_line", 2,
                List.of(reqFeat("powerful_charge", "Powerful Charge"), str(15), bab(6)));

        // --- MELEE EVASION LINE (new chain) ---
        // Melee Evasion grants a partial Reflex bonus immediately; the full dodge
        // AC bonus is deferred to the combat defensive-stance pass.
        FeatRegistry.register(new Feat(
                "melee_evasion", "Melee Evasion",
                "+2 dodge bonus to AC and Reflex saves when fighting defensively.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
                "melee_evasion_line", 0,
                List.of(dex(15), bab(3)),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() + 1),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() - 1)
        ));

        flagFeat("defensive_throw", "Defensive Throw",
                "When an enemy misses you in melee attempt a free trip against them.",
                "melee_evasion_line", 1,
                List.of(reqFeat("melee_evasion", "Melee Evasion"),
                        reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), dex(15)));

        // Parallel branch with defensive_throw at chainOrder 1.
        flagFeat("evasive_reflexes", "Evasive Reflexes",
                "When a foe moves into a square you threaten take a free 5-foot step.",
                "melee_evasion_line", 1,
                List.of(reqFeat("melee_evasion", "Melee Evasion"), dex(13)));

        flagFeat("counterattack", "Counterattack",
                "When an enemy misses you in melee make an immediate counterattack at your highest attack bonus.",
                "melee_evasion_line", 2,
                List.of(reqFeat("evasive_reflexes", "Evasive Reflexes"),
                        reqFeat("melee_evasion", "Melee Evasion"), bab(6)));

        // --- SHIELD LINE EXTENSIONS (PHB "shield_line") ---
        flagFeat("active_shield_defense", "Active Shield Defense",
                "Use your shield to negate one melee attack per round as an immediate action.",
                "shield_line", 2,
                List.of(reqFeat("improved_shield_bash", "Improved Shield Bash"),
                        reqFeat("shield_proficiency", "Shield Proficiency"), bab(3)));

        // Parallel branch with active_shield_defense at chainOrder 2.
        flagFeat("shield_charge", "Shield Charge",
                "Deal shield bash damage and attempt a free bull rush on a charge.",
                "shield_line", 2,
                List.of(reqFeat("shield_proficiency", "Shield Proficiency"), bab(3)));

        flagFeat("shield_slam", "Shield Slam",
                "Free bull rush attempt on any successful shield bash not just charges.",
                "shield_line", 3,
                List.of(reqFeat("shield_charge", "Shield Charge"), bab(6)));

        // --- ARCHERY LINE EXTENSIONS (PHB "archery_line") ---
        flagFeat("bounding_assault", "Bounding Assault",
                "Make two ranged attacks during a move action instead of one.",
                "archery_line", 4,
                List.of(reqFeat("shot_on_the_run", "Shot on the Run"),
                        reqFeat("rapid_shot", "Rapid Shot"), dex(13), bab(6)));

        flagFeat("rapid_blitz", "Rapid Blitz",
                "Make three ranged attacks during a move action.",
                "archery_line", 5,
                List.of(reqFeat("bounding_assault", "Bounding Assault"), dex(15), bab(11)));

        // --- STANDALONE COMPLETE WARRIOR FEATS (no chain) ---
        flagFeat("hold_the_line", "Hold the Line",
                "Make an attack of opportunity against charging enemies before they reach you.",
                null, 0,
                List.of(reqFeat("combat_reflexes", "Combat Reflexes"), bab(3)));

        flagFeat("stand_still", "Stand Still",
                "On a successful attack of opportunity stop the triggering creature from moving.",
                null, 0,
                List.of(reqFeat("combat_reflexes", "Combat Reflexes")));

        flagFeat("street_fighting", "Street Fighting",
                "In confined spaces gain +1 to attack and damage rolls with unarmed strikes and light weapons.",
                null, 0,
                List.of(reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), bab(3)));

        // Proximity ally detection deferred — flag only here.
        flagFeat("phalanx_fighting", "Phalanx Fighting",
                "When adjacent to an ally with this feat gain +1 to attack and +1 shield bonus to AC.",
                null, 0,
                List.of(reqFeat("shield_proficiency", "Shield Proficiency"), bab(1)));

        flagFeat("driving_attack", "Driving Attack",
                "On a charge push the target back 5 feet in addition to normal damage.",
                null, 0,
                List.of(bab(6), str(13)));

        // Ally proximity check deferred — flag only.
        flagFeat("dirty_fighting", "Dirty Fighting",
                "Once per round deal +1d4 bonus damage if you hit a target that is also being attacked by an ally.",
                null, 0,
                List.of(bab(3)));

        flagFeat("overpowering_attack", "Overpowering Attack",
                "Once per round force a Fortitude save or knock target prone on a successful melee hit.",
                null, 0,
                List.of(str(17), bab(8)));

        flagFeat("expert_tactician", "Expert Tactician",
                "When flanking gain one extra attack of opportunity against the flanked target per round.",
                null, 0,
                List.of(bab(3), intel(13)));

        // Flee mechanic deferred — flag only.
        flagFeat("daunting_presence", "Daunting Presence",
                "Demoralize enemies as a swift action causing them to flee for one round.",
                null, 0,
                List.of(cha(13), bab(1)));

        flagFeat("never_outnumbered", "Never Outnumbered",
                "Demoralize all enemies within 10 feet simultaneously instead of one at a time.",
                null, 0,
                List.of(bab(6), cha(13)));

        // Combat Awareness translates ambush resistance as partial save bonuses;
        // full surprise detection is deferred.
        FeatRegistry.register(new Feat(
                "combat_awareness", "Combat Awareness",
                "+2 to AC and saves vs surprise attacks and ambushes.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
                null, 0,
                List.of(bab(4), wis(13)),
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() + 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() + 1);
                },
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() - 1);
                    data.setFeatRefBonus(data.getFeatRefBonus() - 1);
                }
        ));

        // Battle Hardened is the only active HP scaling effect this pass: +2 max
        // HP per hit die (scaled with total level) plus +2 Fortitude. The HP is
        // applied via a MAX_HEALTH modifier in FeatEffectHandler and rescaled on
        // level up in LevelUpPayload.
        FeatRegistry.register(new Feat(
                "battle_hardened", "Battle Hardened",
                "+2 to Fortitude saves and +2 HP per hit die.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
                null, 0,
                List.of(bab(3), con(13)),
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() + 2);
                    data.setBattleHardenedHp(data.getTotalLevel() * 2);
                },
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() - 2);
                    data.setBattleHardenedHp(0);
                }
        ));

        // Improved Toughness reuses the toughness stacking system: incrementing
        // the shared count by total level lets the existing MAX_HEALTH reconcile
        // add the correct HP delta on the next tick. LevelUpPayload bumps the
        // count by one each subsequent level so it keeps scaling.
        FeatRegistry.register(new Feat(
                "improved_toughness", "Improved Toughness",
                "+1 HP per character level stacking with Toughness.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
                null, 0,
                List.of(reqFeat("toughness", "Toughness"), bab(3)),
                data -> data.setFeatStackCount("toughness", data.getFeatStackCount("toughness") + data.getTotalLevel()),
                data -> data.setFeatStackCount("toughness", data.getFeatStackCount("toughness") - data.getTotalLevel())
        ));
    }

    // --- Helpers ---

    /** Registers a Complete Warrior combat feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Warrior combat flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.COMBAT, FeatSource.COMPLETE_WARRIOR,
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

    private static FeatPrerequisite str(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.STRENGTH, minimum);
    }

    private static FeatPrerequisite dex(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.DEXTERITY, minimum);
    }

    private static FeatPrerequisite con(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CONSTITUTION, minimum);
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
