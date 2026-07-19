package com.khimairacraft.client;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.classes.ResourceType;
import com.khimairacraft.network.SelectClassPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ClassSelectionScreen extends Screen {

    private static final DnDClass[] SELECTABLE_CLASSES = {
            DnDClass.FIGHTER, DnDClass.ROGUE, DnDClass.WIZARD,
            DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN
    };

    private static final String[] CLASS_DESCRIPTIONS = {
            "Master of martial combat and endurance",
            "Swift and deadly, striking from the shadows",
            "Wielder of arcane magic and intellect",
            "Divine champion of faith and healing",
            "Hunter and tracker of the wilderness",
            "Unstoppable force of rage and fury"
    };

    private static final String[] HIT_DICE = {
            "d10 Hit Die", "d6 Hit Die", "d4 Hit Die",
            "d8 Hit Die", "d8 Hit Die", "d12 Hit Die"
    };

    private static final int CARD_WIDTH = 130;
    private static final int CARD_HEIGHT = 90;
    private static final int CARD_SPACING = 10;
    private static final int COLS = 3;

    private int selectedIndex = -1;
    private Button confirmButton;

    public ClassSelectionScreen() {
        super(Component.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        selectedIndex = -1;
        confirmButton = Button.builder(
                Component.literal("Select a class first"),
                btn -> confirmSelection()
        ).bounds(this.width / 2 - 80, this.height - 40, 160, 20).build();
        confirmButton.active = false;
        this.addRenderableWidget(confirmButton);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        gui.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        gui.drawCenteredString(this.font, "This choice is permanent!", this.width / 2, 25, 0xFF5555);

        int totalWidth = COLS * CARD_WIDTH + (COLS - 1) * CARD_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int startY = 42;

        for (int i = 0; i < SELECTABLE_CLASSES.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (CARD_WIDTH + CARD_SPACING);
            int y = startY + row * (CARD_HEIGHT + CARD_SPACING);

            DnDClass dndClass = SELECTABLE_CLASSES[i];
            boolean hovered = mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT;
            boolean selected = (i == selectedIndex);

            int bgColor = selected ? 0xC0224422 : (hovered ? 0xC0333355 : 0xC0222222);
            int borderColor = selected ? 0xFF44AA44 : (hovered ? 0xFF6666AA : 0xFF555555);

            gui.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);
            gui.fill(x, y, x + CARD_WIDTH, y + 1, borderColor);
            gui.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
            gui.fill(x, y, x + 1, y + CARD_HEIGHT, borderColor);
            gui.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);

            // Fighter has no generic class resource; preview its dedicated Stamina
            // pool (level-1 base = 10 + 5) so the card doesn't read "None (0)".
            boolean fighter = dndClass == DnDClass.FIGHTER;
            ResourceType displayResource = fighter ? ResourceType.STAMINA : dndClass.getResourceType();

            int nameColor = 0xFF000000 | displayResource.getColor();
            gui.drawCenteredString(this.font, dndClass.getDisplayName(), x + CARD_WIDTH / 2, y + 6, nameColor);

            String resource = fighter
                    ? "Stamina (15)"
                    : dndClass.getResourceType().getDisplayName() + " (" + dndClass.getBaseMaxResource() + ")";
            gui.drawCenteredString(this.font, resource, x + CARD_WIDTH / 2, y + 19, 0xAAAAAA);

            gui.drawCenteredString(this.font, HIT_DICE[i], x + CARD_WIDTH / 2, y + 32, 0xCC4444);

            String hp = "HP: " + dndClass.getBaseHp() + " (+" + dndClass.getHpPerLevel() + "/lvl)";
            gui.drawCenteredString(this.font, hp, x + CARD_WIDTH / 2, y + 44, 0x888888);

            String desc = CLASS_DESCRIPTIONS[i];
            int descWidth = this.font.width(desc);
            if (descWidth <= CARD_WIDTH - 10) {
                gui.drawCenteredString(this.font, desc, x + CARD_WIDTH / 2, y + 60, 0x999999);
            } else {
                int descY = y + 58;
                for (var line : this.font.split(Component.literal(desc), CARD_WIDTH - 10)) {
                    gui.drawString(this.font, line, x + 5, descY, 0x999999);
                    descY += 10;
                }
            }

            if (selected) {
                gui.drawCenteredString(this.font, "✔", x + CARD_WIDTH - 10, y + 4, 0x44FF44);
            }
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int totalWidth = COLS * CARD_WIDTH + (COLS - 1) * CARD_SPACING;
            int startX = (this.width - totalWidth) / 2;
            int startY = 42;

            for (int i = 0; i < SELECTABLE_CLASSES.length; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int x = startX + col * (CARD_WIDTH + CARD_SPACING);
                int y = startY + row * (CARD_HEIGHT + CARD_SPACING);

                if (mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT) {
                    selectedIndex = i;
                    confirmButton.active = true;
                    confirmButton.setMessage(Component.literal("Become a " + SELECTABLE_CLASSES[i].getDisplayName()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirmSelection() {
        if (selectedIndex < 0 || selectedIndex >= SELECTABLE_CLASSES.length) return;
        DnDClass chosen = SELECTABLE_CLASSES[selectedIndex];
        PacketDistributor.sendToServer(new SelectClassPayload(chosen.name()));
        this.onClose();
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
