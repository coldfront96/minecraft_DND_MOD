package com.khimairacraft.combat;

import com.khimairacraft.DnDMods;
import com.khimairacraft.ability.passive.RangerPassives;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * All passive Ranger combat behavior that needs an event hook: Favored Enemy
 * damage, Wild Empathy taming, Combat Style flag reconciliation, Swift Tracker
 * glow, Camouflage / Hide in Plain Sight detection reduction, and Woodland
 * Stride. Ranger has no resource pool — everything here is trigger- or
 * flag-based, mirroring {@link RogueCombatHandler}.
 *
 * <p><b>Swift Tracker color-coding limitation.</b> Vanilla's Glowing color
 * follows a scoreboard team, and team membership is global — a per-viewer,
 * per-player colored glow is not achievable with vanilla mechanics without
 * custom entity-tracking packets. The closest approximation used here assigns
 * nearby <em>non-player</em> entities (mobs and pets) to shared colored glow
 * teams (green = friendly, red = hostile) and applies the Glowing effect. This
 * means: (1) the glow is visible to every player, not only the Ranger, and
 * (2) hostile players are given the Glowing effect but not recolored (recoloring
 * would hijack their real team / nametag). These limitations are inherent to
 * the vanilla team-based glow system.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class RangerCombatHandler {

    private static final String TEAM_FRIENDLY = "khc_ranger_glow_friend";
    private static final String TEAM_HOSTILE = "khc_ranger_glow_foe";

    private static final double SWIFT_TRACKER_RANGE = 20.0;
    private static final int GLOW_DURATION_TICKS = 50; // > 40-tick refresh

    // Scoreboard names of non-player entities we've assigned to a glow team,
    // so we can remove stale ones each scan and keep team membership tidy.
    private static final Set<String> GLOW_MANAGED = new HashSet<>();

    // ------------------------------------------------------------------
    // Server tick: reconcile, combat-style flags, swift tracker, camouflage
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int tickCount = event.getServer().getTickCount();

        if (tickCount % 20 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getClassLevel(DnDClass.RANGER) <= 0) continue;
                RangerPassives.reconcile(data);
                RangerPassives.applyCombatStyleFlags(player, data);
                // Camouflage / Hide in Plain Sight: thin hostile detection.
                applyCamouflage(player, data);
            }
        }

        if (tickCount % 40 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getClassLevel(DnDClass.RANGER) >= 8
                        && data.getAchievementFlag("ranger_swift_tracker")) {
                    applySwiftTracker(player, data);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Woodland Stride (level 7): per-tick terrain slowdown negation
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DnDPlayerData data = PlayerDataHelper.get(player);
        if (!data.getAchievementFlag("ranger_woodland_stride")) return;
        if (!isInSlowingTerrain(player)) return;

        // Counteract the block's movement penalty with a short, invisible speed
        // boost so natural vines / berry bushes / soul sand don't slow the Ranger.
        // (Cobweb's stuck-multiplier is applied and cleared inside the move step
        // before this Post tick, so the boost only partially offsets it — the
        // closest negation achievable without a movement mixin.)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 2, false, false, false));
    }

    private static boolean isInSlowingTerrain(ServerPlayer player) {
        Level level = player.level();
        // Check the block at the feet and the block the eyes/body occupy.
        BlockState feet = level.getBlockState(player.blockPosition());
        BlockState body = level.getBlockState(player.blockPosition().above());
        return isSlowingBlock(feet) || isSlowingBlock(body);
    }

    private static boolean isSlowingBlock(BlockState state) {
        return state.is(Blocks.COBWEB)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.HONEY_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.VINE)
                || state.is(Blocks.CAVE_VINES)
                || state.is(Blocks.CAVE_VINES_PLANT)
                || state.is(Blocks.WEEPING_VINES)
                || state.is(Blocks.WEEPING_VINES_PLANT)
                || state.is(Blocks.TWISTING_VINES)
                || state.is(Blocks.TWISTING_VINES_PLANT);
    }

    // ------------------------------------------------------------------
    // Favored Enemy damage + Woodland Stride berry-bush damage negation
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();

        // Woodland Stride: a Ranger takes no damage from sweet berry bushes.
        if (target instanceof ServerPlayer victim) {
            DnDPlayerData victimData = PlayerDataHelper.get(victim);
            if (victimData.getAchievementFlag("ranger_woodland_stride")
                    && event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) {
                event.setNewDamage(0.0f);
                return;
            }
        }

        // Favored Enemy: a Ranger adds the highest matching bonus as flat damage.
        // No damage-type filter, so this covers both normal weapon strikes and
        // ability damage sources originating from the Ranger.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target) {
            DnDPlayerData atkData = PlayerDataHelper.get(attacker);
            if (atkData.getClassLevel(DnDClass.RANGER) <= 0) return;
            int bonus = RangerPassives.getFavoredEnemyBonus(atkData, target);
            if (bonus > 0) {
                event.setNewDamage(event.getNewDamage() + bonus);
                attacker.displayClientMessage(
                        Component.literal("Favored Enemy! +" + bonus)
                                .withStyle(s -> s.withColor(0x55FF55)), true);
            }
        }
    }

    // ------------------------------------------------------------------
    // Wild Empathy (level 1): additive taming success chance
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof TamableAnimal animal)) return;
        if (animal.isTame()) return;

        DnDPlayerData data = PlayerDataHelper.get(player);
        if (data.getClassLevel(DnDClass.RANGER) <= 0) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;
        // Only trigger on the animal's taming food (bones for wolves, the
        // species' food otherwise) so ordinary right-clicks don't tame.
        boolean isTamingItem = held.is(Items.BONE) || animal.isFood(held);
        if (!isTamingItem) return;

        double bonus = RangerPassives.getWildEmpathyBonus(data);
        if (bonus <= 0.0) return;

        // Roll the Wild Empathy bonus as an independent extra success chance on
        // this interaction. Vanilla taming rolls its own hidden probability, so
        // this additive roll effectively raises the per-attempt success rate.
        if (ThreadLocalRandom.current().nextDouble() < bonus) {
            animal.tame(player);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            animal.level().broadcastEntityEvent(animal, (byte) 7); // taming hearts
            player.swing(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    // ------------------------------------------------------------------
    // Swift Tracker (level 8): 20-block color-coded glow
    // ------------------------------------------------------------------

    private static void applySwiftTracker(ServerPlayer ranger, DnDPlayerData data) {
        if (!(ranger.level() instanceof ServerLevel level)) return;
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam friendly = ensureTeam(scoreboard, TEAM_FRIENDLY, ChatFormatting.GREEN);
        PlayerTeam hostile = ensureTeam(scoreboard, TEAM_HOSTILE, ChatFormatting.RED);

        Set<String> stillManaged = new HashSet<>();

        for (Entity entity : level.getEntities(ranger,
                ranger.getBoundingBox().inflate(SWIFT_TRACKER_RANGE))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == ranger) continue;
            if (ranger.distanceTo(living) > SWIFT_TRACKER_RANGE) continue;

            boolean friend = isFriendlyToRanger(ranger, living);
            boolean foe = !friend && isHostileToRanger(ranger, living);
            if (!friend && !foe) continue;

            living.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    GLOW_DURATION_TICKS, 0, false, false, false));

            // Recolor only non-player entities (mobs/pets); recoloring a player
            // would hijack their real team assignment.
            if (!(living instanceof ServerPlayer)) {
                PlayerTeam target = friend ? friendly : hostile;
                String name = living.getScoreboardName();
                if (scoreboard.getPlayersTeam(name) != target) {
                    scoreboard.addPlayerToTeam(name, target);
                }
                stillManaged.add(name);
            }
        }

        // Drop entities that left range from the glow teams.
        for (String name : new HashSet<>(GLOW_MANAGED)) {
            if (stillManaged.contains(name)) continue;
            PlayerTeam current = scoreboard.getPlayersTeam(name);
            if (current == friendly || current == hostile) {
                scoreboard.removePlayerFromTeam(name, current);
            }
        }
        GLOW_MANAGED.clear();
        GLOW_MANAGED.addAll(stillManaged);
    }

    private static PlayerTeam ensureTeam(Scoreboard scoreboard, String name, ChatFormatting color) {
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
            team.setColor(color);
            team.setSeeFriendlyInvisibles(false);
        }
        return team;
    }

    private static boolean isFriendlyToRanger(ServerPlayer ranger, LivingEntity entity) {
        // Party / guild members and the Ranger's own tamed pets.
        return FriendlyFireChecker.isFriendly(ranger, entity);
    }

    private static boolean isHostileToRanger(ServerPlayer ranger, LivingEntity entity) {
        if (entity instanceof Enemy) return true; // Monster-type mobs
        if (entity instanceof ServerPlayer other) {
            return !FriendlyFireChecker.isFriendly(ranger, other);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Camouflage (13) / Hide in Plain Sight (17): thin hostile detection
    // ------------------------------------------------------------------

    private static void applyCamouflage(ServerPlayer ranger, DnDPlayerData data) {
        float reduction = RangerPassives.getDetectionReduction(data);
        if (reduction <= 0.0f) return;
        // "Natural biome" is simplified to the Overworld — Nether/End are excluded
        // and no manmade-structure exclusion is attempted (documented simplification).
        if (ranger.level().dimension() != Level.OVERWORLD) return;
        if (!(ranger.level() instanceof ServerLevel level)) return;

        boolean evenWhileObserved = data.getAchievementFlag("ranger_hide_in_plain_sight");

        for (Entity entity : level.getEntities(ranger, ranger.getBoundingBox().inflate(32.0))) {
            if (!(entity instanceof Mob mob)) continue;
            if (mob.getTarget() != ranger) continue;

            double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
            double allowed = followRange * (1.0 - reduction);
            if (ranger.distanceTo(mob) <= allowed) continue;

            // Camouflage only breaks a not-yet-visual lock; Hide in Plain Sight
            // breaks it even while the mob has line of sight.
            if (!evenWhileObserved && mob.hasLineOfSight(ranger)) continue;

            mob.setTarget(null);
        }
    }

    private RangerCombatHandler() {}
}
