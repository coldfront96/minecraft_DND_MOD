package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class HealingWord extends Ability {
    public HealingWord() {
        super("cleric_healing_word", "Healing Word", DnDClass.CLERIC, 1, 20, 60);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.heal(8.0f);
        AABB area = player.getBoundingBox().inflate(5.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof Player ally) {
                ally.heal(4.0f);
            }
        }
    }
}
