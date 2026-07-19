package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityCooldownManager;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * Base class for Fighter active abilities. Unlike the generic {@link Ability},
 * these gate on the dedicated Fighter Stamina pool rather than the class
 * resource: they require Fighter levels (maxStamina &gt; 0) and spend Stamina on
 * use, cancelling with a message when there is not enough.
 */
public abstract class FighterAbility extends Ability {

    protected FighterAbility(String id, String name, String description, int requiredLevel,
                             int staminaCost, int cooldownTicks, ClickBehavior clickBehavior) {
        super(id, name, description, DnDClass.FIGHTER, requiredLevel, staminaCost, cooldownTicks, clickBehavior);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (!meetsLevelRequirement(data)) return false;
        if (data.getMaxStamina() <= 0) return false;
        if (data.getCurrentStamina() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        if (!data.trySpendStamina(getResourceCost())) {
            player.sendSystemMessage(Component.literal("§c[DnDMods] Not enough Stamina."));
            return;
        }
        AbilityCooldownManager.setCooldown(player, this, getCooldownTicks());
        onUse(player, data);
    }

    /**
     * Fighter's required class resource is {@link DnDClass#FIGHTER}, which has no
     * generic resource type ({@code ResourceType.NONE}); the base tooltip would
     * therefore label the cost "None". Fighter abilities spend the dedicated
     * Stamina pool, so label the cost accordingly.
     */
    @Override
    public Component getTooltip(DnDPlayerData data) {
        return Component.literal(getName() + " - Cost: " + getResourceCost()
                + " Stamina - Level " + getRequiredLevel());
    }

    /** Shared hostile-target filter: hostile mob or player, not friendly, in line of sight. */
    protected static boolean isValidHostileTarget(ServerPlayer caster, LivingEntity living) {
        if (!(living instanceof Monster) && !(living instanceof ServerPlayer)) return false;
        if (FriendlyFireChecker.isFriendly(caster, living)) return false;
        return caster.hasLineOfSight(living);
    }
}
