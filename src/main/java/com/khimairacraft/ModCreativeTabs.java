package com.khimairacraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DnDMods.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DND_TAB =
            CREATIVE_TABS.register("dnd_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DnDMods.MOD_ID + ".dnd_tab"))
                    .icon(() -> new ItemStack(Items.DIAMOND_SWORD))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COPPER_ARCANE_DUST.get());
                        output.accept(ModItems.IRON_ARCANE_DUST.get());
                        output.accept(ModItems.LAPIS_ARCANE_DUST.get());
                        output.accept(ModItems.GOLD_ARCANE_DUST.get());
                        output.accept(ModItems.EMERALD_ARCANE_DUST.get());
                        output.accept(ModItems.BLAZE_ARCANE_DUST.get());
                        output.accept(ModItems.GHAST_ARCANE_DUST.get());
                        output.accept(ModItems.DIAMOND_ARCANE_DUST.get());
                        output.accept(ModItems.NETHERITE_ARCANE_DUST.get());
                        output.accept(ModItems.NETHER_STAR_ARCANE_DUST.get());
                        output.accept(ModItems.ARCANE_ENCHANTING_TABLE_ITEM.get());
                        output.accept(ModItems.ARCANE_ORE_FRAGMENT.get());
                        output.accept(ModItems.ARCANE_PHYLACTERY_ITEM.get());
                        output.accept(ModItems.ARCANE_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_ARCANE_ORE_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
