package com.deadmind.dndmods.ability;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.SaveType;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class Ability {
    private final String id;
    private final String name;
    private final String description;
    private final DnDClass requiredClass;
    private final int requiredLevel;
    private final int resourceCost;
    private final int cooldownTicks;
    private final ClickBehavior clickBehavior;

    protected Ability(String id, String name, String description, DnDClass requiredClass, int requiredLevel, int resourceCost, int cooldownTicks, ClickBehavior clickBehavior) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredClass = requiredClass;
        this.requiredLevel = requiredLevel;
        this.resourceCost = resourceCost;
        this.cooldownTicks = cooldownTicks;
        this.clickBehavior = clickBehavior;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public DnDClass getRequiredClass() { return requiredClass; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getResourceCost() { return resourceCost; }
    public int getCooldownTicks() { return cooldownTicks; }
    public ClickBehavior getClickBehavior() { return clickBehavior; }

    public float getBaseDamage() { return 0.0f; }

    @Nullable
    public AbilityScoreType getDamageScalingStat() { return null; }

    @Nullable
    public SaveType getRequiredSave() { return null; }

    public boolean isHalfOnSave() { return false; }

    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (!meetsLevelRequirement(data)) return false;
        if (data.getCurrentResource() < resourceCost) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    public boolean meetsLevelRequirement(DnDPlayerData data) {
        if (data.getPrimary().getDnDClass() == requiredClass
                && data.getPrimary().getLevel() >= requiredLevel) {
            return true;
        }
        if (data.getSecondary() != null
                && data.getSecondary().getDnDClass() == requiredClass
                && data.getSecondary().getLevel() >= requiredLevel) {
            return true;
        }
        return false;
    }

    public void execute(ServerPlayer player, DnDPlayerData data) {
        data.consumeResource(resourceCost);
        AbilityCooldownManager.setCooldown(player, this, cooldownTicks);
        onUse(player, data);
    }

    protected abstract void onUse(ServerPlayer player, DnDPlayerData data);

    public Component getDisplayName() {
        return Component.literal(name);
    }

    public Component getTooltip(DnDPlayerData data) {
        return Component.literal(name + " - Cost: " + resourceCost + " " +
                requiredClass.getResourceType().getDisplayName() +
                " - Level " + requiredLevel);
    }
}
