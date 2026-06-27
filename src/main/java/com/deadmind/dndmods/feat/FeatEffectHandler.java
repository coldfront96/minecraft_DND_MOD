package com.deadmind.dndmods.feat;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side event handler for general feats whose effects need an active
 * tick loop or event hook beyond simple stored flags. Feats are identified by
 * the unlock flags set in {@link DnDPlayerData} by the feat registrations in
 * {@code CoreFeats}.
 *
 * <p>Attribute-based feats (Run, Athletic) are reconciled every tick: the
 * modifier is added when the feat flag is set and removed when it is cleared,
 * so granting or revoking a feat takes effect on the next tick and the
 * modifiers survive respawns and relogs without ever stacking. Transient
 * modifiers are used so nothing is written to the entity's persistent NBT.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class FeatEffectHandler {

    private static final ResourceLocation RUN_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "run_feat_speed");
    private static final ResourceLocation ATHLETIC_JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "athletic_feat_jump");
    private static final ResourceLocation TOUGHNESS_HP_ID =
            ResourceLocation.fromNamespaceAndPath(DnDMods.MOD_ID, "toughness_feat_hp");

    /** Toughness: +3 max HP per stack, applied to the vanilla health bar. */
    private static final double TOUGHNESS_HP_PER_STACK = 3.0;

    /** Run: move at 5x speed instead of 4x — modelled as +25% movement speed. */
    private static final double RUN_SPEED_BONUS = 0.25;
    /** Athletic: +10% jump height (flavor translation of the +2 Jump bonus). */
    private static final double ATHLETIC_JUMP_BONUS = 0.10;

    /** Acrobatic: negate the first 4 blocks of every fall. */
    private static final float ACROBATIC_FALL_NEGATE = 4.0f;

    /**
     * Self-Sufficient: ~25% faster natural regeneration. Vanilla heals about
     * 1 HP per 80 ticks while the hunger bar is high, so granting one extra HP
     * every 320 ticks under the same conditions approximates a 25% boost.
     */
    private static final int SELF_SUFFICIENT_INTERVAL = 320;
    private static final int REGEN_FOOD_THRESHOLD = 18;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;

        reconcileAttribute(player, Attributes.MOVEMENT_SPEED, RUN_SPEED_ID,
                RUN_SPEED_BONUS, data.getAchievementFlag("run_unlocked"));

        // JUMP_STRENGTH is a generic attribute in 1.21 but is not guaranteed to
        // be present on the player's attribute map; reconcileAttribute skips
        // gracefully (null instance) if it is absent, leaving the feat as a
        // flag-only effect to be revisited if the attribute becomes available.
        reconcileAttribute(player, Attributes.JUMP_STRENGTH, ATHLETIC_JUMP_ID,
                ATHLETIC_JUMP_BONUS, data.getAchievementFlag("athletic_unlocked"));

        reconcileToughness(player, data);

        handleSelfSufficient(player, data);
    }

    /**
     * Applies +3 max HP per Toughness stack to the player's vanilla MAX_HEALTH
     * attribute. A single ADD_VALUE modifier carries the full stacked amount
     * (count x 3) — functionally identical to many uniquely keyed +3 modifiers
     * but simpler to reconcile and remove. When the bonus grows the player is
     * healed by the delta so the new hit points are immediately usable.
     */
    private static void reconcileToughness(ServerPlayer player, DnDPlayerData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;

        double desired = data.getToughnessFeatCount() * TOUGHNESS_HP_PER_STACK;
        AttributeModifier existing = instance.getModifier(TOUGHNESS_HP_ID);
        double current = existing != null ? existing.amount() : 0.0;

        if (desired == current) return;

        if (existing != null) {
            instance.removeModifier(TOUGHNESS_HP_ID);
        }
        if (desired > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    TOUGHNESS_HP_ID, desired, AttributeModifier.Operation.ADD_VALUE));
        }
        if (desired > current) {
            player.heal((float) (desired - current));
        }
    }

    private static void reconcileAttribute(ServerPlayer player, Holder<Attribute> attribute,
                                           ResourceLocation id, double bonus, boolean shouldHave) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        boolean has = instance.getModifier(id) != null;
        if (shouldHave && !has) {
            instance.addTransientModifier(new AttributeModifier(
                    id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!shouldHave && has) {
            instance.removeModifier(id);
        }
    }

    private static void handleSelfSufficient(ServerPlayer player, DnDPlayerData data) {
        if (!data.getAchievementFlag("self_sufficient_unlocked")) return;
        if (player.getHealth() >= player.getMaxHealth()) return;
        if (player.getFoodData().getFoodLevel() < REGEN_FOOD_THRESHOLD) return;

        if (player.tickCount % SELF_SUFFICIENT_INTERVAL == 0) {
            player.heal(1.0f);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return;
        if (!data.getAchievementFlag("acrobatic_unlocked")) return;

        float reduced = event.getDistance() - ACROBATIC_FALL_NEGATE;
        if (reduced <= 0.0f) {
            event.setCanceled(true);
        } else {
            event.setDistance(reduced);
        }
    }
}
