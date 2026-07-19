package com.deadmind.dndmods.combat;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.ability.passive.RoguePassives;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.party.FriendlyFireChecker;
import com.deadmind.dndmods.playerdata.AbilityScores;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All passive Rogue combat behavior: Sneak Attack, Evasion, Defensive Roll,
 * Opportunist, Crippling Strike STR damage, Slippery Mind rerolls, and
 * Trapfinding detection. Rogue has no resource pool — everything here is
 * trigger-based.
 */
@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class RogueCombatHandler {

    private static final Random RANDOM = new Random();
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    // Crippling Strike STR damage: victim UUID -> {remaining points, next regen epoch millis}.
    // In-memory with logout restore, mirroring the Rage fatigue safety pattern.
    private static final Map<UUID, long[]> STR_DAMAGE = new ConcurrentHashMap<>();

    // Opportunist: rogues who already used their free attack this round (1s).
    private static final Set<UUID> OPPORTUNIST_USED = ConcurrentHashMap.newKeySet();

    // Slippery Mind: pending Will-save rerolls, player UUID -> {dc, due epoch millis}.
    private static final Map<UUID, long[]> PENDING_WILL_REROLLS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int tickCount = event.getServer().getTickCount();

        if (tickCount % 20 == 0) {
            OPPORTUNIST_USED.clear();
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getClassLevel(DnDClass.ROGUE) > 0) {
                    RoguePassives.reconcile(data);
                }
                regenStrDamage(player, data);
            }
        }

        if (tickCount % 40 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getClassLevel(DnDClass.ROGUE) >= 1) {
                    highlightNearbyTraps(player, data);
                }
            }
        }

        if (!PENDING_WILL_REROLLS.isEmpty()) {
            processSlipperyMindRerolls(event);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().is(ModDamageTypes.ABILITY_DAMAGE)) {
            return;
        }

        LivingEntity target = event.getEntity();

        // --- Defender-side: Evasion, then Defensive Roll on what remains ---
        if (target instanceof ServerPlayer defender) {
            DnDPlayerData defData = PlayerDataHelper.get(defender);

            if (defData.isRogueEvasionUnlocked()
                    && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                    && isWearingLightOrNoArmor(defender)) {
                boolean saved = SavingThrowSystem.rollSave(defender, SaveType.REFLEX, 15, null);
                if (saved) {
                    event.setNewDamage(0.0f);
                    defender.displayClientMessage(
                            Component.literal("Evasion!").withStyle(s -> s.withColor(0x55FF55)), true);
                    return;
                } else if (defData.getAchievementFlag("improved_evasion_rogue_unlocked")) {
                    // Improved Evasion: half damage even on a failed save.
                    event.setNewDamage(event.getNewDamage() * 0.5f);
                }
            }

            if (defData.getAchievementFlag("defensive_roll_unlocked")
                    && event.getNewDamage() >= defender.getHealth()
                    && (defData.getDefensiveRollLastUsed() < 0
                        || System.currentTimeMillis() - defData.getDefensiveRollLastUsed() >= DAY_MILLIS)) {
                int dc = (int) event.getNewDamage();
                defData.setDefensiveRollLastUsed(System.currentTimeMillis());
                boolean saved = SavingThrowSystem.rollSave(defender, SaveType.REFLEX, dc, null);
                if (saved) {
                    event.setNewDamage(event.getNewDamage() * 0.5f);
                    defender.displayClientMessage(
                            Component.literal("Defensive Roll!").withStyle(s -> s.withColor(0x55FF55)), true);
                }
            }
        }

        // --- Attacker-side: Sneak Attack ---
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target) {
            DnDPlayerData atkData = PlayerDataHelper.get(attacker);
            int dice = atkData.getRogueSneakAttackDice();
            if (dice > 0) {
                boolean ranged = event.getSource().getDirectEntity() != attacker;
                if (RoguePassives.qualifiesForSneakAttack(attacker, target, ranged)) {
                    // Rolled flat after vanilla crit scaling has already been
                    // applied to base damage — sneak dice are never multiplied.
                    int bonus = RoguePassives.rollSneakAttackBonus(dice);
                    event.setNewDamage(event.getNewDamage() + bonus);
                    attacker.displayClientMessage(
                            Component.literal("Sneak Attack! +" + bonus)
                                    .withStyle(s -> s.withColor(0xBB55FF)), true);

                    applyCripplingStrike(attacker, atkData, target);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        // Opportunist: when an ally damages a target in the Rogue's melee
        // range, the Rogue gets one free melee attack per round against it.
        if (!(event.getSource().getEntity() instanceof LivingEntity allyAttacker)) return;
        if (event.getSource().is(ModDamageTypes.ABILITY_DAMAGE)) return;
        // Only melee hits from the ally trigger it.
        if (event.getSource().getDirectEntity() != allyAttacker) return;

        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !(target.level() instanceof ServerLevel level)) return;

        for (ServerPlayer rogue : level.getServer().getPlayerList().getPlayers()) {
            if (rogue == allyAttacker || rogue == target) continue;
            if (rogue.level() != level) continue;
            if (OPPORTUNIST_USED.contains(rogue.getUUID())) continue;
            if (rogue.distanceTo(target) > 3.5f) continue;

            DnDPlayerData data = PlayerDataHelper.get(rogue);
            if (!data.getAchievementFlag("opportunist_unlocked")) continue;
            if (!FriendlyFireChecker.isFriendly(rogue, allyAttacker)) continue;
            if (FriendlyFireChecker.isFriendly(rogue, target)) continue;

            OPPORTUNIST_USED.add(rogue.getUUID());
            float damage = (float) rogue.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // A real damage event: the Pre hook evaluates Sneak Attack for it.
            target.hurt(rogue.damageSources().playerAttack(rogue), damage);
            rogue.displayClientMessage(
                    Component.literal("Opportunist!").withStyle(s -> s.withColor(0xBB55FF)), true);
            break;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Restore any outstanding Crippling Strike STR damage before the
            // victim's scores persist — same safety rule as Rage score bonuses.
            restoreAllStrDamage(player);
        }
        UUID uuid = event.getEntity().getUUID();
        OPPORTUNIST_USED.remove(uuid);
        PENDING_WILL_REROLLS.remove(uuid);
    }

    /** Called by SavingThrowSystem when a Slippery Mind Rogue fails a Will save. */
    public static void scheduleSlipperyMindReroll(ServerPlayer player, int dc) {
        PENDING_WILL_REROLLS.putIfAbsent(player.getUUID(),
                new long[]{dc, System.currentTimeMillis() + 1000L});
    }

    // ------------------------------------------------------------------
    // Crippling Strike
    // ------------------------------------------------------------------

    private static void applyCripplingStrike(ServerPlayer attacker, DnDPlayerData atkData, LivingEntity target) {
        int stacks = atkData.getRogueCripplingStrikeStacks();
        if (stacks <= 0) return;
        int strDamage = 2 * stacks;

        if (target instanceof ServerPlayer victim) {
            DnDPlayerData victimData = PlayerDataHelper.get(victim);
            AbilityScores scores = victimData.getAbilityScores();
            int before = scores.getStrength();
            scores.setStrength(before - strDamage);
            int applied = before - scores.getStrength();
            if (applied > 0) {
                STR_DAMAGE.merge(victim.getUUID(),
                        new long[]{applied, System.currentTimeMillis() + DAY_MILLIS},
                        (old, add) -> new long[]{old[0] + add[0], old[1]});
                victim.displayClientMessage(
                        Component.literal("Crippling Strike! -" + applied + " STR")
                                .withStyle(s -> s.withColor(0xFF5555)), true);
                PacketDistributor.sendToPlayer(victim, SyncPlayerDataPayload.fromPlayer(victimData));
            }
        } else {
            // Mobs have no STR score — Weakness approximates the reduction.
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200,
                    Math.min(2, stacks - 1), false, true));
        }
    }

    /** Regenerates 1 STR point per stored day-interval for crippled victims. */
    private static void regenStrDamage(ServerPlayer player, DnDPlayerData data) {
        long[] record = STR_DAMAGE.get(player.getUUID());
        if (record == null) return;
        long now = System.currentTimeMillis();
        boolean changed = false;
        while (record[0] > 0 && now >= record[1]) {
            AbilityScores scores = data.getAbilityScores();
            scores.setStrength(scores.getStrength() + 1);
            record[0]--;
            record[1] += DAY_MILLIS;
            changed = true;
        }
        if (record[0] <= 0) {
            STR_DAMAGE.remove(player.getUUID());
        }
        if (changed) {
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        }
    }

    private static void restoreAllStrDamage(ServerPlayer player) {
        long[] record = STR_DAMAGE.remove(player.getUUID());
        if (record == null || record[0] <= 0) return;
        DnDPlayerData data = PlayerDataHelper.get(player);
        AbilityScores scores = data.getAbilityScores();
        scores.setStrength(scores.getStrength() + (int) record[0]);
    }

    // ------------------------------------------------------------------
    // Slippery Mind
    // ------------------------------------------------------------------

    private static void processSlipperyMindRerolls(ServerTickEvent.Post event) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, long[]>> it = PENDING_WILL_REROLLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, long[]> entry = it.next();
            if (now < entry.getValue()[1]) continue;
            it.remove();

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;

            int dc = (int) entry.getValue()[0];
            DnDPlayerData data = PlayerDataHelper.get(player);
            // Rolled directly (not via rollSave) so a second failure can't
            // schedule another reroll — one attempt per failed save instance.
            int roll = RANDOM.nextInt(20) + 1;
            int total = roll + SavingThrowSystem.getPlayerSaveBonus(data, SaveType.WILL);
            if (total >= dc) {
                // Shake off the common enchantment-style debuffs.
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.CONFUSION);
                player.removeEffect(MobEffects.BLINDNESS);
                player.displayClientMessage(
                        Component.literal("Slippery Mind! You shake off the effect.")
                                .withStyle(s -> s.withColor(0x55FFFF)), true);
            }
        }
    }

    // ------------------------------------------------------------------
    // Trapfinding
    // ------------------------------------------------------------------

    private static void highlightNearbyTraps(ServerPlayer player, DnDPlayerData data) {
        if (!(player.level() instanceof ServerLevel level)) return;

        int radius = data.getAchievementFlag("practiced_professional_unlocked") ? 12 : 8;
        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!pos.closerThan(center, radius)) continue;
            BlockState state = level.getBlockState(pos);
            if (isTrapBlock(state)) {
                // Per-player particle send: only the Rogue sees the highlight.
                level.sendParticles(player, ParticleTypes.GLOW, true,
                        pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        3, 0.2, 0.15, 0.2, 0.0);
            }
        }
    }

    /**
     * Trap-tagged blocks: tripwires, tripwire hooks, and every pressure plate
     * variant. No custom mod trap blocks exist in ModBlocks to include yet.
     */
    private static boolean isTrapBlock(BlockState state) {
        return state.is(Blocks.TRIPWIRE)
                || state.is(Blocks.TRIPWIRE_HOOK)
                || state.getBlock() instanceof BasePressurePlateBlock;
    }

    // ------------------------------------------------------------------
    // Armor classification for Evasion
    // ------------------------------------------------------------------

    /**
     * Evasion needs light or no armor. Light = no chestplate, or a leather /
     * chainmail chestplate; anything heavier (iron/gold/diamond/netherite)
     * denies it.
     */
    private static boolean isWearingLightOrNoArmor(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return true;
        if (!(chest.getItem() instanceof ArmorItem)) return true;
        return chest.is(Items.LEATHER_CHESTPLATE) || chest.is(Items.CHAINMAIL_CHESTPLATE);
    }
}
