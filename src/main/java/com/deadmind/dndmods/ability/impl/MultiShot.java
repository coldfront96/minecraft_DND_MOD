package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

public class MultiShot extends Ability {
    public MultiShot() {
        super("ranger_multi_shot", "Multi Shot",
                "Fire 3 arrows in a spread pattern",
                DnDClass.RANGER, 1, 15, 60, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(player.level(), player);
            Vec3 dir = look.add(right.scale(i * 0.15));
            arrow.shoot(dir.x, dir.y, dir.z, 3.0f, 1.0f);
            arrow.setOwner(player);
            arrow.setBaseDamage(2.5);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            player.level().addFreshEntity(arrow);
        }
    }
}
