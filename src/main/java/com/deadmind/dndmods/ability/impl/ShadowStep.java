package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class ShadowStep extends Ability {
    public ShadowStep() {
        super("rogue_shadow_step", "Shadow Step", DnDClass.ROGUE, 7, 30, 160);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        double distance = 8.0;
        player.teleportTo(
                player.getX() + look.x * distance,
                player.getY() + look.y * distance,
                player.getZ() + look.z * distance
        );
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
    }
}
