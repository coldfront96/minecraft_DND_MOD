package com.khimairacraft.client.screen;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityRegistry;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.ClassEntry;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class AbilityPickerOverlay {

    private static final int PANEL_WIDTH = 160;
    private static final int PANEL_HEIGHT = 180;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 2;
    private static final int TITLE_HEIGHT = 16;

    private static final int COLOR_BG = 0xFF111111;
    private static final int COLOR_BORDER = 0xFF505050;
    private static final int COLOR_ROW_DEFAULT = 0xFF1A2A1A;
    private static final int COLOR_ROW_HOVER = 0xFF2A3A2A;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_COST = 0xFFEEDD88;

    private final int slotIndex;
    private final List<Ability> abilities;
    private final BiConsumer<Integer, Ability> onAssign;
    private int scrollOffset = 0;
    private boolean visible = true;

    public AbilityPickerOverlay(int slotIndex, DnDPlayerData data, BiConsumer<Integer, Ability> onAssign) {
        this.slotIndex = slotIndex;
        this.onAssign = onAssign;
        this.abilities = buildUnlockedAbilities(data);
    }

    private List<Ability> buildUnlockedAbilities(DnDPlayerData data) {
        List<Ability> result = new ArrayList<>();
        addUnlocked(result, data.getPrimary());
        ClassEntry secondary = data.getSecondary();
        if (secondary != null && secondary.getDnDClass() != DnDClass.NONE) {
            addUnlocked(result, secondary);
        }
        return result;
    }

    private void addUnlocked(List<Ability> result, ClassEntry classEntry) {
        for (Ability ability : AbilityRegistry.getAbilitiesForClass(classEntry.getDnDClass())) {
            if (classEntry.getLevel() >= ability.getRequiredLevel()) {
                result.add(ability);
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void close() {
        this.visible = false;
    }

    public void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight,
                       int tabPanelLeft, int tabPanelWidth, int mouseX, int mouseY) {
        int left = tabPanelLeft + (tabPanelWidth - PANEL_WIDTH) / 2;
        int top = (screenHeight - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BG);
        renderBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        String title = "Assign to Slot " + (slotIndex + 1);
        int textWidth = font.width(title);
        graphics.drawString(font, title, left + (PANEL_WIDTH - textWidth) / 2, top + 4, TEXT_WHITE, false);

        int contentTop = top + TITLE_HEIGHT;
        int contentBottom = top + PANEL_HEIGHT - 2;
        int contentHeight = contentBottom - contentTop;
        int rowStep = ROW_HEIGHT + ROW_GAP;
        int maxVisible = contentHeight / rowStep;
        int maxScroll = Math.max(0, abilities.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < maxVisible && (i + scrollOffset) < abilities.size(); i++) {
            Ability ability = abilities.get(i + scrollOffset);
            int rowY = contentTop + i * rowStep;

            if (rowY + ROW_HEIGHT > contentBottom) break;

            boolean hovered = mouseX >= left + 2 && mouseX < left + PANEL_WIDTH - 2
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            int bgColor = hovered ? COLOR_ROW_HOVER : COLOR_ROW_DEFAULT;
            graphics.fill(left + 2, rowY, left + PANEL_WIDTH - 2, rowY + ROW_HEIGHT, bgColor);

            graphics.drawString(font, ability.getName(), left + 5, rowY + 5, TEXT_WHITE, false);

            int cost = ability.getResourceCost();
            if (cost > 0) {
                String costText = cost + " " + ability.getRequiredClass().getResourceType().getDisplayName();
                int costWidth = font.width(costText);
                graphics.drawString(font, costText, left + PANEL_WIDTH - costWidth - 5, rowY + 5, TEXT_COST, false);
            }
        }
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight,
                                 int tabPanelLeft, int tabPanelWidth) {
        int left = tabPanelLeft + (tabPanelWidth - PANEL_WIDTH) / 2;
        int top = (screenHeight - PANEL_HEIGHT) / 2;

        if (mouseX < left || mouseX >= left + PANEL_WIDTH || mouseY < top || mouseY >= top + PANEL_HEIGHT) {
            close();
            return true;
        }

        if (button != 0) return true;

        int contentTop = top + TITLE_HEIGHT;
        int contentBottom = top + PANEL_HEIGHT - 2;
        int rowStep = ROW_HEIGHT + ROW_GAP;
        int maxVisible = (contentBottom - contentTop) / rowStep;

        for (int i = 0; i < maxVisible && (i + scrollOffset) < abilities.size(); i++) {
            int rowY = contentTop + i * rowStep;
            if (mouseX >= left + 2 && mouseX < left + PANEL_WIDTH - 2
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                Ability selected = abilities.get(i + scrollOffset);
                onAssign.accept(slotIndex, selected);
                close();
                return true;
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY,
                                  int screenWidth, int screenHeight, int tabPanelLeft, int tabPanelWidth) {
        int left = tabPanelLeft + (tabPanelWidth - PANEL_WIDTH) / 2;
        int top = (screenHeight - PANEL_HEIGHT) / 2;

        if (mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= top && mouseY < top + PANEL_HEIGHT) {
            int contentTop = top + TITLE_HEIGHT;
            int contentBottom = top + PANEL_HEIGHT - 2;
            int rowStep = ROW_HEIGHT + ROW_GAP;
            int maxVisible = (contentBottom - contentTop) / rowStep;
            int maxScroll = Math.max(0, abilities.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
            return true;
        }
        return false;
    }
}
