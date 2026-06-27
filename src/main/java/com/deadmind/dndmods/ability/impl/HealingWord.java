package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class HealingWord extends Ability {
    public HealingWord() {
        super("cleric_healing_word", "Healing Word",
                "Restore 40% max HP to self or nearest ally in 10 blocks",
                DnDClass.CLERIC, 1, 20, 60, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        float healAmount = player.getMaxHealth() * 0.4f;

        Player nearestAlly = null;
        double closestDist = 10.0;
        AABB area = player.getBoundingBox().inflate(10.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof Player ally && ally != player) {
                double dist = ally.distanceTo(player);
                if (dist < closestDist && ally.getHealth() < ally.getMaxHealth()) {
                    closestDist = dist;
                    nearestAlly = ally;
                }
            }
        }

        if (nearestAlly != null && nearestAlly.getHealth() / nearestAlly.getMaxHealth() < player.getHealth() / player.getMaxHealth()) {
            nearestAlly.heal(healAmount);
        } else {
            player.heal(healAmount);
        }
    }
}
