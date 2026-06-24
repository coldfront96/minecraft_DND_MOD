package com.deadmind.dndmods.client;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.AbilityBarState;
import com.deadmind.dndmods.network.UseAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {

    public static KeyMapping ABILITY_BAR_KEY;
    private static boolean vUsedAsModifier = false;

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
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_V && event.getAction() == GLFW.GLFW_PRESS) {
            vUsedAsModifier = false;
        }

        if (event.getAction() == GLFW.GLFW_PRESS
                && event.getKey() >= GLFW.GLFW_KEY_1 && event.getKey() <= GLFW.GLFW_KEY_9) {
            long window = mc.getWindow().getWindow();
            if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_V)) {
                int slot = event.getKey() - GLFW.GLFW_KEY_1;
                String abilityId = AbilityBarState.getAbilityInSlot(slot);
                if (abilityId != null) {
                    PacketDistributor.sendToServer(new UseAbilityPayload(abilityId));
                }
                vUsedAsModifier = true;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ABILITY_BAR_KEY != null && ABILITY_BAR_KEY.consumeClick()) {
            if (!vUsedAsModifier) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new AbilityAssignmentScreen());
                } else if (mc.screen instanceof AbilityAssignmentScreen) {
                    mc.setScreen(null);
                }
            }
            vUsedAsModifier = false;
        }
    }
}
