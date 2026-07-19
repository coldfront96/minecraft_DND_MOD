package com.khimairacraft.block.entity;

import com.khimairacraft.ModBlockEntities;
import com.khimairacraft.block.ArcanePhylacteryTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArcanePhylacteryBlockEntity extends BlockEntity {

    private int tier;
    private long storedXp;
    private long maxXp;
    private long tickCounter;

    public ArcanePhylacteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_PHYLACTERY_BE.get(), pos, state);
        this.tier = 1;
        ArcanePhylacteryTier tierData = ArcanePhylacteryTier.TIER_1;
        this.maxXp = tierData.getMaxXp();
        this.storedXp = 0;
        this.tickCounter = 0;
    }

    public void serverTick() {
        ArcanePhylacteryTier tierData = ArcanePhylacteryTier.fromTier(tier);
        if (storedXp >= maxXp) return;

        tickCounter++;
        if (tickCounter >= tierData.getTickInterval()) {
            tickCounter = 0;
            storedXp = Math.min(storedXp + 1, maxXp);
            setChanged();
        }
    }

    public int getTier() { return tier; }
    public long getStoredXp() { return storedXp; }
    public long getMaxXp() { return maxXp; }

    public void setTier(int newTier) {
        this.tier = Math.max(1, Math.min(4, newTier));
        ArcanePhylacteryTier tierData = ArcanePhylacteryTier.fromTier(this.tier);
        this.maxXp = tierData.getMaxXp();
        setChanged();
    }

    public void setStoredXp(long xp) {
        this.storedXp = Math.max(0, Math.min(maxXp, xp));
        setChanged();
    }

    public long drainXp(long amount) {
        long drained = Math.min(amount, storedXp);
        storedXp -= drained;
        if (drained > 0) setChanged();
        return drained;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier);
        tag.putLong("StoredXp", storedXp);
        tag.putLong("MaxXp", maxXp);
        tag.putLong("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt("Tier");
        if (tier < 1 || tier > 4) tier = 1;
        storedXp = tag.getLong("StoredXp");
        maxXp = tag.getLong("MaxXp");
        tickCounter = tag.getLong("TickCounter");
        ArcanePhylacteryTier tierData = ArcanePhylacteryTier.fromTier(tier);
        if (maxXp <= 0) maxXp = tierData.getMaxXp();
    }
}
