package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Fireball extends Ability {
    public Fireball() {
        super("wizard_fireball", "Fireball", DnDClass.WIZARD, 1, 25, 80);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(8.0));
        double radius = 4.0;
        AABB area = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.setRemainingFireTicks(100);
                living.hurt(player.damageSources().playerAttack(player), 8.0f);
            }
        }
    }
}
