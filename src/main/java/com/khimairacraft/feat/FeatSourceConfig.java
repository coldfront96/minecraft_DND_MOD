package com.khimairacraft.feat;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

public class FeatSourceConfig {
    private static final Map<FeatSource, ModConfigSpec.BooleanValue> SOURCE_FLAGS = new EnumMap<>(FeatSource.class);

    public static void build(ModConfigSpec.Builder builder) {
        builder.push("feat_sources");
        for (FeatSource source : FeatSource.values()) {
            String key = source.name().toLowerCase();
            SOURCE_FLAGS.put(source, builder
                    .comment("Enable feats from " + source.getDisplayName())
                    .define(key, true));
        }
        builder.pop();
    }

    public static boolean isEnabled(FeatSource source) {
        ModConfigSpec.BooleanValue flag = SOURCE_FLAGS.get(source);
        if (flag == null) return true;
        try {
            return flag.get();
        } catch (Exception e) {
            return true;
        }
    }
}
