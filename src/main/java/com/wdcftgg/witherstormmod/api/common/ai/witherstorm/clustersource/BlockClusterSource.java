package com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.WitherStormBlockRules;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Random;

/**
 * 可注册到 {@code WitherStormWorldInteractions} 的方块质量簇来源。
 * 搜索、取块和风暴追踪流程与上游一致，子类只决定各来源的参数和过滤规则。
 */
public abstract class BlockClusterSource {
    protected final int maximumCreationAttempts;

    public BlockClusterSource(int maximumCreationAttempts) {
        this.maximumCreationAttempts = maximumCreationAttempts;
    }

    public void tick(WitherStormEntity storm) {
        if (ForgeEventFactory.getMobGriefingEvent(storm.world, storm)
                && canUse(storm) && storm.ticksExisted % getPickupInterval(storm) == 0) {
            createCluster(storm);
        }
    }

    protected void createCluster(WitherStormEntity storm) {
        createClusterNearby(storm, searchCenter(storm), getClusterSizeRadius(storm),
                getClusterSearchRadius(storm),
                calculateShakeTime(storm, storm.getRNG()), maximumCreationAttempts,
                calculateRotationDelta(storm, storm.getRNG()), shouldScanUpwards(storm),
                shouldntCountToConsumedEntities(storm));
    }

    protected BlockPos searchCenter(WitherStormEntity storm) {
        return new BlockPos(MathHelper.floor(storm.posX),
                Math.min(storm.world.getActualHeight() - 1,
                        MathHelper.floor(storm.posY + storm.getEyeHeight() + 1.0D)),
                MathHelper.floor(storm.posZ));
    }

    protected abstract float getClusterSizeRadius(WitherStormEntity storm);

    protected int getClusterSearchRadius(WitherStormEntity storm) {
        return storm.getEntityConsumptionRadius();
    }

    protected abstract int calculateShakeTime(WitherStormEntity storm, Random random);

    protected Vec2f calculateRotationDelta(WitherStormEntity storm, Random random) {
        float multiplier = storm.getConsumedMass() < storm.adjustAmountForEvolutionSpeed(10000)
                ? 0.125F : 0.75F;
        return new Vec2f((random.nextInt(20) - 10) * multiplier,
                (random.nextInt(20) - 10) * multiplier);
    }

    protected boolean isInvalidInitialStartBlock(WitherStormEntity storm, IBlockState state) {
        return false;
    }

    protected boolean isValidClusterBlock(WitherStormEntity storm, IBlockState state) {
        return WitherStormBlockRules.canConsume(state);
    }

    protected abstract int getPickupInterval(WitherStormEntity storm);

    protected abstract boolean canUse(WitherStormEntity storm);

    protected boolean shouldScanUpwards(WitherStormEntity storm) {
        return false;
    }

    protected boolean shouldntCountToConsumedEntities(WitherStormEntity storm) {
        return shouldNotCountToConsumedMass(storm);
    }

    /**
     * 旧 1.12 适配名保留为桥接，保证已经编译的附属来源仍可工作；新来源应覆写上游同名方法。
     */
    @Deprecated
    protected boolean shouldNotCountToConsumedMass(WitherStormEntity storm) {
        return false;
    }

    protected boolean shouldPullBlocksWithRandomYOffset(WitherStormEntity storm,
                                                         float clusterSizeRadius) {
        return clusterSizeRadius <= 1.0F;
    }

    protected void onClusterAddedToWorld(WitherStormEntity storm,
                                          SupplementalEntities.BlockClusterEntity cluster,
                                          BlockPos startPos, IBlockState startState) {
    }

    protected void createClusterNearby(
            WitherStormEntity storm, BlockPos searchCenter, float clusterSizeRadius,
            int searchRadius, int shakeTime, int maximumAttempts, Vec2f rotationDelta,
            boolean scanUpwards, boolean countToConsumedEntities) {
        Random random = storm.getRNG();
        for (int attempt = 0; attempt < maximumAttempts; attempt++) {
            int randomX = random.nextInt(searchRadius * 2) - searchRadius;
            int randomZ = random.nextInt(searchRadius * 2) - searchRadius;
            if (Math.sqrt(randomX * randomX + randomZ * randomZ) >= searchRadius) continue;

            BlockPos candidate = searchCenter.add(randomX, 0, randomZ);
            if (!storm.world.isBlockLoaded(candidate)) break;
            IBlockState state = storm.world.getBlockState(candidate);
            while (candidate.getY() > 0 && candidate.getY() < storm.world.getActualHeight()
                    && isAirOrWater(storm.world, candidate, state)) {
                candidate = scanUpwards ? candidate.up() : candidate.down();
                state = storm.world.getBlockState(candidate);
            }

            if (!scanUpwards && shouldPullBlocksWithRandomYOffset(storm, clusterSizeRadius)) {
                BlockPos originalCandidate = candidate;
                IBlockState originalState = state;
                int minimumY = candidate.getY();
                int maximumDepthY = Math.max(0, minimumY - 10);
                int randomY = maximumDepthY + random.nextInt(minimumY - maximumDepthY + 1);
                candidate = new BlockPos(candidate.getX(), randomY, candidate.getZ());
                state = storm.world.getBlockState(candidate);
                if (isAirOrWater(storm.world, candidate, state)
                        || !WitherStormBlockRules.canConsume(state)) {
                    candidate = originalCandidate;
                    state = originalState;
                }
            }

            if (isInvalidInitialStartBlock(storm, state)
                    || !WorldUtil.isBlockExposed(storm.world, candidate)
                    || hasNearbySymbiont(storm, candidate)) continue;

            SupplementalEntities.BlockClusterEntity cluster =
                    new SupplementalEntities.BlockClusterEntity(storm.world);
            cluster.populateWithRadius(candidate, clusterSizeRadius,
                    (world, position, blockState) -> isValidClusterBlock(storm, blockState));
            int size = cluster.getBlocks().size();
            if (size <= 0) continue;
            if (size >= 55 && random.nextInt(3) == 0) cluster.setShouldCrumble(true);
            cluster.setTime(50);
            cluster.setShakeTime(shakeTime);
            if (size >= 2) {
                cluster.playSound(ModSounds.get("block_cluster_shake"), 2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            }
            cluster.setRotationDelta(rotationDelta.x, rotationDelta.y);
            cluster.setRotateClockwise(random.nextBoolean());
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setShouldntCountToConsumedEntities(countToConsumedEntities);
            onClusterAddedToWorld(storm, cluster, candidate, state);
            if (storm.world.spawnEntity(cluster)) {
                storm.trackEntityToConsume(cluster);
                return;
            }
            cluster.place();
            return;
        }
    }

    private static boolean hasNearbySymbiont(WitherStormEntity storm, BlockPos position) {
        return !storm.world.getEntitiesWithinAABB(
                SickenedEntities.WitheredSymbiontEntity.class,
                new AxisAlignedBB(position).grow(15.0D),
                EntityLivingBase::isEntityAlive).isEmpty();
    }

    private static boolean isAirOrWater(World world, BlockPos position, IBlockState state) {
        Block block = state.getBlock();
        return block.isAir(state, world, position)
                || block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

}
