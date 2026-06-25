package com.deadmind.dndmods.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LevelUpPlaceholderScreen extends Screen {

    public LevelUpPlaceholderScreen() {
        super(Component.literal("Level Up"));
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Close"),
                btn -> this.onClose())
                .bounds(this.width / 2 - 50, this.height / 2 + 30, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(this.font, "Level Up Menu - Coming Soon", this.width / 2, this.height / 2 - 20, 0xFFFFD700);
    }
}
