package com.deadmind.dndmods.enchanting;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class EnchantingTableHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var blockState = event.getLevel().getBlockState(event.getPos());
        if (!(blockState.getBlock() instanceof EnchantingTableBlock)) return;
        if (blockState.getBlock() instanceof ArcaneEnchantingTableBlock) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        DnDClass dndClass = data.getDnDClass();

        switch (dndClass) {
            case WIZARD, CLERIC -> {}
            case RANGER -> {}
            case FIGHTER, ROGUE, BARBARIAN, NONE -> {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("You lack the arcane knowledge to use this."));
            }
        }
    }
}
