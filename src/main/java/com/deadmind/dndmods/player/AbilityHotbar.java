package com.deadmind.dndmods.player;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class AbilityHotbar {
    public static final int SLOTS = 9;

    private final String[] slotAbilityIds = new String[SLOTS];
    private int activeSlot = 0;

    public void slotAbility(int slot, String abilityId) {
        if (slot < 0 || slot >= SLOTS) return;
        if (abilityId != null) {
            for (int i = 0; i < SLOTS; i++) {
                if (abilityId.equals(slotAbilityIds[i])) {
                    slotAbilityIds[i] = null;
                }
            }
        }
        slotAbilityIds[slot] = abilityId;
    }

    public void clearSlot(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            slotAbilityIds[slot] = null;
        }
    }

    @Nullable
    public String getSlotAbility(int slot) {
        if (slot < 0 || slot >= SLOTS) return null;
        return slotAbilityIds[slot];
    }

    public void setActiveSlot(int slot) {
        this.activeSlot = Math.max(0, Math.min(SLOTS - 1, slot));
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    @Nullable
    public String getActiveAbility() {
        return getSlotAbility(activeSlot);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            list.add(StringTag.valueOf(slotAbilityIds[i] != null ? slotAbilityIds[i] : ""));
        }
        tag.put("Slots", list);
        tag.putInt("ActiveSlot", activeSlot);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Slots")) {
            ListTag list = tag.getList("Slots", Tag.TAG_STRING);
            for (int i = 0; i < SLOTS && i < list.size(); i++) {
                String id = list.getString(i);
                slotAbilityIds[i] = id.isEmpty() ? null : id;
            }
        }
        activeSlot = tag.getInt("ActiveSlot");
    }

    public void validateAndClean(DnDPlayerData data) {
        for (int i = 0; i < SLOTS; i++) {
            String abilityId = slotAbilityIds[i];
            if (abilityId == null) continue;

            Ability ability = AbilityRegistry.getAbility(abilityId);
            if (ability == null || !ability.meetsLevelRequirement(data)) {
                slotAbilityIds[i] = null;
            }
        }
    }

    public void copyFrom(AbilityHotbar other) {
        System.arraycopy(other.slotAbilityIds, 0, this.slotAbilityIds, 0, SLOTS);
        this.activeSlot = other.activeSlot;
    }
}
