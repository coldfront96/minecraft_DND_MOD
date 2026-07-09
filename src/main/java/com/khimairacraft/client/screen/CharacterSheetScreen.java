package com.khimairacraft.client.screen;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.classes.PrestigeClass;
import com.khimairacraft.playerdata.AbilityScores;
import com.khimairacraft.playerdata.ClassEntry;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CharacterSheetScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;

    private static final int LEFT_PANEL_WIDTH = 140;
    private static final int RIGHT_PANEL_MARGIN = 10;

    private static final int COLOR_BG = 0xCC111111;
    private static final int COLOR_BORDER = 0xFF555555;
    private static final int COLOR_DIVIDER = 0xFF404040;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFA0A0A0;
    private static final int TEXT_PLACEHOLDER = 0xFF606060;
    private static final int TEXT_MODIFIER = 0xFFF0D060;

    public CharacterSheetScreen() {
        super(Component.literal("Character Sheet"));
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

        renderBackground(graphics, left, top);

        DnDPlayerData data = getPlayerData();
        boolean hasClass = data != null && data.getDnDClass() != DnDClass.NONE;

        if (hasClass) {
            renderAbilityScores(graphics, left, top, data);
            renderDerivedStats(graphics, left, top, data);
        } else {
            renderPlaceholder(graphics, left, top);
        }

        renderBottomSection(graphics, left, top, data);
    }

    private void renderBackground(GuiGraphics graphics, int left, int top) {
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BG);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, COLOR_BORDER);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, COLOR_BORDER);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_BORDER);

        String title = "Character Sheet";
        int textWidth = this.font.width(title);
        graphics.drawString(this.font, title, left + (PANEL_WIDTH - textWidth) / 2, top + 4, TEXT_WHITE, false);
    }

    private void renderAbilityScores(GuiGraphics graphics, int panelLeft, int panelTop, DnDPlayerData data) {
        int x = panelLeft + 8;
        int y = panelTop + 20;

        drawScaledString(graphics, "Ability Scores", x, y, TEXT_WHITE, 0.9f);
        y += 10;
        graphics.fill(x, y, x + LEFT_PANEL_WIDTH - 16, y + 1, COLOR_DIVIDER);
        y += 4;

        AbilityScores scores = data.getAbilityScores();

        int[][] scoreData = {
                {scores.getStrength(), scores.getStrMod()},
                {scores.getDexterity(), scores.getDexMod()},
                {scores.getConstitution(), scores.getConMod()},
                {scores.getIntelligence(), scores.getIntMod()},
                {scores.getWisdom(), scores.getWisMod()},
                {scores.getCharisma(), scores.getChaMod()}
        };
        String[] labels = {"STR", "DEX", "CON", "INT", "WIS", "CHA"};

        for (int i = 0; i < 6; i++) {
            int rowY = y + i * 22;
            int score = scoreData[i][0];
            int mod = scoreData[i][1];

            drawScaledString(graphics, labels[i], x + 2, rowY + 2, TEXT_WHITE, 0.85f);

            String scoreStr = String.valueOf(score);
            int scoreWidth = (int) (this.font.width(scoreStr) * 0.85f);
            drawScaledString(graphics, scoreStr, x + 42 + (24 - scoreWidth) / 2, rowY + 2, TEXT_WHITE, 0.85f);

            String modStr = (mod >= 0 ? "+" : "") + mod;
            int modWidth = (int) (this.font.width(modStr) * 0.85f);
            drawScaledString(graphics, modStr, x + 78 + (32 - modWidth), rowY + 2, TEXT_MODIFIER, 0.85f);
        }
    }

    private void renderDerivedStats(GuiGraphics graphics, int panelLeft, int panelTop, DnDPlayerData data) {
        int x = panelLeft + LEFT_PANEL_WIDTH + RIGHT_PANEL_MARGIN;
        int y = panelTop + 20;
        int rightEdge = panelLeft + PANEL_WIDTH - 8;

        drawScaledString(graphics, "Derived Stats", x, y, TEXT_WHITE, 0.9f);
        y += 10;
        graphics.fill(x, y, rightEdge, y + 1, COLOR_DIVIDER);
        y += 4;

        AbilityScores scores = data.getAbilityScores();
        int totalLevel = data.getTotalLevel();

        int maxHp = data.getMaxHp();
        drawStatRow(graphics, "Max HP", String.valueOf(maxHp), x, y, rightEdge);
        y += 19;

        Minecraft mc = Minecraft.getInstance();
        int armorValue = mc.player != null ? mc.player.getArmorValue() : 0;
        int ac = Math.max(10, 10 + scores.getDexMod() + armorValue + data.getRacialAcBonus()
                + data.getFeatAcBonus() + data.getFeatShieldBonus() + data.getNaturalArmorBonus());
        drawStatRow(graphics, "Armor Class", String.valueOf(ac), x, y, rightEdge);
        y += 19;

        // Arcane Reflexes (Complete Mage): a Wizard may use INT instead of DEX
        // for initiative when it is higher, mirroring Insightful Reflexes.
        int initBaseMod = scores.getDexMod();
        if (data.getAchievementFlag("arcane_reflexes_unlocked") && scores.getIntMod() > initBaseMod) {
            initBaseMod = scores.getIntMod();
        }
        int initiative = initBaseMod + data.getFeatInitiativeBonus() + data.getDangerSenseInitBonus();
        drawStatRow(graphics, "Initiative", formatSigned(initiative), x, y, rightEdge);
        y += 19;

        int fortitude = scores.getConMod() + (totalLevel / 3) + data.getFeatFortBonus();
        drawStatRow(graphics, "Fortitude", formatSigned(fortitude), x, y, rightEdge);
        y += 19;

        int reflex = scores.getDexMod() + (totalLevel / 4) + data.getFeatRefBonus();
        drawStatRow(graphics, "Reflex", formatSigned(reflex), x, y, rightEdge);
        y += 19;

        int will = scores.getWisMod() + (totalLevel / 3) + data.getFeatWillBonus();
        drawStatRow(graphics, "Will", formatSigned(will), x, y, rightEdge);
        y += 19;

        ClassEntry primary = data.getPrimary();
        if (primary.getDnDClass() != DnDClass.NONE) {
            drawResourceRow(graphics, data, primary, x, y, rightEdge);
            y += 19;
        }

        ClassEntry secondary = data.getSecondary();
        if (secondary != null && secondary.getDnDClass() != DnDClass.NONE) {
            drawResourceRow(graphics, data, secondary, x, y, rightEdge);
        }
    }

    /**
     * Draws a class's resource row. Fighter has no generic class resource, so it
     * reads the dedicated Stamina pool ({@code currentStamina}/{@code maxStamina})
     * off {@link DnDPlayerData}; all other classes use their generic pool.
     */
    private void drawResourceRow(GuiGraphics graphics, DnDPlayerData data, ClassEntry entry,
                                 int x, int y, int rightEdge) {
        String resName;
        String resValue;
        if (entry.getDnDClass() == DnDClass.FIGHTER) {
            resName = "Stamina";
            resValue = data.getCurrentStamina() + " / " + data.getMaxStamina();
        } else {
            resName = entry.getDnDClass().getResourceType().getDisplayName();
            resValue = entry.getCurrentResource() + " / " + entry.getMaxResource();
        }
        drawStatRow(graphics, resName, resValue, x, y, rightEdge);
    }

    private void drawStatRow(GuiGraphics graphics, String label, String value, int x, int y, int rightEdge) {
        drawScaledString(graphics, label, x, y, TEXT_GRAY, 0.8f);
        int valueWidth = (int) (this.font.width(value) * 0.8f);
        drawScaledString(graphics, value, rightEdge - valueWidth, y, TEXT_WHITE, 0.8f);
    }

    private void renderPlaceholder(GuiGraphics graphics, int panelLeft, int panelTop) {
        int leftCenterX = panelLeft + 8 + LEFT_PANEL_WIDTH / 2;
        int rightX = panelLeft + LEFT_PANEL_WIDTH + RIGHT_PANEL_MARGIN;
        int rightCenterX = rightX + (PANEL_WIDTH - LEFT_PANEL_WIDTH - RIGHT_PANEL_MARGIN - 8) / 2;
        int centerY = panelTop + PANEL_HEIGHT / 2 - 4;

        String text = "No class selected";
        int tw = this.font.width(text);
        graphics.drawString(this.font, text, leftCenterX - tw / 2, centerY, TEXT_PLACEHOLDER, false);
        graphics.drawString(this.font, text, rightCenterX - tw / 2, centerY, TEXT_PLACEHOLDER, false);
    }

    private void renderBottomSection(GuiGraphics graphics, int panelLeft, int panelTop, @Nullable DnDPlayerData data) {
        int dividerY = panelTop + PANEL_HEIGHT - 20;
        graphics.fill(panelLeft + 6, dividerY, panelLeft + PANEL_WIDTH - 6, dividerY + 1, COLOR_DIVIDER);

        int textY = dividerY + 5;
        int leftX = panelLeft + 10;
        int rightEdge = panelLeft + PANEL_WIDTH - 10;

        String classStr;
        String totalLevelStr;

        if (data == null || data.getDnDClass() == DnDClass.NONE) {
            classStr = "Unclassed";
            totalLevelStr = "";
        } else {
            classStr = buildClassString(data);
            totalLevelStr = "Total Level: " + data.getTotalLevel();
        }

        drawScaledString(graphics, classStr, leftX, textY, TEXT_WHITE, 0.8f);

        if (!totalLevelStr.isEmpty()) {
            int tw = (int) (this.font.width(totalLevelStr) * 0.8f);
            drawScaledString(graphics, totalLevelStr, rightEdge - tw, textY, TEXT_GRAY, 0.8f);
        }
    }

    private String buildClassString(DnDPlayerData data) {
        ClassEntry primary = data.getPrimary();
        ClassEntry secondary = data.getSecondary();
        PrestigeClass prestige = data.getPrestigeClass();

        StringBuilder sb = new StringBuilder();

        if (secondary != null && secondary.getDnDClass() != DnDClass.NONE) {
            sb.append(primary.getDnDClass().getDisplayName()).append(" ").append(primary.getLevel());
            sb.append(" / ");
            sb.append(secondary.getDnDClass().getDisplayName()).append(" ").append(secondary.getLevel());

            if (prestige != PrestigeClass.NONE) {
                sb.append("  —  ").append(prestige.getDisplayName());
            }
        } else {
            sb.append(primary.getDnDClass().getDisplayName());
            sb.append("  Lvl ").append(primary.getLevel());
        }

        return sb.toString();
    }

    private String formatSigned(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private void drawScaledString(GuiGraphics graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, (int) (x / scale), (int) (y / scale), color, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
