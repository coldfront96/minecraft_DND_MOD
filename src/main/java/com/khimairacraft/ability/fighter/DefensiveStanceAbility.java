package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Level 7 — Defensive Stance. For 8 seconds gain a strong defensive posture at
 * the cost of half movement speed. In this mod's vanilla-damage model the "+4 AC"
 * is realised as incoming-damage reduction (applied in FighterPassives while the
 * stance flag is active); the +2 saves are a display/ability-save value. The
 * movement penalty is a real Slowness effect.
 */
public class DefensiveStanceAbility extends FighterAbility {
    public DefensiveStanceAbility() {
        super("fighter_defensive_stance", "Defensive Stance",
                "For 8 seconds gain +4 AC and +2 saves, but move at half speed.",
                7, 25, 0, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setDefensiveStance(160);
        // Slowness III ~ -45% movement speed for the duration (≈ the spec's -50%).
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2, false, true));
    }
}
