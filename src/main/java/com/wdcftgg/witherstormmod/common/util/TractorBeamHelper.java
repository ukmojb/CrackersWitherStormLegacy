package com.wdcftgg.witherstormmod.common.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


public final class TractorBeamHelper {
    private static final double BEAM_ORIGIN_WIDTH = 30.0D;
    private static final double BEAM_WIDTH_GROWTH = 0.014D;

    private TractorBeamHelper() {
    }

    public static Vec3d calculateClosestPoint(Vec3d position, Vec3d headPosition,
                                               Vec3d headDirection, double cutoffDistance) {
        return calculateClosestPoint(position, headPosition, headDirection, cutoffDistance, 0.0D);
    }

    public static Vec3d calculateClosestPoint(Vec3d position, Vec3d headPosition,
                                               Vec3d headDirection, double cutoffDistance,
                                               double distanceOffset) {
        double distance = Math.max(0.0D, headPosition.distanceTo(position) + distanceOffset);
        if (cutoffDistance >= 0.0D) distance = Math.min(distance, cutoffDistance);
        return headPosition.add(headDirection.scale(distance));
    }

    public static boolean isInsideTractorBeam(Vec3d position, Vec3d headPosition,
                                               Vec3d headDirection, double cutoffDistance,
                                               double beamRadius) {
        Vec3d closestPoint = calculateClosestPoint(
                position, headPosition, headDirection, cutoffDistance);
        double distanceToHead = position.distanceTo(headPosition);
        double width = beamRadius * (distanceToHead + BEAM_ORIGIN_WIDTH) * BEAM_WIDTH_GROWTH;
        return position.squareDistanceTo(closestPoint) <= width * width;
    }

    public static double findCutoffDistance(World world, Vec3d headPosition,
                                            Vec3d headDirection, double maximumDistance) {
        RayTraceResult result = world.rayTraceBlocks(headPosition,
                headPosition.add(headDirection.scale(maximumDistance)), false, true, false);
        return result != null && result.typeOfHit == RayTraceResult.Type.BLOCK
                ? headPosition.distanceTo(result.hitVec) : -1.0D;
    }

    public static Vec3d calculatePullVelocity(Vec3d position, Vec3d destination, double speed) {
        Vec3d delta = destination.subtract(position);
        if (delta.lengthSquared() <= 0.0001D) return Vec3d.ZERO;
        double scaledSpeed = speed * MathHelper.clamp(delta.length(), 0.1D, 1.0D);
        return delta.normalize().scale(scaledSpeed);
    }
}
