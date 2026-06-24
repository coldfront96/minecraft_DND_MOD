package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.classes.DnDClass;
import net.minecraft.nbt.CompoundTag;

public class DnDPlayerData {
    public static final int MAX_LEVEL = 20;

    private DnDClass dndClass = DnDClass.NONE;
    private int level = 1;
    private int xp = 0;
    private final AbilityScores abilityScores = new AbilityScores();
    private int currentHp;
    private int currentResource;

    public DnDPlayerData() {
        this.currentHp = dndClass.getMaxHpAtLevel(level);
        this.currentResource = 0;
    }

    public DnDClass getDnDClass() { return dndClass; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public AbilityScores getAbilityScores() { return abilityScores; }
    public int getCurrentHp() { return currentHp; }
    public int getCurrentResource() { return currentResource; }

    public int getMaxHp() {
        int conBonus = abilityScores.getConMod() * level;
        return dndClass.getMaxHpAtLevel(level) + conBonus;
    }

    public int getMaxResource() {
        if (dndClass == DnDClass.NONE) return 0;
        return dndClass.getMaxResourceAtLevel(level);
    }

    public void setDnDClass(DnDClass dndClass) {
        this.dndClass = dndClass;
        this.currentHp = getMaxHp();
        this.currentResource = getMaxResource();
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public void addXp(int amount) {
        this.xp += amount;
        while (this.xp >= getXpForNextLevel() && this.level < MAX_LEVEL) {
            this.xp -= getXpForNextLevel();
            levelUp();
        }
    }

    public int getXpForNextLevel() {
        return level * 100 + (level * level * 50);
    }

    private void levelUp() {
        int oldMaxHp = getMaxHp();
        this.level++;
        int newMaxHp = getMaxHp();
        this.currentHp += (newMaxHp - oldMaxHp);
        this.currentResource = getMaxResource();
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(getMaxHp(), hp));
    }

    public void setCurrentResource(int resource) {
        this.currentResource = Math.max(0, Math.min(getMaxResource(), resource));
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
        tag.putInt("XP", xp);
        tag.put("AbilityScores", abilityScores.save());
        tag.putInt("CurrentHP", currentHp);
        tag.putInt("CurrentResource", currentResource);
        return tag;
    }

    public void load(CompoundTag tag) {
        dndClass = DnDClass.valueOf(tag.getString("Class"));
        level = tag.getInt("Level");
        xp = tag.getInt("XP");
        abilityScores.load(tag.getCompound("AbilityScores"));
        currentHp = tag.getInt("CurrentHP");
        currentResource = tag.getInt("CurrentResource");
    }

    public void copyFrom(DnDPlayerData other) {
        this.dndClass = other.dndClass;
        this.level = other.level;
        this.xp = other.xp;
        this.abilityScores.copyFrom(other.abilityScores);
        this.currentHp = other.currentHp;
        this.currentResource = other.currentResource;
    }
}
