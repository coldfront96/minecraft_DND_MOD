package com.deadmind.dndmods.network;

import com.deadmind.dndmods.DnDMods;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = DnDMods.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DnDMods.MOD_ID).versioned("1.0");

        registrar.playToClient(
                SyncPlayerDataPayload.TYPE,
                SyncPlayerDataPayload.STREAM_CODEC,
                SyncPlayerDataPayload::handle
        );

        registrar.playToClient(
                OpenClassSelectionPayload.TYPE,
                OpenClassSelectionPayload.STREAM_CODEC,
                OpenClassSelectionPayload::handle
        );

        registrar.playToServer(
                SelectClassPayload.TYPE,
                SelectClassPayload.STREAM_CODEC,
                SelectClassPayload::handle
        );

        registrar.playToServer(
                UseAbilityPayload.TYPE,
                UseAbilityPayload.STREAM_CODEC,
                UseAbilityPayload::handle
        );

        registrar.playToServer(
                OpenAbilityBarPayload.TYPE,
                OpenAbilityBarPayload.STREAM_CODEC,
                OpenAbilityBarPayload::handle
        );

        registrar.playToServer(
                SyncHotbarPayload.TYPE,
                SyncHotbarPayload.STREAM_CODEC,
                SyncHotbarPayload::handle
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, path);
    }
}
