package com.khimairacraft.enchanting;

import java.util.Set;

public class RangerEnchantmentList {
    private static final Set<String> ALLOWED_ENCHANTMENTS = Set.of(
            "power",
            "punch",
            "flame",
            "infinity",
            "sharpness",
            "smite",
            "bane_of_arthropods",
            "protection",
            "feather_falling"
    );

    public static boolean isAllowed(String enchantmentName) {
        return ALLOWED_ENCHANTMENTS.contains(enchantmentName);
    }

    public static Set<String> getAllowed() {
        return ALLOWED_ENCHANTMENTS;
    }
}
