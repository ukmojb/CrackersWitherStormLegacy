package com.wdcftgg.witherstormmod.common.entity.ai;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;









public final class VillagerPanicAI extends EntityAIBase {
    private static final float SENSOR_RADIUS = 300.0F;
    private static final float SMALL_PHASE_RADIUS = 100.0F;
    private static final double PANIC_SPEED = 0.75D;
    private static final double ESCAPE_STEP = 16.0D;
    private final EntityVillager villager;
    private final PathNavigate navigator;
    private WitherStormEntity nearestStorm;
    private Path escapePath;

    public VillagerPanicAI(EntityVillager villager) {
        this.villager = villager;
        this.navigator = villager.getNavigator();
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        nearestStorm = findNearestStorm();
        if (!isInPanicRange(nearestStorm)) return false;
        if (escapePath == null || escapePath.isFinished()) {
            Vec3d destination = panicDestination();
            escapePath = navigator.getPathToXYZ(destination.x, destination.y, destination.z);

            villager.setAttackTarget(null);
        }
        return escapePath != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return isInPanicRange(nearestStorm) && !navigator.noPath();
    }

    @Override
    public void startExecuting() {
        navigator.setPath(escapePath, PANIC_SPEED);
    }

    @Override
    public void updateTask() {
        navigator.setSpeed(PANIC_SPEED);
    }

    @Override
    public void resetTask() {
        nearestStorm = null;
        escapePath = null;
    }

    private boolean isInPanicRange(WitherStormEntity storm) {
        if (storm == null || storm.isDeadOrPlayingDead()) return false;
        if (storm.getPhase() < 4) {
            return villager.getEntityBoundingBox().grow(SMALL_PHASE_RADIUS)
                    .contains(storm.getPositionVector());
        }
        return true;
    }

    private Vec3d panicDestination() {
        Vec3d away = villager.getPositionVector()
                .subtract(nearestStorm.getPositionVector()).normalize().scale(ESCAPE_STEP);
        return villager.getPositionVector().add(away);
    }

    private WitherStormEntity findNearestStorm() {
        World world = villager.world;
        AxisAlignedBB searchArea = villager.getEntityBoundingBox().grow(SENSOR_RADIUS);
        WitherStormEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (WitherStormEntity storm : WorldUtil.getCachedStorms(world)) {
            if (!searchArea.intersects(storm.getEntityBoundingBox())) continue;
            double distance = villager.getDistanceSq(storm);
            if (distance < nearestDistance) {
                nearest = storm;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
