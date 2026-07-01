package com.deadmind.dndmods.party;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side, session-only registry of all active {@link Party} instances.
 *
 * <p>Parties are intentionally <em>not</em> persisted: the singleton is reset on
 * {@code ServerStartingEvent}, so every server boot starts with no parties. All
 * lookups go through {@link #playerToParty} for O(1) resolution. Every mutating
 * method tolerates players logging off mid-operation and null-checks the server
 * and online-player lookups before messaging.
 */
public class PartyManager {

    private static final String PREFIX = "§6[DnDMods] ";
    /** Invites expire 60 seconds after being sent. */
    private static final long INVITE_EXPIRY_MS = 60_000L;
    /** A displaced leader may auto-reclaim within 5 minutes of logging back in. */
    private static final long RECLAIM_WINDOW_MS = 5L * 60_000L;

    private static PartyManager instance = new PartyManager();

    public static PartyManager getInstance() {
        return instance;
    }

    /** Clears all party state. Called on server start so sessions never leak across boots. */
    public static void reset() {
        instance = new PartyManager();
    }

    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToParty = new ConcurrentHashMap<>();

    // Pending invites: target -> data. Kept in lock-step.
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();      // target -> partyId
    private final Map<UUID, Long> inviteTimestamps = new ConcurrentHashMap<>();    // target -> sent-at millis
    private final Map<UUID, UUID> inviteInviters = new ConcurrentHashMap<>();      // target -> inviter

    // Leaders displaced by a logout, and the reclaim window opened on their relogin.
    private final Map<UUID, UUID> displacedLeaders = new ConcurrentHashMap<>();    // originalLeader -> partyId
    private final Map<UUID, Long> leaderReclaimWindows = new ConcurrentHashMap<>();// originalLeader -> login millis

    // Best-effort name cache so offline members can still be displayed.
    private final Map<UUID, String> knownNames = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    @Nullable
    public Party getParty(UUID partyId) {
        return partyId == null ? null : parties.get(partyId);
    }

    @Nullable
    public Party getPartyOf(UUID playerId) {
        if (playerId == null) return null;
        UUID partyId = playerToParty.get(playerId);
        return partyId == null ? null : parties.get(partyId);
    }

    public boolean isInParty(UUID playerId) {
        return playerId != null && playerToParty.containsKey(playerId);
    }

    public Map<UUID, Party> getAllParties() {
        return java.util.Collections.unmodifiableMap(parties);
    }

    // ------------------------------------------------------------------
    // Creation / lifecycle
    // ------------------------------------------------------------------

    @Nullable
    public Party createParty(ServerPlayer leader, String name) {
        if (leader == null) return null;
        cacheName(leader);

        if (name == null || name.isBlank()) {
            msg(leader, PREFIX + "§cParty name cannot be empty.");
            return null;
        }
        if (name.length() > Party.MAX_NAME_LENGTH) {
            msg(leader, PREFIX + "§cParty name must be " + Party.MAX_NAME_LENGTH + " characters or fewer.");
            return null;
        }
        if (isInParty(leader.getUUID())) {
            msg(leader, PREFIX + "§cYou are already in a party. Leave it first with /party leave.");
            return null;
        }

        Party party = new Party(UUID.randomUUID(), name);
        party.addMember(leader.getUUID(), PartyRank.LEADER);
        parties.put(party.getPartyId(), party);
        playerToParty.put(leader.getUUID(), party.getPartyId());
        return party;
    }

    public boolean disbandParty(UUID partyId, ServerPlayer requestor) {
        if (requestor == null) return false;
        Party party = getParty(partyId);
        if (party == null) {
            msg(requestor, PREFIX + "§cThat party no longer exists.");
            return false;
        }
        if (!party.hasRank(requestor.getUUID(), PartyRank.LEADER)) {
            msg(requestor, PREFIX + "§cOnly the party leader can disband the party.");
            return false;
        }

        MinecraftServer server = requestor.getServer();
        broadcast(party, server, PREFIX + "§cParty disbanded.");
        removeParty(party);
        return true;
    }

    public boolean leaveParty(ServerPlayer player) {
        if (player == null) return false;
        Party party = getPartyOf(player.getUUID());
        if (party == null) {
            msg(player, PREFIX + "§cYou are not in a party.");
            return false;
        }

        MinecraftServer server = player.getServer();
        boolean wasLeader = party.hasRank(player.getUUID(), PartyRank.LEADER);

        party.removeMember(player.getUUID());
        playerToParty.remove(player.getUUID());
        displacedLeaders.remove(player.getUUID());
        leaderReclaimWindows.remove(player.getUUID());

        if (party.size() == 0) {
            parties.remove(party.getPartyId());
            msg(player, PREFIX + "§eYou have left the party.");
            return true;
        }

        if (wasLeader) {
            UUID successor = findSuccessor(party, null, false, server);
            if (successor != null) {
                party.transferLeadership(successor);
                broadcast(party, server, PREFIX + "§e" + nameOf(successor, server)
                        + " is now the party leader.");
            }
        }

        msg(player, PREFIX + "§eYou have left the party.");
        broadcast(party, server, PREFIX + "§e" + nameOf(player.getUUID(), server) + " has left the party.");
        return true;
    }

    // ------------------------------------------------------------------
    // Invites
    // ------------------------------------------------------------------

    public boolean invitePlayer(ServerPlayer inviter, ServerPlayer target) {
        if (inviter == null || target == null) return false;
        cacheName(inviter);
        cacheName(target);

        Party party = getPartyOf(inviter.getUUID());
        if (party == null) {
            msg(inviter, PREFIX + "§cYou are not in a party.");
            return false;
        }
        PartyRank rank = party.getRank(inviter.getUUID());
        if (rank == PartyRank.MEMBER) {
            msg(inviter, PREFIX + "§cOnly officers and the leader may invite players.");
            return false;
        }
        if (isInParty(target.getUUID())) {
            msg(inviter, PREFIX + "§c" + target.getName().getString() + " is already in a party.");
            return false;
        }
        if (party.size() >= Party.MAX_MEMBERS) {
            msg(inviter, PREFIX + "§cYour party is full (" + Party.MAX_MEMBERS + " members).");
            return false;
        }
        if (pendingInvites.containsKey(target.getUUID())) {
            msg(inviter, PREFIX + "§c" + target.getName().getString() + " already has a pending invite.");
            return false;
        }

        pendingInvites.put(target.getUUID(), party.getPartyId());
        inviteTimestamps.put(target.getUUID(), System.currentTimeMillis());
        inviteInviters.put(target.getUUID(), inviter.getUUID());

        msg(inviter, PREFIX + "§aInvited " + target.getName().getString() + " to the party.");
        msg(target, PREFIX + "§e" + inviter.getName().getString() + " has invited you to join '"
                + party.getPartyName() + "'. Type /party accept or /party decline.");
        return true;
    }

    public boolean acceptInvite(ServerPlayer player) {
        if (player == null) return false;
        cacheName(player);

        UUID partyId = pendingInvites.get(player.getUUID());
        Long sentAt = inviteTimestamps.get(player.getUUID());
        if (partyId == null || sentAt == null) {
            msg(player, PREFIX + "§cYou have no pending party invite.");
            return false;
        }
        if (System.currentTimeMillis() - sentAt > INVITE_EXPIRY_MS) {
            clearInvite(player.getUUID());
            msg(player, PREFIX + "§cThat invite has expired.");
            return false;
        }
        Party party = getParty(partyId);
        if (party == null) {
            clearInvite(player.getUUID());
            msg(player, PREFIX + "§cThat party no longer exists.");
            return false;
        }
        if (party.size() >= Party.MAX_MEMBERS) {
            clearInvite(player.getUUID());
            msg(player, PREFIX + "§cThat party is now full.");
            return false;
        }

        party.addMember(player.getUUID(), PartyRank.MEMBER);
        playerToParty.put(player.getUUID(), party.getPartyId());
        clearInvite(player.getUUID());

        broadcast(party, player.getServer(), PREFIX + "§a" + player.getName().getString()
                + " has joined the party!");
        return true;
    }

    public boolean declineInvite(ServerPlayer player) {
        if (player == null) return false;
        UUID inviterId = inviteInviters.get(player.getUUID());
        if (!pendingInvites.containsKey(player.getUUID())) {
            msg(player, PREFIX + "§cYou have no pending party invite.");
            return false;
        }
        clearInvite(player.getUUID());
        msg(player, PREFIX + "§eYou declined the party invite.");

        MinecraftServer server = player.getServer();
        if (inviterId != null && server != null) {
            ServerPlayer inviter = server.getPlayerList().getPlayer(inviterId);
            if (inviter != null) {
                msg(inviter, PREFIX + "§e" + player.getName().getString() + " declined your party invite.");
            }
        }
        return true;
    }

    /** Called roughly once per second to drop and notify expired invites. */
    public void tickInviteExpiry(MinecraftServer server) {
        if (server == null) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : new LinkedHashMap<>(inviteTimestamps).entrySet()) {
            if (now - entry.getValue() > INVITE_EXPIRY_MS) {
                UUID target = entry.getKey();
                clearInvite(target);
                ServerPlayer player = server.getPlayerList().getPlayer(target);
                if (player != null) {
                    msg(player, PREFIX + "§eYour party invite has expired.");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Rank operations
    // ------------------------------------------------------------------

    public boolean kickPlayer(UUID partyId, ServerPlayer kicker, ServerPlayer target) {
        if (kicker == null || target == null) return false;
        Party party = getParty(partyId);
        if (party == null) {
            msg(kicker, PREFIX + "§cThat party no longer exists.");
            return false;
        }
        if (!party.canKick(kicker.getUUID(), target.getUUID())) {
            msg(kicker, PREFIX + "§cYou cannot kick that player.");
            return false;
        }

        party.removeMember(target.getUUID());
        playerToParty.remove(target.getUUID());
        displacedLeaders.remove(target.getUUID());
        leaderReclaimWindows.remove(target.getUUID());

        MinecraftServer server = kicker.getServer();
        msg(target, PREFIX + "§cYou have been kicked from the party.");
        broadcast(party, server, PREFIX + "§e" + target.getName().getString()
                + " has been kicked from the party.");
        return true;
    }

    public boolean promotePlayer(UUID partyId, ServerPlayer promoter, ServerPlayer target) {
        if (promoter == null || target == null) return false;
        Party party = getParty(partyId);
        if (party == null) {
            msg(promoter, PREFIX + "§cThat party no longer exists.");
            return false;
        }
        if (!party.canPromote(promoter.getUUID(), target.getUUID())) {
            msg(promoter, PREFIX + "§cYou cannot promote that player.");
            return false;
        }

        PartyRank newRank = nextRankUp(party.getRank(target.getUUID()));
        if (newRank == null) {
            msg(promoter, PREFIX + "§cThat player cannot be promoted further.");
            return false;
        }
        party.setRank(target.getUUID(), newRank);
        broadcast(party, promoter.getServer(), PREFIX + "§e" + target.getName().getString()
                + " has been promoted to " + displayRank(newRank) + ".");
        return true;
    }

    public boolean demotePlayer(UUID partyId, ServerPlayer demoter, ServerPlayer target) {
        if (demoter == null || target == null) return false;
        Party party = getParty(partyId);
        if (party == null) {
            msg(demoter, PREFIX + "§cThat party no longer exists.");
            return false;
        }
        PartyRank demoterRank = party.getRank(demoter.getUUID());
        PartyRank targetRank = party.getRank(target.getUUID());
        if (demoterRank == null || targetRank == null) {
            msg(demoter, PREFIX + "§cThat player is not in your party.");
            return false;
        }
        if (targetRank == PartyRank.LEADER) {
            msg(demoter, PREFIX + "§cYou cannot demote the leader.");
            return false;
        }
        if (!demoterRank.outranks(targetRank)) {
            msg(demoter, PREFIX + "§cYou cannot demote that player.");
            return false;
        }
        PartyRank newRank = nextRankDown(targetRank);
        if (newRank == null) {
            msg(demoter, PREFIX + "§cThat player cannot be demoted further.");
            return false;
        }
        party.setRank(target.getUUID(), newRank);
        broadcast(party, demoter.getServer(), PREFIX + "§e" + target.getName().getString()
                + " has been demoted to " + displayRank(newRank) + ".");
        return true;
    }

    public boolean transferLeadership(ServerPlayer leader, ServerPlayer target) {
        if (leader == null || target == null) return false;
        Party party = getPartyOf(leader.getUUID());
        if (party == null) {
            msg(leader, PREFIX + "§cYou are not in a party.");
            return false;
        }
        if (!party.hasRank(leader.getUUID(), PartyRank.LEADER)) {
            msg(leader, PREFIX + "§cOnly the leader can transfer leadership.");
            return false;
        }
        if (!party.isMember(target.getUUID())) {
            msg(leader, PREFIX + "§cThat player is not in your party.");
            return false;
        }
        if (leader.getUUID().equals(target.getUUID())) {
            msg(leader, PREFIX + "§cYou are already the leader.");
            return false;
        }

        party.transferLeadership(target.getUUID());
        // A deliberate transfer ends any displacement state for the old leader.
        displacedLeaders.remove(leader.getUUID());
        leaderReclaimWindows.remove(leader.getUUID());
        broadcast(party, leader.getServer(), PREFIX + "§e" + target.getName().getString()
                + " is now the party leader.");
        return true;
    }

    public boolean setFriendlyFire(ServerPlayer actor, boolean enabled) {
        if (actor == null) return false;
        Party party = getPartyOf(actor.getUUID());
        if (party == null) {
            msg(actor, PREFIX + "§cYou are not in a party.");
            return false;
        }
        PartyRank rank = party.getRank(actor.getUUID());
        if (rank == PartyRank.MEMBER) {
            msg(actor, PREFIX + "§cOnly officers and the leader may change friendly fire.");
            return false;
        }
        party.setFriendlyFireEnabled(enabled);
        broadcast(party, actor.getServer(), PREFIX + "§eFriendly fire is now "
                + (enabled ? "§cON" : "§aOFF") + "§e.");
        return true;
    }

    /**
     * Lets a displaced original leader reclaim leadership within the reclaim
     * window opened when they logged back in.
     */
    public boolean claimLeadership(ServerPlayer player) {
        if (player == null) return false;
        Party party = getPartyOf(player.getUUID());
        if (party == null) {
            msg(player, PREFIX + "§cYou are not in a party.");
            return false;
        }
        if (party.hasRank(player.getUUID(), PartyRank.LEADER)) {
            msg(player, PREFIX + "§cYou are already the leader.");
            return false;
        }
        Long windowStart = leaderReclaimWindows.get(player.getUUID());
        if (windowStart == null || System.currentTimeMillis() - windowStart > RECLAIM_WINDOW_MS) {
            msg(player, PREFIX + "§cYou have no active leadership claim. Ask the current leader to /party transfer.");
            return false;
        }

        party.transferLeadership(player.getUUID());
        displacedLeaders.remove(player.getUUID());
        leaderReclaimWindows.remove(player.getUUID());
        broadcast(party, player.getServer(), PREFIX + "§e" + player.getName().getString()
                + " has reclaimed party leadership.");
        return true;
    }

    // ------------------------------------------------------------------
    // Login / logout
    // ------------------------------------------------------------------

    /**
     * On leader logout, hand the seat to the highest-ranking <em>online</em>
     * member. If none are online the party stays intact with the (offline)
     * leader demoted to {@code FIRST_IN_COMMAND} and recorded as displaced so it
     * may be reclaimed on relogin.
     */
    public void handlePlayerLogout(ServerPlayer player) {
        if (player == null) return;
        cacheName(player);
        Party party = getPartyOf(player.getUUID());
        if (party == null) return;
        if (!party.hasRank(player.getUUID(), PartyRank.LEADER)) return;
        if (party.size() <= 1) return;

        MinecraftServer server = player.getServer();
        UUID successor = findSuccessor(party, player.getUUID(), true, server);
        if (successor == null) {
            // No online members — leave the party intact under the offline leader.
            return;
        }

        party.transferLeadership(successor);
        displacedLeaders.put(player.getUUID(), party.getPartyId());
        broadcast(party, server, PREFIX + "§e" + nameOf(player.getUUID(), server)
                + " has gone offline. " + nameOf(successor, server) + " is now acting leader.");
    }

    /**
     * On login, if the player is a displaced former leader of a still-active
     * party, open their reclaim window. Leadership is never auto-restored — they
     * must use {@code /party leader claim}.
     */
    public void handlePlayerLogin(ServerPlayer player) {
        if (player == null) return;
        cacheName(player);
        UUID partyId = displacedLeaders.get(player.getUUID());
        if (partyId == null) return;
        Party party = getParty(partyId);
        if (party == null || !party.isMember(player.getUUID())) {
            displacedLeaders.remove(player.getUUID());
            return;
        }
        leaderReclaimWindows.put(player.getUUID(), System.currentTimeMillis());
        msg(player, PREFIX + "§eYou were the leader of '" + party.getPartyName()
                + "'. Use /party leader claim within 5 minutes to reclaim leadership.");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Finds the successor leader in priority order: FIRST, SECOND, THIRD, then
     * the oldest MEMBER. {@code exclude} is skipped (the departing leader), and
     * when {@code onlineOnly} is set, offline candidates are skipped.
     */
    @Nullable
    private UUID findSuccessor(Party party, @Nullable UUID exclude, boolean onlineOnly,
                               @Nullable MinecraftServer server) {
        PartyRank[] order = {
                PartyRank.FIRST_IN_COMMAND, PartyRank.SECOND_IN_COMMAND,
                PartyRank.THIRD_IN_COMMAND, PartyRank.MEMBER
        };
        for (PartyRank rank : order) {
            for (UUID candidate : party.getByRank(rank)) {
                if (exclude != null && candidate.equals(exclude)) continue;
                if (onlineOnly && (server == null || server.getPlayerList().getPlayer(candidate) == null)) continue;
                return candidate;
            }
        }
        return null;
    }

    private void removeParty(Party party) {
        parties.remove(party.getPartyId());
        for (UUID member : party.getMembers().keySet()) {
            playerToParty.remove(member);
            displacedLeaders.remove(member);
            leaderReclaimWindows.remove(member);
        }
    }

    private void clearInvite(UUID target) {
        pendingInvites.remove(target);
        inviteTimestamps.remove(target);
        inviteInviters.remove(target);
    }

    @Nullable
    private static PartyRank nextRankUp(@Nullable PartyRank rank) {
        if (rank == null) return null;
        return switch (rank) {
            case MEMBER -> PartyRank.THIRD_IN_COMMAND;
            case THIRD_IN_COMMAND -> PartyRank.SECOND_IN_COMMAND;
            case SECOND_IN_COMMAND -> PartyRank.FIRST_IN_COMMAND;
            default -> null; // FIRST_IN_COMMAND / LEADER cannot be promoted here
        };
    }

    @Nullable
    private static PartyRank nextRankDown(@Nullable PartyRank rank) {
        if (rank == null) return null;
        return switch (rank) {
            case FIRST_IN_COMMAND -> PartyRank.SECOND_IN_COMMAND;
            case SECOND_IN_COMMAND -> PartyRank.THIRD_IN_COMMAND;
            case THIRD_IN_COMMAND -> PartyRank.MEMBER;
            default -> null; // MEMBER / LEADER cannot be demoted here
        };
    }

    public static String displayRank(PartyRank rank) {
        return switch (rank) {
            case LEADER -> "Leader";
            case FIRST_IN_COMMAND -> "First in Command";
            case SECOND_IN_COMMAND -> "Second in Command";
            case THIRD_IN_COMMAND -> "Third in Command";
            case MEMBER -> "Member";
        };
    }

    public void cacheName(ServerPlayer player) {
        if (player != null) {
            knownNames.put(player.getUUID(), player.getName().getString());
        }
    }

    /** Resolves a display name, preferring the live player, then the cache, then a short UUID. */
    public String nameOf(UUID id, @Nullable MinecraftServer server) {
        if (id == null) return "Unknown";
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) return player.getName().getString();
        }
        String cached = knownNames.get(id);
        return cached != null ? cached : id.toString().substring(0, 8);
    }

    private void broadcast(Party party, @Nullable MinecraftServer server, String message) {
        if (party == null || server == null) return;
        Component component = Component.literal(message);
        for (UUID member : party.getMembers().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                player.sendSystemMessage(component);
            }
        }
    }

    private static void msg(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
}
