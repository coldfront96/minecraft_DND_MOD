package com.khimairacraft.guild;

/**
 * Zone designation for a claimed guild chunk. {@link #ARENA} is special: it
 * forces friendly fire on between all players inside, regardless of party or
 * guild settings (handled in {@code FriendlyFireChecker}).
 */
public enum GuildZoneType {
    /** Default claimed land. */
    GENERAL,
    /** Trading area. */
    MARKET,
    /** PvP zone — friendly fire always enabled inside. */
    ARENA,
    /** Guild hall / main base area. */
    HALL,
    /** Resource gathering area. */
    FARM,
    /** Officers / master only access. */
    RESTRICTED
}
