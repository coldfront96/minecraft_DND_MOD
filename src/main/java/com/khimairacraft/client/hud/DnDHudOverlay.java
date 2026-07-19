package com.khimairacraft.client.hud;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.classes.ResourceType;
import com.khimairacraft.client.ClassSelectionScreen;
import com.khimairacraft.playerdata.ClassEntry;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DnDHudOverlay {

    private static final int MARGIN = 8;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 8;
    private static final int XP_BAR_HEIGHT = 4;
    private static final int BAR_GAP = 4;

    private static final int BAR_BG = 0xFF202020;
    private static final int BAR_BORDER = 0xFFFFFFFF;
    private static final int XP_COLOR = 0xFF40C040;

    private static final int COLOR_STAMINA = 0xFFE8A020;
    private static final int COLOR_MANA = 0xFF2060E8;
    private static final int COLOR_FAITH = 0xFFF0D020;
    private static final int COLOR_FOCUS = 0xFF20C840;
    private static final int COLOR_RAGE = 0xFFC02020;
    private static final int COLOR_NONE = 0xFF606060;
    private static final int COLOR_UNIFIED = 0xFF8020C0;

    private static final int LEVEL_UP_COLOR = 0xFFFFD700;

    public static void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;
        if (mc.player.isDeadOrDying()) return;
        if (mc.screen instanceof ClassSelectionScreen) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        if (data.isLevelUpAvailable()) {
            renderLevelUpNotification(gui, mc, data);
        }

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int x = MARGIN;
        int currentY = screenHeight - MARGIN;

        if (data.isUnifiedPool()) {
            currentY -= BAR_HEIGHT;
            renderResourceBar(gui, x, currentY,
                    data.getUnifiedResource(), data.getUnifiedMaxResource(),
                    COLOR_UNIFIED);
        } else if (data.getSecondary() != null) {
            ClassEntry secondary = data.getSecondary();
            currentY -= BAR_HEIGHT;
            renderClassResourceBar(gui, x, currentY, data, secondary);

            currentY -= BAR_HEIGHT + BAR_GAP;
            renderClassResourceBar(gui, x, currentY, data, data.getPrimary());
        } else {
            currentY -= BAR_HEIGHT;
            renderClassResourceBar(gui, x, currentY, data, data.getPrimary());
        }

        currentY -= XP_BAR_HEIGHT + BAR_GAP;
        renderXpBar(gui, x, currentY, data);

        currentY -= 10;
        renderScaledText(gui, "Level " + data.getTotalLevel(), x, currentY, 0xFFFFFFFF, 0.85f);

        currentY -= 10;
        renderScaledText(gui, getClassDisplayString(data), x, currentY, 0xFFFFFFFF, 0.85f);
    }

    /**
     * Renders a class's resource bar. Fighter is a special case: it has no
     * generic class resource, so its bar reads the dedicated Stamina pool
     * ({@code currentStamina}/{@code maxStamina}) off {@link DnDPlayerData}
     * rather than the generic {@link ClassEntry} resource. All other classes
     * keep reading their generic pool.
     */
    private static void renderClassResourceBar(GuiGraphics gui, int x, int y,
                                               DnDPlayerData data, ClassEntry entry) {
        if (entry.getDnDClass() == DnDClass.FIGHTER) {
            renderResourceBar(gui, x, y,
                    data.getCurrentStamina(), data.getMaxStamina(),
                    getResourceColor(ResourceType.STAMINA));
        } else {
            renderResourceBar(gui, x, y,
                    entry.getCurrentResource(), entry.getMaxResource(),
                    getResourceColor(entry.getDnDClass().getResourceType()));
        }
    }

    private static void renderResourceBar(GuiGraphics gui, int x, int y,
                                           int current, int max, int fillColor) {
        gui.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BAR_BORDER);
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_BG);

        if (max > 0) {
            int fillWidth = (int) ((float) current / max * BAR_WIDTH);
            gui.fill(x, y, x + fillWidth, y + BAR_HEIGHT, fillColor);
        }

        String text = current + " / " + max;
        renderScaledText(gui, text, x + BAR_WIDTH + 4, y + 1, 0xFFCCCCCC, 0.75f);
    }

    private static void renderXpBar(GuiGraphics gui, int x, int y, DnDPlayerData data) {
        gui.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + XP_BAR_HEIGHT + 1, BAR_BORDER);
        gui.fill(x, y, x + BAR_WIDTH, y + XP_BAR_HEIGHT, BAR_BG);

        int xpNeeded = data.getXpForNextLevel();
        if (xpNeeded > 0) {
            float ratio = Math.min(1.0f, (float) data.getXp() / xpNeeded);
            int fillWidth = (int) (BAR_WIDTH * ratio);
            gui.fill(x, y, x + fillWidth, y + XP_BAR_HEIGHT, XP_COLOR);
        }
    }

    private static void renderScaledText(GuiGraphics gui, String text,
                                          int x, int y, int color, float scale) {
        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1.0f);
        gui.drawString(Minecraft.getInstance().font, text,
                (int) (x / scale), (int) (y / scale), color);
        gui.pose().popPose();
    }

    private static String getClassDisplayString(DnDPlayerData data) {
        if (data.isUnifiedPool()) {
            return data.getPrestigeClass().getDisplayName();
        }
        if (data.getSecondary() != null) {
            return data.getPrimary().getDnDClass().getDisplayName() + " / "
                    + data.getSecondary().getDnDClass().getDisplayName();
        }
        return data.getDnDClass().getDisplayName();
    }

    private static void renderLevelUpNotification(GuiGraphics gui, Minecraft mc, DnDPlayerData data) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int y = screenHeight - 60;

        double pulse = Math.sin(System.currentTimeMillis() / 500.0);
        int alpha = (int) (180 + 75 * pulse);
        int color = (alpha << 24) | (LEVEL_UP_COLOR & 0x00FFFFFF);

        gui.drawCenteredString(mc.font, "Level Up Available!", centerX, y, color);

        gui.pose().pushPose();
        gui.pose().scale(0.75f, 0.75f, 1.0f);
        int subAlpha = (int) (140 + 60 * pulse);
        int subColor = (subAlpha << 24) | 0x00FFFFFF;
        gui.drawCenteredString(mc.font, "Press L to open level up menu",
                (int) (centerX / 0.75f), (int) ((y + 12) / 0.75f), subColor);
        gui.pose().popPose();
    }

    private static int getResourceColor(ResourceType type) {
        return switch (type) {
            case STAMINA -> COLOR_STAMINA;
            case MANA -> COLOR_MANA;
            case FAITH -> COLOR_FAITH;
            case FOCUS -> COLOR_FOCUS;
            case RAGE -> COLOR_RAGE;
            case NONE -> COLOR_NONE;
        };
    }
}
