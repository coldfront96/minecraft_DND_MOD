package com.deadmind.dndmods.guild;

import com.deadmind.dndmods.DnDMods;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Sweeps expired (session-only) guild invites: on the joining player's login,
 * and for everyone once per second on the server tick.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class GuildEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                GuildManager.getData(server).tickInviteExpiry(server);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            GuildManager.getData(event.getServer()).tickInviteExpiry(event.getServer());
        }
    }
}
