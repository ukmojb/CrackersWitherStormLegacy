package com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;




public abstract class WitherStormPullBehavior<T extends Entity> {

    private final double defaultSpeed;

    public WitherStormPullBehavior(double defaultSpeed) {
        this.defaultSpeed = defaultSpeed;
    }

    public WitherStormPullBehavior() {
        this(0.5D);
    }






    public abstract Vec3d pullEntity(T entity, WitherStormEntity storm, Vec3d absorptionPoint,
                                     Vec3d defaultVelocity, double defaultSpeed);

    public double getSpeed(T entity, WitherStormEntity storm, Vec3d destination) {
        return defaultSpeed;
    }

    public boolean canPullIn(T entity, WitherStormEntity storm) {
        return true;
    }

    public boolean doClientsideVelocityUpdates(T entity, WitherStormEntity storm) {
        return false;
    }
}
