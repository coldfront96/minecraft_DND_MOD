package com.deadmind.dndmods.party;

import com.deadmind.dndmods.guild.Guild;
import com.deadmind.dndmods.guild.GuildManager;
import com.deadmind.dndmods.guild.GuildZoneType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

/**
 * Single source of truth for "should this attack be blocked as friendly?" used
 * by all ability and combat targeting in the mod. Keep this method robust and
 * side-effect free — every targeting decision routes through it.
 */
public final class FriendlyFireChecker {

    private FriendlyFireChecker() {}

    /**
     * Returns true if {@code target} should be treated as friendly to
     * {@code attacker} (and therefore not damaged).
     */
    public static boolean isFriendly(Entity attacker, Entity target) {
        if (attacker == null || target == null) return false;

        // 1. Never harm yourself with your own abilities.
        if (attacker == target) return true;

        // 2. A player never harms their own tamed pet.
        if (attacker instanceof ServerPlayer && target instanceof TamableAnimal pet) {
            if (pet.isTame() && attacker.getUUID().equals(pet.getOwnerUUID())) {
                return true;
            }
        }

        // 3. Player-vs-player: arena override, then party, then guild.
        if (attacker instanceof ServerPlayer && target instanceof ServerPlayer) {
            MinecraftServer server = attacker.getServer();

            // 3a. An ARENA zone forces friendly fire on regardless of party/guild
            // — checked before any friendly result so PvP zones always allow damage.
            if (server != null) {
                ChunkPos attackerChunk = new ChunkPos(attacker.blockPosition());
                Guild chunkGuild = GuildManager.getGuildAtChunk(attackerChunk, server);
                if (chunkGuild != null && chunkGuild.getZoneType(attackerChunk) == GuildZoneType.ARENA) {
                    return false;
                }
            }

            // 3b. Same party, friendly fire off.
            PartyManager manager = PartyManager.getInstance();
            Party attackerParty = manager.getPartyOf(attacker.getUUID());
            Party targetParty = manager.getPartyOf(target.getUUID());
            if (attackerParty != null && attackerParty == targetParty
                    && !attackerParty.isFriendlyFireEnabled()) {
                return true;
            }

            // 3c. Same guild, friendly fire off.
            if (server != null && GuildManager.isSameGuild(attacker.getUUID(), target.getUUID(), server)) {
                Guild guild = GuildManager.getGuildOf(attacker.getUUID(), server);
                if (guild != null && !guild.isFriendlyFireEnabled()) {
                    return true;
                }
            }
        }

        // 4. Everything else is a valid target.
        return false;
    }

    /**
     * UUID-based overload: resolves both ids to live entities (players first,
     * then any loaded entity across the server's levels) and delegates to the
     * entity-based check.
     */
    public static boolean isFriendly(UUID attackerId, UUID targetId, MinecraftServer server) {
        if (attackerId == null || targetId == null || server == null) return false;
        if (attackerId.equals(targetId)) return true;

        Entity attacker = resolve(attackerId, server);
        Entity target = resolve(targetId, server);
        if (attacker == null || target == null) return false;
        return isFriendly(attacker, target);
    }

    private static Entity resolve(UUID id, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }
}
