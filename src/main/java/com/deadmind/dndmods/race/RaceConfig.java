package com.deadmind.dndmods.race;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

public class RaceConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue CORE_ENABLED;
    private static final ModConfigSpec.BooleanValue EXPANDED_ENABLED;
    private static final ModConfigSpec.BooleanValue UNDEAD_ENABLED;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("race_sources");
        CORE_ENABLED = BUILDER.comment("Enable core PHB races (Human, Elf, Dwarf, Halfling, Gnome, Half-Elf, Half-Orc)")
                .define("core", true);
        EXPANDED_ENABLED = BUILDER.comment("Enable expanded races (Dragonborn, Goliath, Warforged, Tiefling, Aasimar)")
                .define("expanded", true);
        UNDEAD_ENABLED = BUILDER.comment("Enable undead races (Revenant, Dhampir, Shadar-Kai, Vampire Spawn, Skeleton Warrior)")
                .define("undead", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, "dndmods-server.toml");
    }

    public static boolean isSourceEnabled(RaceSource source) {
        return switch (source) {
            case CORE -> CORE_ENABLED.get();
            case EXPANDED -> EXPANDED_ENABLED.get();
            case UNDEAD -> UNDEAD_ENABLED.get();
        };
    }
}
