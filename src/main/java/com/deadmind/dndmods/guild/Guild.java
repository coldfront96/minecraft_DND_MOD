package com.deadmind.dndmods.guild;

import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A persistent guild: a social organization that owns claimed land, survives
 * server restarts (via {@link GuildSavedData}), and may span multiple parties.
 *
 * <p>The chunk-claim cap scales with membership (see {@link #getMaxChunks()}).
 * The {@code guildHallChunk} is the chunk a {@code /guild hall} teleport targets
 * — modelled as a {@link ChunkPos} (the source material's "UUID" typing is a
 * documentation slip; a hall is a place, not an id).
 */
public class Guild {

    public static final int MAX_NAME_LENGTH = 24;
    public static final int MAX_TAG_LENGTH = 5;

    private final UUID guildId;
    private String guildName;
    private String guildTag;
    private final Map<UUID, GuildRank> members = new LinkedHashMap<>();
    private final Set<ChunkPos> claimedChunks = new HashSet<>();
    private final Map<ChunkPos, GuildZoneType> chunkZones = new HashMap<>();
    private boolean friendlyFireEnabled = false;
    @Nullable
    private ChunkPos guildHallChunk = null;
    private long createdAt;
    private int guildLevel = 1;

    public Guild(UUID guildId, String guildName, String guildTag) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.guildTag = guildTag;
        this.createdAt = System.currentTimeMillis();
    }

    // --- Identity ---

    public UUID getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public void setGuildName(String name) { this.guildName = name; }
    public String getGuildTag() { return guildTag; }
    public void setGuildTag(String tag) { this.guildTag = tag; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getGuildLevel() { return guildLevel; }
    public void setGuildLevel(int level) { this.guildLevel = level; }

    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public void setFriendlyFireEnabled(boolean enabled) { this.friendlyFireEnabled = enabled; }

    @Nullable
    public ChunkPos getGuildHallChunk() { return guildHallChunk; }
    public void setGuildHallChunk(@Nullable ChunkPos chunk) { this.guildHallChunk = chunk; }

    // --- Members ---

    public Map<UUID, GuildRank> getMembers() { return members; }
    public int size() { return members.size(); }

    public void addMember(UUID playerId, GuildRank rank) { members.put(playerId, rank); }
    public void removeMember(UUID playerId) { members.remove(playerId); }
    public void setRank(UUID playerId, GuildRank rank) {
        if (members.containsKey(playerId)) members.put(playerId, rank);
    }

    @Nullable
    public UUID getGuildMaster() {
        for (Map.Entry<UUID, GuildRank> entry : members.entrySet()) {
            if (entry.getValue() == GuildRank.GUILD_MASTER) return entry.getKey();
        }
        return null;
    }

    public List<UUID> getByRank(GuildRank rank) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, GuildRank> entry : members.entrySet()) {
            if (entry.getValue() == rank) result.add(entry.getKey());
        }
        return result;
    }

    @Nullable
    public GuildRank getRank(UUID playerId) { return members.get(playerId); }
    public boolean isMember(UUID playerId) { return members.containsKey(playerId); }

    public boolean canKick(UUID kicker, UUID target) {
        if (kicker == null || target == null || kicker.equals(target)) return false;
        if (!isMember(kicker) || !isMember(target)) return false;
        return getRank(kicker).outranks(getRank(target));
    }

    /**
     * A promoter may raise a {@code MEMBER} they outrank to {@code OFFICER}.
     * Promotion never reaches {@code GUILD_MASTER} — use {@link #transferMastership}.
     */
    public boolean canPromote(UUID promoter, UUID target) {
        if (promoter == null || target == null || promoter.equals(target)) return false;
        if (!isMember(promoter) || !isMember(target)) return false;
        GuildRank targetRank = getRank(target);
        if (targetRank != GuildRank.MEMBER) return false;
        return getRank(promoter).outranks(targetRank);
    }

    public void transferMastership(UUID newMaster) {
        if (!isMember(newMaster)) return;
        UUID oldMaster = getGuildMaster();
        if (oldMaster != null && !oldMaster.equals(newMaster)) {
            members.put(oldMaster, GuildRank.OFFICER);
        }
        members.put(newMaster, GuildRank.GUILD_MASTER);
    }

    // --- Claims / zones ---

    public Set<ChunkPos> getClaimedChunks() { return claimedChunks; }
    public Map<ChunkPos, GuildZoneType> getChunkZones() { return chunkZones; }

    /** Maximum claimable chunks, scaling with current membership. */
    public int getMaxChunks() {
        int n = members.size();
        if (n <= 5) return 10;
        if (n <= 10) return 20;
        if (n <= 20) return 40;
        return 60;
    }

    public boolean isChunkClaimed(ChunkPos pos) { return claimedChunks.contains(pos); }
    public boolean canClaimMore() { return claimedChunks.size() < getMaxChunks(); }

    public void claimChunk(ChunkPos pos, GuildZoneType zone) {
        claimedChunks.add(pos);
        chunkZones.put(pos, zone != null ? zone : GuildZoneType.GENERAL);
    }

    public void unclaimChunk(ChunkPos pos) {
        claimedChunks.remove(pos);
        chunkZones.remove(pos);
        if (pos.equals(guildHallChunk)) guildHallChunk = null;
    }

    public GuildZoneType getZoneType(ChunkPos pos) {
        return chunkZones.getOrDefault(pos, GuildZoneType.GENERAL);
    }

    public void setZoneType(ChunkPos pos, GuildZoneType zone) {
        if (claimedChunks.contains(pos)) {
            chunkZones.put(pos, zone != null ? zone : GuildZoneType.GENERAL);
        }
    }
}
