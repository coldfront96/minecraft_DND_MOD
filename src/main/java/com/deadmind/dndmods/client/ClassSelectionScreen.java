package com.deadmind.dndmods.client;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SelectClassPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClassSelectionScreen extends Screen {

    private static final DnDClass[] SELECTABLE_CLASSES = {
            DnDClass.FIGHTER, DnDClass.ROGUE, DnDClass.WIZARD,
            DnDClass.CLERIC, DnDClass.RANGER, DnDClass.BARBARIAN
    };

    private static final String[] CLASS_DESCRIPTIONS = {
            "A master of martial combat. High HP, steady stamina recovery. Excels in melee.",
            "A cunning striker. Quick focus regen, deadly when behind foes. Hit and run.",
            "A wielder of arcane power. Devastating spells but fragile. Intelligence scales damage.",
            "A divine champion. Heals allies, smites undead. Balanced survivability.",
            "A wilderness warrior. Ranged attacks, marks prey, controls the battlefield.",
            "A fury-driven berserker. Highest HP, gains rage by attacking. Unstoppable in melee."
    };

    public ClassSelectionScreen() {
        super(Component.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        int cardWidth = 120;
        int cardHeight = 100;
        int spacing = 8;
        int cols = 3;
        int rows = 2;

        int totalWidth = cols * cardWidth + (cols - 1) * spacing;
        int totalHeight = rows * cardHeight + (rows - 1) * spacing;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2 + 10;

        for (int i = 0; i < SELECTABLE_CLASSES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cardWidth + spacing);
            int y = startY + row * (cardHeight + spacing);

            final DnDClass dndClass = SELECTABLE_CLASSES[i];
            Button selectButton = Button.builder(
                    Component.literal("Select " + dndClass.getDisplayName()),
                    btn -> selectClass(dndClass)
            ).bounds(x + 5, y + cardHeight - 25, cardWidth - 10, 20).build();

            this.addRenderableWidget(selectButton);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        gui.drawCenteredString(this.font, "This choice is permanent!", this.width / 2, 28, 0xFF5555);

        int cardWidth = 120;
        int cardHeight = 100;
        int spacing = 8;
        int cols = 3;
        int rows = 2;

        int totalWidth = cols * cardWidth + (cols - 1) * spacing;
        int totalHeight = rows * cardHeight + (rows - 1) * spacing;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2 + 10;

        for (int i = 0; i < SELECTABLE_CLASSES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cardWidth + spacing);
            int y = startY + row * (cardHeight + spacing);

            DnDClass dndClass = SELECTABLE_CLASSES[i];

            gui.fill(x, y, x + cardWidth, y + cardHeight, 0xC0222222);
            gui.fill(x, y, x + cardWidth, y + 1, 0xFF666666);
            gui.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, 0xFF666666);
            gui.fill(x, y, x + 1, y + cardHeight, 0xFF666666);
            gui.fill(x + cardWidth - 1, y, x + cardWidth, y + cardHeight, 0xFF666666);

            int resourceColor = 0xFF000000 | dndClass.getResourceType().getColor();
            gui.drawCenteredString(this.font, dndClass.getDisplayName(), x + cardWidth / 2, y + 6, resourceColor);

            String resource = dndClass.getResourceType().getDisplayName() + " (" + dndClass.getBaseMaxResource() + ")";
            gui.drawCenteredString(this.font, resource, x + cardWidth / 2, y + 18, 0xAAAAAA);

            String hp = "HP: " + dndClass.getBaseHp() + " (+" + dndClass.getHpPerLevel() + "/lvl)";
            gui.drawCenteredString(this.font, hp, x + cardWidth / 2, y + 30, 0xCC4444);

            String desc = CLASS_DESCRIPTIONS[i];
            int descY = y + 44;
            int maxLineWidth = cardWidth - 10;
            for (var line : this.font.split(Component.literal(desc), maxLineWidth)) {
                gui.drawString(this.font, line, x + 5, descY, 0x999999);
                descY += 10;
            }
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void selectClass(DnDClass dndClass) {
        PacketDistributor.sendToServer(new SelectClassPayload(dndClass.ordinal()));
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
