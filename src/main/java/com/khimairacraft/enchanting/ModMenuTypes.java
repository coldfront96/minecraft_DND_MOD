package com.khimairacraft.enchanting;

import com.khimairacraft.DnDMods;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DnDMods.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneEnchantingMenu>> ARCANE_ENCHANTING_TABLE =
            MENU_TYPES.register("arcane_enchanting_table",
                    () -> IMenuTypeExtension.create(ArcaneEnchantingMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
