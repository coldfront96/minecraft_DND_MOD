package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public class SmiteUndead extends Ability {
    public SmiteUndead() {
        super("cleric_smite_undead", "Smite Undead", DnDClass.CLERIC, 3, 25, 80);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(6.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof Monster monster) {
                monster.hurt(player.damageSources().playerAttack(player), 12.0f);
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
        }
    }
}
