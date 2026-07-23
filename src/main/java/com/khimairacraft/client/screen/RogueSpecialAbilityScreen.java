package com.khimairacraft.client.screen;

import com.khimairacraft.network.SelectRogueSpecialAbilityPayload;
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

/**
 * Fixed six-option picker for the Rogue special ability slots earned at Rogue
 * 10/13/16/19, plus the "general bonus feat instead" alternative that routes
 * through FeatSelectionScreen's rogue-special slot mode. A dedicated screen
 * (rather than reusing FeatSelectionScreen's category/chain machinery) keeps
 * the small fixed pool readable.
 */
@OnlyIn(Dist.CLIENT)
public class RogueSpecialAbilityScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 250;
    private static final int ROW_HEIGHT = 26;

    private record Option(String id, String name, String description, boolean repeatable) {}

    private static final Option[] OPTIONS = {
            new Option("crippling_strike", "Crippling Strike",
                    "Sneak Attacks also deal 2 STR damage per stack. Repeatable.", true),
            new Option("defensive_roll", "Defensive Roll",
                    "Once per day, Reflex save to halve a lethal hit.", false),
            new Option("improved_evasion", "Improved Evasion",
                    "Failed Evasion saves take half damage instead of full.", false),
            new Option("opportunist", "Opportunist",
                    "Free melee attack when an ally strikes a nearby enemy (1/round).", false),
            new Option("slippery_mind", "Slippery Mind",
                    "Automatically reroll a failed Will save one second later.", false),
            new Option("practiced_professional", "Practiced Professional",
                    "Trapfinding radius 12, +2 Reflex saves.", false),
    };

    public RogueSpecialAbilityScreen() {
        super(Component.literal("Rogue Special Ability"));
    }

    @Nullable
    private DnDPlayerData getPlayerData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return mc.player.getData(ModAttachments.PLAYER_DATA);
    }

    private boolean isOwnedNonRepeatable(Option option, DnDPlayerData data) {
        if (option.repeatable()) return false;
        String flag = SelectRogueSpecialAbilityPayload.NON_REPEATABLE_FLAGS.get(option.id());
        return flag != null && data.getAchievementFlag(flag);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        DnDPlayerData data = getPlayerData();
        if (data == null) return;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xCC111111);
        drawBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        graphics.drawString(this.font, "Rogue Special Ability", left + 10, top + 8, 0xFFFFD700);
        graphics.drawString(this.font,
                "Slots available: " + data.getRogueSpecialAbilitySlotsAvailable(),
                left + 10, top + 20, 0xFFFFFFFF);

        int y = top + 36;
        for (Option option : OPTIONS) {
            boolean owned = isOwnedNonRepeatable(option, data);
            boolean hovered = !owned && mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;

            int bg = owned ? 0xFF1A1A2A : hovered ? 0xFF1A3A1A : 0xFF1A1A1A;
            graphics.fill(left + 8, y, left + PANEL_WIDTH - 8, y + ROW_HEIGHT - 2, bg);

            String name = option.name();
            if (option.repeatable() && data.getRogueCripplingStrikeStacks() > 0) {
                name += " (x" + data.getRogueCripplingStrikeStacks() + ")";
            }
            int nameColor = owned ? 0xFF6060C0 : 0xFFFFFFFF;
            graphics.drawString(this.font, owned ? name + " (taken)" : name, left + 12, y + 3, nameColor);

            graphics.pose().pushPose();
            graphics.pose().scale(0.75f, 0.75f, 1.0f);
            graphics.drawString(this.font, option.description(),
                    (int) ((left + 12) / 0.75f), (int) ((y + 14) / 0.75f), 0xFFA0A0A0);
            graphics.pose().popPose();

            y += ROW_HEIGHT;
        }

        // Bonus feat alternative
        int btnY = y + 4;
        boolean btnHover = mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                && mouseY >= btnY && mouseY < btnY + 18;
        graphics.fill(left + 8, btnY, left + PANEL_WIDTH - 8, btnY + 18,
                btnHover ? 0xFF3A3A1A : 0xFF2A2A1A);
        drawBorder(graphics, left + 8, btnY, PANEL_WIDTH - 16, 18, 0xFFC0C040);
        String btnText = "Take a General Bonus Feat Instead";
        graphics.drawString(this.font, btnText,
                left + (PANEL_WIDTH - this.font.width(btnText)) / 2, btnY + 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DnDPlayerData data = getPlayerData();
        if (data == null || data.getRogueSpecialAbilitySlotsAvailable() <= 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        int y = top + 36;
        for (Option option : OPTIONS) {
            if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 2) {
                if (!isOwnedNonRepeatable(option, data)) {
                    PacketDistributor.sendToServer(new SelectRogueSpecialAbilityPayload(option.id()));
                    onClose();
                }
                return true;
            }
            y += ROW_HEIGHT;
        }

        int btnY = y + 4;
        if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                && mouseY >= btnY && mouseY < btnY + 18) {
            Minecraft.getInstance().setScreen(new FeatSelectionScreen(false, false, true));
            return true;
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
