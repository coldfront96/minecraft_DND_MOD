package com.khimairacraft.ability.barbarian;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.combat.SavingThrowSystem;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.resource.RageSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThunderousStompAbility extends Ability {

    public ThunderousStompAbility() {
        super("barbarian_thunderous_stomp", "Thunderous Stomp",
                "Slam the ground, dealing AoE damage and knocking back nearby enemies. Requires Rage.",
                DnDClass.BARBARIAN, 9, 0, 160, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 9) return false;
        if (!RageSystem.isRaging(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("You must be Raging to use Thunderous Stomp!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        return !data.isOnCooldown("barbarian_thunderous_stomp");
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        data.startCooldown("barbarian_thunderous_stomp", getCooldownTicks());
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        double radius = 5.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && !FriendlyFireChecker.isFriendly(player, e));

        int strMod = data.getAbilityScores().getStrMod();
        float baseDamage = 4.0f + strMod * 1.5f;
        int dc = SavingThrowSystem.getAbilityDC(this, data);

        int hit = 0;
        for (LivingEntity target : targets) {
            boolean saved = SavingThrowSystem.rollSave(target, SaveType.REFLEX, dc, player);
            float multiplier = SavingThrowSystem.getSaveMultiplier(saved, true);
            float damage = baseDamage * multiplier;

            if (damage > 0) {
                target.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
                hit++;
            }

            if (!saved) {
                Vec3 knockDir = target.position().subtract(player.position()).normalize();
                target.push(knockDir.x * 1.5, 0.4, knockDir.z * 1.5);
                target.hurtMarked = true;
            }
        }

        player.displayClientMessage(
                Component.literal("Thunderous Stomp! Hit " + hit + " enemies.")
                        .withStyle(s -> s.withColor(0xFFAA00)), true);
    }
}
