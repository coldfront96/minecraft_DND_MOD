package com.khimairacraft.ability;

public class AbilityBarState {
    public static final int SLOTS = 9;
    private static boolean open = false;
    private static final String[] slotAbilityIds = new String[SLOTS];
    private static int selectedSlot = -1;

    public static boolean isOpen() {
        return open;
    }

    public static void toggle() {
        open = !open;
        if (!open) {
            selectedSlot = -1;
        }
    }

    public static void setOpen(boolean value) {
        open = value;
        if (!open) {
            selectedSlot = -1;
        }
    }

    public static String getAbilityInSlot(int slot) {
        if (slot < 0 || slot >= SLOTS) return null;
        return slotAbilityIds[slot];
    }

    public static void setAbilityInSlot(int slot, String abilityId) {
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

    public static void clearSlot(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            slotAbilityIds[slot] = null;
        }
    }

    public static void clearAllSlots() {
        for (int i = 0; i < SLOTS; i++) {
            slotAbilityIds[i] = null;
        }
    }

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    public static void setSelectedSlot(int slot) {
        selectedSlot = slot;
    }
}
