package com.deadmind.dndmods.client;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityBarState;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.classes.ResourceType;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class AbilityBarOverlay {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int BAR_COLOR = 0x80000000;
    private static final int SLOT_COLOR = 0xA0333333;
    private static final int SLOT_BORDER = 0xFF666666;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        renderResourceBar(gui, data, screenWidth, screenHeight);
        renderClassInfo(gui, data, screenWidth, screenHeight);

        if (AbilityBarState.isOpen()) {
            renderAbilityBar(gui, data, screenWidth, screenHeight);
        }
    }

    private static void renderResourceBar(GuiGraphics gui, DnDPlayerData data, int screenWidth, int screenHeight) {
        ResourceType resourceType = data.getDnDClass().getResourceType();
        if (resourceType == ResourceType.NONE) return;

        int barWidth = 80;
        int barHeight = 6;
        int x = screenWidth / 2 - barWidth / 2;
        int y = screenHeight - 52;

        gui.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
        gui.fill(x, y, x + barWidth, y + barHeight, BAR_COLOR);

        float ratio = data.getMaxResource() > 0 ? (float) data.getCurrentResource() / data.getMaxResource() : 0;
        int fillWidth = (int) (barWidth * ratio);
        int color = 0xFF000000 | resourceType.getColor();
        gui.fill(x, y, x + fillWidth, y + barHeight, color);

        String text = data.getCurrentResource() + "/" + data.getMaxResource() + " " + resourceType.getDisplayName();
        gui.drawCenteredString(Minecraft.getInstance().font, text, screenWidth / 2, y - 10, 0xFFFFFFFF);
    }

    private static void renderClassInfo(GuiGraphics gui, DnDPlayerData data, int screenWidth, int screenHeight) {
        String classText = data.getDnDClass().getDisplayName() + " Lv." + data.getLevel();
        gui.drawString(Minecraft.getInstance().font, classText, 4, 4, 0xFFFFFFFF);

        String xpText = "XP: " + data.getXp() + "/" + data.getXpForNextLevel();
        gui.drawString(Minecraft.getInstance().font, xpText, 4, 14, 0xFFCCCCCC);
    }

    private static void renderAbilityBar(GuiGraphics gui, DnDPlayerData data, int screenWidth, int screenHeight) {
        List<Ability> abilities = AbilityRegistry.getAbilitiesForClass(data.getDnDClass());

        int totalWidth = AbilityBarState.SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int startX = screenWidth / 2 - totalWidth / 2;
        int y = screenHeight - 75;

        gui.fill(startX - 3, y - 3, startX + totalWidth + 3, y + SLOT_SIZE + 3, BAR_COLOR);

        for (int i = 0; i < AbilityBarState.SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_SPACING);

            gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_COLOR);
            gui.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_BORDER);
            gui.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER);
            gui.fill(x, y, x + 1, y + SLOT_SIZE, SLOT_BORDER);
            gui.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER);

            if (i < abilities.size()) {
                Ability ability = abilities.get(i);
                boolean available = ability.getRequiredLevel() <= data.getLevel() &&
                        data.getCurrentResource() >= ability.getResourceCost();

                int textColor = available ? 0xFFFFFFFF : 0xFF888888;
                String initial = ability.getName().substring(0, 1);
                gui.drawCenteredString(Minecraft.getInstance().font, initial, x + SLOT_SIZE / 2, y + 6, textColor);
            }

            String slotNum = String.valueOf(i + 1);
            gui.drawString(Minecraft.getInstance().font, slotNum, x + 2, y - 8, 0xFFAAAAAA);
        }

        gui.drawCenteredString(Minecraft.getInstance().font, "[V] Ability Bar", screenWidth / 2, y - 16, 0xFFAAAAAA);
    }
}
