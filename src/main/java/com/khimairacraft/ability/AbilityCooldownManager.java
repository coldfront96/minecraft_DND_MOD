package com.khimairacraft.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityCooldownManager {
    private static final Map<UUID, Map<String, Integer>> cooldowns = new HashMap<>();

    public static boolean isOnCooldown(ServerPlayer player, Ability ability) {
        Map<String, Integer> playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns == null) return false;
        Integer remaining = playerCooldowns.get(ability.getId());
        return remaining != null && remaining > 0;
    }

    public static int getRemainingCooldown(ServerPlayer player, Ability ability) {
        Map<String, Integer> playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns == null) return 0;
        Integer remaining = playerCooldowns.get(ability.getId());
        return remaining != null ? Math.max(0, remaining) : 0;
    }

    public static void setCooldown(ServerPlayer player, Ability ability, int ticks) {
        cooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(ability.getId(), ticks);
    }

    public static void tick() {
        cooldowns.values().forEach(playerCooldowns ->
                playerCooldowns.replaceAll((id, remaining) -> Math.max(0, remaining - 1))
        );
    }

    public static void removePlayer(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
