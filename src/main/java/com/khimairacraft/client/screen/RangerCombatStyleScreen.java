package com.khimairacraft.client.screen;

import com.khimairacraft.network.SelectRangerCombatStylePayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.DnDPlayerData.RangerCombatStyle;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * One-time, permanent Combat Style choice presented when a Ranger reaches
 * level 2. Blocking (cannot be dismissed with Esc) and self-closing once the
 * choice syncs back, mirroring the RaceSelectionScreen commitment pattern but
 * scoped to this level-up moment.
 */
@OnlyIn(Dist.CLIENT)
public class RangerCombatStyleScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 200;

    private record Option(RangerCombatStyle style, String name, String[] tiers) {}

    /** True once a choice has been sent; blocks further clicks until the sync resolves. */
    private boolean sent = false;

    private static final Option[] OPTIONS = {
            new Option(RangerCombatStyle.ARCHERY, "Archery", new String[]{
                    "Lv 2: Rapid Shot", "Lv 6: Manyshot", "Lv 11: Improved Precise Shot"}),
            new Option(RangerCombatStyle.TWO_WEAPON, "Two-Weapon Combat", new String[]{
                    "Lv 2: Two-Weapon Fighting", "Lv 6: Improved Two-Weapon Fighting",
                    "Lv 11: Greater Two-Weapon Fighting"}),
    };

    public RangerCombatStyleScreen() {
        super(Component.literal("Choose Combat Style"));
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

        // Auto-close once the choice has been made (server sync flipped the field).
        DnDPlayerData data = getPlayerData();
        if (data != null && data.getRangerCombatStyle() != RangerCombatStyle.NONE) {
            onClose();
            return;
        }

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0111122);
        drawBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        graphics.drawCenteredString(this.font, "Ranger Combat Style",
                left + PANEL_WIDTH / 2, top + 10, 0xFFFFD700);
        graphics.drawCenteredString(this.font,
                sent ? "Confirming…" : "This choice is permanent.",
                left + PANEL_WIDTH / 2, top + 24, sent ? 0xFF55FF55 : 0xFFAAAAAA);

        int cardW = (PANEL_WIDTH - 30) / 2;
        int cardH = 130;
        int cardY = top + 44;
        for (int i = 0; i < OPTIONS.length; i++) {
            int cardX = left + 10 + i * (cardW + 10);
            boolean hovered = mouseX >= cardX && mouseX < cardX + cardW
                    && mouseY >= cardY && mouseY < cardY + cardH;
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH,
                    hovered ? 0xFF1A3A1A : 0xFF1A1A2A);
            drawBorder(graphics, cardX, cardY, cardW, cardH, hovered ? 0xFF55FF55 : 0xFF555577);

            graphics.drawCenteredString(this.font, OPTIONS[i].name(),
                    cardX + cardW / 2, cardY + 8, 0xFFFFFFFF);

            int ty = cardY + 28;
            for (String tier : OPTIONS[i].tiers()) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.85f, 0.85f, 1.0f);
                graphics.drawString(this.font, tier,
                        (int) ((cardX + 8) / 0.85f), (int) (ty / 0.85f), 0xFFB0B0C0);
                graphics.pose().popPose();
                ty += 16;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || sent) return super.mouseClicked(mouseX, mouseY, button);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int cardW = (PANEL_WIDTH - 30) / 2;
        int cardH = 130;
        int cardY = top + 44;

        for (int i = 0; i < OPTIONS.length; i++) {
            int cardX = left + 10 + i * (cardW + 10);
            if (mouseX >= cardX && mouseX < cardX + cardW
                    && mouseY >= cardY && mouseY < cardY + cardH) {
                PacketDistributor.sendToServer(
                        new SelectRangerCombatStylePayload(OPTIONS[i].style().name()));
                // Keep the screen open until the server sync flips the field
                // (render() auto-closes then). This avoids a stuck-closed screen
                // if the packet is rejected. Block further clicks meanwhile.
                sent = true;
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
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
