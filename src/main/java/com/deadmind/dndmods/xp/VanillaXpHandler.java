package com.deadmind.dndmods.xp;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class VanillaXpHandler {

    public static final TagKey<EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "boss"));

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player attacker = event.getAttackingPlayer();
        if (attacker == null) return;
        if (!(attacker instanceof ServerPlayer serverPlayer)) return;

        DnDPlayerData data = serverPlayer.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        int dndXp = DnDXpValues.convertVanillaXp(event.getDroppedExperience());

        if (event.getEntity().getType().is(BOSS_TAG)) {
            dndXp += DnDXpValues.BOSS_KILL_XP;
        }

        data.addXp(dndXp);
        PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));

        event.setCanceled(true);
    }

    public static void grantXp(ServerPlayer player, int amount) {
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() == DnDClass.NONE) return;

        data.addXp(amount);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
    }
}
