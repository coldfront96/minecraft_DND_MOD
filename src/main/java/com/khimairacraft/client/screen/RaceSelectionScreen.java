package com.khimairacraft.client.screen;

import com.khimairacraft.network.SelectRacePayload;
import com.khimairacraft.race.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RaceSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 260;

    private static final int HEADER_HEIGHT = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int LIST_WIDTH = 160;
    private static final int DETAIL_LEFT_MARGIN = 10;
    private static final int DETAIL_WIDTH = 180;
    private static final int RACE_ROW_HEIGHT = 22;
    private static final int RACE_ROW_GAP = 3;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;

    private static final int COLOR_BG = 0xCC111111;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_TAB_ACTIVE_UNDERLINE = 0xFF40C040;
    private static final int COLOR_ROW_NORMAL = 0xFF1A1A2A;
    private static final int COLOR_ROW_HOVER = 0xFF2A2A3A;
    private static final int COLOR_ROW_SELECTED = 0xFF1A3A1A;
    private static final int COLOR_ROW_SELECTED_BORDER = 0xFF40C040;
    private static final int COLOR_BUTTON_INACTIVE_BG = 0xFF404040;
    private static final int COLOR_BUTTON_ACTIVE_BG = 0xFF1A3A1A;
    private static final int COLOR_BUTTON_ACTIVE_BORDER = 0xFF40C040;
    private static final int COLOR_MOD_POSITIVE = 0xFF40C040;
    private static final int COLOR_MOD_NEGATIVE = 0xFFC04040;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFF808080;
    private static final int TEXT_SUBTITLE = 0xFFA0A0A0;
    private static final int TEXT_PLACEHOLDER = 0xFF606060;
    private static final int TEXT_TRAIT = 0xFFC0C0C0;
    private static final int TEXT_FLAVOR = 0xFF909090;

    private final List<SourceTab> tabs = new ArrayList<>();
    private int selectedTabIndex = 0;
    private int scrollOffset = 0;

    @Nullable
    private DnDRace selectedRace = null;
    @Nullable
    private DnDRace hoveredRace = null;

    public RaceSelectionScreen() {
        super(Component.literal("Choose Your Race"));
    }

    @Override
    protected void init() {
        super.init();
        buildTabs();
        selectedTabIndex = 0;
        scrollOffset = 0;
        selectedRace = null;
        hoveredRace = null;
    }

    private void buildTabs() {
        tabs.clear();
        tabs.add(new SourceTab("Core", RaceSource.CORE));
        if (isSourceAvailable(RaceSource.EXPANDED)) {
            tabs.add(new SourceTab("Expanded", RaceSource.EXPANDED));
        }
        if (isSourceAvailable(RaceSource.UNDEAD)) {
            tabs.add(new SourceTab("Undead", RaceSource.UNDEAD));
        }
    }

    private boolean isSourceAvailable(RaceSource source) {
        try {
            return RaceConfig.isSourceEnabled(source);
        } catch (Exception e) {
            return true;
        }
    }

    private List<RaceDefinition> getRacesForCurrentTab() {
        if (tabs.isEmpty()) return List.of();
        SourceTab tab = tabs.get(selectedTabIndex);
        return RaceRegistry.getBySource(tab.source);
    }

    @Nullable
    private DnDRace getDisplayRace() {
        return hoveredRace != null ? hoveredRace : selectedRace;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        renderPanelBackground(graphics, left, top);
        renderHeader(graphics, left, top);
        renderTabs(graphics, left, top, mouseX, mouseY);

        int contentTop = top + HEADER_HEIGHT + TAB_HEIGHT + 4;
        hoveredRace = null;
        renderRaceList(graphics, left, contentTop, mouseX, mouseY);
        renderDetailPanel(graphics, left, contentTop);
        renderConfirmButton(graphics, left, top, mouseX, mouseY);
    }

    private void renderPanelBackground(GuiGraphics graphics, int left, int top) {
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BG);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, COLOR_BORDER);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);
    }

    private void renderHeader(GuiGraphics graphics, int left, int top) {
        String title = "Choose Your Race";
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, left + (PANEL_WIDTH - titleWidth) / 2, top + 4, TEXT_WHITE, false);

        String subtitle = "Your race cannot be changed after selection.";
        drawScaledString(graphics, subtitle,
                left + (PANEL_WIDTH - (int)(this.font.width(subtitle) * 0.75f)) / 2,
                top + 15, TEXT_SUBTITLE, 0.75f);
    }

    private void renderTabs(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int tabY = top + HEADER_HEIGHT;
        int currentX = left + 8;

        for (int i = 0; i < tabs.size(); i++) {
            SourceTab tab = tabs.get(i);
            int tabWidth = this.font.width(tab.label) + 12;
            boolean isSelected = i == selectedTabIndex;

            int textColor = isSelected ? TEXT_WHITE : TEXT_GRAY;
            graphics.drawString(this.font, tab.label, currentX + 6, tabY + 6, textColor, false);

            if (isSelected) {
                graphics.fill(currentX, tabY + TAB_HEIGHT - 2, currentX + tabWidth, tabY + TAB_HEIGHT, COLOR_TAB_ACTIVE_UNDERLINE);
            }

            currentX += tabWidth + 4;
        }
    }

    private void renderRaceList(GuiGraphics graphics, int left, int contentTop, int mouseX, int mouseY) {
        int listLeft = left + 8;
        int listTop = contentTop;
        int listBottom = left + PANEL_HEIGHT - 30 + (this.height - PANEL_HEIGHT) / 2;

        List<RaceDefinition> races = getRacesForCurrentTab();
        if (races.isEmpty()) {
            drawScaledString(graphics, "No races available", listLeft + 4, listTop + 4, TEXT_PLACEHOLDER, 0.85f);
            return;
        }

        int rowStep = RACE_ROW_HEIGHT + RACE_ROW_GAP;
        int visibleHeight = listBottom - listTop;
        int maxVisible = visibleHeight / rowStep;
        int maxScroll = Math.max(0, races.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < maxVisible && (i + scrollOffset) < races.size(); i++) {
            RaceDefinition def = races.get(i + scrollOffset);
            int rowY = listTop + i * rowStep;

            if (rowY + RACE_ROW_HEIGHT > listBottom) break;

            boolean isSelected = def.getRace() == selectedRace;
            boolean isHovered = mouseX >= listLeft && mouseX < listLeft + LIST_WIDTH
                    && mouseY >= rowY && mouseY < rowY + RACE_ROW_HEIGHT;

            int bgColor;
            if (isSelected) {
                bgColor = COLOR_ROW_SELECTED;
            } else if (isHovered) {
                bgColor = COLOR_ROW_HOVER;
            } else {
                bgColor = COLOR_ROW_NORMAL;
            }

            graphics.fill(listLeft, rowY, listLeft + LIST_WIDTH, rowY + RACE_ROW_HEIGHT, bgColor);

            if (isSelected) {
                graphics.fill(listLeft, rowY, listLeft + 2, rowY + RACE_ROW_HEIGHT, COLOR_ROW_SELECTED_BORDER);
            }

            graphics.drawString(this.font, def.getDisplayName(), listLeft + 6, rowY + 7, TEXT_WHITE, false);

            if (isHovered) {
                hoveredRace = def.getRace();
            }
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int left, int contentTop) {
        int detailLeft = left + 8 + LIST_WIDTH + DETAIL_LEFT_MARGIN;
        int detailRight = left + PANEL_WIDTH - 8;
        int detailWidth = detailRight - detailLeft;
        int y = contentTop;

        DnDRace displayRace = getDisplayRace();
        if (displayRace == null) {
            String placeholder = "Select a race to see details";
            int pw = (int)(this.font.width(placeholder) * 0.85f);
            drawScaledString(graphics, placeholder, detailLeft + (detailWidth - pw) / 2,
                    contentTop + 40, TEXT_PLACEHOLDER, 0.85f);
            return;
        }

        RaceDefinition def = RaceRegistry.get(displayRace);
        if (def == null) return;

        drawScaledString(graphics, def.getDisplayName(), detailLeft, y, TEXT_WHITE, 0.9f);
        y += 12;

        String sourceLabel = getSourceLabel(def.getSource());
        drawScaledString(graphics, sourceLabel, detailLeft, y, TEXT_GRAY, 0.75f);
        y += 14;

        drawScaledString(graphics, "Ability Score Modifiers", detailLeft, y, TEXT_SUBTITLE, 0.8f);
        y += 11;

        AbilityScoreModifiers mods = def.getModifiers();
        boolean hasAnyMod = false;

        y = renderModLine(graphics, "Strength", mods.getStrMod(), detailLeft, y);
        y = renderModLine(graphics, "Dexterity", mods.getDexMod(), detailLeft, y);
        y = renderModLine(graphics, "Constitution", mods.getConMod(), detailLeft, y);
        y = renderModLine(graphics, "Intelligence", mods.getIntMod(), detailLeft, y);
        y = renderModLine(graphics, "Wisdom", mods.getWisMod(), detailLeft, y);
        y = renderModLine(graphics, "Charisma", mods.getChaMod(), detailLeft, y);

        hasAnyMod = mods.getStrMod() != 0 || mods.getDexMod() != 0 || mods.getConMod() != 0
                || mods.getIntMod() != 0 || mods.getWisMod() != 0 || mods.getChaMod() != 0;

        if (!hasAnyMod) {
            if (displayRace == DnDRace.HUMAN) {
                drawScaledString(graphics, "+1 to All Ability Scores", detailLeft + 4, y, COLOR_MOD_POSITIVE, 0.75f);
            } else {
                drawScaledString(graphics, "No modifiers", detailLeft + 4, y, TEXT_PLACEHOLDER, 0.75f);
            }
            y += 10;
        }

        y += 4;
        drawScaledString(graphics, "Racial Traits", detailLeft, y, TEXT_SUBTITLE, 0.8f);
        y += 11;

        List<String> traitKeys = def.getTraitKeys();
        if (traitKeys.isEmpty()) {
            drawScaledString(graphics, "No traits yet", detailLeft + 4, y, TEXT_PLACEHOLDER, 0.75f);
            y += 10;
        } else {
            for (String key : traitKeys) {
                String traitName = formatTraitKey(key);
                drawScaledString(graphics, "• " + traitName, detailLeft + 4, y, TEXT_TRAIT, 0.75f);
                y += 10;
            }
        }

        y += 4;
        String desc = def.getDescription();
        if (desc != null && !desc.isEmpty()) {
            List<String> lines = wrapText(desc, detailWidth - 4, 0.75f);
            for (String line : lines) {
                drawScaledString(graphics, line, detailLeft, y, TEXT_FLAVOR, 0.75f);
                y += 9;
            }
        }
    }

    private int renderModLine(GuiGraphics graphics, String statName, int mod, int x, int y) {
        if (mod == 0) return y;
        String sign = mod > 0 ? "+" : "";
        String text = sign + mod + " " + statName;
        int color = mod > 0 ? COLOR_MOD_POSITIVE : COLOR_MOD_NEGATIVE;
        drawScaledString(graphics, text, x + 4, y, color, 0.75f);
        return y + 10;
    }

    private String getSourceLabel(RaceSource source) {
        return switch (source) {
            case CORE -> "Core — Player's Handbook";
            case EXPANDED -> "Expanded";
            case UNDEAD -> "Undead";
        };
    }

    private String formatTraitKey(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private List<String> wrapText(String text, int maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        int scaledMax = (int)(maxWidth / scale);
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() > 0) {
                String test = current + " " + word;
                if (this.font.width(test) > scaledMax) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current.append(" ").append(word);
                }
            } else {
                current.append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private void renderConfirmButton(GuiGraphics graphics, int panelLeft, int panelTop, int mouseX, int mouseY) {
        int btnX = panelLeft + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int btnY = panelTop + PANEL_HEIGHT - 26;
        boolean active = selectedRace != null;

        int bgColor = active ? COLOR_BUTTON_ACTIVE_BG : COLOR_BUTTON_INACTIVE_BG;
        graphics.fill(btnX, btnY, btnX + BUTTON_WIDTH, btnY + BUTTON_HEIGHT, bgColor);

        if (active) {
            graphics.fill(btnX, btnY, btnX + BUTTON_WIDTH, btnY + 1, COLOR_BUTTON_ACTIVE_BORDER);
            graphics.fill(btnX, btnY + BUTTON_HEIGHT - 1, btnX + BUTTON_WIDTH, btnY + BUTTON_HEIGHT, COLOR_BUTTON_ACTIVE_BORDER);
            graphics.fill(btnX, btnY, btnX + 1, btnY + BUTTON_HEIGHT, COLOR_BUTTON_ACTIVE_BORDER);
            graphics.fill(btnX + BUTTON_WIDTH - 1, btnY, btnX + BUTTON_WIDTH, btnY + BUTTON_HEIGHT, COLOR_BUTTON_ACTIVE_BORDER);
        }

        String btnText = "Confirm Race";
        int textColor = active ? TEXT_WHITE : TEXT_GRAY;
        int textWidth = this.font.width(btnText);
        graphics.drawString(this.font, btnText, btnX + (BUTTON_WIDTH - textWidth) / 2, btnY + 6, textColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        if (handleTabClick(mouseX, mouseY, left, top)) return true;
        if (handleRaceListClick(mouseX, mouseY, left, top)) return true;
        if (handleConfirmClick(mouseX, mouseY, left, top)) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTabClick(double mouseX, double mouseY, int left, int top) {
        int tabY = top + HEADER_HEIGHT;
        int currentX = left + 8;

        for (int i = 0; i < tabs.size(); i++) {
            SourceTab tab = tabs.get(i);
            int tabWidth = this.font.width(tab.label) + 12;

            if (mouseX >= currentX && mouseX < currentX + tabWidth
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                selectedTabIndex = i;
                scrollOffset = 0;
                return true;
            }
            currentX += tabWidth + 4;
        }
        return false;
    }

    private boolean handleRaceListClick(double mouseX, double mouseY, int left, int top) {
        int listLeft = left + 8;
        int contentTop = top + HEADER_HEIGHT + TAB_HEIGHT + 4;
        int listBottom = top + PANEL_HEIGHT - 30;

        if (mouseX < listLeft || mouseX >= listLeft + LIST_WIDTH) return false;
        if (mouseY < contentTop || mouseY >= listBottom) return false;

        List<RaceDefinition> races = getRacesForCurrentTab();
        int rowStep = RACE_ROW_HEIGHT + RACE_ROW_GAP;

        for (int i = 0; i + scrollOffset < races.size(); i++) {
            int rowY = contentTop + i * rowStep;
            if (rowY + RACE_ROW_HEIGHT > listBottom) break;

            if (mouseY >= rowY && mouseY < rowY + RACE_ROW_HEIGHT) {
                selectedRace = races.get(i + scrollOffset).getRace();
                return true;
            }
        }
        return false;
    }

    private boolean handleConfirmClick(double mouseX, double mouseY, int left, int top) {
        if (selectedRace == null) return false;

        int btnX = left + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int btnY = top + PANEL_HEIGHT - 26;

        if (mouseX >= btnX && mouseX < btnX + BUTTON_WIDTH
                && mouseY >= btnY && mouseY < btnY + BUTTON_HEIGHT) {
            PacketDistributor.sendToServer(new SelectRacePayload(selectedRace.name()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int listLeft = left + 8;
        int contentTop = top + HEADER_HEIGHT + TAB_HEIGHT + 4;
        int listBottom = top + PANEL_HEIGHT - 30;

        if (mouseX >= listLeft && mouseX < listLeft + LIST_WIDTH
                && mouseY >= contentTop && mouseY < listBottom) {
            List<RaceDefinition> races = getRacesForCurrentTab();
            int rowStep = RACE_ROW_HEIGHT + RACE_ROW_GAP;
            int maxVisible = (listBottom - contentTop) / rowStep;
            int maxScroll = Math.max(0, races.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawScaledString(GuiGraphics graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, (int)(x / scale), (int)(y / scale), color, false);
        graphics.pose().popPose();
    }

    private record SourceTab(String label, RaceSource source) {}
}
