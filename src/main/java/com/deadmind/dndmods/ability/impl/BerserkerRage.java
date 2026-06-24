package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BerserkerRage extends Ability {
    public BerserkerRage() {
        super("barbarian_berserker_rage", "Berserker Rage",
                "Reckless effect + 30% move speed for 8 seconds",
                DnDClass.BARBARIAN, 8, 40, 300);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // Reckless component: +damage, +incoming damage
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 2, false, true));
        player.getPersistentData().putLong("dndmods_reckless_until", player.level().getGameTime() + 160);

        // +30% move speed (Speed I = +20%, Speed II = +40%; use I as closest without overshooting)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, true));

        // Regeneration for sustain
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, false, true));
    }
}
