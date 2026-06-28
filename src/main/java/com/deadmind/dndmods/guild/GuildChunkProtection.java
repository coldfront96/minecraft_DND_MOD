package com.deadmind.dndmods.guild;

import com.deadmind.dndmods.DnDMods;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Protects claimed guild land from non-members. Block breaking and placing are
 * blocked for players who are not members of the owning guild; within a {@code
 * RESTRICTED} zone only the guild master and officers may build. Mob spawning
 * and other non-player interactions are deliberately left unrestricted.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class GuildChunkProtection {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (isBlocked(player, event.getPos(), "break blocks")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isBlocked(player, event.getPos(), "place blocks")) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Returns true (and notifies the player) if the build action at {@code pos}
     * should be denied: a non-member acting on claimed land, or a plain member
     * acting inside a RESTRICTED zone.
     */
    private static boolean isBlocked(ServerPlayer player, BlockPos pos, String action) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ChunkPos chunk = new ChunkPos(pos);
        Guild guild = GuildManager.getGuildAtChunk(chunk, server);
        if (guild == null) return false; // unclaimed land is unrestricted

        if (!guild.isMember(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[DnDMods] This land belongs to "
                    + guild.getGuildName() + ". You cannot " + action + " here."));
            return true;
        }

        if (guild.getZoneType(chunk) == GuildZoneType.RESTRICTED
                && guild.getRank(player.getUUID()) == GuildRank.MEMBER) {
            player.sendSystemMessage(Component.literal("§c[DnDMods] This is a restricted zone. "
                    + "Only officers and the guild master may build here."));
            return true;
        }

        return false;
    }
}
