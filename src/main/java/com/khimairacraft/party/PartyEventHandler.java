package com.khimairacraft.party;

import com.khimairacraft.DnDMods;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wires the session-only {@link PartyManager} into the server lifecycle: it is
 * reset on server start (parties never persist), leadership succession runs on
 * logout, the reclaim window opens on login, and invite expiry is swept once per
 * second.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class PartyEventHandler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Sessions never leak across server boots.
        PartyManager.reset();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PartyManager.getInstance().handlePlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PartyManager.getInstance().handlePlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            PartyManager.getInstance().tickInviteExpiry(event.getServer());
        }
    }
}
