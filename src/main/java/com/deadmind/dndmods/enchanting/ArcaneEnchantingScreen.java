package com.deadmind.dndmods.enchanting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArcaneEnchantingScreen extends AbstractContainerScreen<ArcaneEnchantingMenu> {
    private static final Component TITLE = Component.translatable("container.dndmods.arcane_enchanting_table");

    public ArcaneEnchantingScreen(ArcaneEnchantingMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Dark background
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1A2E);
        // Border
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF8020C0);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF8020C0);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF8020C0);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF8020C0);

        // Item slot outline (slot 0)
        drawSlotOutline(guiGraphics, x + 24, y + 33);
        // Dust slot outline (slot 1)
        drawSlotOutline(guiGraphics, x + 75, y + 33);

        // Labels
        guiGraphics.drawString(this.font, "Item", x + 22, y + 20, 0xFFAAAAAA, false);
        guiGraphics.drawString(this.font, "Dust", x + 73, y + 20, 0xFFAAAAAA, false);

        // Arrow between slots
        guiGraphics.fill(x + 46, y + 38, x + 70, y + 40, 0xFF8020C0);

        // Player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotOutline(guiGraphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotOutline(guiGraphics, x + 7 + col * 18, y + 141);
        }
    }

    private void drawSlotOutline(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF2A2A4E);
        guiGraphics.fill(x, y, x + 18, y + 1, 0xFF555555);
        guiGraphics.fill(x, y + 17, x + 18, y + 18, 0xFF555555);
        guiGraphics.fill(x, y, x + 1, y + 18, 0xFF555555);
        guiGraphics.fill(x + 17, y, x + 18, y + 18, 0xFF555555);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF8020C0, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFAAAAAA, false);
    }
}
