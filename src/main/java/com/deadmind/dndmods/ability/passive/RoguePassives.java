package com.deadmind.dndmods.ability.passive;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.party.FriendlyFireChecker;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class RoguePassives {

    private RoguePassives() {}

    /** 1d6 at Rogue 1, +1d6 every odd level: 10d6 at 19. */
    public static int getSneakAttackDiceForLevel(int rogueLevel) {
        if (rogueLevel <= 0) return 0;
        return Math.min(10, (rogueLevel + 1) / 2);
    }

    /** +1 at Rogue 3, +1 more at 6/9/12/15/18 (max +6). */
    public static int getTrapSenseBonusForLevel(int rogueLevel) {
        return Math.min(6, rogueLevel / 3);
    }

    /**
     * Reconciles all level-derived Rogue fields and flags from the current
     * Rogue class level. Called on level up and periodically from the tick
     * handler so existing characters self-heal without a migration.
     */
    public static void reconcile(DnDPlayerData data) {
        int level = data.getClassLevel(DnDClass.ROGUE);
        data.setRogueSneakAttackDice(getSneakAttackDiceForLevel(level));
        data.setRogueEvasionUnlocked(level >= 2);
        data.setRogueTrapSenseBonus(getTrapSenseBonusForLevel(level));
        data.setAchievementFlag("rogue_uncanny_dodge_unlocked", level >= 4);
        data.setAchievementFlag("rogue_improved_uncanny_dodge_unlocked", level >= 8);
    }

    /** Rolls the Sneak Attack bonus: dice x d6, flat (never crit-multiplied). */
    public static int rollSneakAttackBonus(int dice) {
        int total = 0;
        for (int i = 0; i < dice; i++) {
            total += ThreadLocalRandom.current().nextInt(1, 7);
        }
        return total;
    }

    /**
     * Sneak Attack only affects living creatures with discernible anatomy.
     * Only undead are identifiable in the current entity-type system — no
     * construct/ooze/plant classification exists to also exclude.
     */
    public static boolean isSneakAttackImmune(LivingEntity target) {
        return target.getType().is(EntityTypeTags.UNDEAD);
    }

    /** Backstab arc: same math as the BackStab ability — target facing away. */
    public static boolean isBackstab(ServerPlayer attacker, LivingEntity target) {
        Vec3 targetLook = target.getLookAngle();
        Vec3 attackerToTarget = target.position().subtract(attacker.position()).normalize();
        return targetLook.dot(attackerToTarget) > 0.0;
    }

    /**
     * Simplified flanking: an ally of the attacker (friendly player or the
     * attacker's tamed pet) within 3 blocks of the target, with line of sight
     * to it, on roughly the opposite side (dot product of the two
     * target-relative direction vectors below -0.3).
     */
    public static boolean isFlanking(ServerPlayer attacker, LivingEntity target) {
        Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
        for (Entity entity : target.level().getEntities(target, target.getBoundingBox().inflate(3.0))) {
            if (entity == attacker || !(entity instanceof LivingEntity ally)) continue;

            boolean isAlly =
                    (entity instanceof ServerPlayer other && FriendlyFireChecker.isFriendly(attacker, other))
                    || (entity instanceof TamableAnimal pet && pet.isTame()
                        && attacker.getUUID().equals(pet.getOwnerUUID()));
            if (!isAlly) continue;

            if (!ally.hasLineOfSight(target)) continue;

            Vec3 toAlly = ally.position().subtract(target.position()).normalize();
            if (toAttacker.dot(toAlly) < -0.3) {
                return true;
            }
        }
        return false;
    }

    /**
     * Target unaware: a mob that has not aggroed the attacker, or a player
     * who has neither attacked the attacker nor been last hurt by them.
     */
    public static boolean isUnaware(ServerPlayer attacker, LivingEntity target) {
        if (target instanceof Mob mob) {
            return mob.getTarget() != attacker;
        }
        if (target instanceof ServerPlayer targetPlayer) {
            return targetPlayer.getLastHurtByMob() != attacker
                    && targetPlayer.getLastHurtMob() != attacker;
        }
        return false;
    }

    /**
     * Full Sneak Attack qualification: type restriction, ranged 6-block limit,
     * then any of the three triggers — with Uncanny Dodge denials applied when
     * the defender is a player. Regular Uncanny Dodge (Rogue 4 / Barbarian 2)
     * denies the unaware trigger; Improved Uncanny Dodge (Rogue 8 /
     * Barbarian 5) denies flanking unless the attacker's Rogue level exceeds
     * the defender's relevant class level by 4+.
     */
    public static boolean qualifiesForSneakAttack(ServerPlayer attacker, LivingEntity target, boolean ranged) {
        if (isSneakAttackImmune(target)) return false;
        if (ranged && attacker.distanceTo(target) > 6.0f) return false;

        boolean unawareDenied = false;
        boolean flankingDenied = false;
        if (target instanceof ServerPlayer defender) {
            DnDPlayerData defData = PlayerDataHelper.get(defender);
            unawareDenied = defData.getAchievementFlag("rogue_uncanny_dodge_unlocked")
                    || BarbarianPassives.hasUncannyDodge(defData);

            int defenderDodgeLevel = 0;
            if (defData.getAchievementFlag("rogue_improved_uncanny_dodge_unlocked")) {
                defenderDodgeLevel = Math.max(defenderDodgeLevel, defData.getClassLevel(DnDClass.ROGUE));
            }
            if (BarbarianPassives.hasImprovedUncannyDodge(defData)) {
                defenderDodgeLevel = Math.max(defenderDodgeLevel, defData.getClassLevel(DnDClass.BARBARIAN));
            }
            if (defenderDodgeLevel > 0) {
                int attackerRogueLevel = PlayerDataHelper.get(attacker).getClassLevel(DnDClass.ROGUE);
                flankingDenied = attackerRogueLevel < defenderDodgeLevel + 4;
            }
        }

        if (isBackstab(attacker, target)) return true;
        if (!flankingDenied && isFlanking(attacker, target)) return true;
        return !unawareDenied && isUnaware(attacker, target);
    }
}
