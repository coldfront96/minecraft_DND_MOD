package com.deadmind.dndmods.client;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.AbilityBarState;
import com.deadmind.dndmods.network.OpenAbilityBarPayload;
import com.deadmind.dndmods.network.UseAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {

    public static KeyMapping ABILITY_BAR_KEY;
    public static KeyMapping[] ABILITY_SLOT_KEYS = new KeyMapping[9];

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

            for (int i = 0; i < 9; i++) {
                ABILITY_SLOT_KEYS[i] = new KeyMapping(
                        "key." + DnDMods.MOD_ID + ".ability_slot_" + (i + 1),
                        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + i,
                        "key.categories." + DnDMods.MOD_ID
                );
                event.register(ABILITY_SLOT_KEYS[i]);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ABILITY_BAR_KEY != null && ABILITY_BAR_KEY.consumeClick()) {
            AbilityBarState.toggle();
            PacketDistributor.sendToServer(new OpenAbilityBarPayload(AbilityBarState.isOpen()));
        }

        if (AbilityBarState.isOpen()) {
            for (int i = 0; i < 9; i++) {
                if (ABILITY_SLOT_KEYS[i] != null && ABILITY_SLOT_KEYS[i].consumeClick()) {
                    PacketDistributor.sendToServer(new UseAbilityPayload(i));
                }
            }
        }
    }
}
