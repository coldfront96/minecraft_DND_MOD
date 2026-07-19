package com.khimairacraft.client;

import com.khimairacraft.DnDMods;
import com.khimairacraft.client.hud.DnDHudOverlay;
import com.khimairacraft.enchanting.ArcaneEnchantingMenu;
import com.khimairacraft.enchanting.ArcaneEnchantingScreen;
import com.khimairacraft.enchanting.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = DnDMods.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "dnd_hud"),
                DnDHudOverlay::render
        );
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_ENCHANTING_TABLE.get(), ArcaneEnchantingScreen::new);
    }
}
