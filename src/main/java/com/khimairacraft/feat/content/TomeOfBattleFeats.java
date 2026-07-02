package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Tome of Battle — partial sourcebook pass.
 *
 * <p>Intentionally partial: the full maneuver system (strikes, boosts, stances,
 * counters) is NOT implemented. Only passive feats that gate on BAB / ability
 * scores and grant flat bonuses or unlock flags are registered; maneuver
 * execution is deferred to a future martial ability pass. Every feat here is
 * COMBAT category. Five discipline chains: iron heart, diamond mind, setting
 * sun, stone dragon, and white raven.
 *
 * <p>The primary active effect is the shared {@code martialDamageBonus} flat
 * melee bonus (iron_will_warrior, mountain_hammer, elder_mountain_hammer,
 * martial_study), applied in {@link FeatEffectHandler}. Also active there:
 * {@code adamantine_body} flat reduction, {@code concentration_of_might}
 * once-per-second INT bonus, and {@code blood_of_the_martyr} sub-50%-HP fury.
 */
public class TomeOfBattleFeats {

    public static void register() {

        // --- IRON HEART LINE (raw martial power) ---
        // Iron Will (Warrior): +2 Will and +1 flat melee damage. Distinct ID from
        // PHB iron_will. The primary active effect feeds martialDamageBonus.
        FeatRegistry.register(new Feat(
                "iron_will_warrior", "Iron Will (Warrior)",
                "Your martial training hardens your resolve. +2 to Will saves and +1 to all melee damage rolls.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "iron_heart_line", 0,
                List.of(bab(3), str(13)),
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() + 2);
                    data.setMartialDamageBonus(data.getMartialDamageBonus() + 1);
                },
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() - 2);
                    data.setMartialDamageBonus(data.getMartialDamageBonus() - 1);
                }
        ));

        flagFeat("iron_heart_surge", "Iron Heart Surge",
                "Once per minute remove one negative status effect from yourself through sheer martial willpower.",
                "iron_heart_line", 1,
                List.of(reqFeat("iron_will_warrior", "Iron Will (Warrior)"), bab(6), str(15)));

        // Adamantine Body grants flat physical-melee damage reduction.
        FeatRegistry.register(new Feat(
                "adamantine_body", "Adamantine Body",
                "Reduce all incoming physical damage by 3. Your body is hardened to near-metallic toughness.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "iron_heart_line", 2,
                List.of(reqFeat("iron_heart_surge", "Iron Heart Surge"), bab(12), str(17), con(15)),
                data -> data.setAdamantineReduction(3),
                data -> data.setAdamantineReduction(0)
        ));

        // --- DIAMOND MIND LINE (mental focus) ---
        flagFeat("diamond_mind_focus", "Diamond Mind Focus",
                "Use INT modifier instead of STR for one melee attack per round.",
                "diamond_mind_line", 0,
                List.of(bab(3), intel(13)));

        // Concentration of Might: INT-based bonus on one melee hit per second.
        FeatRegistry.register(new Feat(
                "concentration_of_might", "Concentration of Might",
                "Once per round deal bonus damage equal to INT modifier on a melee hit.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "diamond_mind_line", 1,
                List.of(reqFeat("diamond_mind_focus", "Diamond Mind Focus"), bab(6), intel(15)),
                data -> data.setConcentrationMightBonus(data.getAbilityScores().getIntMod()),
                data -> data.setConcentrationMightBonus(0)
        ));

        // Perfect Clarity grants a daily INT-attack reroll charge (reset on login).
        FeatRegistry.register(new Feat(
                "perfect_clarity", "Perfect Clarity",
                "Once per day reroll any attack roll using INT modifier as your attack bonus instead of STR or DEX.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "diamond_mind_line", 2,
                List.of(reqFeat("concentration_of_might", "Concentration of Might"), bab(11), intel(17)),
                data -> {
                    data.setAchievementFlag("perfect_clarity_unlocked", true);
                    data.setPerfectClarityCharges(1);
                },
                data -> {
                    data.setAchievementFlag("perfect_clarity_unlocked", false);
                    data.setPerfectClarityCharges(0);
                }
        ));

        // --- SETTING SUN LINE (redirection and throws) ---
        flagFeat("setting_sun_throw", "Setting Sun Throw",
                "When you trip an enemy they are knocked back 3 blocks instead of just falling prone.",
                "setting_sun_line", 0,
                List.of(reqFeat("improved_trip", "Improved Trip"), bab(4), dex(13)));

        flagFeat("counter_charge", "Counter Charge",
                "When an enemy charges you make an immediate attack before they hit dealing bonus damage equal to DEX modifier.",
                "setting_sun_line", 1,
                List.of(reqFeat("setting_sun_throw", "Setting Sun Throw"), bab(8), dex(15)));

        flagFeat("tornado_throw", "Tornado Throw",
                "Throw an enemy into adjacent foes dealing STR modifier damage to all involved.",
                "setting_sun_line", 2,
                List.of(reqFeat("counter_charge", "Counter Charge"), bab(14), dex(17), str(15)));

        // --- STONE DRAGON LINE (immovable defense) ---
        flagFeat("stone_dragon_resilience", "Stone Dragon Resilience",
                "While standing still for one full second gain +3 AC until you move.",
                "stone_dragon_line", 0,
                List.of(bab(3), con(13)));

        // Mountain Hammer: +3 flat melee (average of +2d6) via the shared field.
        FeatRegistry.register(new Feat(
                "mountain_hammer", "Mountain Hammer",
                "Your attacks ignore damage reduction and hardness. +2d6 bonus damage on melee attacks.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "stone_dragon_line", 1,
                List.of(reqFeat("stone_dragon_resilience", "Stone Dragon Resilience"), bab(6), str(15), con(15)),
                data -> data.setMartialDamageBonus(data.getMartialDamageBonus() + 3),
                data -> data.setMartialDamageBonus(data.getMartialDamageBonus() - 3)
        ));

        // Elder Mountain Hammer: a further +6 flat melee (average of +6d6).
        FeatRegistry.register(new Feat(
                "elder_mountain_hammer", "Elder Mountain Hammer",
                "Mountain Hammer bonus damage increases to +6d6 equivalent. Attacks knock back targets 2 blocks.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "stone_dragon_line", 2,
                List.of(reqFeat("mountain_hammer", "Mountain Hammer"), bab(12), str(19), con(17)),
                data -> data.setMartialDamageBonus(data.getMartialDamageBonus() + 6),
                data -> data.setMartialDamageBonus(data.getMartialDamageBonus() - 6)
        ));

        // --- WHITE RAVEN LINE (battle leadership) ---
        flagFeat("white_raven_tactics", "White Raven Tactics",
                "Once per minute grant an ally within 30 blocks a bonus standard action on their next turn.",
                "white_raven_line", 0,
                List.of(bab(6), cha(13)));

        flagFeat("rallying_strike", "Rallying Strike",
                "On a critical hit grant all allies within 30 blocks +2 to attack rolls for 5 seconds.",
                "white_raven_line", 1,
                List.of(reqFeat("white_raven_tactics", "White Raven Tactics"), bab(10), cha(15)));

        // Order Forged from Chaos grants a daily party-buff charge (reset on login).
        FeatRegistry.register(new Feat(
                "order_forged_from_chaos", "Order Forged from Chaos",
                "Your presence in battle grants all allies within 30 blocks immunity to fear and +3 to all saves for 10 seconds once per day.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                "white_raven_line", 2,
                List.of(reqFeat("rallying_strike", "Rallying Strike"), bab(14), cha(17), level(12)),
                data -> {
                    data.setAchievementFlag("order_forged_unlocked", true);
                    data.setOrderForgedCharges(1);
                },
                data -> {
                    data.setAchievementFlag("order_forged_unlocked", false);
                    data.setOrderForgedCharges(0);
                }
        ));

        // --- STANDALONE TOME OF BATTLE FEATS ---
        // Martial Study: general martial training, +1 to the shared melee bonus.
        FeatRegistry.register(new Feat(
                "martial_study", "Martial Study",
                "Gain familiarity with one martial discipline unlocking its passive bonuses and future maneuver access.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                null, 0,
                List.of(bab(3)),
                data -> {
                    data.setAchievementFlag("martial_study_unlocked", true);
                    data.setMartialDamageBonus(data.getMartialDamageBonus() + 1);
                },
                data -> {
                    data.setAchievementFlag("martial_study_unlocked", false);
                    data.setMartialDamageBonus(data.getMartialDamageBonus() - 1);
                }
        ));

        flagFeat("martial_stance", "Martial Stance",
                "Adopt a martial stance granting passive combat bonuses while maintained.",
                null, 0,
                List.of(reqFeat("martial_study", "Martial Study"), bab(5)));

        flagFeat("adaptive_style", "Adaptive Style",
                "Switch between martial stances as a swift action instead of a full action.",
                null, 0,
                List.of(reqFeat("martial_stance", "Martial Stance"), bab(8), intel(13)));

        // Advanced Combat Training: +2 Fortitude and +3 HP via toughness stacking.
        FeatRegistry.register(new Feat(
                "advanced_combat_training", "Advanced Combat Training",
                "Your body is conditioned for sustained combat. +2 to Fortitude saves and +4 HP.",
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                null, 0,
                List.of(bab(6), str(15), con(13)),
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() + 2);
                    data.setToughnessFeatCount(data.getToughnessFeatCount() + 1);
                },
                data -> {
                    data.setFeatFortBonus(data.getFeatFortBonus() - 2);
                    data.setToughnessFeatCount(data.getToughnessFeatCount() - 1);
                }
        ));

        flagFeat("superior_combat_expertise", "Superior Combat Expertise",
                "Combat Expertise AC bonus increases by +2 and has no minimum attack penalty.",
                null, 0,
                List.of(reqFeat("combat_expertise", "Combat Expertise"), bab(8), intel(15)));

        // Blood of the Martyr: flag only here; bloodyFuryBonus is reconciled each
        // tick by FeatEffectHandler based on the 50% HP threshold.
        flagFeatFlag("blood_of_the_martyr", "blood_of_martyr_unlocked", "Blood of the Martyr",
                "When below 50% HP gain +2 to attack rolls and +2 to damage rolls from battle fury.",
                null, 0,
                List.of(bab(4), con(15)));
    }

    // --- Helpers (all Tome of Battle feats are COMBAT category) ---

    private static void flagFeat(String id, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    private static void flagFeatFlag(String id, String flag, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.COMBAT, FeatSource.TOME_OF_BATTLE,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    private static FeatPrerequisite reqFeat(String id, String name) {
        return new FeatPrerequisite.RequiredFeatPrerequisite(id, name);
    }

    private static FeatPrerequisite level(int minimum) {
        return new FeatPrerequisite.LevelPrerequisite(minimum);
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

    private static FeatPrerequisite cha(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CHARISMA, minimum);
    }
}
