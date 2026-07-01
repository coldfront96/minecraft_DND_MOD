package com.deadmind.dndmods.guild;

/**
 * Authority ranks within a {@link Guild}, highest authority first. As with
 * {@code PartyRank}, a lower {@link #ordinal()} means greater authority.
 */
public enum GuildRank {
    GUILD_MASTER,
    OFFICER,
    MEMBER;

    /** Returns true if this rank has strictly higher authority than {@code other}. */
    public boolean outranks(GuildRank other) {
        if (other == null) return true;
        return this.ordinal() < other.ordinal();
    }
}
