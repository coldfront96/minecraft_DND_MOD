package com.khimairacraft.playerdata;

import net.minecraft.world.entity.player.Player;

public class PlayerDataHelper {
    public static DnDPlayerData get(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA);
    }
}
