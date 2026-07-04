package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;

/**
 * Level 11 — Rally. Removes one harmful status effect from the Fighter and heals
 * 10% of max HP. Self-targeted, so no friendly-fire check is required.
 */
public class RallyAbility extends FighterAbility {
    public RallyAbility() {
        super("fighter_rally", "Rally",
                "Shrug off one negative effect and heal 10% of your max HP.",
                11, 30, 400, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
                break;
            }
        }
        player.heal(player.getMaxHealth() * 0.10f);
    }
}
