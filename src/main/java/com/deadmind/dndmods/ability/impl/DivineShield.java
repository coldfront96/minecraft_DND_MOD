package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class DivineShield extends Ability {
    public DivineShield() {
        super("cleric_divine_shield", "Divine Shield", DnDClass.CLERIC, 7, 40, 240);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        MobEffectInstance resistance = new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3);
        player.addEffect(resistance);
        AABB area = player.getBoundingBox().inflate(5.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof Player ally) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
            }
        }
    }
}
