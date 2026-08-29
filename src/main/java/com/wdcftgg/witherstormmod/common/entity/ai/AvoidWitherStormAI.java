package com.wdcftgg.witherstormmod.common.entity.ai;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.BossVisibility;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class AvoidWitherStormAI extends EntityAIBase {
    private static final int OPEN_AREA_CACHE_TICKS = 20;
    private static final int PORTAL_SEARCH_INTERVAL_TICKS = 20;

    private final EntityCreature creature;
    private final float maximumDistance;
    private final float smallPhaseMaximumDistance;
    private final double walkSpeed;
    private final double sprintSpeed;
    private final PathNavigate navigator;
    private WitherStormEntity nearestStorm;
    private Path escapePath;
    private BlockPos cachedOpenAreaPosition;
    private long openAreaCacheExpiry = Long.MIN_VALUE;
    private boolean cachedOpenArea;
    private BlockPos cachedPortal;
    private long nextPortalSearchTick = Long.MIN_VALUE;

    private long lastSightCheckTick = Long.MIN_VALUE;
    private WitherStormEntity cachedSightStorm;
    private boolean cachedSightResult;

    public AvoidWitherStormAI(EntityCreature creature, float maximumDistance,
                              double walkSpeed, double sprintSpeed) {
        this.creature = creature;
        this.maximumDistance = maximumDistance;
        this.smallPhaseMaximumDistance = maximumDistance / 8.0F;
        this.walkSpeed = walkSpeed;
        this.sprintSpeed = sprintSpeed;
        this.navigator = creature.getNavigator();
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        nearestStorm = findNearestStorm();
        if (!canEscapeFrom(nearestStorm)) return false;

        BlockPos portal = WitherStormConfig.mobsRunIntoPortals ? findNearbyPortal() : null;
        Vec3d destination;
        if (portal != null) {
            destination = new Vec3d(portal.getX() + 0.5D, portal.getY() + 1.0D, portal.getZ() + 0.5D);
        } else {
            Vec3d away = creature.getPositionVector()
                    .subtract(nearestStorm.getPositionVector()).normalize().scale(16.0D);
            destination = creature.getPositionVector().add(away);
        }

        if (escapePath == null || escapePath.isFinished()) {
            escapePath = navigator.getPathToXYZ(destination.x, destination.y, destination.z);
        }
        return escapePath != null;
    }

    @Nullable
    private WitherStormEntity findNearestStorm() {
        AxisAlignedBB searchArea = creature.getEntityBoundingBox().grow(maximumDistance);
        WitherStormEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (WitherStormEntity storm : WorldUtil.getCachedStorms(creature.world)) {
            if (storm == null || !storm.isEntityAlive()
                    || !searchArea.intersects(storm.getEntityBoundingBox())) continue;
            double distance = creature.getDistanceSq(storm);
            if (distance < nearestDistance) {
                nearest = storm;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public static void clearWorldCache(World world) {

    }

    private boolean canEscapeFrom(@Nullable WitherStormEntity storm) {
        if (storm == null || storm.isDeadOrPlayingDead()) return false;
        int phase = getPhase(storm);
        if (phase < 2) return false;
        if (phase <= 3 && !creature.getEntityBoundingBox().grow(smallPhaseMaximumDistance)
                .contains(storm.getPositionVector())) return false;
        if (storm.isInsideTractorBeam(creature, 4.0D) && !creature.onGround) return false;
        long currentTick = creature.world.getTotalWorldTime();
        if (currentTick != lastSightCheckTick || cachedSightStorm != storm) {
            cachedSightResult = BossVisibility.hasLineOfSight(storm, creature);
            lastSightCheckTick = currentTick;
            cachedSightStorm = storm;
        }
        return isCreatureInOpenArea() || cachedSightResult;
    }

    private boolean isCreatureInOpenArea() {

        BlockPos currentPosition = new BlockPos(
                Math.floor(creature.posX), Math.floor(creature.posY + 0.5D), Math.floor(creature.posZ));
        long currentTick = creature.world.getTotalWorldTime();
        if (!currentPosition.equals(cachedOpenAreaPosition) || currentTick >= openAreaCacheExpiry) {
            cachedOpenAreaPosition = currentPosition;
            cachedOpenArea = BossVisibility.isInOpenArea(creature);
            openAreaCacheExpiry = currentTick + OPEN_AREA_CACHE_TICKS;
        }
        return cachedOpenArea;
    }

    private static int getPhase(WitherStormEntity storm) {
        return storm.getPhase();
    }

    @Nullable
    private BlockPos findNearbyPortal() {
        BlockPos center = new BlockPos(
                Math.floor(creature.posX), Math.floor(creature.posY + 0.5D), Math.floor(creature.posZ));
        if (isNearbyPortal(center, cachedPortal)) return cachedPortal;
        cachedPortal = null;
        long currentTick = creature.world.getTotalWorldTime();
        if (currentTick < nextPortalSearchTick) return null;
        nextPortalSearchTick = currentTick + PORTAL_SEARCH_INTERVAL_TICKS;
        BlockPos minimum = center.add(-16, -4, -16);
        BlockPos maximum = center.add(16, 4, 16);
        for (BlockPos.MutableBlockPos position : BlockPos.getAllInBoxMutable(minimum, maximum)) {
            if (creature.world.isBlockLoaded(position)
                    && creature.world.getBlockState(position).getBlock() == Blocks.PORTAL) {
                cachedPortal = position.toImmutable();
                return cachedPortal;
            }
        }
        return null;
    }

    private boolean isNearbyPortal(BlockPos center, @Nullable BlockPos portal) {
        return portal != null
                && Math.abs(portal.getX() - center.getX()) <= 16
                && Math.abs(portal.getY() - center.getY()) <= 4
                && Math.abs(portal.getZ() - center.getZ()) <= 16
                && creature.world.isBlockLoaded(portal)
                && creature.world.getBlockState(portal).getBlock() == Blocks.PORTAL;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return canEscapeFrom(nearestStorm) && !navigator.noPath();
    }

    @Override
    public void startExecuting() {
        navigator.setPath(escapePath, walkSpeed);
    }

    @Override
    public void updateTask() {
        if (nearestStorm == null) return;
        navigator.setSpeed(creature.getDistanceSq(nearestStorm) < 49.0D ? sprintSpeed : walkSpeed);
    }

    @Override
    public void resetTask() {
        nearestStorm = null;
        escapePath = null;
    }
}
