package com.khimairacraft.client.screen;

import com.khimairacraft.network.SelectFavoredEnemyPayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import com.khimairacraft.ranger.FavoredEnemyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Favored Enemy picker: the full 19-category pool laid out in two columns. A new
 * category is added at +2; re-picking an already-chosen one (shown with its
 * current bonus) raises it by +2. Structurally mirrors
 * {@link RogueSpecialAbilityScreen}. Stays open while slots remain so a Ranger
 * with several unspent picks can spend them in one sitting.
 */
@OnlyIn(Dist.CLIENT)
public class FavoredEnemySelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 300;
    private static final int COLS = 2;
    private static final int ROW_HEIGHT = 22;

    private static final FavoredEnemyType[] TYPES = FavoredEnemyType.values();

    public FavoredEnemySelectionScreen() {
        super(Component.literal("Favored Enemy"));
    }

    @Nullable
    private DnDPlayerData getPlayerData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return mc.player.getData(ModAttachments.PLAYER_DATA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        DnDPlayerData data = getPlayerData();
        if (data == null) return;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0111122);
        drawBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF55AA55);

        graphics.drawString(this.font, "Choose a Favored Enemy", left + 10, top + 8, 0xFF55FF55);
        graphics.drawString(this.font, "Slots available: " + data.getFavoredEnemySlotsAvailable(),
                left + 10, top + 20, 0xFFFFFFFF);

        int cellW = (PANEL_WIDTH - 20) / COLS;
        int gridTop = top + 36;
        for (int i = 0; i < TYPES.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = left + 10 + col * cellW;
            int cy = gridTop + row * ROW_HEIGHT;

            boolean hovered = mouseX >= cx && mouseX < cx + cellW - 4
                    && mouseY >= cy && mouseY < cy + ROW_HEIGHT - 2;
            int current = data.getFavoredEnemyBonusFor(TYPES[i]);

            int bg = hovered ? 0xFF1F3A1F : (current > 0 ? 0xFF1A2A1A : 0xFF1A1A22);
            graphics.fill(cx, cy, cx + cellW - 4, cy + ROW_HEIGHT - 2, bg);

            String label = TYPES[i].getDisplayName();
            if (current > 0) label += " (+" + current + ")";
            int color = current > 0 ? 0xFF88FF88 : 0xFFDDDDDD;
            graphics.pose().pushPose();
            graphics.pose().scale(0.9f, 0.9f, 1.0f);
            graphics.drawString(this.font, label,
                    (int) ((cx + 4) / 0.9f), (int) ((cy + 6) / 0.9f), color);
            graphics.pose().popPose();
        }

        graphics.drawCenteredString(this.font, "[Esc to close]",
                left + PANEL_WIDTH / 2, top + PANEL_HEIGHT - 14, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DnDPlayerData data = getPlayerData();
        if (data == null || data.getFavoredEnemySlotsAvailable() <= 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int cellW = (PANEL_WIDTH - 20) / COLS;
        int gridTop = top + 36;

        for (int i = 0; i < TYPES.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = left + 10 + col * cellW;
            int cy = gridTop + row * ROW_HEIGHT;
            if (mouseX >= cx && mouseX < cx + cellW - 4
                    && mouseY >= cy && mouseY < cy + ROW_HEIGHT - 2) {
                PacketDistributor.sendToServer(new SelectFavoredEnemyPayload(TYPES[i].name()));
                // Stay open; the sync will decrement the slot count and refresh
                // the displayed bonuses. Server rejects extra picks past 0 slots.
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
