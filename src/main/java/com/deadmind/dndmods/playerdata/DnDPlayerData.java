package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.classes.PrestigeClass;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DnDPlayerData {
    public static final int MAX_LEVEL = 20;

    private ClassEntry primary = new ClassEntry(DnDClass.NONE, 1);
    @Nullable
    private ClassEntry secondary = null;
    private PrestigeClass prestigeClass = PrestigeClass.NONE;
    private int xp = 0;
    private final AbilityScores abilityScores = new AbilityScores();
    private int currentHp;
    private final Map<String, Boolean> achievementFlags = new HashMap<>();

    public DnDPlayerData() {
        this.currentHp = primary.getMaxHp();
    }

    // --- Backward-compatible accessors (delegate to primary) ---

    public DnDClass getDnDClass() { return primary.getDnDClass(); }
    public int getLevel() { return primary.getLevel(); }
    public int getXp() { return xp; }
    public AbilityScores getAbilityScores() { return abilityScores; }
    public int getCurrentHp() { return currentHp; }

    public int getCurrentResource() {
        if (isUnifiedPool()) {
            return primary.getCurrentResource() + (secondary != null ? secondary.getCurrentResource() : 0);
        }
        return primary.getCurrentResource();
    }

    public int getMaxResource() {
        if (isUnifiedPool()) {
            return primary.getMaxResource() + (secondary != null ? secondary.getMaxResource() : 0);
        }
        return primary.getMaxResource();
    }

    public int getMaxHp() {
        int hp = primary.getMaxHp();
        if (secondary != null) {
            hp += secondary.getMaxHp();
        }
        hp += abilityScores.getConMod() * getTotalLevel();
        return hp;
    }

    public void setDnDClass(DnDClass dndClass) {
        this.primary = new ClassEntry(dndClass, 1);
        this.currentHp = getMaxHp();
    }

    public void setLevel(int level) {
        primary.setLevel(Math.max(1, Math.min(MAX_LEVEL, level)));
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(getMaxHp(), hp));
    }

    public void setCurrentResource(int resource) {
        primary.setCurrentResource(resource);
    }

    public boolean consumeResource(int amount) {
        if (isUnifiedPool()) {
            int total = getCurrentResource();
            if (total < amount) return false;
            int remaining = amount;
            int fromPrimary = Math.min(remaining, primary.getCurrentResource());
            primary.setCurrentResource(primary.getCurrentResource() - fromPrimary);
            remaining -= fromPrimary;
            if (remaining > 0 && secondary != null) {
                secondary.setCurrentResource(secondary.getCurrentResource() - remaining);
            }
            return true;
        }
        return primary.consumeResource(amount);
    }

    public void restoreResource(int amount) {
        if (isUnifiedPool()) {
            int remaining = amount;
            int primarySpace = primary.getMaxResource() - primary.getCurrentResource();
            int toPrimary = Math.min(remaining, primarySpace);
            primary.restoreResource(toPrimary);
            remaining -= toPrimary;
            if (remaining > 0 && secondary != null) {
                secondary.restoreResource(remaining);
            }
            return;
        }
        primary.restoreResource(amount);
    }

    // --- Multiclass accessors ---

    public ClassEntry getPrimary() { return primary; }
    @Nullable public ClassEntry getSecondary() { return secondary; }
    public PrestigeClass getPrestigeClass() { return prestigeClass; }

    public int getTotalLevel() {
        return primary.getLevel() + (secondary != null ? secondary.getLevel() : 0);
    }

    public boolean isUnifiedPool() {
        return prestigeClass.unifiesPools() && secondary != null;
    }

    public int getUnifiedResource() {
        return primary.getCurrentResource() + (secondary != null ? secondary.getCurrentResource() : 0);
    }

    public int getUnifiedMaxResource() {
        return primary.getMaxResource() + (secondary != null ? secondary.getMaxResource() : 0);
    }

    public boolean canMulticlass() {
        return primary.getLevel() >= 3 && secondary == null && getTotalLevel() < MAX_LEVEL;
    }

    public void setSecondaryClass(DnDClass dndClass) {
        if (!canMulticlass()) return;
        if (dndClass == DnDClass.NONE || dndClass == primary.getDnDClass()) return;
        this.secondary = new ClassEntry(dndClass, 1);
    }

    public boolean canLevelUp(boolean advancePrimary) {
        if (getTotalLevel() >= MAX_LEVEL) return false;
        if (xp < getXpForNextLevel()) return false;
        ClassEntry entry = advancePrimary ? primary : secondary;
        return entry != null && entry.getDnDClass() != DnDClass.NONE;
    }

    public void levelUp(boolean advancePrimary) {
        if (!canLevelUp(advancePrimary)) return;
        xp -= getXpForNextLevel();
        ClassEntry entry = advancePrimary ? primary : secondary;
        int oldMaxHp = getMaxHp();
        entry.setLevel(entry.getLevel() + 1);
        int newMaxHp = getMaxHp();
        currentHp += (newMaxHp - oldMaxHp);
        entry.setCurrentResource(entry.getMaxResource());
    }

    public void setPrestigeClass(PrestigeClass prestige) {
        this.prestigeClass = prestige;
    }

    // --- Achievement flags ---

    public void setAchievementFlag(String key, boolean value) {
        achievementFlags.put(key, value);
    }

    public boolean getAchievementFlag(String key) {
        return achievementFlags.getOrDefault(key, false);
    }

    public Map<String, Boolean> getAchievementFlags() {
        return achievementFlags;
    }

    // --- XP ---

    public void addXp(int amount) {
        this.xp += amount;
        if (secondary == null) {
            while (this.xp >= getXpForNextLevel() && getTotalLevel() < MAX_LEVEL) {
                this.xp -= getXpForNextLevel();
                int oldMaxHp = getMaxHp();
                primary.setLevel(primary.getLevel() + 1);
                int newMaxHp = getMaxHp();
                currentHp += (newMaxHp - oldMaxHp);
                primary.setCurrentResource(primary.getMaxResource());
            }
        }
        // When multiclassed, XP banks — player must choose via levelUp(boolean)
    }

    public int getXpForNextLevel() {
        int totalLevel = getTotalLevel();
        return totalLevel * 100 + (totalLevel * totalLevel * 50);
    }

    // --- Serialization ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Primary", primary.save());
        if (secondary != null) {
            tag.put("Secondary", secondary.save());
        }
        tag.putString("PrestigeClass", prestigeClass.name());
        tag.putInt("XP", xp);
        tag.put("AbilityScores", abilityScores.save());
        tag.putInt("CurrentHP", currentHp);

        CompoundTag flags = new CompoundTag();
        achievementFlags.forEach(flags::putBoolean);
        tag.put("Achievements", flags);

        return tag;
    }

    public void load(CompoundTag tag) {
        // Support old format (single Class/Level/CurrentResource)
        if (tag.contains("Class")) {
            DnDClass cls = DnDClass.valueOf(tag.getString("Class"));
            int level = tag.getInt("Level");
            primary = new ClassEntry(cls, level);
            primary.setCurrentResource(tag.getInt("CurrentResource"));
            secondary = null;
            prestigeClass = PrestigeClass.NONE;
        } else {
            primary = new ClassEntry();
            primary.load(tag.getCompound("Primary"));
            if (tag.contains("Secondary")) {
                secondary = new ClassEntry();
                secondary.load(tag.getCompound("Secondary"));
            } else {
                secondary = null;
            }
            try {
                prestigeClass = PrestigeClass.valueOf(tag.getString("PrestigeClass"));
            } catch (IllegalArgumentException e) {
                prestigeClass = PrestigeClass.NONE;
            }
        }

        xp = tag.getInt("XP");
        abilityScores.load(tag.getCompound("AbilityScores"));
        currentHp = tag.getInt("CurrentHP");

        achievementFlags.clear();
        if (tag.contains("Achievements")) {
            CompoundTag flags = tag.getCompound("Achievements");
            for (String key : flags.getAllKeys()) {
                achievementFlags.put(key, flags.getBoolean(key));
            }
        }
    }

    public void copyFrom(DnDPlayerData other) {
        this.primary = new ClassEntry();
        this.primary.copyFrom(other.primary);
        if (other.secondary != null) {
            this.secondary = new ClassEntry();
            this.secondary.copyFrom(other.secondary);
        } else {
            this.secondary = null;
        }
        this.prestigeClass = other.prestigeClass;
        this.xp = other.xp;
        this.abilityScores.copyFrom(other.abilityScores);
        this.currentHp = other.currentHp;
        this.achievementFlags.clear();
        this.achievementFlags.putAll(other.achievementFlags);
    }
}
