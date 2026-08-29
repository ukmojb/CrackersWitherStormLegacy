package com.wdcftgg.witherstormmod.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;


public final class BossVisibility {
    private static final int OPEN_AREA_RADIUS = 5;
    private static final double MAX_LINE_OF_SIGHT_DISTANCE = 300.0D;

    private BossVisibility() {
    }

    public static boolean canSeeOrIsNotInSmallArea(Entity boss, Entity viewer) {
        return boss != null && viewer != null && boss.world != null && boss.world == viewer.world
                && (isInOpenArea(viewer) || hasLineOfSight(boss, viewer));
    }

    public static boolean isInOpenArea(Entity entity) {
        if (entity == null || entity.world == null) return false;
        int lowestSurface = Integer.MAX_VALUE;
        int centerX = (int) Math.floor(entity.posX);
        int centerY = (int) Math.floor(entity.posY + 0.5D);
        int centerZ = (int) Math.floor(entity.posZ);
        for (int offsetX = -OPEN_AREA_RADIUS; offsetX < OPEN_AREA_RADIUS; offsetX++) {
            for (int offsetZ = -OPEN_AREA_RADIUS; offsetZ < OPEN_AREA_RADIUS; offsetZ++) {
                int surface = entity.world.getHeight(
                        new BlockPos(centerX + offsetX, centerY, centerZ + offsetZ)).getY();
                lowestSurface = Math.min(lowestSurface, surface);
            }
        }
        return entity.posY >= lowestSurface - 10.0D;
    }

    public static boolean hasLineOfSight(Entity boss, Entity viewer) {
        if (boss == null || viewer == null || boss.world == null || boss.world != viewer.world) {
            return false;
        }
        Vec3d start = boss.getPositionEyes(1.0F);
        Vec3d delta = viewer.getPositionEyes(1.0F).subtract(start);
        double distance = delta.length();
        if (distance > MAX_LINE_OF_SIGHT_DISTANCE) {
            delta = delta.scale(MAX_LINE_OF_SIGHT_DISTANCE / distance);
        }
        RayTraceResult result = boss.world.rayTraceBlocks(start, start.add(delta), false, true, false);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }
}
