package com.deadmind.dndmods.ability.barbarian;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.ModDamageTypes;
import com.deadmind.dndmods.party.FriendlyFireChecker;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class UnstoppableChargeAbility extends Ability {

    public UnstoppableChargeAbility() {
        super("barbarian_unstoppable_charge", "Unstoppable Charge",
                "Dash 8 blocks forward, damaging and knocking aside enemies in your path.",
                DnDClass.BARBARIAN, 15, 0, 240, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 15) return false;
        return !data.isOnCooldown("barbarian_unstoppable_charge");
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        data.startCooldown("barbarian_unstoppable_charge", getCooldownTicks());
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 chargeVector = new Vec3(look.x, 0, look.z).normalize().scale(8.0);

        Vec3 startPos = player.position();
        Vec3 endPos = startPos.add(chargeVector);

        player.teleportTo(endPos.x, player.getY(), endPos.z);

        AABB chargePath = player.getBoundingBox().expandTowards(chargeVector).inflate(1.0);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, chargePath,
                e -> e != player && !FriendlyFireChecker.isFriendly(player, e));

        int strMod = data.getAbilityScores().getStrMod();
        float damage = 3.0f + strMod;

        int hit = 0;
        for (LivingEntity target : targets) {
            target.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
            Vec3 knockDir = target.position().subtract(startPos).normalize();
            target.push(knockDir.x * 1.2, 0.3, knockDir.z * 1.2);
            target.hurtMarked = true;
            hit++;
        }

        player.displayClientMessage(
                Component.literal("Unstoppable Charge! Trampled " + hit + " enemies.")
                        .withStyle(s -> s.withColor(0xFF6600)), true);
    }
}
