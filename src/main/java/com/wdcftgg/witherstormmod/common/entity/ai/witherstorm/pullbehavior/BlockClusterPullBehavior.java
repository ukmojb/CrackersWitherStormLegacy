package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.pullbehavior;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior.WitherStormPullBehavior;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


public final class BlockClusterPullBehavior
        extends WitherStormPullBehavior<SupplementalEntities.BlockClusterEntity> {
    private static final double HUNCH_MAX_SPEED = 2.0D;
    private static final double MAX_SPEED = 5.0D;
    private static final double CLUSTER_TIME = 15.0D;
    private static final double BEAM_RADIUS = 4.0D;

    @Override
    public Vec3d pullEntity(SupplementalEntities.BlockClusterEntity cluster,
                            WitherStormEntity storm, Vec3d absorptionPoint,
                            Vec3d defaultVelocity, double defaultSpeed) {
        cluster.setFadePos(storm.getPosition());
        if (storm.getPhase() > 3) {
            cluster.setFadeStrength(75.0F);
            cluster.setFadeDistanceOffset(20);
        }
        if (cluster.getPositionVector().distanceTo(absorptionPoint) <= storm.width * 1.5D) {
            return defaultVelocity;
        }

        double rotationSpeed = storm.getPhase() <= 3
                ? 0.2D : getClusterRotationSpeed(cluster, storm);
        boolean rotateClockwise = WitherStormConfig.canClustersSpiralCounterClockwise
                && cluster.isRotateClockwise();
        Vec3d inward = absorptionPoint.subtract(cluster.getPositionVector()).normalize();
        Vec3d rotation = inward.crossProduct(
                new Vec3d(0.0D, rotateClockwise ? 1.0D : -1.0D, 0.0D))
                .normalize().scale(rotationSpeed);

        if (!cluster.createdFromTractorBeam()
                || cluster.getPositionVector().distanceTo(storm.getPositionEyes(1.0F)) <= 25.0D) {
            return defaultVelocity.add(rotation);
        }

        int head = cluster.getHeadCreatedFrom();
        if (!storm.tractorBeamActive(head)) return defaultVelocity.add(rotation);
        Vec3d headPosition = storm.getHeadPosition(head, 1.0F);
        Vec3d closestPoint = TractorBeamHelper.calculateClosestPoint(
                cluster.getPositionVector(), headPosition,
                storm.getHeadManager().getLookVector(head),
                storm.getHeadManager().getTractorBeamCutoff(head));
        double distanceToBeam = cluster.getPositionVector().distanceTo(closestPoint);
        double distanceToHead = cluster.getPositionVector().distanceTo(headPosition);
        double beamRadius = BEAM_RADIUS * (distanceToHead + 20.0D) * 0.015D;
        double threshold = cluster.getTractorBeamDistanceThreshold()
                * MathHelper.clamp((distanceToHead - 60.0D) * 0.1D, 0.0D, 1.0D);
        double correctionSpeed = MathHelper.clamp(
                distanceToBeam + threshold - beamRadius, 0.0D, 4.0D);
        Vec3d correction = closestPoint.subtract(cluster.getPositionVector())
                .normalize().scale(correctionSpeed);
        Vec3d towardHead = headPosition.subtract(cluster.getPositionVector())
                .normalize().scale(defaultSpeed);
        return towardHead.add(correction);
    }

    @Override
    public double getSpeed(SupplementalEntities.BlockClusterEntity cluster,
                           WitherStormEntity storm, Vec3d absorptionPoint) {
        double distance = cluster.getPositionVector().distanceTo(absorptionPoint);
        double speed = storm.getPhase() <= 3 ? 0.125D
                : distance >= 240.0D && !cluster.createdFromTractorBeam() ? 0.375D : 0.0625D;
        speed += cluster.getTime() * 0.005D;
        double modifier = cluster.createdFromTractorBeam()
                ? WitherStormConfig.tractorBeamClusterSpeedModifier
                : WitherStormConfig.blockClusterPullSpeedModifier;
        speed *= modifier;
        return speed * MathHelper.clamp(distance / modifier, 0.1D, 1.0D);
    }

    @Override
    public boolean canPullIn(SupplementalEntities.BlockClusterEntity cluster,
                             WitherStormEntity storm) {
        return cluster.getShakeTime() <= 0;
    }

    @Override
    public boolean doClientsideVelocityUpdates(SupplementalEntities.BlockClusterEntity cluster,
                                               WitherStormEntity storm) {
        return true;
    }

    private static double getClusterRotationSpeed(
            SupplementalEntities.BlockClusterEntity cluster, WitherStormEntity storm) {
        int time = cluster.getTime();
        double initialSpeed = 0.0002D + time * CLUSTER_TIME * 4.0D;
        double sizeFactor = Math.pow(cluster.getBlocks().size(), -0.25D);
        double speed = storm.getPhase() <= 3
                ? Math.min(initialSpeed * 10000.0D + time * CLUSTER_TIME * sizeFactor, sizeFactor)
                : Math.min(initialSpeed + time * CLUSTER_TIME * sizeFactor, sizeFactor);
        if (speed > MAX_SPEED && storm.getPhase() >= 4) return MAX_SPEED;
        return speed > HUNCH_MAX_SPEED && storm.getPhase() <= 3 ? HUNCH_MAX_SPEED : speed;
    }
}
