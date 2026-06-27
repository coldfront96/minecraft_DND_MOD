package com.deadmind.dndmods.enchanting;

import com.deadmind.dndmods.items.enchanting.DustTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentSpellLevel {

    private record EnchantmentLevelData(int enchantLevel, int spellLevel, DustTier minimumDustTier) {}

    private static final Map<String, EnchantmentLevelData> SPELL_LEVEL_MAP = new HashMap<>();

    static {
        // Spell Level 1 (Tier 1 minimum)
        register("sharpness", 1, 1, DustTier.COPPER);
        register("protection", 1, 1, DustTier.COPPER);
        register("power", 1, 1, DustTier.COPPER);
        register("efficiency", 1, 1, DustTier.COPPER);
        register("unbreaking", 1, 1, DustTier.COPPER);
        register("swift_sneak", 1, 1, DustTier.COPPER);

        // Spell Level 2 (Tier 2 minimum)
        register("protection", 2, 2, DustTier.IRON);
        register("sharpness", 2, 2, DustTier.IRON);
        register("power", 2, 2, DustTier.IRON);
        register("efficiency", 2, 2, DustTier.IRON);
        register("feather_falling", 1, 2, DustTier.IRON);
        register("respiration", 1, 2, DustTier.IRON);

        // Spell Level 3 (Tier 3 minimum)
        register("protection", 3, 3, DustTier.LAPIS);
        register("sharpness", 3, 3, DustTier.LAPIS);
        register("power", 3, 3, DustTier.LAPIS);
        register("smite", 1, 3, DustTier.LAPIS);
        register("bane_of_arthropods", 1, 3, DustTier.LAPIS);
        register("flame", 1, 3, DustTier.LAPIS);
        register("efficiency", 3, 3, DustTier.LAPIS);
        register("unbreaking", 2, 3, DustTier.LAPIS);

        // Spell Level 4 (Tier 4 minimum)
        register("protection", 4, 4, DustTier.GOLD);
        register("sharpness", 4, 4, DustTier.GOLD);
        register("power", 4, 4, DustTier.GOLD);
        register("smite", 2, 4, DustTier.GOLD);
        register("punch", 1, 4, DustTier.GOLD);
        register("fire_aspect", 1, 4, DustTier.GOLD);
        register("feather_falling", 2, 4, DustTier.GOLD);
        register("depth_strider", 1, 4, DustTier.GOLD);

        // Spell Level 5 (Tier 5 minimum)
        register("sharpness", 5, 5, DustTier.EMERALD);
        register("power", 5, 5, DustTier.EMERALD);
        register("smite", 3, 5, DustTier.EMERALD);
        register("fire_aspect", 2, 5, DustTier.EMERALD);
        register("looting", 1, 5, DustTier.EMERALD);
        register("efficiency", 4, 5, DustTier.EMERALD);
        register("depth_strider", 2, 5, DustTier.EMERALD);
        register("respiration", 2, 5, DustTier.EMERALD);

        // Spell Level 6 (Tier 6 minimum)
        register("smite", 4, 6, DustTier.BLAZE);
        register("looting", 2, 6, DustTier.BLAZE);
        register("fortune", 1, 6, DustTier.BLAZE);
        register("efficiency", 5, 6, DustTier.BLAZE);
        register("unbreaking", 3, 6, DustTier.BLAZE);
        register("depth_strider", 3, 6, DustTier.BLAZE);
        register("aqua_affinity", 1, 6, DustTier.BLAZE);

        // Spell Level 7 (Tier 7 minimum)
        register("smite", 5, 7, DustTier.GHAST);
        register("looting", 3, 7, DustTier.GHAST);
        register("fortune", 2, 7, DustTier.GHAST);
        register("silk_touch", 1, 7, DustTier.GHAST);
        register("infinity", 1, 7, DustTier.GHAST);
        register("punch", 2, 7, DustTier.GHAST);
        register("feather_falling", 3, 7, DustTier.GHAST);
        register("respiration", 3, 7, DustTier.GHAST);

        // Spell Level 8 (Tier 8 minimum)
        register("fortune", 3, 8, DustTier.DIAMOND);
        register("mending", 1, 8, DustTier.DIAMOND);
        register("feather_falling", 4, 8, DustTier.DIAMOND);
        register("thorns", 3, 8, DustTier.DIAMOND);
        register("soul_speed", 3, 8, DustTier.DIAMOND);

        // Spell Level 9 (Tier 9 minimum)
        register("sweeping_edge", 3, 9, DustTier.NETHERITE);
        register("channeling", 1, 9, DustTier.NETHERITE);
        register("riptide", 3, 9, DustTier.NETHERITE);
        register("loyalty", 3, 9, DustTier.NETHERITE);
        register("multishot", 1, 9, DustTier.NETHERITE);
        register("piercing", 4, 9, DustTier.NETHERITE);
        register("quick_charge", 3, 9, DustTier.NETHERITE);
    }

    private static void register(String enchantmentName, int enchantLevel, int spellLevel, DustTier minimumDustTier) {
        String key = enchantmentName + ":" + enchantLevel;
        SPELL_LEVEL_MAP.put(key, new EnchantmentLevelData(enchantLevel, spellLevel, minimumDustTier));
    }

    public static int getSpellLevel(String enchantmentName, int enchantLevel) {
        String key = enchantmentName + ":" + enchantLevel;
        EnchantmentLevelData data = SPELL_LEVEL_MAP.get(key);
        if (data != null) {
            return data.spellLevel;
        }
        return enchantLevel;
    }

    public static DustTier getMinimumDustTier(String enchantmentName, int enchantLevel) {
        String key = enchantmentName + ":" + enchantLevel;
        EnchantmentLevelData data = SPELL_LEVEL_MAP.get(key);
        if (data != null) {
            return data.minimumDustTier;
        }
        return DustTier.COPPER;
    }

    public static int getPlayerSpellLevel(int playerLevel, int totalLevel) {
        int spellLevel = (int) Math.ceil(playerLevel / 2.0);
        if (totalLevel >= 20) {
            return Math.min(spellLevel, 10);
        }
        return Math.min(spellLevel, 9);
    }

    public static ResourceKey<Enchantment> enchantmentKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(name));
    }
}
