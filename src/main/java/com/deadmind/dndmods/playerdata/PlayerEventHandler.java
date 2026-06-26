package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.OpenClassSelectionPayload;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);

            data.getAbilityHotbar().validateAndClean(data);

            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));

            if (data.getDnDClass() == DnDClass.NONE) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenClassSelectionPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            data.setCurrentHp(data.getMaxHp());
            data.setCurrentResource(data.getMaxResource());
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
        }
    }
}
