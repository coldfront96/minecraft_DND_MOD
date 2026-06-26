package com.deadmind.dndmods.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CharacterSheetScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 200;

    public CharacterSheetScreen() {
        super(Component.literal("Character Sheet"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xCC111111);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0xFF555555);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF555555);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, 0xFF555555);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF555555);

        String title = "Character Sheet — Coming Soon";
        int textWidth = this.font.width(title);
        graphics.drawString(this.font, title,
                left + (PANEL_WIDTH - textWidth) / 2,
                top + (PANEL_HEIGHT - 8) / 2,
                0xFFAAAAAA, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
