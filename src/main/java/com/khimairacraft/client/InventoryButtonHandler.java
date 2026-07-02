package com.khimairacraft.client;

import com.khimairacraft.DnDMods;
import com.khimairacraft.client.screen.AbilityManagerScreen;
import com.khimairacraft.client.screen.CharacterSheetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class InventoryButtonHandler {

    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 3;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) return;

        int leftPos = (inventory.width - 176) / 2;
        int topPos = (inventory.height - 166) / 2;

        int buttonX = leftPos - BUTTON_WIDTH - 4;
        int buttonY = topPos + 10;

        event.addListener(Button.builder(Component.literal("Abilities"), btn ->
                Minecraft.getInstance().setScreen(new AbilityManagerScreen())
        ).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        event.addListener(Button.builder(Component.literal("Character"), btn ->
                Minecraft.getInstance().setScreen(new CharacterSheetScreen())
        ).bounds(buttonX, buttonY + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }
}
