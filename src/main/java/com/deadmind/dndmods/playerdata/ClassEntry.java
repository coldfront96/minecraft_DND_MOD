package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.classes.DnDClass;
import net.minecraft.nbt.CompoundTag;

public class ClassEntry {
    private DnDClass dndClass;
    private int level;
    private int currentResource;

    public ClassEntry(DnDClass dndClass, int level) {
        this.dndClass = dndClass;
        this.level = level;
        this.currentResource = getMaxResource();
    }

    public ClassEntry() {
        this(DnDClass.NONE, 1);
    }

    public DnDClass getDnDClass() { return dndClass; }
    public int getLevel() { return level; }
    public int getCurrentResource() { return currentResource; }

    public void setDnDClass(DnDClass dndClass) { this.dndClass = dndClass; }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(DnDPlayerData.MAX_LEVEL, level));
    }

    public void setCurrentResource(int resource) {
        this.currentResource = Math.max(0, Math.min(getMaxResource(), resource));
    }

    public int getMaxResource() {
        if (dndClass == DnDClass.NONE) return 0;
        return dndClass.getMaxResourceAtLevel(level);
    }

    public int getMaxHp() {
        return dndClass.getMaxHpAtLevel(level);
    }

    public boolean consumeResource(int amount) {
        if (currentResource < amount) return false;
        currentResource -= amount;
        return true;
    }

    public void restoreResource(int amount) {
        currentResource = Math.min(getMaxResource(), currentResource + amount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Class", dndClass.name());
        tag.putInt("Level", level);
        tag.putInt("Resource", currentResource);
        return tag;
    }

    public void load(CompoundTag tag) {
        dndClass = DnDClass.valueOf(tag.getString("Class"));
        level = tag.getInt("Level");
        currentResource = tag.getInt("Resource");
    }

    public void copyFrom(ClassEntry other) {
        this.dndClass = other.dndClass;
        this.level = other.level;
        this.currentResource = other.currentResource;
    }
}
