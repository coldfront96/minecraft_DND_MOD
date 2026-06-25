package com.deadmind.dndmods.combat;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.AbilityCooldownManager;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
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

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PlayerDataHelper.get(player).tickCooldowns();
        }

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

        // Clean up Nature's Grasp webs
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(20.0))) {
                if (entity instanceof LivingEntity living) {
                    CompoundTag tags = living.getPersistentData();
                    if (tags.contains("dndmods_web_remove_at")) {
                        long removeAt = tags.getLong("dndmods_web_remove_at");
                        if (living.level().getGameTime() >= removeAt) {
                            long posLong = tags.getLong("dndmods_web_pos");
                            BlockPos pos = BlockPos.of(posLong);
                            if (living.level().getBlockState(pos).is(Blocks.COBWEB)) {
                                living.level().removeBlock(pos, false);
                            }
                            tags.remove("dndmods_web_remove_at");
                            tags.remove("dndmods_web_pos");
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        // Attacker bonuses
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DnDPlayerData data = PlayerDataHelper.get(attacker);
            if (data.getDnDClass() == DnDClass.NONE) return;

            // Barbarian rage generation on attack
            if (data.getDnDClass() == DnDClass.BARBARIAN && data.getCurrentResource() < data.getMaxResource()) {
                data.restoreResource(5);
                PacketDistributor.sendToPlayer(attacker, SyncPlayerDataPayload.fromPlayer(data));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();

        // Evasion: 50% dodge chance when active
        if (target instanceof ServerPlayer defender) {
            CompoundTag tags = defender.getPersistentData();
            if (tags.contains("dndmods_evasion_until")) {
                long evasionUntil = tags.getLong("dndmods_evasion_until");
                if (defender.level().getGameTime() <= evasionUntil) {
                    if (defender.getRandom().nextFloat() < 0.5f) {
                        event.setNewDamage(0.0f);
                        return;
                    }
                }
            }
        }

        // Reckless: +20% incoming damage when active
        if (target instanceof ServerPlayer defender) {
            CompoundTag tags = defender.getPersistentData();
            if (tags.contains("dndmods_reckless_until")) {
                long recklessUntil = tags.getLong("dndmods_reckless_until");
                if (defender.level().getGameTime() <= recklessUntil) {
                    event.setNewDamage(event.getNewDamage() * 1.2f);
                }
            }
        }

        // Hunter's Mark: +25% damage from marker
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            CompoundTag targetTags = target.getPersistentData();
            if (targetTags.contains("dndmods_marked_by") && targetTags.contains("dndmods_mark_until")) {
                long markUntil = targetTags.getLong("dndmods_mark_until");
                if (target.level().getGameTime() <= markUntil) {
                    try {
                        java.util.UUID markerUuid = targetTags.getUUID("dndmods_marked_by");
                        if (attacker.getUUID().equals(markerUuid)) {
                            event.setNewDamage(event.getNewDamage() * 1.25f);
                        }
                    } catch (Exception ignored) {}
                }
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
