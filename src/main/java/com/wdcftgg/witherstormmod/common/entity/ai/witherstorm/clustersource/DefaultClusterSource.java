package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.util.math.Vec2f;

import java.util.Random;


public final class DefaultClusterSource extends BlockClusterSource {
    public DefaultClusterSource() {
        super(256);
    }

    @Override
    protected float getClusterSizeRadius(WitherStormEntity storm) {
        return storm.getClusterRadius() + WitherStormConfig.clusterSizeModifier;
    }

    @Override
    protected int calculateShakeTime(WitherStormEntity storm, Random random) {
        return 20 + random.nextInt(10);
    }

    @Override
    protected Vec2f calculateRotationDelta(WitherStormEntity storm, Random random) {
        return new Vec2f(random.nextInt(20) * 0.05F, random.nextInt(20) * 0.05F);
    }

    @Override
    protected int getPickupInterval(WitherStormEntity storm) {
        if (storm.shouldSpeedUp()) {
            return WitherStormConfig.devourerClusterPickupInterval * 4;
        }
        return storm.getPhase() < 6
                ? WitherStormConfig.clusterPickupInterval
                : WitherStormConfig.devourerClusterPickupInterval;
    }

    @Override
    protected boolean canUse(WitherStormEntity storm) {
        return storm.getPhase() >= 4;
    }
}
