package com.deadmind.dndmods;

import com.deadmind.dndmods.items.enchanting.ArcaneDust;
import com.deadmind.dndmods.items.enchanting.DustTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DnDMods.MOD_ID);

    public static final DeferredHolder<Item, ArcaneDust> COPPER_ARCANE_DUST =
            ITEMS.register("copper_arcane_dust", () -> new ArcaneDust(DustTier.COPPER, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> IRON_ARCANE_DUST =
            ITEMS.register("iron_arcane_dust", () -> new ArcaneDust(DustTier.IRON, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> LAPIS_ARCANE_DUST =
            ITEMS.register("lapis_arcane_dust", () -> new ArcaneDust(DustTier.LAPIS, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> GOLD_ARCANE_DUST =
            ITEMS.register("gold_arcane_dust", () -> new ArcaneDust(DustTier.GOLD, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> EMERALD_ARCANE_DUST =
            ITEMS.register("emerald_arcane_dust", () -> new ArcaneDust(DustTier.EMERALD, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> BLAZE_ARCANE_DUST =
            ITEMS.register("blaze_arcane_dust", () -> new ArcaneDust(DustTier.BLAZE, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> GHAST_ARCANE_DUST =
            ITEMS.register("ghast_arcane_dust", () -> new ArcaneDust(DustTier.GHAST, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> DIAMOND_ARCANE_DUST =
            ITEMS.register("diamond_arcane_dust", () -> new ArcaneDust(DustTier.DIAMOND, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> NETHERITE_ARCANE_DUST =
            ITEMS.register("netherite_arcane_dust", () -> new ArcaneDust(DustTier.NETHERITE, new Item.Properties()));
    public static final DeferredHolder<Item, ArcaneDust> NETHER_STAR_ARCANE_DUST =
            ITEMS.register("nether_star_arcane_dust", () -> new ArcaneDust(DustTier.NETHER_STAR, new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> ARCANE_ENCHANTING_TABLE_ITEM =
            ITEMS.register("arcane_enchanting_table", () -> new BlockItem(ModBlocks.ARCANE_ENCHANTING_TABLE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
