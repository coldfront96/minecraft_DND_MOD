package com.khimairacraft.client;

import com.khimairacraft.DnDMods;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = DnDMods.MOD_ID, value = Dist.CLIENT)
public class VanillaXpBarSuppressor {

    @SubscribeEvent
    public static void onPreRenderExperienceBar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPreRenderExperienceLevel(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        if (data.getDnDClass() != DnDClass.NONE) {
            event.setCanceled(true);
        }
    }
}
