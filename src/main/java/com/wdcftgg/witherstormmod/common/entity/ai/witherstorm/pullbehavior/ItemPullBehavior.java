package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.pullbehavior;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior.WitherStormPullBehavior;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


public final class ItemPullBehavior extends WitherStormPullBehavior<EntityItem> {
    private static final double MAX_SPEED = 5.0D;
    private static final double ROTATION_SPEED = 0.25D;

    @Override
    public Vec3d pullEntity(EntityItem entity, WitherStormEntity storm, Vec3d absorptionPoint,
                            Vec3d defaultVelocity, double defaultSpeed) {
        double distance = entity.getPositionVector().distanceTo(absorptionPoint);
        if (distance > storm.width * 1.5D && distance > 4.0D) {
            Vec3d inward = absorptionPoint.subtract(entity.getPositionVector()).normalize();
            Vec3d rotation = inward.crossProduct(new Vec3d(0.0D, -1.0D, 0.0D))
                    .normalize().scale(ROTATION_SPEED);
            Vec3d velocity = inward.scale(defaultSpeed).add(rotation);
            return velocity.length() > MAX_SPEED
                    ? velocity.normalize().scale(MAX_SPEED) : velocity;
        }
        return defaultVelocity;
    }

    @Override
    public double getSpeed(EntityItem entity, WitherStormEntity storm, Vec3d absorptionPoint) {
        double modifier = WitherStormConfig.blockClusterPullSpeedModifier;
        double speed = 0.375D * modifier;
        return speed * MathHelper.clamp(
                entity.getPositionVector().distanceTo(absorptionPoint) / modifier, 0.1D, 1.0D);
    }

    @Override
    public boolean doClientsideVelocityUpdates(EntityItem entity, WitherStormEntity storm) {
        return true;
    }
}
