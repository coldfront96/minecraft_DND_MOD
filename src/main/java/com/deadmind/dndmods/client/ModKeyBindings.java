package com.deadmind.dndmods.client;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.network.UseAbilityPayload;
import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {

    public static KeyMapping ABILITY_BAR_KEY;
    public static KeyMapping LEVEL_UP_KEY;

    private static int savedHotbarSlot = -1;

    @EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            ABILITY_BAR_KEY = new KeyMapping(
                    "key." + DnDMods.MOD_ID + ".ability_bar",
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
                    "key.categories." + DnDMods.MOD_ID
            );
            event.register(ABILITY_BAR_KEY);

            LEVEL_UP_KEY = new KeyMapping(
                    "key." + DnDMods.MOD_ID + ".level_up",
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L,
                    "key.categories." + DnDMods.MOD_ID
            );
            event.register(LEVEL_UP_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (event.getAction() == GLFW.GLFW_PRESS && LEVEL_UP_KEY != null
                && event.getKey() == LEVEL_UP_KEY.getKey().getValue()
                && mc.screen == null) {
            DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
            if (data.isLevelUpAvailable()) {
                mc.setScreen(new com.deadmind.dndmods.client.screen.LevelUpScreen());
                return;
            }
        }

        long window = mc.getWindow().getWindow();
        int vKey = ABILITY_BAR_KEY != null ? ABILITY_BAR_KEY.getKey().getValue() : GLFW.GLFW_KEY_V;

        if (event.getKey() == vKey) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (mc.screen != null) return;

                if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                        || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
                    mc.setScreen(new AbilityAssignmentScreen());
                    return;
                }

                savedHotbarSlot = mc.player.getInventory().selected;
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                if (savedHotbarSlot >= 0 && mc.screen == null) {
                    mc.player.getInventory().selected = savedHotbarSlot;
                }
                savedHotbarSlot = -1;
            }
            return;
        }

        if (mc.screen != null) return;
        if (!isVHeld(mc)) return;

        if (event.getAction() == GLFW.GLFW_PRESS
                && event.getKey() >= GLFW.GLFW_KEY_1 && event.getKey() <= GLFW.GLFW_KEY_9) {
            int slot = event.getKey() - GLFW.GLFW_KEY_1;
            DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
            data.getAbilityHotbar().setActiveSlot(slot);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!isVHeld(mc)) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        AbilityHotbar hotbar = data.getAbilityHotbar();
        int current = hotbar.getActiveSlot();

        if (event.getScrollDeltaY() > 0) {
            hotbar.setActiveSlot(current > 0 ? current - 1 : AbilityHotbar.SLOTS - 1);
        } else if (event.getScrollDeltaY() < 0) {
            hotbar.setActiveSlot(current < AbilityHotbar.SLOTS - 1 ? current + 1 : 0);
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!event.isAttack()) return;
        if (!isVHeld(mc)) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        String abilityId = data.getAbilityHotbar().getActiveAbility();
        if (abilityId != null) {
            PacketDistributor.sendToServer(new UseAbilityPayload(abilityId));
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static boolean isVHeld(Minecraft mc) {
        if (ABILITY_BAR_KEY == null) return false;
        return InputConstants.isKeyDown(
                mc.getWindow().getWindow(),
                ABILITY_BAR_KEY.getKey().getValue()
        );
    }
}
