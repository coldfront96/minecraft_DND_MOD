package com.deadmind.dndmods.ability.barbarian;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.SaveType;
import com.deadmind.dndmods.combat.SavingThrowSystem;
import com.deadmind.dndmods.party.FriendlyFireChecker;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class IntimidatingRoarAbility extends Ability {

    public IntimidatingRoarAbility() {
        super("barbarian_intimidating_roar", "Intimidating Roar",
                "Let out a terrifying roar that inflicts fear on nearby enemies. Requires Rage.",
                DnDClass.BARBARIAN, 6, 0, 200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 6) return false;
        if (!RageSystem.isRaging(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("You must be Raging to use Intimidating Roar!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        return !data.isOnCooldown("barbarian_intimidating_roar");
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        data.startCooldown("barbarian_intimidating_roar", getCooldownTicks());
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        double radius = 6.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && !FriendlyFireChecker.isFriendly(player, e));

        int dc = SavingThrowSystem.getAbilityDC(this, data);
        int affected = 0;

        for (LivingEntity target : targets) {
            if (!SavingThrowSystem.rollSave(target, SaveType.WILL, dc, player)) {
                int duration = 60 + data.getAbilityScores().getStrMod() * 10;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
                affected++;
            }
        }

        player.displayClientMessage(
                Component.literal("Intimidating Roar! " + affected + " enemies feared (DC " + dc + ")")
                        .withStyle(s -> s.withColor(0xFF6600)), true);
    }
}
