package com.deadmind.dndmods.race;

import java.util.*;

public class RaceRegistry {
    private static final Map<DnDRace, RaceDefinition> REGISTRY = new EnumMap<>(DnDRace.class);

    static {
        register(DnDRace.HUMAN, "Versatile and ambitious, humans adapt to any environment.",
                List.of("bonus_feat"));
        register(DnDRace.ELF, "Graceful and long-lived, elves possess keen senses and a deep connection to nature.",
                List.of("darkvision", "sleep_immunity", "enchantment_save"));
        register(DnDRace.DWARF, "Stout and resilient, dwarves are master crafters who thrive underground.",
                List.of("darkvision", "poison_resistance"));
        register(DnDRace.HALFLING, "Small and nimble, halflings rely on luck and stealth to survive.",
                List.of("fear_resist", "all_saves_bonus"));
        register(DnDRace.GNOME, "Curious and inventive, gnomes possess a natural affinity for illusion and tinkering.",
                List.of("darkvision", "illusion_save"));
        register(DnDRace.HALF_ELF, "Blending human ambition with elven grace, half-elves walk between two worlds.",
                List.of("darkvision", "sleep_immunity", "enchantment_save"));
        register(DnDRace.HALF_ORC, "Fierce and powerful, half-orcs channel their orcish fury with surprising cunning.",
                List.of("darkvision"));
        register(DnDRace.DRAGONBORN, "Proud descendants of dragons, dragonborn carry the might of their draconic heritage.",
                List.of("darkvision", "breath_weapon"));
        register(DnDRace.GOLIATH, "Towering and powerful, goliaths thrive in harsh mountain environments.",
                List.of("powerful_build", "cold_resistance"));
        register(DnDRace.WARFORGED, "Constructed for war, warforged are sentient beings of wood, metal, and magic.",
                List.of("darkvision", "poison_resistance", "disease_immunity", "hunger_immunity", "slow_regen"));
        register(DnDRace.TIEFLING, "Bearing an infernal bloodline, tieflings possess innate arcane talents.",
                List.of("darkvision", "fire_resistance", "darkness_ability"));
        register(DnDRace.AASIMAR, "Touched by celestial power, aasimar are champions of light and healing.",
                List.of("darkvision", "acid_cold_elec_resistance", "light_ability"));
        register(DnDRace.REVENANT, "Risen from death by sheer willpower, revenants are driven by unfinished purpose.",
                List.of("hunger_immunity", "poison_immunity", "disease_immunity", "negative_energy_healing",
                        "undying_resolve", "slow_regen"));
        register(DnDRace.DHAMPIR, "Half-vampire outcasts who straddle the line between life and undeath.",
                List.of("hunger_immunity", "partial_poison_resist", "disease_immunity", "negative_energy_healing",
                        "blood_drain", "sunlight_sensitivity_minor"));
        register(DnDRace.SHADAR_KAI, "Shadow-touched elves bound to the Shadowfell, masters of stealth and shadow magic.",
                List.of("hunger_immunity", "poison_immunity", "disease_immunity", "negative_energy_healing",
                        "phase_step", "necrotic_resistance"));
        register(DnDRace.VAMPIRE_SPAWN, "Lesser vampires retaining a spark of their mortal selves, empowered by dark blood.",
                List.of("hunger_immunity", "poison_immunity", "disease_immunity", "negative_energy_healing",
                        "sunlight_damage", "spider_climb", "charm_gaze"));
        register(DnDRace.SKELETON_WARRIOR, "Animated warriors sustained by necromantic energy, tireless and unyielding.",
                List.of("hunger_immunity", "poison_immunity", "disease_immunity", "negative_energy_healing",
                        "arrow_resistance", "blunt_vulnerability", "bone_armor"));
    }

    private static void register(DnDRace race, String description, List<String> traitKeys) {
        REGISTRY.put(race, new RaceDefinition(
                race, race.getDisplayName(), description,
                race.getModifiers(), race.getSource(), traitKeys));
    }

    public static RaceDefinition get(DnDRace race) {
        return REGISTRY.get(race);
    }

    public static List<RaceDefinition> getBySource(RaceSource source) {
        List<RaceDefinition> result = new ArrayList<>();
        if (!RaceConfig.isSourceEnabled(source)) return result;
        for (RaceDefinition def : REGISTRY.values()) {
            if (def.getRace() != DnDRace.NONE && def.getSource() == source) {
                result.add(def);
            }
        }
        return result;
    }

    public static List<RaceDefinition> getAll() {
        List<RaceDefinition> result = new ArrayList<>();
        for (RaceDefinition def : REGISTRY.values()) {
            if (def.getRace() != DnDRace.NONE && RaceConfig.isSourceEnabled(def.getSource())) {
                result.add(def);
            }
        }
        return result;
    }
}
