package com.deadmind.dndmods.combat;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.AbilityCooldownManager;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class CombatEventHandler {

    private static int resourceRegenCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AbilityCooldownManager.tick();

        resourceRegenCounter++;
        if (resourceRegenCounter >= 20) {
            resourceRegenCounter = 0;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getDnDClass() == DnDClass.NONE) continue;

                int regenAmount = getResourceRegenRate(data.getDnDClass());
                if (data.getCurrentResource() < data.getMaxResource()) {
                    data.restoreResource(regenAmount);
                    PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DnDPlayerData data = PlayerDataHelper.get(attacker);
            if (data.getDnDClass() == DnDClass.NONE) return;

            float bonusDamage = getStrengthBonusDamage(data);
            if (bonusDamage > 0 && event.getEntity() instanceof LivingEntity) {
                // Strength bonus is applied passively through the attack
            }

            if (data.getDnDClass() == DnDClass.BARBARIAN && data.getCurrentResource() < data.getMaxResource()) {
                data.restoreResource(5);
                PacketDistributor.sendToPlayer(attacker, SyncPlayerDataPayload.fromPlayer(data));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            DnDPlayerData data = PlayerDataHelper.get(killer);
            if (data.getDnDClass() == DnDClass.NONE) return;

            int xpReward = calculateXpReward(event.getEntity());
            data.addXp(xpReward);
            PacketDistributor.sendToPlayer(killer, SyncPlayerDataPayload.fromPlayer(data));
        }
    }

    private static float getStrengthBonusDamage(DnDPlayerData data) {
        return data.getAbilityScores().getStrMod() * 0.5f;
    }

    private static int getResourceRegenRate(DnDClass dndClass) {
        return switch (dndClass) {
            case FIGHTER -> 3;
            case ROGUE -> 4;
            case WIZARD -> 2;
            case CLERIC -> 3;
            case RANGER -> 4;
            case BARBARIAN -> 1;
            case NONE -> 0;
        };
    }

    private static int calculateXpReward(LivingEntity entity) {
        return (int) (entity.getMaxHealth() * 2);
    }
}
