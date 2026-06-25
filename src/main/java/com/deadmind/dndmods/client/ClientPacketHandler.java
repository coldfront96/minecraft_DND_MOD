package com.deadmind.dndmods.client;

import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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

    public static void handleSyncPlayerData(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            DnDPlayerData playerData = mc.player.getData(ModAttachments.PLAYER_DATA);
            playerData.load(data);
        }
    }
}
