package com.deadmind.dndmods.feat;

import java.util.*;

public class FeatChainHelper {

    public record FeatChain(String chainGroup, List<Feat> feats) {}

    public static List<FeatChain> buildChains(List<Feat> feats) {
        Map<String, List<Feat>> grouped = new LinkedHashMap<>();
        List<Feat> solo = new ArrayList<>();

        for (Feat feat : feats) {
            String group = feat.getChainGroup();
            if (group == null) {
                solo.add(feat);
            } else {
                grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(feat);
            }
        }

        List<FeatChain> chains = new ArrayList<>();
        for (Map.Entry<String, List<Feat>> entry : grouped.entrySet()) {
            List<Feat> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparingInt(Feat::getChainOrder));
            chains.add(new FeatChain(entry.getKey(), sorted));
        }
        for (Feat feat : solo) {
            chains.add(new FeatChain(null, List.of(feat)));
        }
        return chains;
    }

    public static String chainGroupToDisplayName(String chainGroup) {
        if (chainGroup == null) return "";
        String[] parts = chainGroup.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.equals("line")) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }
}
