package com.khimairacraft.party;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A temporary, session-based grouping of up to {@code MAX_MEMBERS} players.
 *
 * <p>Member ordering is significant: {@code members} is a {@link LinkedHashMap}
 * so iteration follows join order, which lets leadership-succession logic fall
 * back to the "oldest member" deterministically. A party is never persisted to
 * disk — it lives only in {@link PartyManager} for the current server session.
 */
public class Party {

    public static final int MAX_MEMBERS = 10;
    public static final int MAX_NAME_LENGTH = 32;

    private final UUID partyId;
    private String partyName;
    private final Map<UUID, PartyRank> members = new LinkedHashMap<>();
    private boolean friendlyFireEnabled = false;
    private final long createdAt;

    public Party(UUID partyId, String partyName) {
        this.partyId = partyId;
        this.partyName = partyName;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getPartyId() { return partyId; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String name) { this.partyName = name; }
    public long getCreatedAt() { return createdAt; }

    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public void setFriendlyFireEnabled(boolean enabled) { this.friendlyFireEnabled = enabled; }

    /** Live view of the member→rank map; iteration order is join order. */
    public Map<UUID, PartyRank> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public int size() {
        return members.size();
    }

    void addMember(UUID playerId, PartyRank rank) {
        members.put(playerId, rank);
    }

    void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    void setRank(UUID playerId, PartyRank rank) {
        if (members.containsKey(playerId)) {
            members.put(playerId, rank);
        }
    }

    /** Returns the UUID of the member holding {@link PartyRank#LEADER}, or null. */
    @Nullable
    public UUID getLeader() {
        for (Map.Entry<UUID, PartyRank> entry : members.entrySet()) {
            if (entry.getValue() == PartyRank.LEADER) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** All members holding the given rank, in join order. */
    public List<UUID> getByRank(PartyRank rank) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, PartyRank> entry : members.entrySet()) {
            if (entry.getValue() == rank) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Nullable
    public PartyRank getRank(UUID playerId) {
        return members.get(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean hasRank(UUID playerId, PartyRank rank) {
        return rank == members.get(playerId);
    }

    /** A player may kick anyone they strictly outrank (and never themselves). */
    public boolean canKick(UUID kicker, UUID target) {
        if (kicker == null || target == null) return false;
        if (kicker.equals(target)) return false;
        if (!isMember(kicker) || !isMember(target)) return false;
        return getRank(kicker).outranks(getRank(target));
    }

    /**
     * A player may promote a target they outrank, but never to {@link
     * PartyRank#LEADER} — a target already at {@code FIRST_IN_COMMAND} (or
     * {@code LEADER}) cannot be promoted. Use {@link #transferLeadership} for
     * leadership changes.
     */
    public boolean canPromote(UUID promoter, UUID target) {
        if (promoter == null || target == null) return false;
        if (promoter.equals(target)) return false;
        if (!isMember(promoter) || !isMember(target)) return false;
        PartyRank targetRank = getRank(target);
        if (targetRank == PartyRank.LEADER || targetRank == PartyRank.FIRST_IN_COMMAND) return false;
        return getRank(promoter).outranks(targetRank);
    }

    /**
     * Promotes the current leader's seat to {@code newLeader}, demoting the old
     * leader to {@code FIRST_IN_COMMAND}. No-op if {@code newLeader} is not a member.
     */
    public void transferLeadership(UUID newLeader) {
        if (!isMember(newLeader)) return;
        UUID oldLeader = getLeader();
        if (oldLeader != null && !oldLeader.equals(newLeader)) {
            members.put(oldLeader, PartyRank.FIRST_IN_COMMAND);
        }
        members.put(newLeader, PartyRank.LEADER);
    }
}
