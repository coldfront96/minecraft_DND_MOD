package com.khimairacraft.feat;

import com.khimairacraft.playerdata.DnDPlayerData;

import java.util.*;

public class FeatRegistry {
    private static final Map<String, Feat> REGISTRY = new LinkedHashMap<>();

    public static void register(Feat feat) {
        REGISTRY.put(feat.getFeatId(), feat);
    }

    public static Feat get(String featId) {
        return REGISTRY.get(featId);
    }

    public static List<Feat> getByCategory(FeatCategory category) {
        List<Feat> result = new ArrayList<>();
        for (Feat feat : REGISTRY.values()) {
            if (feat.getCategory() == category && FeatSourceConfig.isEnabled(feat.getSource())) {
                result.add(feat);
            }
        }
        return result;
    }

    public static List<Feat> getBySource(FeatSource source) {
        List<Feat> result = new ArrayList<>();
        if (!FeatSourceConfig.isEnabled(source)) return result;
        for (Feat feat : REGISTRY.values()) {
            if (feat.getSource() == source) {
                result.add(feat);
            }
        }
        return result;
    }

    public static List<Feat> getByChainGroup(String chainGroup) {
        List<Feat> result = new ArrayList<>();
        for (Feat feat : REGISTRY.values()) {
            if (chainGroup.equals(feat.getChainGroup()) && FeatSourceConfig.isEnabled(feat.getSource())) {
                result.add(feat);
            }
        }
        result.sort(Comparator.comparingInt(Feat::getChainOrder));
        return result;
    }

    public static List<Feat> getAll() {
        List<Feat> result = new ArrayList<>();
        for (Feat feat : REGISTRY.values()) {
            if (FeatSourceConfig.isEnabled(feat.getSource())) {
                result.add(feat);
            }
        }
        return result;
    }

    public static List<Feat> getAvailable(DnDPlayerData data) {
        List<Feat> result = new ArrayList<>();
        for (Feat feat : REGISTRY.values()) {
            if (!FeatSourceConfig.isEnabled(feat.getSource())) continue;
            if (data.hasFeat(feat.getFeatId())) continue;
            if (!feat.allPrerequisitesMet(data)) continue;
            result.add(feat);
        }
        return result;
    }
}
