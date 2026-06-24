package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

public class MultiShot extends Ability {
    public MultiShot() {
        super("ranger_multi_shot", "Multi Shot", DnDClass.RANGER, 1, 15, 60);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(player.level(), player);
            double spread = i * 0.15;
            arrow.shoot(look.x + spread, look.y, look.z + spread, 3.0f, 1.0f);
            arrow.setOwner(player);
            arrow.setBaseDamage(2.5);
            player.level().addFreshEntity(arrow);
        }
    }
}
