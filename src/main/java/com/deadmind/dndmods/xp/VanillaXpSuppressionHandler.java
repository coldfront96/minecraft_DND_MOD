package com.deadmind.dndmods.xp;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class VanillaXpSuppressionHandler {

    @SubscribeEvent
    public static void onXpLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE) {
            event.setCanceled(true);
        }
    }
}
