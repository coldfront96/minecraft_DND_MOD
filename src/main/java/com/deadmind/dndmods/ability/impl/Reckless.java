package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Reckless extends Ability {
    public Reckless() {
        super("barbarian_reckless", "Reckless Attack",
                "Deal +40% damage but take +20% more for 10 seconds",
                DnDClass.BARBARIAN, 1, 10, 60, ClickBehavior.ENHANCES_ATTACK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // Strength II for +40% damage approximation
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true));
        // Mark as reckless so CombatEventHandler applies +20% incoming damage
        player.getPersistentData().putLong("dndmods_reckless_until", player.level().getGameTime() + 200);
    }
}
