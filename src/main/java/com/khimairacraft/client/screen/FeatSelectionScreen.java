package com.khimairacraft.client.screen;

import com.khimairacraft.feat.*;
import com.khimairacraft.network.SelectFeatPayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FeatSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 280;
    private static final int CAT_TAB_WIDTH = 80;
    private static final int FEAT_LIST_WIDTH = 200;
    private static final int DETAIL_WIDTH = 180;
    private static final int DETAIL_MARGIN = 10;
    private static final int ROW_HEIGHT = 20;

    private static final int COLOR_BG = 0xCC111111;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_TAB_ACTIVE_BG = 0xFF1A1A1A;
    private static final int COLOR_TAB_INACTIVE_BG = 0xFF0D0D0D;
    private static final int COLOR_TAB_ACTIVE_BORDER = 0xFF40C040;
    private static final int COLOR_AVAILABLE = 0xFF1A2A1A;
    private static final int COLOR_GRANTED = 0xFF1A1A2A;
    private static final int COLOR_LOCKED = 0xFF1A1A1A;
    private static final int COLOR_CHAIN_HEADER = 0xFF303030;
    private static final int COLOR_CONFIRM_ACTIVE = 0xFF1A3A1A;
    private static final int COLOR_CONFIRM_INACTIVE = 0xFF404040;
    private static final int COLOR_CONFIRM_BORDER = 0xFF40C040;

    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFF808080;
    private static final int TEXT_GRANTED = 0xFF6060C0;
    private static final int TEXT_LOCKED = 0xFF606060;
    private static final int TEXT_GREEN = 0xFF40C040;
    private static final int TEXT_RED = 0xFFC04040;
    private static final int TEXT_DETAIL_DESC = 0xFFC0C0C0;
    private static final int TEXT_DETAIL_LABEL = 0xFFA0A0A0;
    private static final int TEXT_PLACEHOLDER = 0xFF606060;

    private final boolean isRacialBonusSlot;
    private FeatCategory selectedCategory = FeatCategory.GENERAL;
    @Nullable
    private Feat selectedFeat = null;
    private int scrollOffset = 0;

    public FeatSelectionScreen(boolean isRacialBonusSlot) {
        super(Component.literal("Feat Selection"));
        this.isRacialBonusSlot = isRacialBonusSlot;
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

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BG);
        renderBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        String title = isRacialBonusSlot ? "Racial Bonus Feat" : "Feat Selection";
        int tw = this.font.width(title);
        graphics.drawString(this.font, title, left + (PANEL_WIDTH - tw) / 2, top + 4, TEXT_WHITE, false);

        renderCategoryTabs(graphics, left, top + 18);
        renderFeatList(graphics, left + CAT_TAB_WIDTH, top + 18, mouseX, mouseY);
        renderDetailPanel(graphics, left + CAT_TAB_WIDTH + FEAT_LIST_WIDTH + DETAIL_MARGIN, top + 18);
        renderConfirmButton(graphics, left, top, mouseX, mouseY);
    }

    private void renderCategoryTabs(GuiGraphics graphics, int left, int top) {
        FeatCategory[] categories = FeatCategory.values();
        int tabHeight = 22;

        for (int i = 0; i < categories.length; i++) {
            int tabY = top + i * (tabHeight + 1);
            boolean active = categories[i] == selectedCategory;

            int bgColor = active ? COLOR_TAB_ACTIVE_BG : COLOR_TAB_INACTIVE_BG;
            graphics.fill(left, tabY, left + CAT_TAB_WIDTH, tabY + tabHeight, bgColor);

            if (active) {
                graphics.fill(left, tabY, left + 2, tabY + tabHeight, COLOR_TAB_ACTIVE_BORDER);
            }

            int textColor = active ? TEXT_WHITE : TEXT_GRAY;
            String name = categories[i].getDisplayName();
            if (this.font.width(name) > CAT_TAB_WIDTH - 8) {
                name = name.substring(0, Math.min(name.length(), 8)) + "..";
            }
            graphics.drawString(this.font, name, left + 6, tabY + 7, textColor, false);
        }
    }

    private void renderFeatList(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        DnDPlayerData data = getPlayerData();
        if (data == null) return;

        List<Feat> feats = FeatRegistry.getByCategory(selectedCategory);
        List<FeatChainHelper.FeatChain> chains = FeatChainHelper.buildChains(feats);

        int y = top - scrollOffset * ROW_HEIGHT;
        int contentBottom = top + PANEL_HEIGHT - 50;
        int rowIndex = 0;

        for (FeatChainHelper.FeatChain chain : chains) {
            if (chain.chainGroup() != null && chain.feats().size() > 1) {
                if (y >= top && y < contentBottom) {
                    String headerName = FeatChainHelper.chainGroupToDisplayName(chain.chainGroup());
                    graphics.fill(left, y, left + FEAT_LIST_WIDTH, y + 14, COLOR_CHAIN_HEADER);
                    graphics.drawString(this.font, headerName, left + 4, y + 3, TEXT_GRAY, false);
                }
                y += 16;
            }

            for (Feat feat : chain.feats()) {
                if (y >= top && y < contentBottom) {
                    renderFeatRow(graphics, feat, data, left, y, chain.chainGroup() != null);
                }
                y += ROW_HEIGHT;
                rowIndex++;
            }
            y += 2;
        }
    }

    private void renderFeatRow(GuiGraphics graphics, Feat feat, DnDPlayerData data, int left, int y, boolean indented) {
        boolean granted = data.hasFeat(feat.getFeatId());
        boolean available = !granted && feat.allPrerequisitesMet(data);
        boolean selected = selectedFeat != null && selectedFeat.getFeatId().equals(feat.getFeatId());

        int bgColor;
        int textColor;
        String prefix = "";

        if (granted) {
            bgColor = COLOR_GRANTED;
            textColor = TEXT_GRANTED;
            prefix = "✓ ";
        } else if (available) {
            bgColor = COLOR_AVAILABLE;
            textColor = TEXT_WHITE;
        } else {
            bgColor = COLOR_LOCKED;
            textColor = TEXT_LOCKED;
            prefix = "🔒 ";
        }

        int indent = indented ? 8 : 0;
        graphics.fill(left + indent, y, left + FEAT_LIST_WIDTH, y + ROW_HEIGHT - 1, bgColor);

        if (selected) {
            renderBorder(graphics, left + indent, y, FEAT_LIST_WIDTH - indent, ROW_HEIGHT - 1, COLOR_CONFIRM_BORDER);
        }

        String displayText = prefix + feat.getDisplayName();
        if (this.font.width(displayText) > FEAT_LIST_WIDTH - indent - 8) {
            while (this.font.width(displayText + "..") > FEAT_LIST_WIDTH - indent - 8 && displayText.length() > 1) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += "..";
        }
        graphics.drawString(this.font, displayText, left + indent + 4, y + 6, textColor, false);
    }

    private void renderDetailPanel(GuiGraphics graphics, int left, int top) {
        DnDPlayerData data = getPlayerData();
        int contentBottom = top + PANEL_HEIGHT - 50;

        if (selectedFeat == null) {
            String msg = "Select a feat to";
            String msg2 = "see details";
            graphics.drawString(this.font, msg, left + (DETAIL_WIDTH - DETAIL_MARGIN - this.font.width(msg)) / 2,
                    top + 60, TEXT_PLACEHOLDER, false);
            graphics.drawString(this.font, msg2, left + (DETAIL_WIDTH - DETAIL_MARGIN - this.font.width(msg2)) / 2,
                    top + 72, TEXT_PLACEHOLDER, false);
            return;
        }

        int y = top + 4;

        graphics.drawString(this.font, selectedFeat.getDisplayName(), left, y, TEXT_WHITE, false);
        y += 12;

        graphics.drawString(this.font, selectedFeat.getSource().getDisplayName(), left, y, TEXT_GRAY, false);
        y += 10;

        graphics.drawString(this.font, selectedFeat.getCategory().getDisplayName(), left, y, TEXT_GRAY, false);
        y += 14;

        String desc = selectedFeat.getDescription();
        List<String> wrappedDesc = wrapText(desc, DETAIL_WIDTH - DETAIL_MARGIN - 4);
        for (String line : wrappedDesc) {
            if (y + 10 > contentBottom) break;
            graphics.drawString(this.font, line, left, y, TEXT_DETAIL_DESC, false);
            y += 10;
        }
        y += 6;

        if (y + 10 < contentBottom) {
            graphics.drawString(this.font, "Requirements:", left, y, TEXT_DETAIL_LABEL, false);
            y += 10;
        }

        List<FeatPrerequisite> prereqs = selectedFeat.getPrerequisites();
        if (prereqs.isEmpty()) {
            if (y + 10 < contentBottom) {
                graphics.drawString(this.font, "None", left + 4, y, TEXT_PLACEHOLDER, false);
            }
        } else if (data != null) {
            for (FeatPrerequisite prereq : prereqs) {
                if (y + 10 > contentBottom) break;
                boolean met = prereq.isMet(data);
                String prefix = met ? "✓ " : "✗ ";
                int color = met ? TEXT_GREEN : TEXT_RED;
                graphics.drawString(this.font, prefix + prereq.getDescription(), left + 4, y, color, false);
                y += 10;
            }
        }
    }

    private void renderConfirmButton(GuiGraphics graphics, int panelLeft, int panelTop, int mouseX, int mouseY) {
        DnDPlayerData data = getPlayerData();
        boolean canConfirm = selectedFeat != null && data != null
                && !data.hasFeat(selectedFeat.getFeatId())
                && selectedFeat.allPrerequisitesMet(data);

        int btnW = 120;
        int btnH = 20;
        int btnX = panelLeft + (PANEL_WIDTH - btnW) / 2;
        int btnY = panelTop + PANEL_HEIGHT - 28;

        int bgColor = canConfirm ? COLOR_CONFIRM_ACTIVE : COLOR_CONFIRM_INACTIVE;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgColor);

        if (canConfirm) {
            renderBorder(graphics, btnX, btnY, btnW, btnH, COLOR_CONFIRM_BORDER);
        }

        String text = "Select Feat";
        int textColor = canConfirm ? TEXT_WHITE : TEXT_GRAY;
        int textX = btnX + (btnW - this.font.width(text)) / 2;
        graphics.drawString(this.font, text, textX, btnY + 6, textColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        if (handleCategoryTabClick(mouseX, mouseY, left, top + 18)) return true;
        if (handleFeatListClick(mouseX, mouseY, left + CAT_TAB_WIDTH, top + 18)) return true;
        if (handleConfirmClick(mouseX, mouseY, left, top)) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCategoryTabClick(double mouseX, double mouseY, int left, int top) {
        FeatCategory[] categories = FeatCategory.values();
        int tabHeight = 22;

        for (int i = 0; i < categories.length; i++) {
            int tabY = top + i * (tabHeight + 1);
            if (mouseX >= left && mouseX < left + CAT_TAB_WIDTH
                    && mouseY >= tabY && mouseY < tabY + tabHeight) {
                selectedCategory = categories[i];
                selectedFeat = null;
                scrollOffset = 0;
                return true;
            }
        }
        return false;
    }

    private boolean handleFeatListClick(double mouseX, double mouseY, int left, int top) {
        DnDPlayerData data = getPlayerData();
        if (data == null) return false;

        List<Feat> feats = FeatRegistry.getByCategory(selectedCategory);
        List<FeatChainHelper.FeatChain> chains = FeatChainHelper.buildChains(feats);

        int y = top - scrollOffset * ROW_HEIGHT;
        int contentBottom = top + PANEL_HEIGHT - 50;

        for (FeatChainHelper.FeatChain chain : chains) {
            if (chain.chainGroup() != null && chain.feats().size() > 1) {
                y += 16;
            }

            for (Feat feat : chain.feats()) {
                if (y >= top && y < contentBottom) {
                    if (mouseX >= left && mouseX < left + FEAT_LIST_WIDTH
                            && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                        if (!data.hasFeat(feat.getFeatId())) {
                            selectedFeat = feat;
                        }
                        return true;
                    }
                }
                y += ROW_HEIGHT;
            }
            y += 2;
        }
        return false;
    }

    private boolean handleConfirmClick(double mouseX, double mouseY, int panelLeft, int panelTop) {
        DnDPlayerData data = getPlayerData();
        if (selectedFeat == null || data == null) return false;
        if (data.hasFeat(selectedFeat.getFeatId())) return false;
        if (!selectedFeat.allPrerequisitesMet(data)) return false;

        int btnW = 120;
        int btnH = 20;
        int btnX = panelLeft + (PANEL_WIDTH - btnW) / 2;
        int btnY = panelTop + PANEL_HEIGHT - 28;

        if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
            PacketDistributor.sendToServer(new SelectFeatPayload(selectedFeat.getFeatId(), isRacialBonusSlot));
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (this.width - PANEL_WIDTH) / 2 + CAT_TAB_WIDTH;
        int top = (this.height - PANEL_HEIGHT) / 2 + 18;

        if (mouseX >= left && mouseX < left + FEAT_LIST_WIDTH
                && mouseY >= top && mouseY < top + PANEL_HEIGHT - 50) {
            scrollOffset = Math.max(0, scrollOffset - (int) scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (!current.isEmpty() && this.font.width(current + " " + word) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (!current.isEmpty()) current.append(" ");
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
