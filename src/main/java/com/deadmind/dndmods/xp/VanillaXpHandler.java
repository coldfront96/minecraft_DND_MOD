package com.deadmind.dndmods.xp;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import com.deadmind.dndmods.systems.MobXpRewardSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class VanillaXpHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() == null) return;
        if (!(event.getAttackingPlayer() instanceof ServerPlayer serverPlayer)) return;

        DnDPlayerData data = serverPlayer.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        LivingEntity victim = event.getEntity();

        DnDPlayerData data = killer.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        int xpAmount = MobXpRewardSystem.getXpForEntity(victim);
        if (xpAmount <= 0) return;

        // Racial XP feats (e.g. Quick Learner) scale all DnD XP gains.
        xpAmount = Math.round(xpAmount * (1.0f + data.getRacialXpBonus()));

        boolean wasAvailable = data.isLevelUpAvailable();
        data.addXp(xpAmount);

        if (!wasAvailable && data.isLevelUpAvailable()) {
            killer.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eYou have enough experience to level up! Press L to open the level up menu."));
        }

        PacketDistributor.sendToPlayer(killer, SyncPlayerDataPayload.fromPlayer(data));
    }

    public static void grantXp(ServerPlayer player, int amount) {
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        boolean wasAvailable = data.isLevelUpAvailable();
        data.addXp(amount);

        if (!wasAvailable && data.isLevelUpAvailable()) {
            player.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eYou have enough experience to level up! Press L to open the level up menu."));
        }

        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
    }
}
