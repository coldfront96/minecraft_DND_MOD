package com.deadmind.dndmods.feat.content;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.feat.*;

import java.util.List;

/**
 * Complete Divine feat content pass — the second expanded-sourcebook pass.
 *
 * <p>All feats here use {@link FeatSource#COMPLETE_DIVINE}. Several extend the
 * existing PHB divine chains by reusing the exact {@code chainGroup} string from
 * the PHB registration in {@link CoreFeats} — {@code "turning_line"} and
 * {@code "holy_warrior_line"}. New chains introduced here: sacred healing,
 * divine spell power, ranger divine, aura, and domain.
 *
 * <p>Most feats are flag only — their active mechanics arrive in the Cleric /
 * party / spell-system ability passes and are gated on the
 * {@code "<id>_unlocked"} achievement flag. The handful with mechanical impact
 * this pass feed real fields on {@link com.deadmind.dndmods.playerdata.DnDPlayerData}:
 * <ul>
 *   <li>{@code holy_warrior} — the only active damage effect; stores a WIS-derived
 *       bonus applied in {@link FeatEffectHandler} for Clerics.</li>
 *   <li>{@code improved_turning} — grants a +4 effective turning-level bonus.</li>
 *   <li>{@code domain_focus} — grants +2 to domain spell save DCs.</li>
 *   <li>{@code pious_defiance} — grants a daily charge reset on login.</li>
 *   <li>{@code divine_spell_power} — declares the caster-level field; the active
 *       toggle is deferred to the Cleric ability pass (flag + field only here).</li>
 * </ul>
 */
public class CompleteDivineFeats {

    public static void register() {

        // --- DIVINE MIGHT LINE EXTENSIONS (PHB "turning_line") ---
        // Parallel order-2 branches alongside the PHB order-0/1 turning feats.
        flagFeat("divine_accuracy", "Divine Accuracy",
                "Spend a turning attempt to gain +4 sacred bonus to ranged attack rolls for one minute.",
                "turning_line", 2,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));

        flagFeat("divine_cleansing", "Divine Cleansing",
                "Spend a turning attempt to grant yourself and allies within 60 feet a +4 bonus on saves vs poison and disease for one minute.",
                "turning_line", 2,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));

        flagFeat("divine_fortune", "Divine Fortune",
                "Spend a turning attempt to gain a +4 sacred bonus on your next saving throw.",
                "turning_line", 2,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        flagFeat("divine_armor", "Divine Armor",
                "Spend a turning attempt to gain sacred bonus to AC equal to CHA modifier for one minute.",
                "turning_line", 2,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));

        // --- SACRED HEALING LINE (new, sequential) ---
        flagFeat("sacred_healing", "Sacred Healing",
                "Spend a turning attempt to heal all allies within 10 feet for 1d6 HP.",
                "sacred_healing_line", 0,
                List.of(cls(DnDClass.CLERIC), wis(13), level(3)));

        flagFeat("mass_sacred_healing", "Mass Sacred Healing",
                "Sacred Healing now affects all allies within 30 feet and heals 2d6 HP.",
                "sacred_healing_line", 1,
                List.of(reqFeat("sacred_healing", "Sacred Healing"), cls(DnDClass.CLERIC), wis(15), level(7)));

        flagFeat("empowered_healing", "Empowered Sacred Healing",
                "Sacred Healing heals maximum possible HP and removes one negative effect.",
                "sacred_healing_line", 2,
                List.of(reqFeat("mass_sacred_healing", "Mass Sacred Healing"), cls(DnDClass.CLERIC), level(12)));

        // --- DIVINE SPELL POWER LINE (new, sequential) ---
        // Flag and field only this pass; the active caster-level toggle is
        // deferred to the Cleric ability pass.
        flagFeat("divine_spell_power", "Divine Spell Power",
                "Spend a turning attempt to gain +1 caster level for one minute.",
                "divine_spell_power_line", 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        flagFeat("greater_divine_spell_power", "Greater Divine Spell Power",
                "Spend a turning attempt to gain +3 caster levels for one minute.",
                "divine_spell_power_line", 1,
                List.of(reqFeat("divine_spell_power", "Divine Spell Power"), cls(DnDClass.CLERIC), level(10)));

        // --- TURN UNDEAD EXTENSIONS (PHB "turning_line") ---
        // Improved Turning grants a +4 effective turning-level bonus.
        FeatRegistry.register(new Feat(
                "improved_turning", "Improved Turning",
                "Turn undead as if your Cleric level were 4 higher.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_DIVINE,
                "turning_line", 1,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")),
                data -> {
                    data.setAchievementFlag("improved_turning_unlocked", true);
                    data.setTurningLevelBonus(data.getTurningLevelBonus() + 4);
                },
                data -> {
                    data.setAchievementFlag("improved_turning_unlocked", false);
                    data.setTurningLevelBonus(data.getTurningLevelBonus() - 4);
                }
        ));

        flagFeat("intensify_turning", "Intensify Turning",
                "Turned undead are destroyed rather than merely turned.",
                "turning_line", 2,
                List.of(reqFeat("improved_turning", "Improved Turning"), cls(DnDClass.CLERIC), cha(15)));

        flagFeat("reach_turning", "Reach Turning",
                "Spend two turning attempts to turn undead at double the normal range.",
                "turning_line", 2,
                List.of(reqFeat("extra_turning", "Extra Turning"), cls(DnDClass.CLERIC)));

        // --- NATURE AND RANGER DIVINE LINE (new; parallel roots) ---
        flagFeat("natural_bond", "Natural Bond",
                "Your animal companion gains +3 effective druid levels for determining companion abilities.",
                "ranger_divine_line", 0,
                List.of(cls(DnDClass.RANGER)));

        flagFeat("beast_totem", "Beast Totem",
                "Choose an animal totem granting a passive bonus: Bear (+2 CON saves), Wolf (trip on hit), Eagle (+2 Perception).",
                "ranger_divine_line", 1,
                List.of(reqFeat("natural_bond", "Natural Bond"), cls(DnDClass.RANGER), wis(13)));

        flagFeat("divine_hunter", "Divine Hunter",
                "Add WIS modifier to damage rolls against favored enemies.",
                "ranger_divine_line", 0,
                List.of(cls(DnDClass.RANGER), wis(13)));

        flagFeat("sacred_strike", "Sacred Strike",
                "Your ranged attacks against evil outsiders deal bonus radiant damage equal to WIS modifier.",
                "ranger_divine_line", 1,
                List.of(reqFeat("divine_hunter", "Divine Hunter"), cls(DnDClass.RANGER), bab(4)));

        // --- AURA LINE (new) ---
        flagFeat("aura_of_courage", "Aura of Courage",
                "Allies within 10 blocks are immune to fear effects.",
                "aura_line", 0,
                List.of(cls(DnDClass.CLERIC), level(5)));

        flagFeat("aura_of_faith", "Aura of Faith",
                "Allies within 10 blocks gain +2 to all saving throws.",
                "aura_line", 1,
                List.of(reqFeat("aura_of_courage", "Aura of Courage"), cls(DnDClass.CLERIC), level(8)));

        flagFeat("aura_of_terror", "Aura of Terror",
                "Enemies within 10 blocks must make a Will save or become frightened.",
                "aura_line", 1,
                List.of(reqFeat("aura_of_courage", "Aura of Courage"), cls(DnDClass.CLERIC), level(8), cha(15)));

        flagFeat("aura_of_vitality", "Aura of Vitality",
                "Allies within 10 blocks regenerate 1 HP every 40 ticks while in the aura.",
                "aura_line", 2,
                List.of(reqFeat("aura_of_faith", "Aura of Faith"), cls(DnDClass.CLERIC), level(12)));

        // --- DOMAIN POWER LINE (new) ---
        // Domain Focus grants +2 to domain spell save DCs (field, no flag).
        FeatRegistry.register(new Feat(
                "domain_focus", "Domain Focus",
                "+2 to save DCs for spells from your chosen domain.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_DIVINE,
                "domain_line", 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_domain", "Extra Domain")),
                data -> data.setDomainSaveDcBonus(data.getDomainSaveDcBonus() + 2),
                data -> data.setDomainSaveDcBonus(data.getDomainSaveDcBonus() - 2)
        ));

        flagFeat("domain_mastery", "Domain Mastery",
                "Cast domain spells without expending spell slots once per day each.",
                "domain_line", 1,
                List.of(reqFeat("domain_focus", "Domain Focus"), cls(DnDClass.CLERIC), level(8)));

        // --- HOLY WARRIOR LINE EXTENSION (PHB "holy_warrior_line") ---
        flagFeatFlag("divine_ward_greater", "greater_divine_ward_unlocked", "Greater Divine Ward",
                "Divine Ward now protects from two attacks and lasts until triggered.",
                "holy_warrior_line", 2,
                List.of(reqFeat("divine_ward", "Divine Ward"), cls(DnDClass.CLERIC), level(10)));

        // --- STANDALONE COMPLETE DIVINE FEATS (no chain) ---
        flagFeat("holy_ki_strike", "Holy Ki Strike",
                "Your unarmed strikes are treated as holy weapons dealing bonus radiant damage to undead and evil outsiders.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("improved_unarmed_strike", "Improved Unarmed Strike"), level(6)));

        // Pious Defiance grants a daily auto-success charge (reset on login).
        FeatRegistry.register(new Feat(
                "pious_defiance", "Pious Defiance",
                "Once per day automatically succeed on one saving throw vs a mind-affecting effect.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_DIVINE,
                null, 0,
                List.of(cls(DnDClass.CLERIC), wis(15)),
                data -> {
                    data.setAchievementFlag("pious_defiance_unlocked", true);
                    data.setPiousDefianceCharges(1);
                },
                data -> {
                    data.setAchievementFlag("pious_defiance_unlocked", false);
                    data.setPiousDefianceCharges(0);
                }
        ));

        flagFeat("divine_justice", "Divine Justice",
                "When you smite an evil creature that creature takes a -2 penalty on attacks against you for 1 minute.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(5)));

        flagFeat("aligned_strike", "Aligned Strike",
                "Your weapons count as aligned for overcoming damage reduction.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(3)));

        flagFeat("investiture", "Investiture",
                "Grant a willing ally the ability to turn undead once using your turning ability.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), level(7), cha(13)));

        flagFeat("faith_healing", "Faith Healing",
                "Cure spells cast on yourself heal maximum possible HP.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), wis(13)));

        flagFeat("divine_metamagic", "Divine Metamagic",
                "Spend turning attempts to apply a metamagic feat to a divine spell without increasing its spell slot.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        flagFeatFlag("spontaneous_domain_access", "spontaneous_domain_unlocked", "Spontaneous Domain Access",
                "Convert any prepared spell into a domain spell of equal or lower level.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("domain_spontaneity", "Domain Spontaneity"), level(5)));

        // Holy Warrior is the only active damage effect this pass: a Cleric adds
        // their WIS modifier as flat bonus melee damage, applied in
        // FeatEffectHandler and recalculated on WIS change (LevelUpPayload).
        FeatRegistry.register(new Feat(
                "holy_warrior", "Holy Warrior",
                "Add WIS modifier as bonus damage on all melee attacks.",
                FeatCategory.DIVINE, FeatSource.COMPLETE_DIVINE,
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(6), wis(15)),
                data -> {
                    data.setAchievementFlag("holy_warrior_unlocked", true);
                    data.setHolyWarriorDamageBonus(data.getAbilityScores().getWisMod());
                },
                data -> {
                    data.setAchievementFlag("holy_warrior_unlocked", false);
                    data.setHolyWarriorDamageBonus(0);
                }
        ));

        flagFeat("consecrated_ground", "Consecrated Ground",
                "Once per day consecrate a 10 block radius area for 10 minutes. Undead take 1 damage per second while inside. Allies regen 1 HP per 40 ticks inside.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), level(9), wis(15)));

        flagFeat("sacred_purification", "Sacred Purification",
                "Spend a turning attempt to remove one negative effect from yourself or an adjacent ally.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));
    }

    // --- Helpers ---

    /** Registers a Complete Divine feat whose only effect is an unlock flag (flag = id + "_unlocked"). */
    private static void flagFeat(String id, String name, String description,
                                 String chainGroup, int chainOrder,
                                 List<FeatPrerequisite> prerequisites) {
        flagFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a Complete Divine flag feat with an explicit unlock-flag key. */
    private static void flagFeatFlag(String id, String flag, String name, String description,
                                     String chainGroup, int chainOrder,
                                     List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.DIVINE, FeatSource.COMPLETE_DIVINE,
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

    private static FeatPrerequisite wis(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.WISDOM, minimum);
    }

    private static FeatPrerequisite cha(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CHARISMA, minimum);
    }
}
