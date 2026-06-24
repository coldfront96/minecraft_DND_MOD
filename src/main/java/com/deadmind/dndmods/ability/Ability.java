package com.deadmind.dndmods.ability;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public abstract class Ability {
    private final String id;
    private final String name;
    private final String description;
    private final DnDClass requiredClass;
    private final int requiredLevel;
    private final int resourceCost;
    private final int cooldownTicks;

    protected Ability(String id, String name, String description, DnDClass requiredClass, int requiredLevel, int resourceCost, int cooldownTicks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredClass = requiredClass;
        this.requiredLevel = requiredLevel;
        this.resourceCost = resourceCost;
        this.cooldownTicks = cooldownTicks;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public DnDClass getRequiredClass() { return requiredClass; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getResourceCost() { return resourceCost; }
    public int getCooldownTicks() { return cooldownTicks; }

    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getDnDClass() != requiredClass) return false;
        if (data.getLevel() < requiredLevel) return false;
        if (data.getCurrentResource() < resourceCost) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
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
