package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.WitherStormWorldInteractions;
import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior.WitherStormPullBehavior;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


final class WitherStormPulling {
    private static final double DEFAULT_PULL_SPEED = 0.5D;
    private static final double ORBIT_SPEED = 0.25D;
    private static final double MAX_ORBIT_SPEED = 5.0D;
    private static final double CLUSTER_BEAM_RADIUS = 4.0D;

    private WitherStormPulling() {
    }

    interface Source {
        default WitherStormEntity getStorm() {
            return null;
        }

        default boolean usesRegisteredPullBehaviors() {
            return true;
        }

        int getPhase();

        float getWidth();

        Vec3d getEyePosition();

        BlockPos getBlockPosition();

        boolean isTractorBeamActive(int head);

        Vec3d getHeadPosition(int head);

        Vec3d getHeadDirection(int head);

        double getTractorBeamCutoff(int head);
    }

    static boolean canPullIn(Entity entity, Source source) {
        WitherStormPullBehavior<Entity> behavior = getPullBehavior(entity, source);
        if (behavior != null) return behavior.canPullIn(entity, source.getStorm());
        return !(entity instanceof SupplementalEntities.BlockClusterEntity)
                || ((SupplementalEntities.BlockClusterEntity) entity).getShakeTime() <= 0;
    }

    @SuppressWarnings("unchecked")
    static Vec3d getPullVelocity(Entity entity, Source source, Vec3d absorptionPoint) {
        WitherStormPullBehavior<Entity> behavior = getPullBehavior(entity, source);
        double speed = behavior == null
                ? getPullSpeed(entity, source, absorptionPoint)
                : behavior.getSpeed(entity, source.getStorm(), absorptionPoint);
        Vec3d baseVelocity = getBaseVelocity(entity, absorptionPoint, speed);
        if (behavior != null) {
            return behavior.pullEntity(entity, source.getStorm(), absorptionPoint,
                    baseVelocity, speed);
        }
        if (entity instanceof SupplementalEntities.BlockClusterEntity) {
            return getBlockClusterVelocity((SupplementalEntities.BlockClusterEntity) entity,
                    source, absorptionPoint, baseVelocity);
        }
        if (entity instanceof EntityItem) {
            return getOrbitVelocity(entity, source, absorptionPoint, baseVelocity, true);
        }
        if (entity instanceof EntitySlime) {
            return getOrbitVelocity(entity, source, absorptionPoint, baseVelocity, false);
        }
        return baseVelocity;
    }

    static void applyVelocity(Entity entity, Vec3d velocity, Source source) {
        entity.motionX = velocity.x;
        entity.motionY = velocity.y;
        entity.motionZ = velocity.z;
        WitherStormPullBehavior<Entity> behavior = getPullBehavior(entity, source);
        entity.velocityChanged = entity instanceof EntityItem
                || behavior != null && behavior.doClientsideVelocityUpdates(entity, source.getStorm())
                || behavior == null && entity instanceof SupplementalEntities.BlockClusterEntity;
    }

    static boolean reachesAbsorptionBox(Entity entity, AxisAlignedBB absorptionBox, Vec3d velocity) {
        Vec3d start = entity.getPositionVector();
        if (absorptionBox.contains(start)) return true;
        Vec3d end = start.add(velocity);
        return absorptionBox.contains(end)
                || absorptionBox.calculateIntercept(start, end) != null;
    }

    @SuppressWarnings("unchecked")
    private static WitherStormPullBehavior<Entity> getPullBehavior(Entity entity, Source source) {
        if (!source.usesRegisteredPullBehaviors() || source.getStorm() == null) return null;
        WitherStormWorldInteractions interactions = WitherStormWorldInteractions.getInstance();
        return interactions.hasPullBehavior(entity)
                ? (WitherStormPullBehavior<Entity>) interactions.getPullBehavior(entity) : null;
    }

    private static double getPullSpeed(Entity entity, Source source, Vec3d absorptionPoint) {
        if (entity instanceof SupplementalEntities.BlockClusterEntity) {
            return getBlockClusterSpeed((SupplementalEntities.BlockClusterEntity) entity,
                    source, absorptionPoint);
        }
        if (entity instanceof EntityItem || entity instanceof EntitySlime) {
            double modifier = WitherStormConfig.blockClusterPullSpeedModifier;
            double speed = 0.375D * modifier;
            return speed * MathHelper.clamp(entity.getPositionVector().distanceTo(absorptionPoint) / modifier,
                    0.1D, 1.0D);
        }
        return DEFAULT_PULL_SPEED;
    }

    private static Vec3d getBaseVelocity(Entity entity, Vec3d absorptionPoint, double speed) {
        return absorptionPoint.subtract(entity.getPositionVector()).normalize().scale(speed);
    }

    private static Vec3d getOrbitVelocity(Entity entity, Source source, Vec3d absorptionPoint,
                                          Vec3d baseVelocity, boolean isItem) {
        double distance = entity.getPositionVector().distanceTo(absorptionPoint);
        if (distance <= source.getWidth() * 1.5D || isItem && distance <= 4.0D) {
            return baseVelocity;
        }
        Vec3d inward = absorptionPoint.subtract(entity.getPositionVector()).normalize();
        Vec3d rotation = inward.crossProduct(new Vec3d(0.0D, -1.0D, 0.0D)).normalize().scale(ORBIT_SPEED);
        Vec3d velocity = inward.scale(getPullSpeed(entity, source, absorptionPoint)).add(rotation);
        return velocity.length() > MAX_ORBIT_SPEED ? velocity.normalize().scale(MAX_ORBIT_SPEED) : velocity;
    }

    private static Vec3d getBlockClusterVelocity(SupplementalEntities.BlockClusterEntity cluster,
                                                  Source source, Vec3d absorptionPoint,
                                                  Vec3d baseVelocity) {
        cluster.setFadePos(source.getBlockPosition());
        if (source.getPhase() > 3) {
            cluster.setFadeStrength(75.0F);
            cluster.setFadeDistanceOffset(20);
        }
        if (cluster.getPositionVector().distanceTo(absorptionPoint) <= source.getWidth() * 1.5D) {
            return baseVelocity;
        }

        double rotationSpeed = source.getPhase() <= 3 ? 0.2D : getClusterRotationSpeed(cluster, source);
        boolean rotateClockwise = WitherStormConfig.canClustersSpiralCounterClockwise
                && cluster.isRotateClockwise();
        Vec3d inward = absorptionPoint.subtract(cluster.getPositionVector()).normalize();
        Vec3d rotation = inward.crossProduct(new Vec3d(0.0D, rotateClockwise ? 1.0D : -1.0D, 0.0D))
                .normalize().scale(rotationSpeed);

        if (!cluster.createdFromTractorBeam()
                || cluster.getPositionVector().distanceTo(source.getEyePosition()) <= 25.0D) {
            return baseVelocity.add(rotation);
        }

        int head = cluster.getHeadCreatedFrom();
        if (!source.isTractorBeamActive(head)) return baseVelocity.add(rotation);
        Vec3d headPosition = source.getHeadPosition(head);
        Vec3d closestPoint = TractorBeamHelper.calculateClosestPoint(cluster.getPositionVector(), headPosition,
                source.getHeadDirection(head), source.getTractorBeamCutoff(head));
        double distanceToBeam = cluster.getPositionVector().distanceTo(closestPoint);
        double distanceToHead = cluster.getPositionVector().distanceTo(headPosition);
        double beamRadius = CLUSTER_BEAM_RADIUS * (distanceToHead + 20.0D) * 0.015D;
        double threshold = cluster.getTractorBeamDistanceThreshold()
                * MathHelper.clamp((distanceToHead - 60.0D) * 0.1D, 0.0D, 1.0D);
        double correctionSpeed = MathHelper.clamp(distanceToBeam + threshold - beamRadius, 0.0D, 4.0D);
        Vec3d correction = closestPoint.subtract(cluster.getPositionVector()).normalize().scale(correctionSpeed);
        Vec3d towardHead = headPosition.subtract(cluster.getPositionVector()).normalize()
                .scale(getBlockClusterSpeed(cluster, source, absorptionPoint));
        return towardHead.add(correction);
    }

    private static double getBlockClusterSpeed(SupplementalEntities.BlockClusterEntity cluster,
                                               Source source, Vec3d absorptionPoint) {
        double distance = cluster.getPositionVector().distanceTo(absorptionPoint);
        double speed;
        if (source.getPhase() <= 3) {
            speed = 0.125D;
        } else if (distance >= 240.0D && !cluster.createdFromTractorBeam()) {
            speed = 0.375D;
        } else {
            speed = 0.0625D;
        }
        speed += cluster.getTime() * 0.005D;
        double modifier = cluster.createdFromTractorBeam()
                ? WitherStormConfig.tractorBeamClusterSpeedModifier
                : WitherStormConfig.blockClusterPullSpeedModifier;
        speed *= modifier;
        return speed * MathHelper.clamp(distance / modifier, 0.1D, 1.0D);
    }

    private static double getClusterRotationSpeed(SupplementalEntities.BlockClusterEntity cluster,
                                                  Source source) {
        int time = cluster.getTime();
        double initialSpeed = 0.0002D + time * 15.0D * 4.0D;
        double sizeFactor = Math.pow(cluster.getBlocks().size(), -0.25D);
        double speed = source.getPhase() <= 3
                ? Math.min(initialSpeed * 10000.0D + time * 15.0D * sizeFactor, sizeFactor)
                : Math.min(initialSpeed + time * 15.0D * sizeFactor, sizeFactor);
        if (speed > 5.0D && source.getPhase() >= 4) return 5.0D;
        return speed > 2.0D && source.getPhase() <= 3 ? 2.0D : speed;
    }
}
