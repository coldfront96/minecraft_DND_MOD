package com.khimairacraft.enchanting.epic;

import com.khimairacraft.DnDMods;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> SHARPNESS_X =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "sharpness_x"));

    public static final ResourceKey<Enchantment> PROTECTION_V =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "protection_v"));

    public static final ResourceKey<Enchantment> POWER_VI =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "power_vi"));
}
