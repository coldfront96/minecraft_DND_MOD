package com.deadmind.dndmods.feat.content;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.feat.*;

import java.util.List;

/**
 * Complete Champion feat content pass — the sixth expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_CHAMPION}. The aura feats
 * extend the existing {@code "aura_line"} chain from {@link CompleteDivineFeats}
 * (exact string match). New chains: smite, devoted, and champion combat.
 *
 * <p>Most feats are flag only — their active aura / party / Cleric mechanics
 * arrive in later passes, gated on the {@code "<id>_unlocked"} flag. The effects
 * live this pass feed real fields on
 * {@link com.deadmind.dndmods.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code holy_resilience} — flat WIS-based damage reduction (active in
 *       {@link FeatEffectHandler}).</li>
 *   <li>{@code zealous_surge} — daily divine death-save (active in
 *       {@link FeatEffectHandler}, ordered after Half-Orc Ferocity).</li>
 *   <li>{@code stand_firm} — knockback immunity (active in {@link FeatEffectHandler}).</li>
 *   <li>{@code smite_evil} line — daily smite charges + Cleric-level damage bonus.</li>
 *   <li>{@code sacred_healing_devotion} — WIS-based heal bonus.</li>
 *   <li>{@code faith_unswerving} — partial Will bonus; {@code sacred_defender} —
 *       reuses naturalArmorBonus; {@code divine_impetus} — daily speed charge.</li>
 * </ul>
 */
public class CompleteChampionFeats {

    public static void register() {

        // --- SMITE LINE (smite_evil -> improved_smite / smite_heretic -> greater_smite) ---
        // Smite Evil grants the first daily smite charge.
        FeatRegistry.register(new Feat(
                "smite_evil", "Smite Evil",
                "Once per day add CHA modifier to attack and WIS modifier to damage against an evil target.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                "smite_line", 0,
                List.of(cls(DnDClass.CLERIC), cha(13), level(3)),
                data -> {
                    data.setAchievementFlag("smite_evil_unlocked", true);
                    data.setSmiteCharges(1);
                },
                data -> {
                    data.setAchievementFlag("smite_evil_unlocked", false);
                    data.setSmiteCharges(0);
                }
        ));

        // Improved Smite adds a second charge and a Cleric-level radiant bonus.
        FeatRegistry.register(new Feat(
                "improved_smite", "Improved Smite",
                "Smite Evil may be used twice per day and deals bonus radiant damage equal to Cleric level.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                "smite_line", 1,
                List.of(reqFeat("smite_evil", "Smite Evil"), cls(DnDClass.CLERIC), level(6)),
                data -> {
                    data.setAchievementFlag("improved_smite_unlocked", true);
                    data.setSmiteCharges(data.getSmiteCharges() + 1);
                    data.setSmiteDamageBonus(data.getClassLevel(DnDClass.CLERIC));
                },
                data -> {
                    data.setAchievementFlag("improved_smite_unlocked", false);
                    data.setSmiteCharges(data.getSmiteCharges() - 1);
                    data.setSmiteDamageBonus(0);
                }
        ));

        // Greater Smite adds a third charge.
        FeatRegistry.register(new Feat(
                "greater_smite", "Greater Smite",
                "Smite Evil three times per day. On a critical hit the target is blinded for 3 seconds.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                "smite_line", 2,
                List.of(reqFeat("improved_smite", "Improved Smite"), cls(DnDClass.CLERIC), level(12), cha(15)),
                data -> {
                    data.setAchievementFlag("greater_smite_unlocked", true);
                    data.setSmiteCharges(data.getSmiteCharges() + 1);
                },
                data -> {
                    data.setAchievementFlag("greater_smite_unlocked", false);
                    data.setSmiteCharges(data.getSmiteCharges() - 1);
                }
        ));

        flagFeat("smite_heretic", FeatCategory.DIVINE, "Smite Heretic",
                "Apply Smite Evil against any creature opposing your deity not just evil aligned ones.",
                "smite_line", 1,
                List.of(reqFeat("smite_evil", "Smite Evil"), cls(DnDClass.CLERIC), level(5)));

        // --- AURA LINE EXTENSIONS (Complete Divine "aura_line") ---
        flagFeat("aura_of_menace", FeatCategory.DIVINE, "Aura of Menace",
                "Enemies within 10 blocks take -2 to attack rolls and saves while your aura is active.",
                "aura_line", 2,
                List.of(reqFeat("aura_of_faith", "Aura of Faith"), cls(DnDClass.CLERIC), level(10), cha(15)));

        flagFeat("aura_of_justice", FeatCategory.DIVINE, "Aura of Justice",
                "Allies within 10 blocks gain the ability to Smite Evil once using your CHA modifier.",
                "aura_line", 3,
                List.of(reqFeat("aura_of_vitality", "Aura of Vitality"), cls(DnDClass.CLERIC), level(15)));

        // --- DEVOTED LINE (parallel roots devoted_tracker + sacred_healing_devotion) ---
        flagFeat("devoted_tracker", FeatCategory.DIVINE, "Devoted Tracker",
                "Your animal companion and favored enemy bonuses stack. Companion gains favored enemy bonuses on attacks.",
                "devoted_line", 0,
                List.of(cls(DnDClass.RANGER), wis(13), level(4)));

        flagFeat("holy_mount", FeatCategory.DIVINE, "Holy Mount",
                "Your mount gains celestial template — +2 STR and CON, resistance to acid/cold/lightning, darkvision.",
                "devoted_line", 1,
                List.of(reqFeat("devoted_tracker", "Devoted Tracker"), cls(DnDClass.RANGER), level(8)));

        // Sacred Healing Devotion grants a WIS-based heal bonus field (no flag).
        FeatRegistry.register(new Feat(
                "sacred_healing_devotion", "Sacred Healing Devotion",
                "Healing spells cast by you heal an additional WIS modifier HP.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                "devoted_line", 0,
                List.of(cls(DnDClass.CLERIC), wis(15), level(5)),
                data -> data.setDevotionHealingBonusWis(data.getAbilityScores().getWisMod()),
                data -> data.setDevotionHealingBonusWis(0)
        ));

        // --- CHAMPION COMBAT LINE (divine_interception -> shield_of_the_faithful / holy_strike -> stand_firm) ---
        flagFeat("divine_interception", FeatCategory.COMBAT, "Divine Interception",
                "Once per round intercept an attack targeting an adjacent ally taking the hit yourself.",
                "champion_combat_line", 0,
                List.of(cls(DnDClass.CLERIC), bab(4), cha(13)));

        // Explicit unlock-flag key "shield_of_faithful_unlocked" (drops "the").
        flagFeatFlag("shield_of_the_faithful", "shield_of_faithful_unlocked", FeatCategory.COMBAT, "Shield of the Faithful",
                "Allies adjacent to you gain +2 AC from your divine protection.",
                "champion_combat_line", 1,
                List.of(reqFeat("divine_interception", "Divine Interception"), cls(DnDClass.CLERIC), bab(6)));

        flagFeat("holy_strike", FeatCategory.COMBAT, "Holy Strike",
                "Your melee attacks deal bonus radiant damage equal to WIS modifier against undead and evil outsiders.",
                "champion_combat_line", 1,
                List.of(reqFeat("divine_interception", "Divine Interception"), cls(DnDClass.CLERIC), bab(6), wis(13)));

        // Stand Firm grants knockback immunity via a boolean checked in
        // FeatEffectHandler's LivingKnockBackEvent hook.
        FeatRegistry.register(new Feat(
                "stand_firm", "Stand Firm",
                "Cannot be bull rushed, tripped, or knocked prone while wearing armor.",
                FeatCategory.COMBAT, FeatSource.COMPLETE_CHAMPION,
                "champion_combat_line", 2,
                List.of(reqFeat("shield_of_the_faithful", "Shield of the Faithful"),
                        cls(DnDClass.CLERIC), str(15), bab(9)),
                data -> {
                    data.setAchievementFlag("stand_firm_unlocked", true);
                    data.setStandFirmUnlocked(true);
                },
                data -> {
                    data.setAchievementFlag("stand_firm_unlocked", false);
                    data.setStandFirmUnlocked(false);
                }
        ));

        // --- DEVOTION FEATS (no chain) ---
        // Faith Unswerving grants a partial Will bonus now; full fear immunity later.
        FeatRegistry.register(new Feat(
                "faith_unswerving", "Faith Unswerving",
                "Immune to fear and once per day automatically succeed on a Will save.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                null, 0,
                List.of(cls(DnDClass.CLERIC), wis(15), level(7)),
                data -> {
                    data.setAchievementFlag("faith_unswerving_unlocked", true);
                    data.setFeatWillBonus(data.getFeatWillBonus() + 2);
                },
                data -> {
                    data.setAchievementFlag("faith_unswerving_unlocked", false);
                    data.setFeatWillBonus(data.getFeatWillBonus() - 2);
                }
        ));

        flagFeatFlag("champion_of_the_wild", "champion_of_wild_unlocked", FeatCategory.DIVINE, "Champion of the Wild",
                "Animal companions within 30 feet gain +2 to attack and damage rolls.",
                null, 0,
                List.of(cls(DnDClass.RANGER), level(6), wis(13)));

        // Divine Impetus grants a daily burst-of-speed charge (reset on login).
        FeatRegistry.register(new Feat(
                "divine_impetus", "Divine Impetus",
                "Once per day move at double speed for 3 seconds as a divine burst of speed.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(5), cha(13)),
                data -> {
                    data.setAchievementFlag("divine_impetus_unlocked", true);
                    data.setDivineImpetusCharges(1);
                },
                data -> {
                    data.setAchievementFlag("divine_impetus_unlocked", false);
                    data.setDivineImpetusCharges(0);
                }
        ));

        // Sacred Defender reuses the existing naturalArmorBonus field (no flag).
        FeatRegistry.register(new Feat(
                "sacred_defender", "Sacred Defender",
                "+2 natural armor AC while wielding a holy symbol or divine focus.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                null, 0,
                List.of(cls(DnDClass.CLERIC), con(13), level(5)),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() + 2),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() - 2)
        ));

        flagFeatFlag("martyr_s_cry", "martyrs_cry_unlocked", FeatCategory.DIVINE, "Martyr's Cry",
                "When reduced below 25% HP emit a divine cry granting allies within 20 blocks +2 to attack and saves for 10 seconds.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), level(8), cha(15)));

        // Holy Resilience grants flat WIS-based damage reduction (active in
        // FeatEffectHandler; no flag — the reduction value gates the effect).
        FeatRegistry.register(new Feat(
                "holy_resilience", "Holy Resilience",
                "Reduce all incoming damage by WIS modifier minimum 1.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                null, 0,
                List.of(cls(DnDClass.CLERIC), con(15), level(6)),
                data -> data.setHolyResilienceReduction(Math.max(1, data.getAbilityScores().getWisMod())),
                data -> data.setHolyResilienceReduction(0)
        ));

        // Zealous Surge grants a daily divine death-save (active in
        // FeatEffectHandler, ordered after Half-Orc Ferocity).
        FeatRegistry.register(new Feat(
                "zealous_surge", "Zealous Surge",
                "Once per day when reduced to 0 HP heal for WIS modifier x 3 HP instead of dying.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_CHAMPION,
                null, 0,
                List.of(cls(DnDClass.CLERIC), level(10), wis(15)),
                data -> {
                    data.setAchievementFlag("zealous_surge_unlocked", true);
                    data.setZealousSurgeCharges(1);
                },
                data -> {
                    data.setAchievementFlag("zealous_surge_unlocked", false);
                    data.setZealousSurgeCharges(0);
                }
        ));
    }

    // --- Helpers ---

    /** Registers a Complete Champion feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, FeatCategory category, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", category, name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Champion flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, FeatCategory category, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                category, FeatSource.COMPLETE_CHAMPION,
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

    private static FeatPrerequisite con(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CONSTITUTION, minimum);
    }

    private static FeatPrerequisite wis(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.WISDOM, minimum);
    }

    private static FeatPrerequisite cha(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CHARISMA, minimum);
    }
}
