package com.deadmind.dndmods.ability;

public class AbilityBarState {
    public static final int SLOTS = 9;
    private static boolean open = false;

    public static boolean isOpen() {
        return open;
    }

    public static void toggle() {
        open = !open;
    }

    public static void setOpen(boolean value) {
        open = value;
    }
}
