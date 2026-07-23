package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;

import java.util.List;

/**
 * Complete Adventurer feat content pass — the fourth expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_ADVENTURER}. Skill-rank-only
 * feats from the source are intentionally omitted (this mod has no skill-rank
 * system); where a source feat mixed skill ranks with real prerequisites, the
 * skill ranks are dropped and the rest kept. New chains: rogue combat, ranger
 * combat, mobility/evasion, shadow, and trap.
 *
 * <p>Most feats are flag only — their active mechanics arrive in the Rogue /
 * Ranger ability passes and are gated on the {@code "<id>_unlocked"} flag. The
 * effects that are live this pass feed real fields on
 * {@link com.khimairacraft.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code fast_movement} — flat MOVEMENT_SPEED bonus reconciled in
 *       {@link FeatEffectHandler} (one of two active effects this pass).</li>
 *   <li>{@code evasion} / {@code improved_evasion} — area-damage Reflex-save
 *       negation / halving in {@link FeatEffectHandler} (the other active effect).</li>
 *   <li>{@code improved_favored_enemy} — favoredEnemyBonus.</li>
 *   <li>{@code trap_sense} — +2 featRefBonus.</li>
 *   <li>{@code craven} — intentional -2 featFortBonus tradeoff.</li>
 *   <li>{@code danger_sense} — +2 dangerSenseInitBonus (shown on the sheet).</li>
 * </ul>
 */
public class CompleteAdventurerFeats {

    public static void register() {

        // --- ROGUE COMBAT LINE (parallel roots acrobatic_strike + deadly_defense) ---
        flagFeat("acrobatic_strike", FeatCategory.COMBAT, "Acrobatic Strike",
                "If you move at least 10 feet before attacking gain +4 to hit and deal sneak attack damage even if target is not flanked.",
                "rogue_combat_line", 0,
                List.of(cls(DnDClass.ROGUE), dex(13), bab(4)));

        flagFeat("deadly_defense", FeatCategory.COMBAT, "Deadly Defense",
                "When fighting defensively deal +1d6 sneak attack damage on any hit.",
                "rogue_combat_line", 0,
                List.of(cls(DnDClass.ROGUE), bab(3)));

        flagFeat("deft_strike", FeatCategory.COMBAT, "Deft Strike",
                "Your sneak attacks deal an additional die of damage and ignore 5 points of damage reduction.",
                "rogue_combat_line", 1,
                List.of(reqFeat("acrobatic_strike", "Acrobatic Strike"), cls(DnDClass.ROGUE), dex(15), bab(6)));

        flagFeat("brutal_strike_rogue", FeatCategory.COMBAT, "Brutal Strike (Rogue)",
                "Once per round your sneak attack stuns the target for one round on a failed Fortitude save.",
                "rogue_combat_line", 2,
                List.of(reqFeat("deft_strike", "Deft Strike"), cls(DnDClass.ROGUE), bab(10)));

        // --- RANGER COMBAT LINE (parallel roots hunter_s_mercy + ranged_disarm) ---
        flagFeatFlag("hunter_s_mercy", "hunters_mercy_unlocked", FeatCategory.COMBAT, "Hunter's Mercy",
                "Your next bow attack automatically confirms a critical hit if it hits.",
                "ranger_combat_line", 0,
                List.of(cls(DnDClass.RANGER), bab(3)));

        flagFeat("ranged_disarm", FeatCategory.COMBAT, "Ranged Disarm",
                "Attempt a disarm via ranged attack without provoking attacks of opportunity.",
                "ranger_combat_line", 0,
                List.of(cls(DnDClass.RANGER), reqFeat("point_blank_shot", "Point Blank Shot"), dex(13)));

        // Improved Favored Enemy grants a stacking attack/damage bonus field.
        FeatRegistry.register(new Feat(
                "improved_favored_enemy", "Improved Favored Enemy",
                "Add +2 to attack and damage rolls against favored enemies on top of existing favored enemy bonus.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_ADVENTURER,
                "ranger_combat_line", 1,
                List.of(reqFeat("hunter_s_mercy", "Hunter's Mercy"), cls(DnDClass.RANGER), bab(6)),
                data -> data.setFavoredEnemyBonus(data.getFavoredEnemyBonus() + 2),
                data -> data.setFavoredEnemyBonus(data.getFavoredEnemyBonus() - 2)
        ));

        flagFeat("penetrating_shot", FeatCategory.COMBAT, "Penetrating Shot",
                "Your ranged attacks pass through one target and hit the next enemy behind them in line.",
                "ranger_combat_line", 1,
                List.of(cls(DnDClass.RANGER), reqFeat("point_blank_shot", "Point Blank Shot"), bab(5)));

        flagFeat("swift_ambusher", FeatCategory.COMBAT, "Swift Ambusher",
                "Stack Ranger and Rogue sneak attack dice when ambushing a flat-footed target.",
                "ranger_combat_line", 2,
                List.of(reqFeat("improved_favored_enemy", "Improved Favored Enemy"), cls(DnDClass.RANGER), level(8)));

        // --- MOBILITY AND EVASION LINE (parallel roots leaping_attack + fast_movement) ---
        flagFeat("leaping_attack", FeatCategory.COMBAT, "Leaping Attack",
                "Jump and attack in the same action dealing +2 damage on the hit.",
                "mobility_line", 0,
                List.of(dex(13), bab(3)));

        // Fast Movement is flag-only here; FeatEffectHandler applies the actual
        // MOVEMENT_SPEED modifier each tick. One of two active effects this pass.
        flagFeat("fast_movement", FeatCategory.COMBAT, "Fast Movement",
                "+10 feet to base movement speed.",
                "mobility_line", 0,
                List.of(dex(15), bab(2)));

        flagFeat("ground_fighting", FeatCategory.COMBAT, "Ground Fighting",
                "No penalty to attack while prone and can trip standing foes from prone position.",
                "mobility_line", 1,
                List.of(reqFeat("leaping_attack", "Leaping Attack"),
                        reqFeat("improved_unarmed_strike", "Improved Unarmed Strike")));

        flagFeat("bounding_charge", FeatCategory.COMBAT, "Bounding Charge",
                "Charge over difficult terrain and up slopes without movement penalty.",
                "mobility_line", 1,
                List.of(reqFeat("fast_movement", "Fast Movement"), dex(15), bab(4)));

        // Evasion stores a boolean and is checked in FeatEffectHandler's
        // area-damage Reflex hook. One of two active effects this pass.
        FeatRegistry.register(new Feat(
                "evasion", "Evasion",
                "On a successful Reflex save take no damage instead of half from area effects.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_ADVENTURER,
                "mobility_line", 2,
                List.of(reqFeat("bounding_charge", "Bounding Charge"), cls(DnDClass.ROGUE), dex(15)),
                data -> {
                    data.setAchievementFlag("evasion_unlocked", true);
                    data.setEvasionUnlocked(true);
                },
                data -> {
                    data.setAchievementFlag("evasion_unlocked", false);
                    data.setEvasionUnlocked(false);
                }
        ));

        flagFeat("improved_evasion", FeatCategory.COMBAT, "Improved Evasion",
                "On a failed Reflex save still take only half damage from area effects.",
                "mobility_line", 3,
                List.of(reqFeat("evasion", "Evasion"), cls(DnDClass.ROGUE), dex(17), level(10)));

        // --- SHADOW AND STEALTH LINE ---
        flagFeat("shadow_striker", FeatCategory.COMBAT, "Shadow Striker",
                "Deal sneak attack damage against targets in dim light or darkness even if not flanking.",
                "shadow_line", 0,
                List.of(cls(DnDClass.ROGUE), dex(13)));

        flagFeat("shadowstep", FeatCategory.COMBAT, "Shadowstep",
                "Teleport up to 10 blocks between areas of dim light or darkness once per minute.",
                "shadow_line", 1,
                List.of(reqFeat("shadow_striker", "Shadow Striker"), cls(DnDClass.ROGUE), dex(15), level(6)));

        flagFeat("darkstalker", FeatCategory.COMBAT, "Darkstalker",
                "Hide from creatures with blindsense, tremorsense, and scent.",
                "shadow_line", 1,
                List.of(reqFeat("shadow_striker", "Shadow Striker"), cls(DnDClass.ROGUE)));

        // --- TRAPFINDING LINE ---
        flagFeat("trapfinding", FeatCategory.GENERAL, "Trapfinding",
                "Find and disarm magical traps. In Minecraft context: detect hidden tripwires and pressure plates within 8 blocks via particle effect highlight.",
                "trap_line", 0,
                List.of(cls(DnDClass.ROGUE)));

        flagFeat("improved_trapfinding", FeatCategory.GENERAL, "Improved Trapfinding",
                "Disarm magical traps at twice the normal speed. Detection range increases to 16 blocks.",
                "trap_line", 1,
                List.of(reqFeat("trapfinding", "Trapfinding"), cls(DnDClass.ROGUE), level(5)));

        // Trap Sense grants a real Reflex-save bonus (no flag).
        FeatRegistry.register(new Feat(
                "trap_sense", "Trap Sense",
                "+2 Reflex saves vs traps and +2 AC against trap attacks.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ADVENTURER,
                "trap_line", 1,
                List.of(reqFeat("trapfinding", "Trapfinding"), cls(DnDClass.ROGUE)),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() + 2),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() - 2)
        ));

        // --- STANDALONE COMPLETE ADVENTURER FEATS (no chain) ---
        flagFeat("quick_reconnoiter", FeatCategory.GENERAL, "Quick Reconnoiter",
                "Make Spot and Listen checks as free actions. In Minecraft context: detect all hostile mobs within 24 blocks passively shown on a compass-style HUD element.",
                null, 0,
                List.of(dex(13), bab(1)));

        flagFeat("hear_the_unseen", FeatCategory.GENERAL, "Hear the Unseen",
                "Make melee attacks against invisible creatures at only -4 penalty instead of -8.",
                null, 0,
                List.of(wis(13)));

        // Craven trades a -2 Fortitude penalty for bonus sneak damage; the
        // negative featFortBonus is intentional.
        FeatRegistry.register(new Feat(
                "craven", "Craven",
                "Add character level to sneak attack damage but take -2 to saves vs fear.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_ADVENTURER,
                null, 0,
                List.of(cls(DnDClass.ROGUE)),
                data -> {
                    data.setAchievementFlag("craven_unlocked", true);
                    data.setFeatFortBonus(data.getFeatFortBonus() - 2);
                },
                data -> {
                    data.setAchievementFlag("craven_unlocked", false);
                    data.setFeatFortBonus(data.getFeatFortBonus() + 2);
                }
        ));

        flagFeat("cunning_strike", FeatCategory.COMBAT, "Cunning Strike",
                "Apply a condition (blinded, entangled, or shaken) on a successful sneak attack instead of dealing extra damage.",
                null, 0,
                List.of(cls(DnDClass.ROGUE), bab(5), intel(13)));

        flagFeat("swift_tracker", FeatCategory.GENERAL, "Swift Tracker",
                "Track at full speed without penalty. In Minecraft context: footprint particles persist 30 seconds longer for this player.",
                null, 0,
                List.of(cls(DnDClass.RANGER), reqFeat("track", "Track")));

        flagFeat("wild_cohort", FeatCategory.GENERAL, "Wild Cohort",
                "Gain an animal companion as a Druid of your Ranger level.",
                null, 0,
                List.of(cls(DnDClass.RANGER), level(3)));

        flagFeat("flyby_attack", FeatCategory.COMBAT, "Flyby Attack",
                "When mounted on a flying mount make one attack at any point during movement without stopping.",
                null, 0,
                List.of(bab(4)));

        flagFeat("open_minded", FeatCategory.GENERAL, "Open Minded",
                "Gain 5 additional skill points. In Minecraft context grants a permanent +1 to INT modifier for feat prerequisite checks only.",
                null, 0,
                List.of());

        // Danger Sense grants a +2 initiative bonus shown on the character sheet.
        FeatRegistry.register(new Feat(
                "danger_sense", "Danger Sense",
                "+2 to initiative and cannot be surprised.",
                FeatCategory.GENERAL, FeatSource.COMPLETE_ADVENTURER,
                null, 0,
                List.of(wis(13), bab(3)),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() + 2),
                data -> data.setDangerSenseInitBonus(data.getDangerSenseInitBonus() - 2)
        ));

        flagFeat("defensive_fighting", FeatCategory.COMBAT, "Defensive Fighting",
                "Fight defensively without penalty to attack rolls. Gain +3 dodge bonus to AC when fighting defensively.",
                null, 0,
                List.of(bab(2), dex(13)));

        flagFeat("overpowering_strike", FeatCategory.COMBAT, "Overpowering Strike",
                "Deal bonus damage equal to half STR modifier on any attack that exceeds enemy AC by 5 or more.",
                null, 0,
                List.of(str(15), bab(5)));
    }

    // --- Helpers ---

    /** Registers a Complete Adventurer feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", category, name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Adventurer flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, FeatCategory category, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.COMPLETE_ADVENTURER,
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
