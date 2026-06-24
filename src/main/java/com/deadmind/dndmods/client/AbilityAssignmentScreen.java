package com.deadmind.dndmods.client;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityBarState;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AbilityAssignmentScreen extends Screen {

    private static final int SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 4;
    private static final int ABILITY_ENTRY_HEIGHT = 20;

    private String selectedAbilityId = null;
    private List<Ability> availableAbilities;

    public AbilityAssignmentScreen() {
        super(Component.literal("Assign Abilities"));
    }

    @Override
    protected void init() {
        DnDPlayerData data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);
        availableAbilities = AbilityRegistry.getAvailableAbilities(data.getDnDClass(), data.getLevel());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        DnDPlayerData data = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_DATA);

        gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        gui.drawCenteredString(this.font,
                "Click an ability, then click a slot. Right-click a slot to clear it.",
                this.width / 2, 22, 0x999999);

        renderAbilityList(gui, data, mouseX, mouseY);
        renderSlotBar(gui, data, mouseX, mouseY);

        if (selectedAbilityId != null) {
            Ability sel = AbilityRegistry.getAbility(selectedAbilityId);
            if (sel != null) {
                gui.drawCenteredString(this.font, "Selected: " + sel.getName(), this.width / 2, this.height - 25, 0x55FF55);
            }
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderAbilityList(GuiGraphics gui, DnDPlayerData data, int mouseX, int mouseY) {
        int listWidth = 200;
        int listX = (this.width - listWidth) / 2;
        int listY = 40;

        gui.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + availableAbilities.size() * ABILITY_ENTRY_HEIGHT + 2, 0xC0111111);

        for (int i = 0; i < availableAbilities.size(); i++) {
            Ability ability = availableAbilities.get(i);
            int y = listY + i * ABILITY_ENTRY_HEIGHT;

            boolean hovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ABILITY_ENTRY_HEIGHT;
            boolean selected = ability.getId().equals(selectedAbilityId);
            boolean slotted = isAbilitySlotted(ability.getId());

            int bgColor = selected ? 0xC0225522 : (hovered ? 0xC0333355 : 0xC0222222);
            gui.fill(listX, y, listX + listWidth, y + ABILITY_ENTRY_HEIGHT - 1, bgColor);

            int nameColor = slotted ? 0x55FF55 : (ability.getRequiredLevel() <= data.getLevel() ? 0xFFFFFF : 0x666666);
            gui.drawString(this.font, ability.getName(), listX + 4, y + 2, nameColor);

            String info = "Lv" + ability.getRequiredLevel() + " | Cost: " + ability.getResourceCost()
                    + " | CD: " + String.format("%.1fs", ability.getCooldownTicks() / 20.0);
            gui.drawString(this.font, info, listX + 4, y + 11, 0x888888);
        }
    }

    private void renderSlotBar(GuiGraphics gui, DnDPlayerData data, int mouseX, int mouseY) {
        int totalWidth = AbilityBarState.SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height - 60;

        gui.drawCenteredString(this.font, "Ability Slots (V + 1-9 to activate in-game)", this.width / 2, y - 14, 0xAAAAAA);

        for (int i = 0; i < AbilityBarState.SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_SPACING);

            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            int bgColor = hovered ? 0xC0444466 : 0xC0333333;
            gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

            gui.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF666666);
            gui.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF666666);
            gui.fill(x, y, x + 1, y + SLOT_SIZE, 0xFF666666);
            gui.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF666666);

            String slotAbilityId = AbilityBarState.getAbilityInSlot(i);
            if (slotAbilityId != null) {
                Ability ability = AbilityRegistry.getAbility(slotAbilityId);
                if (ability != null) {
                    String initial = ability.getName().substring(0, 2);
                    gui.drawCenteredString(this.font, initial, x + SLOT_SIZE / 2, y + (SLOT_SIZE - 8) / 2, 0xFFFFFF);
                }
            }

            gui.drawString(this.font, String.valueOf(i + 1), x + 1, y - 10, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listWidth = 200;
        int listX = (this.width - listWidth) / 2;
        int listY = 40;

        for (int i = 0; i < availableAbilities.size(); i++) {
            int y = listY + i * ABILITY_ENTRY_HEIGHT;
            if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ABILITY_ENTRY_HEIGHT) {
                if (button == 0) {
                    selectedAbilityId = availableAbilities.get(i).getId();
                    return true;
                }
            }
        }

        int totalWidth = AbilityBarState.SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int startX = (this.width - totalWidth) / 2;
        int slotY = this.height - 60;

        for (int i = 0; i < AbilityBarState.SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_SPACING);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                if (button == 0 && selectedAbilityId != null) {
                    AbilityBarState.setAbilityInSlot(i, selectedAbilityId);
                    selectedAbilityId = null;
                    return true;
                } else if (button == 1) {
                    AbilityBarState.clearSlot(i);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isAbilitySlotted(String abilityId) {
        for (int i = 0; i < AbilityBarState.SLOTS; i++) {
            if (abilityId.equals(AbilityBarState.getAbilityInSlot(i))) return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
