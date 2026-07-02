package com.khimairacraft.client.screen;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityRegistry;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.network.LevelUpPayload;
import com.khimairacraft.playerdata.AbilityScores;
import com.khimairacraft.playerdata.ClassEntry;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LevelUpScreen extends Screen {

    private static final int GOLD = 0xFFFFD700;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK_BG = 0xE0101020;
    private static final int PANEL_BG = 0xC0181830;
    private static final int SELECTED_BORDER = 0xFFFFD700;
    private static final int UNSELECTED_BORDER = 0xFF555555;
    private static final int HOVER_BORDER = 0xFF8888AA;

    private static final String[] SCORE_NAMES = {"STR", "DEX", "CON", "INT", "WIS", "CHA"};
    private static final String[] CLASS_DESCRIPTIONS = {
            "Master of martial combat and endurance",
            "Swift and deadly, striking from the shadows",
            "Wielder of arcane magic and intellect",
            "Divine champion of faith and healing",
            "Hunter and tracker of the wilderness",
            "Unstoppable force of rage and fury"
    };

    private DnDPlayerData data;

    private enum AdvanceChoice { PRIMARY, SECONDARY, NEW_CLASS }
    private AdvanceChoice advanceChoice = null;
    private boolean showingMulticlassPanel = false;
    private DnDClass selectedNewClass = null;

    private int[] asiAllocations = new int[6];
    private int asiPointsRemaining = 0;
    private boolean asiRequired = false;

    private boolean featRequired = false;
    private boolean featAcknowledged = false;

    private Button confirmButton;
    private Button deferButton;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    private int featBtnX, featBtnY, featBtnW, featBtnH;

    public LevelUpScreen() {
        super(Component.literal("Level Up"));
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) return;

        data = minecraft.player.getData(ModAttachments.PLAYER_DATA);

        panelWidth = Math.min(500, this.width - 40);
        panelHeight = Math.min(340, this.height - 40);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        if (data.getSecondary() == null) {
            advanceChoice = AdvanceChoice.PRIMARY;
        }

        updateRequirements();

        int btnY = panelTop + panelHeight - 28;
        confirmButton = Button.builder(Component.literal("Confirm Level Up"),
                        btn -> confirmLevelUp())
                .bounds(panelLeft + panelWidth / 2 - 110, btnY, 120, 20)
                .build();
        this.addRenderableWidget(confirmButton);

        deferButton = Button.builder(Component.literal("Defer"),
                        btn -> this.onClose())
                .bounds(panelLeft + panelWidth / 2 + 10, btnY, 80, 20)
                .build();
        this.addRenderableWidget(deferButton);

        updateConfirmButton();
    }

    private void updateRequirements() {
        asiRequired = false;
        asiPointsRemaining = 0;
        featRequired = false;
        featAcknowledged = false;

        if (advanceChoice == AdvanceChoice.NEW_CLASS) {
            return;
        }

        ClassEntry advancing = getAdvancingEntry();
        if (advancing == null) return;

        int newLevel = advancing.getLevel() + 1;
        if (newLevel % 4 == 0) {
            asiRequired = true;
            asiPointsRemaining = 2;
            asiAllocations = new int[6];
        }

        int[] featLevels = {1, 3, 6, 9, 12, 15, 18, 20};
        for (int fl : featLevels) {
            if (newLevel == fl) {
                featRequired = true;
                featAcknowledged = false;
                break;
            }
        }
    }

    private ClassEntry getAdvancingEntry() {
        if (advanceChoice == AdvanceChoice.PRIMARY) return data.getPrimary();
        if (advanceChoice == AdvanceChoice.SECONDARY) return data.getSecondary();
        return null;
    }

    private void updateConfirmButton() {
        boolean ready = true;

        if (advanceChoice == null) ready = false;
        if (advanceChoice == AdvanceChoice.NEW_CLASS && selectedNewClass == null) ready = false;
        if (asiRequired && asiPointsRemaining > 0) ready = false;
        if (featRequired && !featAcknowledged) ready = false;

        confirmButton.active = ready;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, DARK_BG);
        gui.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL_BG);
        drawBorder(gui, panelLeft, panelTop, panelWidth, panelHeight, SELECTED_BORDER);

        renderHeader(gui);

        if (showingMulticlassPanel) {
            renderMulticlassPanel(gui, mouseX, mouseY);
        } else {
            int dividerX = panelLeft + panelWidth / 2 - 1;
            gui.fill(dividerX, panelTop + 40, dividerX + 2, panelTop + panelHeight - 35, 0xFF333355);

            renderLeftPanel(gui, mouseX, mouseY);
            renderRightPanel(gui);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics gui) {
        int centerX = panelLeft + panelWidth / 2;
        int y = panelTop + 6;

        gui.drawCenteredString(this.font, "Level Up!", centerX, y, GOLD);

        String levelText = "Level " + data.getTotalLevel() + " → Level " + (data.getTotalLevel() + 1);
        gui.drawCenteredString(this.font, levelText, centerX, y + 12, WHITE);

        String classText = getClassDisplayText();
        gui.drawCenteredString(this.font, classText, centerX, y + 24, GRAY);
    }

    private String getClassDisplayText() {
        if (data.getSecondary() != null) {
            return data.getDnDClass().getDisplayName() + " / " + data.getSecondary().getDnDClass().getDisplayName();
        }
        return data.getDnDClass().getDisplayName();
    }

    private void renderLeftPanel(GuiGraphics gui, int mouseX, int mouseY) {
        int x = panelLeft + 10;
        int y = panelTop + 46;
        int cardWidth = panelWidth / 2 - 20;

        gui.drawString(this.font, "Advance Which Class?", x, y, GOLD);
        y += 14;

        // Primary class card
        y = renderClassCard(gui, x, y, cardWidth, data.getPrimary(),
                advanceChoice == AdvanceChoice.PRIMARY, mouseX, mouseY, AdvanceChoice.PRIMARY);

        // Secondary class card (if exists)
        if (data.getSecondary() != null) {
            y += 4;
            y = renderClassCard(gui, x, y, cardWidth, data.getSecondary(),
                    advanceChoice == AdvanceChoice.SECONDARY, mouseX, mouseY, AdvanceChoice.SECONDARY);
        }

        // Multiclass option
        if (data.canMulticlass() && data.getPrestigeClass() == com.khimairacraft.classes.PrestigeClass.NONE) {
            y += 4;
            int cardHeight = 25;
            boolean hovered = mouseX >= x && mouseX < x + cardWidth && mouseY >= y && mouseY < y + cardHeight;
            boolean selected = advanceChoice == AdvanceChoice.NEW_CLASS;

            int bg = selected ? 0xC0223322 : (hovered ? 0xC0333355 : 0xC0222233);
            int border = selected ? SELECTED_BORDER : (hovered ? HOVER_BORDER : UNSELECTED_BORDER);

            gui.fill(x, y, x + cardWidth, y + cardHeight, bg);
            drawBorder(gui, x, y, cardWidth, cardHeight, border);
            gui.drawCenteredString(this.font, "+ Add New Class", x + cardWidth / 2, y + 8, 0xFF44FF44);
        }
    }

    private int renderClassCard(GuiGraphics gui, int x, int y, int width,
                                 ClassEntry entry, boolean selected, int mouseX, int mouseY,
                                 AdvanceChoice choice) {
        int cardHeight = 40;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + cardHeight;

        int bg = selected ? 0xC0223322 : (hovered ? 0xC0333355 : 0xC0222233);
        int border = selected ? SELECTED_BORDER : (hovered ? HOVER_BORDER : UNSELECTED_BORDER);

        gui.fill(x, y, x + width, y + cardHeight, bg);
        drawBorder(gui, x, y, width, cardHeight, border);

        int nameColor = 0xFF000000 | entry.getDnDClass().getResourceType().getColor();
        gui.drawString(this.font, entry.getDnDClass().getDisplayName(), x + 6, y + 5, nameColor);

        String levelStr = "Level " + entry.getLevel();
        gui.drawString(this.font, levelStr, x + width - this.font.width(levelStr) - 6, y + 5, GRAY);

        int newLevel = entry.getLevel() + 1;
        String hpStr = "HP: +" + entry.getDnDClass().getHpPerLevel();
        int resGain = entry.getDnDClass().getMaxResourceAtLevel(newLevel) - entry.getDnDClass().getMaxResourceAtLevel(entry.getLevel());
        String resStr = entry.getDnDClass().getResourceType().getDisplayName() + ": +" + resGain;
        gui.drawString(this.font, hpStr, x + 6, y + 18, 0xFFCC4444);
        gui.drawString(this.font, resStr, x + 6, y + 28, 0xFF4488FF);

        return y + cardHeight;
    }

    private void renderRightPanel(GuiGraphics gui) {
        int x = panelLeft + panelWidth / 2 + 10;
        int y = panelTop + 46;
        int panelW = panelWidth / 2 - 20;

        gui.drawString(this.font, "What You Get:", x, y, GOLD);
        y += 14;

        ClassEntry advancing = getAdvancingEntry();
        if (advancing == null && advanceChoice == AdvanceChoice.NEW_CLASS) {
            renderNewClassPreview(gui, x, y, panelW);
            return;
        }
        if (advancing == null) {
            gui.drawString(this.font, "Select a class to advance", x, y, GRAY);
            return;
        }

        DnDClass cls = advancing.getDnDClass();
        int newLevel = advancing.getLevel() + 1;

        gui.drawString(this.font, "+" + cls.getHpPerLevel() + " Max HP", x, y, 0xFFCC4444);
        y += 12;

        int resGain = cls.getMaxResourceAtLevel(newLevel) - cls.getMaxResourceAtLevel(advancing.getLevel());
        gui.drawString(this.font, "+" + resGain + " " + cls.getResourceType().getDisplayName(), x, y, 0xFF4488FF);
        y += 14;

        // New abilities
        List<Ability> newAbilities = AbilityRegistry.getAbilitiesForClass(cls).stream()
                .filter(a -> a.getRequiredLevel() == newLevel)
                .toList();
        if (!newAbilities.isEmpty()) {
            gui.drawString(this.font, "New Abilities:", x, y, 0xFF44FF44);
            y += 11;
            for (Ability ability : newAbilities) {
                gui.drawString(this.font, "• " + ability.getName(), x + 4, y, WHITE);
                y += 10;
                gui.pose().pushPose();
                gui.pose().scale(0.75f, 0.75f, 1.0f);
                gui.drawString(this.font, ability.getDescription(),
                        (int)(( x + 8) / 0.75f), (int)(y / 0.75f), GRAY);
                gui.pose().popPose();
                y += 10;
            }
        }

        // Spell level notification
        if (cls == DnDClass.WIZARD || cls == DnDClass.CLERIC) {
            int oldSpellLevel = (int) Math.ceil(advancing.getLevel() / 2.0);
            int newSpellLevel = (int) Math.ceil(newLevel / 2.0);
            if (newSpellLevel > oldSpellLevel) {
                y += 2;
                gui.drawString(this.font, "New Spell Level Unlocked!", x, y, 0xFF8020C0);
                y += 11;
                gui.drawString(this.font, "Level " + newSpellLevel + " spells now available", x + 4, y, GRAY);
                y += 14;
            }
        }

        // ASI
        if (asiRequired) {
            y += 2;
            renderAsiPanel(gui, x, y, panelW);
            y += 70;
        }

        // Feat section
        if (featRequired) {
            y += 2;
            renderFeatStub(gui, x, y, panelW);
        } else if (data.getFeatSlotsAvailable() > 0) {
            y += 2;
            renderExistingFeatSlots(gui, x, y, panelW);
        }
    }

    private void renderNewClassPreview(GuiGraphics gui, int x, int y, int panelW) {
        if (selectedNewClass == null) {
            gui.drawString(this.font, "Choose a new class from the left", x, y, GRAY);
            return;
        }

        gui.drawString(this.font, "Adding: " + selectedNewClass.getDisplayName(), x, y, 0xFF44FF44);
        y += 12;
        gui.drawString(this.font, "Starts at Level 1", x, y, WHITE);
        y += 12;
        gui.drawString(this.font, "HP: +" + selectedNewClass.getBaseHp(), x, y, 0xFFCC4444);
        y += 12;
        gui.drawString(this.font, selectedNewClass.getResourceType().getDisplayName() + ": " + selectedNewClass.getBaseMaxResource(), x, y, 0xFF4488FF);
    }

    private void renderAsiPanel(GuiGraphics gui, int x, int y, int panelW) {
        gui.drawString(this.font, "Ability Score Improvement", x, y, GOLD);
        y += 11;

        String modeText = "+2 to one score OR +1 to two scores";
        gui.pose().pushPose();
        gui.pose().scale(0.75f, 0.75f, 1.0f);
        gui.drawString(this.font, modeText, (int)(x / 0.75f), (int)(y / 0.75f), GRAY);
        gui.pose().popPose();
        y += 10;

        gui.drawString(this.font, "Points: " + asiPointsRemaining, x, y, asiPointsRemaining > 0 ? 0xFFFF4444 : 0xFF44FF44);
        y += 12;

        AbilityScores scores = data.getAbilityScores();
        int[] baseValues = {scores.getStrength(), scores.getDexterity(), scores.getConstitution(),
                scores.getIntelligence(), scores.getWisdom(), scores.getCharisma()};

        int btnSize = 18;
        int spacing = 4;
        int cols = 3;
        for (int i = 0; i < 6; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx = x + col * (panelW / cols);
            int by = y + row * (btnSize + spacing);

            int totalVal = baseValues[i] + asiAllocations[i];
            String label = SCORE_NAMES[i] + " [" + totalVal + "]";
            int color = asiAllocations[i] > 0 ? 0xFF44FF44 : WHITE;
            gui.drawString(this.font, label, bx, by + 4, color);
        }
    }

    private void renderFeatStub(GuiGraphics gui, int x, int y, int panelW) {
        gui.drawString(this.font, "Feat Slot Earned!", x, y, GOLD);
        y += 11;

        int slotsAvailable = data.getFeatSlotsAvailable() + 1;
        gui.drawString(this.font, "Slots available: " + slotsAvailable, x, y, WHITE);
        y += 14;

        int btnW = Math.min(panelW, 140);
        int btnH = 16;
        featBtnX = x;
        featBtnY = y;
        featBtnW = btnW;
        featBtnH = btnH;

        int bgColor = 0xFF1A3A1A;
        gui.fill(x, y, x + btnW, y + btnH, bgColor);
        drawBorder(gui, x, y, btnW, btnH, 0xFF40C040);
        String btnText = "Open Feat Selection";
        int textX = x + (btnW - this.font.width(btnText)) / 2;
        gui.drawString(this.font, btnText, textX, y + 4, WHITE);
        y += btnH + 4;

        if (data.isHumanBonusFeatAvailable()) {
            gui.drawString(this.font, "Racial Bonus Feat Available!", x, y, 0xFF44FF44);
        }

        featAcknowledged = true;
    }

    private void renderExistingFeatSlots(GuiGraphics gui, int x, int y, int panelW) {
        gui.drawString(this.font, "Unspent Feat Slots: " + data.getFeatSlotsAvailable(), x, y, 0xFFFF8844);
        y += 14;

        int btnW = Math.min(panelW, 140);
        int btnH = 16;
        featBtnX = x;
        featBtnY = y;
        featBtnW = btnW;
        featBtnH = btnH;

        gui.fill(x, y, x + btnW, y + btnH, 0xFF1A3A1A);
        drawBorder(gui, x, y, btnW, btnH, 0xFF40C040);
        String btnText = "Open Feat Selection";
        int textX = x + (btnW - this.font.width(btnText)) / 2;
        gui.drawString(this.font, btnText, textX, y + 4, WHITE);
    }

    private void renderMulticlassPanel(GuiGraphics gui, int mouseX, int mouseY) {
        int x = panelLeft + 20;
        int y = panelTop + 46;
        int contentWidth = panelWidth - 40;
        int cardWidth = 140;
        int cardHeight = 65;
        int spacing = 8;

        gui.drawString(this.font, "Choose a New Secondary Class:", x, y, GOLD);
        y += 16;

        DnDClass[] selectableClasses = {DnDClass.FIGHTER, DnDClass.ROGUE, DnDClass.WIZARD,
                DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN};

        int cols = 3;
        int totalGridWidth = cols * cardWidth + (cols - 1) * spacing;
        int gridX = panelLeft + (panelWidth - totalGridWidth) / 2;

        int index = 0;
        for (DnDClass cls : selectableClasses) {
            if (cls == data.getDnDClass()) continue;

            int col = index % cols;
            int row = index / cols;
            int cx = gridX + col * (cardWidth + spacing);
            int cy = y + row * (cardHeight + spacing);

            boolean hovered = mouseX >= cx && mouseX < cx + cardWidth && mouseY >= cy && mouseY < cy + cardHeight;
            boolean selected = cls == selectedNewClass;

            int bg = selected ? 0xC0223322 : (hovered ? 0xC0333355 : 0xC0222233);
            int border = selected ? SELECTED_BORDER : (hovered ? HOVER_BORDER : UNSELECTED_BORDER);

            gui.fill(cx, cy, cx + cardWidth, cy + cardHeight, bg);
            drawBorder(gui, cx, cy, cardWidth, cardHeight, border);

            int nameColor = 0xFF000000 | cls.getResourceType().getColor();
            gui.drawCenteredString(this.font, cls.getDisplayName(), cx + cardWidth / 2, cy + 5, nameColor);

            String res = cls.getResourceType().getDisplayName() + " (" + cls.getBaseMaxResource() + ")";
            gui.drawCenteredString(this.font, res, cx + cardWidth / 2, cy + 17, GRAY);

            String hp = "HP: " + cls.getBaseHp() + " (+" + cls.getHpPerLevel() + "/lvl)";
            gui.drawCenteredString(this.font, hp, cx + cardWidth / 2, cy + 29, 0xFF888888);

            int descIdx = getClassDescIndex(cls);
            if (descIdx >= 0) {
                gui.pose().pushPose();
                gui.pose().scale(0.75f, 0.75f, 1.0f);
                gui.drawCenteredString(this.font, CLASS_DESCRIPTIONS[descIdx],
                        (int)((cx + cardWidth / 2) / 0.75f), (int)((cy + 43) / 0.75f), 0xFF999999);
                gui.pose().popPose();
            }

            index++;
        }

        // Back button area
        int backY = panelTop + panelHeight - 55;
        gui.drawCenteredString(this.font, "[Right-click to go back]",
                panelLeft + panelWidth / 2, backY, GRAY);
    }

    private int getClassDescIndex(DnDClass cls) {
        DnDClass[] ordered = {DnDClass.FIGHTER, DnDClass.ROGUE, DnDClass.WIZARD,
                DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN};
        for (int i = 0; i < ordered.length; i++) {
            if (ordered[i] == cls) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && showingMulticlassPanel) {
            showingMulticlassPanel = false;
            selectedNewClass = null;
            if (data.getSecondary() == null) {
                advanceChoice = AdvanceChoice.PRIMARY;
            }
            updateRequirements();
            updateConfirmButton();
            return true;
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (showingMulticlassPanel) {
            return handleMulticlassClick(mouseX, mouseY);
        }

        // Left panel clicks
        int leftX = panelLeft + 10;
        int y = panelTop + 60;
        int cardWidth = panelWidth / 2 - 20;

        // Primary class card
        if (mouseX >= leftX && mouseX < leftX + cardWidth && mouseY >= y && mouseY < y + 40) {
            advanceChoice = AdvanceChoice.PRIMARY;
            showingMulticlassPanel = false;
            updateRequirements();
            updateConfirmButton();
            return true;
        }
        y += 44;

        // Secondary class card
        if (data.getSecondary() != null) {
            if (mouseX >= leftX && mouseX < leftX + cardWidth && mouseY >= y && mouseY < y + 40) {
                advanceChoice = AdvanceChoice.SECONDARY;
                showingMulticlassPanel = false;
                updateRequirements();
                updateConfirmButton();
                return true;
            }
            y += 44;
        }

        // Multiclass card
        if (data.canMulticlass() && data.getPrestigeClass() == com.khimairacraft.classes.PrestigeClass.NONE) {
            if (mouseX >= leftX && mouseX < leftX + cardWidth && mouseY >= y && mouseY < y + 25) {
                advanceChoice = AdvanceChoice.NEW_CLASS;
                showingMulticlassPanel = true;
                selectedNewClass = null;
                updateRequirements();
                updateConfirmButton();
                return true;
            }
        }

        // Right panel clicks — ASI buttons
        if (asiRequired && asiPointsRemaining > 0) {
            int rx = panelLeft + panelWidth / 2 + 10;
            int ry = panelTop + 46;

            // Find ASI panel Y offset
            ClassEntry advancing = getAdvancingEntry();
            if (advancing != null) {
                DnDClass cls = advancing.getDnDClass();
                int newLevel = advancing.getLevel() + 1;

                ry += 14; // "What You Get"
                ry += 12; // HP
                ry += 14; // Resource

                List<Ability> newAbilities = AbilityRegistry.getAbilitiesForClass(cls).stream()
                        .filter(a -> a.getRequiredLevel() == newLevel).toList();
                if (!newAbilities.isEmpty()) {
                    ry += 11;
                    for (Ability a : newAbilities) ry += 20;
                }

                if (cls == DnDClass.WIZARD || cls == DnDClass.CLERIC) {
                    int oldSl = (int) Math.ceil(advancing.getLevel() / 2.0);
                    int newSl = (int) Math.ceil(newLevel / 2.0);
                    if (newSl > oldSl) ry += 27;
                }

                ry += 2 + 11 + 10 + 12; // ASI header + mode text + points text
                int panelW = panelWidth / 2 - 20;

                for (int i = 0; i < 6; i++) {
                    int col = i % 3;
                    int row = i / 3;
                    int bx = rx + col * (panelW / 3);
                    int by = ry + row * 22;

                    AbilityScores scores = data.getAbilityScores();
                    int[] baseValues = {scores.getStrength(), scores.getDexterity(), scores.getConstitution(),
                            scores.getIntelligence(), scores.getWisdom(), scores.getCharisma()};
                    String label = SCORE_NAMES[i] + " [" + (baseValues[i] + asiAllocations[i]) + "]";
                    int labelWidth = this.font.width(label);

                    if (mouseX >= bx && mouseX < bx + labelWidth + 4 && mouseY >= by && mouseY < by + 14) {
                        handleAsiClick(i);
                        updateConfirmButton();
                        return true;
                    }
                }
            }
        }

        // Feat selection button click
        if (featBtnW > 0) {
            if (mouseX >= featBtnX && mouseX < featBtnX + featBtnW
                    && mouseY >= featBtnY && mouseY < featBtnY + featBtnH) {
                Minecraft.getInstance().setScreen(new FeatSelectionScreen(false));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleAsiClick(int index) {
        if (asiAllocations[index] > 0) {
            asiPointsRemaining += asiAllocations[index];
            asiAllocations[index] = 0;
        } else if (asiPointsRemaining >= 2) {
            int totalAllocated = 0;
            for (int a : asiAllocations) totalAllocated += a;
            if (totalAllocated == 0) {
                asiAllocations[index] = 2;
                asiPointsRemaining -= 2;
            } else {
                asiAllocations[index] = 1;
                asiPointsRemaining -= 1;
            }
        } else if (asiPointsRemaining == 1) {
            boolean hasExistingAlloc = false;
            for (int i = 0; i < 6; i++) {
                if (asiAllocations[i] > 0 && i != index) {
                    hasExistingAlloc = true;
                    break;
                }
            }
            if (hasExistingAlloc) {
                for (int i = 0; i < 6; i++) {
                    if (asiAllocations[i] == 2) {
                        asiAllocations[i] = 1;
                        asiPointsRemaining += 1;
                        break;
                    }
                }
                if (asiPointsRemaining > 0) {
                    asiAllocations[index] = 1;
                    asiPointsRemaining -= 1;
                }
            }
        }
    }

    private boolean handleMulticlassClick(double mouseX, double mouseY) {
        DnDClass[] selectableClasses = {DnDClass.FIGHTER, DnDClass.ROGUE, DnDClass.WIZARD,
                DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN};

        int cardWidth = 140;
        int cardHeight = 65;
        int spacing = 8;
        int cols = 3;
        int totalGridWidth = cols * cardWidth + (cols - 1) * spacing;
        int gridX = panelLeft + (panelWidth - totalGridWidth) / 2;
        int y = panelTop + 62;

        int index = 0;
        for (DnDClass cls : selectableClasses) {
            if (cls == data.getDnDClass()) continue;

            int col = index % cols;
            int row = index / cols;
            int cx = gridX + col * (cardWidth + spacing);
            int cy = y + row * (cardHeight + spacing);

            if (mouseX >= cx && mouseX < cx + cardWidth && mouseY >= cy && mouseY < cy + cardHeight) {
                selectedNewClass = cls;
                updateConfirmButton();
                return true;
            }
            index++;
        }
        return false;
    }

    private void confirmLevelUp() {
        if (advanceChoice == null) return;

        boolean advancePrimary = advanceChoice == AdvanceChoice.PRIMARY;
        boolean addingNewClass = advanceChoice == AdvanceChoice.NEW_CLASS;
        String newClassName = addingNewClass && selectedNewClass != null ? selectedNewClass.name() : "";

        int asiScore1 = -1;
        int asiScore2 = -1;

        if (asiRequired) {
            int firstIdx = -1;
            int secondIdx = -1;
            for (int i = 0; i < 6; i++) {
                if (asiAllocations[i] == 2) {
                    asiScore1 = i;
                    asiScore2 = -1;
                    break;
                }
                if (asiAllocations[i] == 1) {
                    if (firstIdx == -1) firstIdx = i;
                    else secondIdx = i;
                }
            }
            if (asiScore1 == -1) {
                asiScore1 = firstIdx;
                asiScore2 = secondIdx;
            }
        }

        PacketDistributor.sendToServer(new LevelUpPayload(
                advancePrimary,
                addingNewClass,
                newClassName,
                asiScore1,
                asiScore2,
                featAcknowledged
        ));

        this.onClose();
    }

    private void drawBorder(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x, y, x + w, y + 1, color);
        gui.fill(x, y + h - 1, x + w, y + h, color);
        gui.fill(x, y, x + 1, y + h, color);
        gui.fill(x + w - 1, y, x + w, y + h, color);
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
