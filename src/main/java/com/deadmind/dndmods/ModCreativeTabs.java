package com.deadmind.dndmods;

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
                        // Items will be added here as they are created
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
