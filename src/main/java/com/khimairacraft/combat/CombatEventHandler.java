package com.khimairacraft.combat;

import com.khimairacraft.DnDMods;
import com.khimairacraft.ability.AbilityCooldownManager;
import com.khimairacraft.ability.passive.BarbarianPassives;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.network.SyncPlayerDataPayload;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import com.khimairacraft.resource.RageSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class CombatEventHandler {

    private static int resourceRegenCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AbilityCooldownManager.tick();
        PerPlayerCombatState.tickAll();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PlayerDataHelper.get(player).tickCooldowns();
            RageSystem.tickPlayer(player);
        }

        resourceRegenCounter++;
        if (resourceRegenCounter >= 20) {
            resourceRegenCounter = 0;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                DnDPlayerData data = PlayerDataHelper.get(player);
                if (data.getDnDClass() == DnDClass.NONE) continue;

                // Barbarian fast movement passive (reapply every second)
                if (data.getClassLevel(DnDClass.BARBARIAN) >= 1) {
                    BarbarianPassives.applyFastMovement(player, data);
                }

                int regenAmount = getResourceRegenRate(data.getDnDClass());
                if (regenAmount > 0 && data.getCurrentResource() < data.getMaxResource()) {
                    data.restoreResource(regenAmount);
                    PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
                }
            }
        }

        // Throttle the Nature's Grasp web cleanup scan: a 20-block entity
        // search every tick for every player is a needless TPS drain. Once
        // every 10 ticks (0.5s) is more than precise enough for web expiry.
        if (event.getServer().getTickCount() % 10 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(20.0))) {
                    if (entity instanceof LivingEntity living) {
                        CompoundTag tags = living.getPersistentData();
                        if (tags.contains("khimairacraft_web_remove_at")) {
                            long removeAt = tags.getLong("khimairacraft_web_remove_at");
                            if (living.level().getGameTime() >= removeAt) {
                                long posLong = tags.getLong("khimairacraft_web_pos");
                                BlockPos pos = BlockPos.of(posLong);
                                if (living.level().getBlockState(pos).is(Blocks.COBWEB)) {
                                    living.level().removeBlock(pos, false);
                                }
                                tags.remove("khimairacraft_web_remove_at");
                                tags.remove("khimairacraft_web_pos");
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(player.getUUID());

        if (state.consumeNextAttack()) {
            event.setCanceled(true);
        }
    }

    /**
     * Runs at HIGHEST priority so a raging player's rage is ended — and the
     * rage cooldown stamped — at the moment of death, before any other death
     * processing (and before respawn). fromDeath=true skips fatigue per RAW.
     * Also the Bloodlust Surge trigger: killing blow by a raging Barbarian.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dying
                && RageSystem.isRaging(dying.getUUID())) {
            RageSystem.endRage(dying, true);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer
                && killer != event.getEntity()) {
            DnDPlayerData data = PlayerDataHelper.get(killer);
            if (data.getClassLevel(DnDClass.BARBARIAN) >= 12) {
                BarbarianPassives.onKillWhileRaging(killer, data);
                PacketDistributor.sendToPlayer(killer, SyncPlayerDataPayload.fromPlayer(data));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();

        if (event.getSource().is(ModDamageTypes.ABILITY_DAMAGE)) {
            return;
        }

        // Evasion: 50% dodge chance when active
        if (target instanceof ServerPlayer defender) {
            CompoundTag tags = defender.getPersistentData();
            if (tags.contains("khimairacraft_evasion_until")) {
                long evasionUntil = tags.getLong("khimairacraft_evasion_until");
                if (defender.level().getGameTime() <= evasionUntil) {
                    if (defender.getRandom().nextFloat() < 0.5f) {
                        event.setNewDamage(0.0f);
                        return;
                    }
                }
            }
        }

        // Reckless: +20% incoming damage when active (via PerPlayerCombatState)
        if (target instanceof ServerPlayer defender) {
            PerPlayerCombatState.CombatState defState = PerPlayerCombatState.get(defender.getUUID());
            if (defState.isRecklessActive()) {
                event.setNewDamage(event.getNewDamage() * 1.2f);
            }

            // Barbarian passive damage reduction (level 7+, active at all times)
            DnDPlayerData defData = PlayerDataHelper.get(defender);
            int dr = BarbarianPassives.getDamageReduction(defData);
            if (dr > 0) {
                event.setNewDamage(Math.max(1.0f, event.getNewDamage() - dr));
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(attacker.getUUID());

            // Hunter's Mark: +25% damage to marked target
            UUID markTarget = state.getHuntersMarkTarget();
            if (markTarget != null && target.getUUID().equals(markTarget)) {
                event.setNewDamage(event.getNewDamage() * 1.25f);
                attacker.displayClientMessage(
                        Component.literal("Marked!").withStyle(s -> s.withColor(0xFF4444)), true);
            }

            // Pending enhancement from ENHANCES_ATTACK abilities
            String enhancement = state.consumePendingEnhancement();
            if (enhancement != null) {
                switch (enhancement) {
                    case "fighter_power_strike" ->
                            event.setNewDamage(event.getNewDamage() * 1.5f);
                    case "fighter_shield_bash" ->
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
                    case "barbarian_reckless_attack" -> {
                        DnDPlayerData atkData2 = PlayerDataHelper.get(attacker);
                        int strBonus = atkData2.getAbilityScores().getStrMod() * 2;
                        event.setNewDamage(event.getNewDamage() + strBonus);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Rage/fatigue score changes must be reverted before the player data
        // saves, so handleLogout runs with the live player, not just the UUID.
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RageSystem.handleLogout(serverPlayer);
        }
        UUID uuid = event.getEntity().getUUID();
        PerPlayerCombatState.remove(uuid);
        AbilityCooldownManager.removePlayer(uuid);
    }

    private static int getResourceRegenRate(DnDClass dndClass) {
        return switch (dndClass) {
            case FIGHTER -> 3;
            case ROGUE -> 4;
            case WIZARD -> 2;
            case CLERIC -> 3;
            case RANGER -> 4;
            case BARBARIAN -> 1;
            case NONE -> 0;
        };
    }
}
