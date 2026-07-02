package com.khimairacraft.items.enchanting;

import net.minecraft.world.item.Item;

public class ArcaneDust extends Item {
    private final DustTier tier;

    public ArcaneDust(DustTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public DustTier getTier() {
        return tier;
    }
}
