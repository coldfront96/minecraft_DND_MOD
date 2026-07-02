package com.khimairacraft.xp;

import com.khimairacraft.DnDMods;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class VanillaXpSuppressionHandler {

    @SubscribeEvent
    public static void onXpLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Only suppress vanilla level *gains* for classed players (they use the
        // DnD XP system instead). Negative changes are spending — enchanting and
        // anvil costs, and the phylactery XP drain — which must still go through.
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE && event.getLevels() > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // As above: suppress only XP gains, never deductions (which pay for
        // enchanting/anvil work and feed the phylactery system).
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE && event.getAmount() > 0) {
            event.setCanceled(true);
        }
    }
}
