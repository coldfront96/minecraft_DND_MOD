package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BerserkerRage extends Ability {
    public BerserkerRage() {
        super("barbarian_berserker_rage", "Berserker Rage",
                "Reckless effect + 30% move speed for 8 seconds",
                DnDClass.BARBARIAN, 8, 40, 300, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, false, true));

        PerPlayerCombatState.get(player.getUUID()).setReckless(160);
    }
}
