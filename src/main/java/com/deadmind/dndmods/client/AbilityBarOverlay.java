package com.deadmind.dndmods.client;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class AbilityBarOverlay {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int ACTIVE_BORDER = 0xFFFFFFFF;
    private static final int INACTIVE_BORDER = 0xFF808080;
    private static final int SLOT_BG = 0xC0222222;
    private static final int SLOT_BG_EMPTY = 0xC0111111;
    private static final int COOLDOWN_OVERLAY = 0xA0AA0000;
    private static final int INSUFFICIENT_TINT = 0x80444444;

    @SubscribeEvent
    public static void onPreRenderHotbar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        if (!isAbilityBarActive()) return;

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        renderAbilityBar(gui, data, screenWidth, screenHeight);
    }

    public static boolean isAbilityBarActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return false;
        if (ModKeyBindings.ABILITY_BAR_KEY == null) return false;
        return InputConstants.isKeyDown(
                mc.getWindow().getWindow(),
                ModKeyBindings.ABILITY_BAR_KEY.getKey().getValue()
        );
    }

    private static void renderAbilityBar(GuiGraphics gui, DnDPlayerData data,
                                          int screenWidth, int screenHeight) {
        AbilityHotbar hotbar = data.getAbilityHotbar();
        int totalWidth = AbilityHotbar.SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int startX = screenWidth / 2 - totalWidth / 2;
        int y = screenHeight - SLOT_SIZE - 2;

        gui.fill(startX - 3, y - 3, startX + totalWidth + 3, y + SLOT_SIZE + 3, 0x80000000);

        for (int i = 0; i < AbilityHotbar.SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_SPACING);
            boolean active = (i == hotbar.getActiveSlot());
            String abilityId = hotbar.getSlotAbility(i);
            Ability ability = abilityId != null ? AbilityRegistry.getAbility(abilityId) : null;

            gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, ability != null ? SLOT_BG : SLOT_BG_EMPTY);

            int borderColor = active ? ACTIVE_BORDER : INACTIVE_BORDER;
            gui.fill(x, y, x + SLOT_SIZE, y + 1, borderColor);
            gui.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
            gui.fill(x, y, x + 1, y + SLOT_SIZE, borderColor);
            gui.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

            if (ability != null) {
                boolean canAfford = data.getCurrentResource() >= ability.getResourceCost();
                boolean onCooldown = data.isOnCooldown(abilityId);

                if (!canAfford) {
                    gui.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, INSUFFICIENT_TINT);
                }

                int textColor = (canAfford && !onCooldown) ? 0xFFFFFFFF : 0xFF888888;
                String initial = ability.getName().substring(0, Math.min(2, ability.getName().length()));
                gui.drawCenteredString(Minecraft.getInstance().font, initial,
                        x + SLOT_SIZE / 2, y + 6, textColor);

                if (onCooldown) {
                    int cdRemaining = data.getCooldownRemaining(abilityId);
                    float cdRatio = Math.min(1.0f, (float) cdRemaining / ability.getCooldownTicks());
                    int cdHeight = (int) ((SLOT_SIZE - 2) * cdRatio);
                    gui.fill(x + 1, y + SLOT_SIZE - 1 - cdHeight,
                            x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, COOLDOWN_OVERLAY);
                }

                gui.pose().pushPose();
                gui.pose().scale(0.75f, 0.75f, 1.0f);
                String costText = String.valueOf(ability.getResourceCost());
                int costX = (int) ((x + SLOT_SIZE - 2) / 0.75f) - Minecraft.getInstance().font.width(costText);
                int costY = (int) ((y + SLOT_SIZE - 8) / 0.75f);
                gui.drawString(Minecraft.getInstance().font, costText, costX, costY, 0xFFAAAAFF);
                gui.pose().popPose();
            }

            gui.pose().pushPose();
            gui.pose().scale(0.75f, 0.75f, 1.0f);
            gui.drawString(Minecraft.getInstance().font, String.valueOf(i + 1),
                    (int) ((x + 2) / 0.75f), (int) ((y + 1) / 0.75f), 0xFFAAAAAA);
            gui.pose().popPose();
        }

        gui.drawCenteredString(Minecraft.getInstance().font,
                "[1-9] Select  [Click] Fire  [Scroll] Cycle",
                screenWidth / 2, y - 12, 0xFFAAAAAA);
    }
}
