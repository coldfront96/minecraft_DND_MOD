package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

public class MultiShot extends Ability {
    public MultiShot() {
        super("ranger_multi_shot", "Multi Shot",
                "Fire 3 arrows in a spread pattern",
                DnDClass.RANGER, 1, 15, 60, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 3.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.DEXTERITY; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        // Derive the horizontal "right" vector from yaw so the spread is stable
        // even when the player looks straight up or down (a cross product with
        // the up vector would collapse to zero in those cases).
        float yawRad = player.getYRot() * ((float) Math.PI / 180F);
        Vec3 right = new Vec3(-Math.cos(yawRad), 0.0, -Math.sin(yawRad));

        int dexMod = data.getAbilityScores().getDexMod();
        double arrowDamage = getBaseDamage() + dexMod;

        double spreadAngle = Math.toRadians(15);

        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(EntityType.ARROW, player.level());
            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            Vec3 dir;
            if (i == 0) {
                dir = look;
            } else {
                double cos = Math.cos(spreadAngle * i);
                double sin = Math.sin(spreadAngle * i);
                dir = look.scale(cos).add(right.scale(sin));
            }
            arrow.shoot(dir.x, dir.y, dir.z, 3.0f, 1.0f);
            arrow.setOwner(player);
            arrow.setBaseDamage(arrowDamage);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            player.level().addFreshEntity(arrow);
        }
    }
}
