package com.khimairacraft.feat.content;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.*;
import com.khimairacraft.race.DnDRace;

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
        registerGeneralFeats();
        registerSaveFeats();
        registerMetamagicFeats();
        registerDivineFeats();
        registerRacialFeats();
    }

    // ------------------------------------------------------------------
    // PHB racial feats. Every feat is gated by a RacePrerequisite. Most
    // are flag only — the cooldown/range/resistance tweaks they describe
    // are applied later in the relevant ability or RacialTraitHandler by
    // checking the unlock flag. A handful feed real fields or have active
    // effects in FeatEffectHandler (quick_learner XP, dwarven_toughness,
    // natural armor, save bonuses, racialAcBonus, warforged/dwarf/half-orc
    // combat procs, natural_athlete attributes).
    // ------------------------------------------------------------------
    private static void registerRacialFeats() {

        // --- HUMAN LINE ---
        FeatRegistry.register(new Feat(
                "quick_learner", "Quick Learner",
                "Gain an additional skill point per level. Grants +5% bonus DnD XP from all sources.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "human_line", 0,
                List.of(race(DnDRace.HUMAN)),
                data -> data.setRacialXpBonus(data.getRacialXpBonus() + 0.05f),
                data -> data.setRacialXpBonus(data.getRacialXpBonus() - 0.05f)
        ));

        racialFeat("human_adaptability", "Human Adaptability",
                "Humans may retrain one feat selection once per level up.",
                "human_line", 1,
                List.of(race(DnDRace.HUMAN), reqFeat("quick_learner", "Quick Learner")));

        // --- ELF LINE ---
        racialFeat("elven_accuracy", "Elven Accuracy",
                "Reroll one ranged attack roll per round, taking the better result.",
                "elf_line", 0,
                List.of(race(DnDRace.ELF)));

        racialFeat("elven_archery", "Elven Archery",
                "+2 damage with longbows and shortbows.",
                "elf_line", 1,
                List.of(race(DnDRace.ELF), reqFeat("elven_accuracy", "Elven Accuracy"), bab(6)));

        racialFeat("city_elf_grace", "City Elf Grace",
                "+2 bonus to AC in urban and indoor environments.",
                "elf_line", 0,
                List.of(race(DnDRace.ELF)));

        // --- DWARF LINE ---
        FeatRegistry.register(new Feat(
                "dwarven_toughness", "Dwarven Toughness",
                "+1 HP per character level.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "dwarf_line", 0,
                List.of(race(DnDRace.DWARF)),
                data -> data.setDwarvenToughnessHp(data.getTotalLevel()),
                data -> data.setDwarvenToughnessHp(0)
        ));

        racialFeat("dwarven_resilience", "Dwarven Resilience",
                "Once per day, when reduced below 25% HP, gain resistance to all damage for 5 seconds.",
                "dwarf_line", 1,
                List.of(race(DnDRace.DWARF), reqFeat("dwarven_toughness", "Dwarven Toughness")));

        racialFeatFlag("stonecunning_feat", "stonecunning_unlocked", "Stonecunning",
                "Automatically detect unusual stonework within 10 feet. Highlights nearby ore blocks.",
                "dwarf_line", 0,
                List.of(race(DnDRace.DWARF)));

        // --- HALFLING LINE ---
        racialFeat("halfling_agility", "Halfling Agility",
                "+2 to AC and Reflex saves when adjacent to a larger creature.",
                "halfling_line", 0,
                List.of(race(DnDRace.HALFLING)));

        FeatRegistry.register(new Feat(
                "halfling_luck", "Halfling Luck",
                "Once per day, reroll any saving throw, taking the better result.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "halfling_line", 1,
                List.of(race(DnDRace.HALFLING), reqFeat("halfling_agility", "Halfling Agility")),
                data -> {
                    data.setAchievementFlag("halfling_luck_unlocked", true);
                    data.setHalflingLuckCharges(1);
                },
                data -> {
                    data.setAchievementFlag("halfling_luck_unlocked", false);
                    data.setHalflingLuckCharges(0);
                }
        ));

        // --- GNOME LINE ---
        FeatRegistry.register(new Feat(
                "gnome_cunning", "Gnome Cunning",
                "+2 to Will saves vs illusions and mind-affecting spells.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "gnome_line", 0,
                List.of(race(DnDRace.GNOME)),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() + 2),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() - 2)
        ));

        racialFeat("gnome_trickery", "Gnome Trickery",
                "Once per day, cast Minor Illusion as a free action, drawing nearby mob attention.",
                "gnome_line", 1,
                List.of(race(DnDRace.GNOME), reqFeat("gnome_cunning", "Gnome Cunning")));

        // --- HALF-ORC LINE ---
        racialFeat("half_orc_ferocity", "Half-Orc Ferocity",
                "Once per day, when reduced to 0 HP, remain conscious and fight for 5 more seconds.",
                "half_orc_line", 0,
                List.of(race(DnDRace.HALF_ORC)));

        racialFeat("orc_blood", "Orc Blood",
                "Count as both Human and Orc for feat and effect prerequisites.",
                "half_orc_line", 1,
                List.of(race(DnDRace.HALF_ORC), reqFeat("half_orc_ferocity", "Half-Orc Ferocity")));

        // --- HALF-ELF LINE ---
        racialFeat("half_elf_versatility", "Half-Elf Versatility",
                "Choose one feat from either the Human or Elf racial feat lists, ignoring race prerequisite.",
                "half_elf_line", 0,
                List.of(race(DnDRace.HALF_ELF)));

        FeatRegistry.register(new Feat(
                "social_intuition", "Social Intuition",
                "+2 to all saving throws vs charm and fear effects.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "half_elf_line", 1,
                List.of(race(DnDRace.HALF_ELF), reqFeat("half_elf_versatility", "Half-Elf Versatility")),
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() + 1);
                    data.setFeatFortBonus(data.getFeatFortBonus() + 1);
                },
                data -> {
                    data.setFeatWillBonus(data.getFeatWillBonus() - 1);
                    data.setFeatFortBonus(data.getFeatFortBonus() - 1);
                }
        ));

        // --- DRAGONBORN LINE ---
        racialFeatFlag("draconic_breath_mastery", "breath_mastery_unlocked", "Draconic Breath Mastery",
                "Reduce breath weapon cooldown by 50 ticks and increase damage by 2.",
                "dragonborn_line", 0,
                List.of(race(DnDRace.DRAGONBORN)));

        FeatRegistry.register(new Feat(
                "draconic_resilience", "Draconic Resilience",
                "Gain natural armor +2 AC from draconic scales.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "dragonborn_line", 1,
                List.of(race(DnDRace.DRAGONBORN), reqFeat("draconic_breath_mastery", "Draconic Breath Mastery")),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() + 2),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() - 2)
        ));

        // --- GOLIATH LINE ---
        racialFeat("mountain_born", "Mountain Born",
                "Ignore movement penalties in mountains and ignore cold weather damage.",
                "goliath_line", 0,
                List.of(race(DnDRace.GOLIATH)));

        racialFeat("natural_athlete", "Natural Athlete",
                "+4 to Athletics checks. Grants +15% movement speed and +20% jump height.",
                "goliath_line", 1,
                List.of(race(DnDRace.GOLIATH), reqFeat("mountain_born", "Mountain Born")));

        // --- WARFORGED LINE ---
        racialFeatFlag("warforged_resilience_feat", "warforged_resilience_unlocked", "Warforged Resilience",
                "Reduce all incoming damage by 1 (minimum 0). The warforged chassis absorbs minor hits.",
                "warforged_line", 0,
                List.of(race(DnDRace.WARFORGED)));

        FeatRegistry.register(new Feat(
                "integrated_protection", "Integrated Protection",
                "+2 natural armor AC from integrated plating.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "warforged_line", 1,
                List.of(race(DnDRace.WARFORGED), reqFeat("warforged_resilience_feat", "Warforged Resilience")),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() + 2),
                data -> data.setNaturalArmorBonus(data.getNaturalArmorBonus() - 2)
        ));

        // --- TIEFLING LINE ---
        racialFeat("infernal_heritage", "Infernal Heritage",
                "Gain resistance to fire damage, increasing from 50% to 75%.",
                "tiefling_line", 0,
                List.of(race(DnDRace.TIEFLING)));

        racialFeat("darkness_mastery", "Darkness Mastery",
                "Reduce Tiefling Darkness ability cooldown by 100 ticks and extend blindness to 8 seconds.",
                "tiefling_line", 1,
                List.of(race(DnDRace.TIEFLING), reqFeat("infernal_heritage", "Infernal Heritage")));

        // --- AASIMAR LINE ---
        racialFeat("celestial_heritage", "Celestial Heritage",
                "Increase acid, cold, and lightning resistance from 25% to 50%.",
                "aasimar_line", 0,
                List.of(race(DnDRace.AASIMAR)));

        racialFeat("radiance_mastery", "Radiance Mastery",
                "Reduce Aasimar Radiant Burst cooldown by 100 ticks and extend the Strength buff to 10 seconds.",
                "aasimar_line", 1,
                List.of(race(DnDRace.AASIMAR), reqFeat("celestial_heritage", "Celestial Heritage")));

        // --- UNDEAD RACIAL FEATS (parallel roots, all chainOrder 0) ---
        racialFeat("undying_fortitude", "Undying Fortitude",
                "Reduce the cooldown on Undying Resolve from 1 hour to 30 minutes.",
                "undead_racial_line", 0,
                List.of(race(DnDRace.REVENANT)));

        racialFeat("shadow_step_mastery", "Shadow Step Mastery",
                "Reduce Phase Step cooldown by 100 ticks and increase teleport range to 12 blocks.",
                "undead_racial_line", 0,
                List.of(race(DnDRace.SHADAR_KAI)));

        racialFeat("blood_drain_mastery", "Blood Drain Mastery",
                "Blood drain heals 2 HP per hit instead of 1.",
                "undead_racial_line", 0,
                List.of(race(DnDRace.DHAMPIR)));

        racialFeatFlag("vampire_lord_ascension", "vampire_lord_unlocked", "Vampire Lord Ascension",
                "Charm Gaze cooldown reduced by 200 ticks and affects two targets simultaneously.",
                "undead_racial_line", 0,
                List.of(race(DnDRace.VAMPIRE_SPAWN), level(10)));

        FeatRegistry.register(new Feat(
                "bone_lord", "Bone Lord",
                "Arrow resistance increases from 50% to 75%. Bone Armor AC bonus increases from +1 to +3.",
                FeatCategory.RACIAL, FeatSource.PHB,
                "undead_racial_line", 0,
                List.of(race(DnDRace.SKELETON_WARRIOR), level(10)),
                data -> {
                    data.setAchievementFlag("bone_lord_unlocked", true);
                    data.setRacialAcBonus(data.getRacialAcBonus() + 2);
                },
                data -> {
                    data.setAchievementFlag("bone_lord_unlocked", false);
                    data.setRacialAcBonus(data.getRacialAcBonus() - 2);
                }
        ));
    }

    // ------------------------------------------------------------------
    // PHB divine feats. Most are flag only — the active turning/domain/
    // cure mechanics arrive in the Cleric ability pass. A few feed real
    // int fields read by other systems (extraTurningCharges, devotion
    // damage/healing bonuses). distracting_attack is the only runtime
    // effect this pass and lives in FeatEffectHandler. insightful_reflexes
    // is gated by a flag and applied directly in SavingThrowSystem.
    // ------------------------------------------------------------------
    private static void registerDivineFeats() {

        // --- TURN UNDEAD LINE (extra_turning is the chainOrder-0 root) ---
        FeatRegistry.register(new Feat(
                "extra_turning", "Extra Turning",
                "Four additional turning attempts per day.",
                FeatCategory.DIVINE, FeatSource.PHB,
                "turning_line", 0,
                List.of(cls(DnDClass.CLERIC)),
                data -> {
                    data.setAchievementFlag("extra_turning_unlocked", true);
                    data.setExtraTurningCharges(data.getExtraTurningCharges() + 4);
                },
                data -> {
                    data.setAchievementFlag("extra_turning_unlocked", false);
                    data.setExtraTurningCharges(data.getExtraTurningCharges() - 4);
                }
        ));

        divineFeat("divine_might", "Divine Might",
                "Spend a turning attempt to add CHA modifier to weapon damage for one minute.",
                "turning_line", 1,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));

        divineFeat("divine_shield", "Divine Shield",
                "Spend a turning attempt to add CHA modifier to AC for one minute.",
                "turning_line", 1,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning"), cha(13)));

        divineFeat("sacred_vengeance", "Sacred Vengeance",
                "Spend a turning attempt to deal extra radiant damage on melee attacks for one minute.",
                "turning_line", 1,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        // --- DOMAIN FEATS ---
        divineFeat("extra_domain", "Extra Domain",
                "Gain access to one additional cleric domain.",
                null, 0,
                List.of(cls(DnDClass.CLERIC)));

        divineFeat("domain_spontaneity", "Domain Spontaneity",
                "Convert prepared spells into domain spells spontaneously.",
                null, 0,
                List.of(cls(DnDClass.CLERIC)));

        // --- DEVOTION LINE (parallel roots; grant real int bonuses) ---
        FeatRegistry.register(new Feat(
                "warrior_of_darkness", "Warrior of Darkness",
                "Channel negative energy more effectively. +2 to negative energy damage rolls.",
                FeatCategory.DIVINE, FeatSource.PHB,
                "devotion_line", 0,
                List.of(cls(DnDClass.CLERIC), level(5)),
                data -> data.setDevotionDamageBonus(data.getDevotionDamageBonus() + 2),
                data -> data.setDevotionDamageBonus(data.getDevotionDamageBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "servant_of_the_heavens", "Servant of the Heavens",
                "Channel positive energy more effectively. +2 to positive energy healing rolls.",
                FeatCategory.DIVINE, FeatSource.PHB,
                "devotion_line", 0,
                List.of(cls(DnDClass.CLERIC), level(5)),
                data -> data.setDevotionHealingBonus(data.getDevotionHealingBonus() + 2),
                data -> data.setDevotionHealingBonus(data.getDevotionHealingBonus() - 2)
        ));

        // --- HOLY WARRIOR LINE (sequential) ---
        divineFeat("sacred_boost", "Sacred Boost",
                "Maximize the healing of your next cure spell by spending a turning attempt.",
                "holy_warrior_line", 0,
                List.of(cls(DnDClass.CLERIC), wis(13)));

        divineFeat("divine_ward", "Divine Ward",
                "Protect an ally from the next attack that would hit them once per day.",
                "holy_warrior_line", 1,
                List.of(reqFeat("sacred_boost", "Sacred Boost"), cls(DnDClass.CLERIC), wis(13)));

        // --- RANGER DIVINE FEATS ---
        divineFeat("swift_hunter", "Swift Hunter",
                "Stack Ranger and Rogue levels for sneak attack and favored enemy bonuses.",
                null, 0,
                List.of(cls(DnDClass.RANGER)));

        divineFeat("distracting_attack", "Distracting Attack",
                "Your attack makes it easier for allies to hit the same target. Applies Glowing on hit.",
                null, 0,
                List.of(cls(DnDClass.RANGER), bab(4)));

        // --- STANDALONE DIVINE FEATS ---
        divineFeat("divine_vengeance", "Divine Vengeance",
                "Spend a turning attempt to deal bonus fire or cold damage against undead.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        divineFeat("divine_resistance", "Divine Resistance",
                "Spend a turning attempt to grant yourself and allies resistance to a chosen energy type for one minute.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), reqFeat("extra_turning", "Extra Turning")));

        divineFeat("zen_archery", "Zen Archery",
                "Use WIS modifier instead of DEX for ranged attack rolls.",
                null, 0,
                List.of(cls(DnDClass.RANGER), wis(13), bab(1)));

        divineFeat("insightful_reflexes", "Insightful Reflexes",
                "Use INT modifier instead of DEX for Reflex saving throws.",
                null, 0,
                List.of());

        divineFeat("holy_radiance", "Holy Radiance",
                "Emit a burst of holy light dealing radiant damage to undead in a 10 block radius once per day.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), cha(15), level(10)));

        divineFeatFlag("sanctify_martial_strike", "sanctify_martial_unlocked", "Sanctify Martial Strike",
                "Your unarmed strikes deal radiant damage against evil outsiders.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(3)));

        divineFeat("battle_blessing", "Battle Blessing",
                "Cast swift action spells as free actions when you confirm a critical hit.",
                null, 0,
                List.of(cls(DnDClass.CLERIC), bab(1)));
    }

    // ------------------------------------------------------------------
    // Saving-throw feats. These were specified in the architecture task
    // but were absent from the codebase; added here. Each grants a real
    // numeric bonus read by SavingThrowSystem (and the character sheet).
    // Toughness raises max HP and stacks via toughnessFeatCount, applied
    // to the vanilla health bar by FeatEffectHandler.
    // ------------------------------------------------------------------
    private static void registerSaveFeats() {
        FeatRegistry.register(new Feat(
                "toughness", "Toughness",
                "+3 hit points permanently.",
                FeatCategory.GENERAL, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setToughnessFeatCount(data.getToughnessFeatCount() + 1),
                data -> data.setToughnessFeatCount(data.getToughnessFeatCount() - 1)
        ));

        FeatRegistry.register(new Feat(
                "iron_will", "Iron Will",
                "+2 bonus on Will saving throws.",
                FeatCategory.GENERAL, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() + 2),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "lightning_reflexes", "Lightning Reflexes",
                "+2 bonus on Reflex saving throws.",
                FeatCategory.GENERAL, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() + 2),
                data -> data.setFeatRefBonus(data.getFeatRefBonus() - 2)
        ));

        FeatRegistry.register(new Feat(
                "great_fortitude", "Great Fortitude",
                "+2 bonus on Fortitude saving throws.",
                FeatCategory.GENERAL, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setFeatFortBonus(data.getFeatFortBonus() + 2),
                data -> data.setFeatFortBonus(data.getFeatFortBonus() - 2)
        ));
    }

    // ------------------------------------------------------------------
    // PHB metamagic feats. All flag only this pass — the spell system
    // pass will read these flags to gate which metamagic options a
    // caster may apply. Metamagic feats are parallel entries within the
    // "metamagic_line" group; chainOrder is for sort/power grouping only,
    // there is no sequential prerequisite chain between them.
    // ------------------------------------------------------------------
    private static void registerMetamagicFeats() {
        metamagicFeat("enlarge_spell", "Enlarge Spell",
                "Double the range of a spell. Spell uses a slot one level higher.", 0);
        metamagicFeat("extend_spell", "Extend Spell",
                "Double the duration of a spell. Spell uses a slot one level higher.", 0);
        metamagicFeat("heighten_spell", "Heighten Spell",
                "Cast a spell as if it were a higher level spell slot.", 0);
        metamagicFeat("widen_spell", "Widen Spell",
                "Double the area of effect of a spell. Spell uses a slot three levels higher.", 0);
        metamagicFeat("silent_spell", "Silent Spell",
                "Cast a spell without verbal components. Spell uses a slot one level higher.", 0);
        metamagicFeat("still_spell", "Still Spell",
                "Cast a spell without somatic components. Spell uses a slot one level higher.", 0);
        metamagicFeat("cooperative_spell", "Cooperative Spell",
                "Combine your spell with an ally's spell for increased effect.", 0);
        metamagicFeat("energy_substitution", "Energy Substitution",
                "Replace a spell's energy type with a different energy type at no slot cost.", 0);

        metamagicFeat("empower_spell", "Empower Spell",
                "Increase all variable numeric effects of a spell by 50%. Spell uses a slot two levels higher.", 1);
        metamagicFeat("repeat_spell", "Repeat Spell",
                "Automatically cast a spell again on the next round. Spell uses a slot two levels higher.", 1);
        metamagicFeat("sculpt_spell", "Sculpt Spell",
                "Change the area of effect shape of a spell. Spell uses a slot one level higher.", 1);

        metamagicFeat("maximize_spell", "Maximize Spell",
                "Maximize all variable numeric effects of a spell. Spell uses a slot three levels higher.", 2);
        metamagicFeat("persistent_spell", "Persistent Spell",
                "Extend a spell's duration to 24 hours. Spell uses a slot six levels higher.", 2);
        metamagicFeat("chain_spell", "Chain Spell",
                "Arc a targeted spell to secondary targets. Spell uses a slot three levels higher.", 2);

        metamagicFeat("quicken_spell", "Quicken Spell",
                "Cast a spell as a free action. Spell uses a slot four levels higher.", 3);
        metamagicFeat("twin_spell", "Twin Spell",
                "Cast a spell twice simultaneously affecting the same or different targets. Spell uses a slot four levels higher.", 3);
    }

    // ------------------------------------------------------------------
    // PHB general feat content pass. Most are flag only — their effects
    // are wired up in future system passes (companions, crafting,
    // enchanting, loot, stealth). The handful with mechanical impact in
    // this pass either feed a real int field (Negotiator -> featWillBonus)
    // or are driven by FeatEffectHandler reading their unlock flags
    // (Run, Athletic, Acrobatic, Self-Sufficient).
    // ------------------------------------------------------------------
    private static void registerGeneralFeats() {

        // --- LEADERSHIP LINE ---
        generalFeat("leadership", "Leadership",
                "Attract a cohort and followers based on your Leadership score.",
                "leadership_line", 0,
                List.of(level(6)));

        generalFeat("legendary_leader", "Legendary Leader",
                "Gain a bonus to your Leadership score when leading from the front.",
                "leadership_line", 1,
                List.of(reqFeat("leadership", "Leadership"), level(6)));

        // --- SPELLCASTING UTILITY FEATS ---
        generalFeat("spell_mastery", "Spell Mastery",
                "Prepare a small number of spells without a spellbook.",
                null, 0,
                List.of(cls(DnDClass.WIZARD)));

        generalFeat("augment_summoning", "Augment Summoning",
                "Summoned creatures gain +4 STR and +4 CON.",
                "summoning_line", 0,
                List.of());

        generalFeat("natural_spell", "Natural Spell",
                "Cast spells while in wild shape form.",
                null, 0,
                List.of(wis(13)));

        generalFeat("extra_wild_shape", "Extra Wild Shape",
                "Use wild shape two additional times per day.",
                null, 0,
                List.of(cls(DnDClass.RANGER), level(5)));

        generalFeat("combat_casting", "Combat Casting",
                "+4 bonus on concentration checks when casting defensively or while grappled.",
                null, 0,
                List.of());

        generalFeat("eschew_materials", "Eschew Materials",
                "Cast spells without material components worth 1 gold or less.",
                null, 0,
                List.of());

        // --- ITEM CREATION LINE (parallel roots at chainOrder 0) ---
        generalFeat("scribe_scroll", "Scribe Scroll",
                "Create scrolls of spells you know.",
                "crafting_line", 0,
                List.of(casterLevel(1)));

        generalFeat("brew_potion", "Brew Potion",
                "Create potions of spells you know of up to 3rd level.",
                "crafting_line", 0,
                List.of(casterLevel(3)));

        generalFeatFlag("craft_wondrous_item", "craft_wondrous_unlocked", "Craft Wondrous Item",
                "Create wondrous magic items.",
                "crafting_line", 0,
                List.of(casterLevel(3)));

        generalFeatFlag("craft_magic_arms_armor", "craft_magic_arms_unlocked", "Craft Magic Arms and Armor",
                "Create magic weapons and armor.",
                "crafting_line", 0,
                List.of(casterLevel(5)));

        generalFeat("craft_rod", "Craft Rod",
                "Create magic rods.",
                "crafting_line", 0,
                List.of(casterLevel(9)));

        generalFeat("craft_staff", "Craft Staff",
                "Create magic staffs.",
                "crafting_line", 0,
                List.of(casterLevel(12)));

        generalFeat("forge_ring", "Forge Ring",
                "Create magic rings.",
                "crafting_line", 0,
                List.of(casterLevel(12)));

        // --- RESERVE / SKILL-BOOST FEATS ---
        generalFeat("run", "Run",
                "Run at five times normal speed instead of four. Retain DEX bonus to AC while running.",
                null, 0,
                List.of());

        generalFeat("athletic", "Athletic",
                "+2 bonus on Climb and Jump checks. Grants a +10% jump height bonus.",
                null, 0,
                List.of());

        generalFeat("acrobatic", "Acrobatic",
                "+2 bonus on Jump and Tumble checks. Negates the first 4 blocks of fall damage.",
                null, 0,
                List.of());

        generalFeat("animal_affinity", "Animal Affinity",
                "+2 bonus on Handle Animal and Ride checks. Tamed animals gain +20% max HP.",
                null, 0,
                List.of());

        // Negotiator grants a real, stackable Will save bonus.
        FeatRegistry.register(new Feat(
                "negotiator", "Negotiator",
                "+2 on Diplomacy and Sense Motive. Grants +1 to all Will saves.",
                FeatCategory.GENERAL, FeatSource.PHB,
                null, 0,
                List.of(),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() + 1),
                data -> data.setFeatWillBonus(data.getFeatWillBonus() - 1)
        ));

        generalFeat("self_sufficient", "Self-Sufficient",
                "+2 on Heal and Survival checks. Natural health regeneration is 25% faster.",
                null, 0,
                List.of());

        generalFeat("stealthy", "Stealthy",
                "+2 on Hide and Move Silently. Reduces mob detection range by 15%.",
                null, 0,
                List.of());

        generalFeat("diligent", "Diligent",
                "+2 on Appraise and Decipher Script. +1 to INT modifier for bonus feat calculation.",
                null, 0,
                List.of());

        generalFeat("investigator", "Investigator",
                "+2 on Gather Information and Search. One extra roll on chest loot.",
                null, 0,
                List.of());

        generalFeat("magical_aptitude", "Magical Aptitude",
                "+2 on Spellcraft and Use Magic Device. Reduces arcane dust cost for enchanting by one tier.",
                null, 0,
                List.of());

        generalFeat("persuasive", "Persuasive",
                "+2 on Bluff and Intimidate. +1 to CHA modifier for class resource maximum.",
                null, 0,
                List.of());

        generalFeat("nimble_fingers", "Nimble Fingers",
                "+2 on Disable Device and Open Lock. 20% faster block interaction speed.",
                null, 0,
                List.of());
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

        // NOTE: extra_turning is registered in the divine feat pass
        // (registerDivineFeats) as the turning_line root, where it also
        // grants extraTurningCharges and several divine feats depend on it.

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

    /** Registers a PHB general feat whose only effect is setting an unlock flag (flag = id + "_unlocked"). */
    private static void generalFeat(String id, String name, String description,
                                    String chainGroup, int chainOrder,
                                    List<FeatPrerequisite> prerequisites) {
        generalFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a PHB general flag feat with an explicit unlock-flag key (for ids whose flag name differs). */
    private static void generalFeatFlag(String id, String flag, String name, String description,
                                        String chainGroup, int chainOrder,
                                        List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.GENERAL, FeatSource.PHB,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    /** Registers a PHB racial flag feat (flag = id + "_unlocked"). */
    private static void racialFeat(String id, String name, String description,
                                   String chainGroup, int chainOrder,
                                   List<FeatPrerequisite> prerequisites) {
        racialFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a PHB racial flag feat with an explicit unlock-flag key. */
    private static void racialFeatFlag(String id, String flag, String name, String description,
                                       String chainGroup, int chainOrder,
                                       List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.RACIAL, FeatSource.PHB,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    private static FeatPrerequisite race(DnDRace requiredRace) {
        return new FeatPrerequisite.RacePrerequisite(requiredRace);
    }

    /** Registers a PHB divine flag feat (flag = id + "_unlocked"). */
    private static void divineFeat(String id, String name, String description,
                                   String chainGroup, int chainOrder,
                                   List<FeatPrerequisite> prerequisites) {
        divineFeatFlag(id, id + "_unlocked", name, description, chainGroup, chainOrder, prerequisites);
    }

    /** Registers a PHB divine flag feat with an explicit unlock-flag key. */
    private static void divineFeatFlag(String id, String flag, String name, String description,
                                       String chainGroup, int chainOrder,
                                       List<FeatPrerequisite> prerequisites) {
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.DIVINE, FeatSource.PHB,
                chainGroup, chainOrder,
                prerequisites,
                data -> data.setAchievementFlag(flag, true),
                data -> data.setAchievementFlag(flag, false)
        ));
    }

    /** Registers a flag-only PHB metamagic feat in the shared metamagic_line group (no prerequisites). */
    private static void metamagicFeat(String id, String name, String description, int chainOrder) {
        String flag = id + "_unlocked";
        FeatRegistry.register(new Feat(
                id, name, description,
                FeatCategory.METAMAGIC, FeatSource.PHB,
                "metamagic_line", chainOrder,
                List.of(),
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

    private static FeatPrerequisite casterLevel(int minimum) {
        return new FeatPrerequisite.CasterLevelPrerequisite(minimum);
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

    private static FeatPrerequisite cha(int minimum) {
        return new FeatPrerequisite.AbilityScorePrerequisite(AbilityScoreType.CHARISMA, minimum);
    }
}
