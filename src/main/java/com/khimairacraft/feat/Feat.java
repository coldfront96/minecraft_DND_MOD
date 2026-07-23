package com.khimairacraft.feat;

import com.khimairacraft.playerdata.DnDPlayerData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class Feat {
    private final String featId;
    private final String displayName;
    private final String description;
    private final FeatCategory category;
    private final FeatSource source;
    @Nullable
    private final String chainGroup;
    private final int chainOrder;
    private final List<FeatPrerequisite> prerequisites;
    private final Consumer<DnDPlayerData> onGrant;
    private final Consumer<DnDPlayerData> onRevoke;
    /** True if a Fighter may select this feat with a Fighter bonus feat slot. */
    private boolean isFighterBonusFeat = false;
    /**
     * True if this feat may be taken more than once, each take adding a stack.
     * Repeatable feats track their times-taken count in
     * {@link DnDPlayerData#getFeatStackCount(String)} and keep appearing as
     * selectable after being granted. Their {@code onGrant} is re-run on every
     * take and must be additive (it increments the stack / applies per-stack
     * effect). Defaults to false.
     */
    private boolean isRepeatable = false;

    public Feat(String featId, String displayName, String description,
                FeatCategory category, FeatSource source,
                @Nullable String chainGroup, int chainOrder,
                List<FeatPrerequisite> prerequisites,
                Consumer<DnDPlayerData> onGrant, Consumer<DnDPlayerData> onRevoke) {
        this.featId = featId;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.source = source;
        this.chainGroup = chainGroup;
        this.chainOrder = chainOrder;
        this.prerequisites = prerequisites;
        this.onGrant = onGrant;
        this.onRevoke = onRevoke;
    }

    public String getFeatId() { return featId; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public FeatCategory getCategory() { return category; }
    public FeatSource getSource() { return source; }
    @Nullable
    public String getChainGroup() { return chainGroup; }
    public int getChainOrder() { return chainOrder; }
    public List<FeatPrerequisite> getPrerequisites() { return prerequisites; }

    public boolean isFighterBonusFeat() { return isFighterBonusFeat; }
    public void setFighterBonusFeat(boolean value) { this.isFighterBonusFeat = value; }

    public boolean isRepeatable() { return isRepeatable; }
    public void setRepeatable(boolean value) { this.isRepeatable = value; }

    public void applyGrant(DnDPlayerData data) {
        if (onGrant != null) onGrant.accept(data);
    }

    public void applyRevoke(DnDPlayerData data) {
        if (onRevoke != null) onRevoke.accept(data);
    }

    public boolean allPrerequisitesMet(DnDPlayerData data) {
        return FeatPrerequisite.allMet(prerequisites, data);
    }
}
