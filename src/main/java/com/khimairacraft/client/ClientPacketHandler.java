package com.khimairacraft.client;

import com.khimairacraft.client.screen.RaceSelectionScreen;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void openClassSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof ClassSelectionScreen)) {
            mc.setScreen(new ClassSelectionScreen());
        }
    }

    public static void openRaceSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof RaceSelectionScreen)) {
            mc.setScreen(new RaceSelectionScreen());
        }
    }

    public static void handleSyncPlayerData(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            DnDPlayerData playerData = mc.player.getData(ModAttachments.PLAYER_DATA);
            playerData.load(data);
        }
    }

    public static void handleAbilityAssignRejected(int slotIndex, String reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DnDPlayerData data = mc.player.getData(ModAttachments.PLAYER_DATA);
        data.getAbilityHotbar().clearSlot(slotIndex);

        mc.player.displayClientMessage(
                Component.literal("§c[DnDMods] " + reason), false);
    }
}
