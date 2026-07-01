package com.deadmind.dndmods.party;

/**
 * Authority ranks within a {@link Party}, declared in descending order of
 * authority. {@code LEADER} is the highest, {@code MEMBER} the lowest. Because
 * the enum is declared highest-first, a lower {@link #ordinal()} means greater
 * authority.
 */
public enum PartyRank {
    LEADER,
    FIRST_IN_COMMAND,
    SECOND_IN_COMMAND,
    THIRD_IN_COMMAND,
    MEMBER;

    /** Returns true if this rank has strictly higher authority than {@code other}. */
    public boolean outranks(PartyRank other) {
        if (other == null) return true;
        return this.ordinal() < other.ordinal();
    }
}
