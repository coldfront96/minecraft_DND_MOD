package com.deadmind.dndmods.client.screen;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.playerdata.ClassEntry;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AbilityManagerScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;

    private static final int HOTBAR_PANEL_WIDTH = 80;
    private static final int SLOT_WIDTH = 60;
    private static final int SLOT_HEIGHT = 18;
    private static final int SLOT_GAP = 2;

    private static final int TAB_PANEL_LEFT_MARGIN = 10;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_GAP = 2;
    private static final int TAB_PANEL_WIDTH = 230;

    private static final int ABILITY_ROW_HEIGHT = 18;
    private static final int ABILITY_ROW_GAP = 2;

    private static final int COLOR_BG = 0xCC111111;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_SLOT_EMPTY = 0xFF202020;
    private static final int COLOR_SLOT_EMPTY_BORDER = 0xFF404040;
    private static final int COLOR_SLOT_OCCUPIED = 0xFF303030;
    private static final int COLOR_TAB_ACTIVE = 0xFF333333;
    private static final int COLOR_TAB_INACTIVE = 0xFF1A1A1A;
    private static final int COLOR_ABILITY_UNLOCKED = 0xFF1A2A1A;
    private static final int COLOR_ABILITY_LOCKED = 0xFF1A1A1A;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFF808080;
    private static final int TEXT_LOCKED_NAME = 0xFF606060;
    private static final int TEXT_LOCKED_LEVEL = 0xFF804040;
    private static final int TEXT_COST = 0xFFEEDD88;

    private final List<TabEntry> tabs = new ArrayList<>();
    private int selectedTab = 0;
    private int scrollOffset = 0;

    public AbilityManagerScreen() {
        super(Component.literal("Ability Manager"));
    }

    @Override
    protected void init() {
        super.init();
        buildTabs();
        selectedTab = 0;
        scrollOffset = 0;
    }

    private void buildTabs() {
        tabs.clear();

        DnDPlayerData data = getPlayerData();
        if (data == null) return;

        tabs.add(new TabEntry("All", null));
        tabs.add(new TabEntry("Feats", null));
        tabs.add(new TabEntry("Skills", null));

        DnDClass primaryClass = data.getPrimary().getDnDClass();
        DnDClass secondaryClass = data.getSecondary() != null ? data.getSecondary().getDnDClass() : DnDClass.NONE;

        for (DnDClass cls : new DnDClass[]{DnDClass.FIGHTER, DnDClass.WIZARD, DnDClass.ROGUE,
                DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN}) {
            if (primaryClass == cls || secondaryClass == cls) {
                tabs.add(new TabEntry(cls.getDisplayName(), cls));
            }
        }
    }

    @Nullable
    private DnDPlayerData getPlayerData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return mc.player.getData(ModAttachments.PLAYER_DATA);
    }

    private List<AbilityDisplayEntry> getAbilitiesForCurrentTab() {
        DnDPlayerData data = getPlayerData();
        if (data == null) return List.of();
        if (tabs.isEmpty()) return List.of();

        TabEntry tab = tabs.get(selectedTab);

        if ("Feats".equals(tab.name) || "Skills".equals(tab.name)) {
            return List.of();
        }

        List<AbilityDisplayEntry> entries = new ArrayList<>();

        if ("All".equals(tab.name)) {
            addUnlockedAbilities(entries, data, data.getPrimary());
            ClassEntry secondary = data.getSecondary();
            if (secondary != null && secondary.getDnDClass() != DnDClass.NONE) {
                addUnlockedAbilities(entries, data, secondary);
            }
        } else if (tab.dndClass != null) {
            ClassEntry classEntry = getClassEntry(data, tab.dndClass);
            if (classEntry != null) {
                addAllClassAbilities(entries, classEntry);
            }
        }

        return entries;
    }

    private void addUnlockedAbilities(List<AbilityDisplayEntry> entries, DnDPlayerData data, ClassEntry classEntry) {
        List<Ability> allForClass = AbilityRegistry.getAbilitiesForClass(classEntry.getDnDClass());
        for (Ability ability : allForClass) {
            if (classEntry.getLevel() >= ability.getRequiredLevel()) {
                entries.add(new AbilityDisplayEntry(ability, true, classEntry));
            }
        }
    }

    private void addAllClassAbilities(List<AbilityDisplayEntry> entries, ClassEntry classEntry) {
        List<Ability> allForClass = AbilityRegistry.getAbilitiesForClass(classEntry.getDnDClass());
        List<Ability> sorted = new ArrayList<>(allForClass);
        sorted.sort((a, b) -> Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));
        for (Ability ability : sorted) {
            boolean unlocked = classEntry.getLevel() >= ability.getRequiredLevel();
            entries.add(new AbilityDisplayEntry(ability, unlocked, classEntry));
        }
    }

    @Nullable
    private ClassEntry getClassEntry(DnDPlayerData data, DnDClass dndClass) {
        if (data.getPrimary().getDnDClass() == dndClass) return data.getPrimary();
        ClassEntry secondary = data.getSecondary();
        if (secondary != null && secondary.getDnDClass() == dndClass) return secondary;
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        renderPanelBackground(graphics, left, top);
        renderHotbarPanel(graphics, left, top);
        renderTabBar(graphics, left, top, mouseX, mouseY);
        renderTabContent(graphics, left, top, mouseX, mouseY);
    }

    private void renderPanelBackground(GuiGraphics graphics, int left, int top) {
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BG);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, COLOR_BORDER);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);

        String title = "Ability Manager";
        int textWidth = this.font.width(title);
        graphics.drawString(this.font, title, left + (PANEL_WIDTH - textWidth) / 2, top + 4, TEXT_WHITE, false);
    }

    private void renderHotbarPanel(GuiGraphics graphics, int panelLeft, int panelTop) {
        int startX = panelLeft + 10;
        int startY = panelTop + 18;

        DnDPlayerData data = getPlayerData();
        AbilityHotbar hotbar = data != null ? data.getAbilityHotbar() : null;

        for (int i = 0; i < 9; i++) {
            int slotX = startX;
            int slotY = startY + i * (SLOT_HEIGHT + SLOT_GAP);

            String abilityId = hotbar != null ? hotbar.getSlotAbility(i) : null;
            Ability ability = abilityId != null ? AbilityRegistry.getAbility(abilityId) : null;
            boolean occupied = ability != null;

            int bgColor = occupied ? COLOR_SLOT_OCCUPIED : COLOR_SLOT_EMPTY;
            graphics.fill(slotX, slotY, slotX + SLOT_WIDTH, slotY + SLOT_HEIGHT, bgColor);
            renderBorder(graphics, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT, COLOR_SLOT_EMPTY_BORDER);

            if (occupied) {
                String name = truncateText(ability.getName(), SLOT_WIDTH - 4);
                graphics.drawString(this.font, name, slotX + 2, slotY + 5, TEXT_WHITE, false);
            } else {
                graphics.drawString(this.font, "Slot " + (i + 1), slotX + 2, slotY + 5, TEXT_WHITE, false);
            }
        }
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int getTabPanelLeft(int panelLeft) {
        return panelLeft + HOTBAR_PANEL_WIDTH + TAB_PANEL_LEFT_MARGIN;
    }

    private void renderTabBar(GuiGraphics graphics, int panelLeft, int panelTop, int mouseX, int mouseY) {
        int tabX = getTabPanelLeft(panelLeft);
        int tabY = panelTop + 18;
        int maxRight = tabX + TAB_PANEL_WIDTH;

        int currentX = tabX;
        for (int i = 0; i < tabs.size(); i++) {
            TabEntry tab = tabs.get(i);
            int tabWidth = this.font.width(tab.name) + 10;

            if (currentX + tabWidth > maxRight) break;

            boolean isSelected = i == selectedTab;
            boolean isHovered = mouseX >= currentX && mouseX < currentX + tabWidth
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;

            int bgColor = isSelected ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE;
            graphics.fill(currentX, tabY, currentX + tabWidth, tabY + TAB_HEIGHT, bgColor);

            if (isSelected) {
                graphics.fill(currentX, tabY + TAB_HEIGHT - 1, currentX + tabWidth, tabY + TAB_HEIGHT, 0xFF44AA44);
            }

            int textColor = isSelected ? TEXT_WHITE : TEXT_GRAY;
            graphics.drawString(this.font, tab.name, currentX + 5, tabY + 4, textColor, false);

            currentX += tabWidth + TAB_GAP;
        }
    }

    private void renderTabContent(GuiGraphics graphics, int panelLeft, int panelTop, int mouseX, int mouseY) {
        int contentLeft = getTabPanelLeft(panelLeft);
        int contentTop = panelTop + 18 + TAB_HEIGHT + 4;
        int contentBottom = panelTop + PANEL_HEIGHT - 8;

        if (tabs.isEmpty()) return;
        TabEntry tab = tabs.get(selectedTab);

        if ("Feats".equals(tab.name)) {
            graphics.drawString(this.font, "No feats assigned yet", contentLeft + 4, contentTop + 4, TEXT_GRAY, false);
            return;
        }
        if ("Skills".equals(tab.name)) {
            graphics.drawString(this.font, "No skills assigned yet", contentLeft + 4, contentTop + 4, TEXT_GRAY, false);
            return;
        }

        List<AbilityDisplayEntry> abilities = getAbilitiesForCurrentTab();
        if (abilities.isEmpty()) {
            graphics.drawString(this.font, "No abilities available", contentLeft + 4, contentTop + 4, TEXT_GRAY, false);
            return;
        }

        int rowStep = ABILITY_ROW_HEIGHT + ABILITY_ROW_GAP;
        int visibleHeight = contentBottom - contentTop;
        int maxVisible = visibleHeight / rowStep;
        int maxScroll = Math.max(0, abilities.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < maxVisible && (i + scrollOffset) < abilities.size(); i++) {
            AbilityDisplayEntry entry = abilities.get(i + scrollOffset);
            int rowY = contentTop + i * rowStep;

            if (rowY + ABILITY_ROW_HEIGHT > contentBottom) break;

            renderAbilityRow(graphics, entry, contentLeft, rowY, TAB_PANEL_WIDTH);
        }
    }

    private void renderAbilityRow(GuiGraphics graphics, AbilityDisplayEntry entry, int left, int top, int width) {
        Ability ability = entry.ability;
        boolean unlocked = entry.unlocked;

        int bgColor = unlocked ? COLOR_ABILITY_UNLOCKED : COLOR_ABILITY_LOCKED;
        graphics.fill(left, top, left + width, top + ABILITY_ROW_HEIGHT, bgColor);

        if (unlocked) {
            graphics.drawString(this.font, ability.getName(), left + 4, top + 5, TEXT_WHITE, false);

            int cost = ability.getResourceCost();
            if (cost > 0) {
                String resourceName = ability.getRequiredClass().getResourceType().getDisplayName();
                String costText = cost + " " + resourceName;
                int costWidth = this.font.width(costText);
                graphics.drawString(this.font, costText, left + width - costWidth - 4, top + 5, TEXT_COST, false);
            }
        } else {
            graphics.drawString(this.font, ability.getName(), left + 4, top + 5, TEXT_LOCKED_NAME, false);

            String levelText = "Unlocks at Lvl " + ability.getRequiredLevel();
            int levelWidth = this.font.width(levelText);
            graphics.drawString(this.font, levelText, left + width - levelWidth - 4, top + 5, TEXT_LOCKED_LEVEL, false);
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        while (this.font.width(text + "..") > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;

        int tabX = getTabPanelLeft(panelLeft);
        int tabY = panelTop + 18;
        int maxRight = tabX + TAB_PANEL_WIDTH;

        int currentX = tabX;
        for (int i = 0; i < tabs.size(); i++) {
            TabEntry tab = tabs.get(i);
            int tabWidth = this.font.width(tab.name) + 10;
            if (currentX + tabWidth > maxRight) break;

            if (mouseX >= currentX && mouseX < currentX + tabWidth
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                selectedTab = i;
                scrollOffset = 0;
                return true;
            }
            currentX += tabWidth + TAB_GAP;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        int contentLeft = getTabPanelLeft(panelLeft);
        int contentTop = panelTop + 18 + TAB_HEIGHT + 4;
        int contentBottom = panelTop + PANEL_HEIGHT - 8;

        if (mouseX >= contentLeft && mouseX < contentLeft + TAB_PANEL_WIDTH
                && mouseY >= contentTop && mouseY < contentBottom) {
            List<AbilityDisplayEntry> abilities = getAbilitiesForCurrentTab();
            int rowStep = ABILITY_ROW_HEIGHT + ABILITY_ROW_GAP;
            int maxVisible = (contentBottom - contentTop) / rowStep;
            int maxScroll = Math.max(0, abilities.size() - maxVisible);

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record TabEntry(String name, @Nullable DnDClass dndClass) {}
    private record AbilityDisplayEntry(Ability ability, boolean unlocked, ClassEntry classEntry) {}
}
