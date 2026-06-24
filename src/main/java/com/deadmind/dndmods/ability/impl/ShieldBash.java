package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ShieldBash extends Ability {
    public ShieldBash() {
        super("fighter_shield_bash", "Shield Bash", DnDClass.FIGHTER, 3, 20, 80);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(3.0);
        List<Entity> nearby = player.level().getEntities(player, area);
        for (Entity entity : nearby) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                living.knockback(1.5, player.getX() - entity.getX(), player.getZ() - entity.getZ());
            }
        }
    }
}
