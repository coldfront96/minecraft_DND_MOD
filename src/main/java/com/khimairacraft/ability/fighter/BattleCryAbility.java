package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Level 3 — Battle Cry. Self buff granting +2 damage (and +2 attack, deferred)
 * for 10 seconds. The damage bonus is applied in FighterPassives while active.
 */
public class BattleCryAbility extends FighterAbility {
    public BattleCryAbility() {
        super("fighter_battle_cry", "Battle Cry",
                "Roar a battle cry: +2 attack and +2 damage for 10 seconds.",
                3, 20, 1200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setBattleCry(200);
        player.sendSystemMessage(Component.literal("§6[DnDMods] §eBattle Cry! Your strikes are emboldened."));
    }
}
