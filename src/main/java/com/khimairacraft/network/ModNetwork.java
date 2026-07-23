package com.khimairacraft.network;

import com.khimairacraft.DnDMods;
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

        registrar.playToServer(
                LevelUpPayload.TYPE,
                LevelUpPayload.STREAM_CODEC,
                LevelUpPayload::handle
        );

        registrar.playToServer(
                AssignHotbarSlotPayload.TYPE,
                AssignHotbarSlotPayload.STREAM_CODEC,
                AssignHotbarSlotPayload::handle
        );

        registrar.playToServer(
                ClearHotbarSlotPayload.TYPE,
                ClearHotbarSlotPayload.STREAM_CODEC,
                ClearHotbarSlotPayload::handle
        );

        registrar.playToClient(
                AbilityAssignRejectedPayload.TYPE,
                AbilityAssignRejectedPayload.STREAM_CODEC,
                AbilityAssignRejectedPayload::handle
        );

        registrar.playToClient(
                OpenRaceSelectionPayload.TYPE,
                OpenRaceSelectionPayload.STREAM_CODEC,
                OpenRaceSelectionPayload::handle
        );

        registrar.playToServer(
                SelectRacePayload.TYPE,
                SelectRacePayload.STREAM_CODEC,
                SelectRacePayload::handle
        );

        registrar.playToServer(
                SelectFeatPayload.TYPE,
                SelectFeatPayload.STREAM_CODEC,
                SelectFeatPayload::handle
        );

        registrar.playToServer(
                SelectRogueSpecialAbilityPayload.TYPE,
                SelectRogueSpecialAbilityPayload.STREAM_CODEC,
                SelectRogueSpecialAbilityPayload::handle
        );

        registrar.playToClient(
                OpenRangerCombatStylePayload.TYPE,
                OpenRangerCombatStylePayload.STREAM_CODEC,
                OpenRangerCombatStylePayload::handle
        );

        registrar.playToServer(
                SelectRangerCombatStylePayload.TYPE,
                SelectRangerCombatStylePayload.STREAM_CODEC,
                SelectRangerCombatStylePayload::handle
        );

        registrar.playToServer(
                SelectFavoredEnemyPayload.TYPE,
                SelectFavoredEnemyPayload.STREAM_CODEC,
                SelectFavoredEnemyPayload::handle
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, path);
    }
}
