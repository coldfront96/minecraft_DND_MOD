package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class DivineShield extends Ability {
    public DivineShield() {
        super("cleric_divine_shield", "Divine Shield",
                "Grant Resistance II for 8 seconds",
                DnDClass.CLERIC, 7, 40, 240, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, true));
    }
}
