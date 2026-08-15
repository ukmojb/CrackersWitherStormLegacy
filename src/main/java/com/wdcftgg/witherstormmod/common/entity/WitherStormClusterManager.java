package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.WitherStormWorldInteractions;
import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.WitherStormBlockRules;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Consumer;

/** 调度上游四类方块簇来源，并统一复用外部 JAR 中的方块标签。 */
final class WitherStormClusterManager {
    private final WitherStormEntity storm;

    WitherStormClusterManager(WitherStormEntity storm) {
        this.storm = storm;
    }

    void tick() {
        if (storm.world.isRemote || storm.isDeadOrPlayingDead()
                || !ForgeEventFactory.getMobGriefingEvent(storm.world, storm)) return;
        for (BlockClusterSource source
                : WitherStormWorldInteractions.getInstance().getClusterSources()) {
            source.tick(storm);
        }
    }

    /** 上游在大型阶段撞上方块时，会从随机碰撞面连续剥离十个方块簇。 */
    void createCollisionClusters() {
        if (storm.getPhase() <= 3) return;
        createCollisionClusters(storm, storm::trackEntityToConsume);
    }

    static void createCollisionClusters(EntityLivingBase source,
                                        Consumer<SupplementalEntities.BlockClusterEntity> tracker) {
        if (source.world.isRemote
                || (!source.collidedHorizontally && !source.collidedVertically)
                || !ForgeEventFactory.getMobGriefingEvent(source.world, source)) return;
        AxisAlignedBB bounds = source.getEntityBoundingBox();
        Random random = source.getRNG();
        for (int index = 0; index < 10; index++) {
            EnumFacing direction = EnumFacing.values()[random.nextInt(EnumFacing.values().length)];
            BlockPos center = getCollisionClusterPosition(bounds, direction, 2.0D, random);
            SupplementalEntities.BlockClusterEntity cluster =
                    new SupplementalEntities.BlockClusterEntity(source.world);
            cluster.populateWithRadius(center, 3.0F + random.nextFloat() * 2.0F,
                    (world, position, state) -> !isBlacklisted(state));
            if (cluster.getBlocks().isEmpty()) continue;
            cluster.setRotationDelta(random.nextInt(20) * 0.05F,
                    random.nextInt(20) * 0.05F);
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setShakeTime(5);
            if (source.world.spawnEntity(cluster)) {
                tracker.accept(cluster);
            } else {
                cluster.place();
            }
        }
    }

    private static BlockPos getCollisionClusterPosition(AxisAlignedBB bounds, EnumFacing direction,
                                                         double outsideOffset, Random random) {
        double x = randomBetween(bounds.minX, bounds.maxX, random);
        double y = randomBetween(bounds.minY, bounds.maxY, random);
        double z = randomBetween(bounds.minZ, bounds.maxZ, random);
        switch (direction.getAxis()) {
            case X:
                x = direction == EnumFacing.WEST ? bounds.minX - outsideOffset
                        : bounds.maxX + outsideOffset;
                break;
            case Y:
                y = direction == EnumFacing.DOWN ? bounds.minY - outsideOffset
                        : bounds.maxY + outsideOffset;
                break;
            case Z:
                z = direction == EnumFacing.NORTH ? bounds.minZ - outsideOffset
                        : bounds.maxZ + outsideOffset;
                break;
            default:
                break;
        }
        return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    private static double randomBetween(double minimum, double maximum, Random random) {
        return minimum + (maximum - minimum) * random.nextDouble();
    }

    @Nullable
    private SupplementalEntities.BlockClusterEntity createCluster(Source source) {
        BlockPos searchCenter = new BlockPos(MathHelper.floor(storm.posX),
                Math.min(storm.world.getActualHeight() - 1,
                        MathHelper.floor(storm.posY + storm.getEyeHeight() + 1.0D)),
                MathHelper.floor(storm.posZ));
        return createCluster(source, storm, searchCenter,
                Math.max(1, getSearchRadius(source)), true);
    }

    @Nullable
    SupplementalEntities.BlockClusterEntity createDefaultClusterForSegment(
            SupplementalEntities.WitherStormSegmentEntity segment) {
        BlockPos searchCenter = new BlockPos(MathHelper.floor(segment.posX),
                Math.min(storm.world.getActualHeight() - 1,
                        MathHelper.floor(segment.posY + segment.getEyeHeight() + 1.0D)),
                MathHelper.floor(segment.posZ));
        return createCluster(Source.DEFAULT, segment, searchCenter, 64, false);
    }

    @Nullable
    private SupplementalEntities.BlockClusterEntity createCluster(Source source,
                                                                  EntityLivingBase clusterOwner,
                                                                  BlockPos searchCenter,
                                                                  int searchRadius,
                                                                  boolean trackForStorm) {
        Random random = clusterOwner.getRNG();
        float clusterRadius = getClusterRadius(source);
        int shakeTime = getShakeTime(source, random);
        float rotationDeltaX = getRotationDelta(source, random);
        float rotationDeltaY = getRotationDelta(source, random);
        boolean scanUpwards = source == Source.HUNCHBACK && shouldScanUpwards();
        for (int attempt = 0; attempt < source.maximumAttempts; attempt++) {
            int randomX = random.nextInt(searchRadius * 2) - searchRadius;
            int randomZ = random.nextInt(searchRadius * 2) - searchRadius;
            if (Math.sqrt(randomX * randomX + randomZ * randomZ) >= searchRadius) continue;
            BlockPos candidate = searchCenter.add(randomX, 0, randomZ);
            if (!storm.world.isBlockLoaded(candidate)) break;
            IBlockState state = storm.world.getBlockState(candidate);
            while (candidate.getY() > 0 && candidate.getY() < storm.world.getActualHeight()
                    && isAirOrWater(state)) {
                candidate = scanUpwards ? candidate.up() : candidate.down();
                state = storm.world.getBlockState(candidate);
            }

            if (!scanUpwards && clusterRadius <= 1.0F) {
                BlockPos originalCandidate = candidate;
                IBlockState originalState = state;
                int minimumY = candidate.getY();
                int maximumDepthY = Math.max(0, minimumY - 10);
                int randomY = maximumDepthY + random.nextInt(minimumY - maximumDepthY + 1);
                candidate = new BlockPos(candidate.getX(), randomY, candidate.getZ());
                state = storm.world.getBlockState(candidate);
                if (isAirOrWater(state) || isBlacklisted(state)) {
                    candidate = originalCandidate;
                    state = originalState;
                }
            }

            if (isInvalidInitialBlock(source, state) || !WorldUtil.isBlockExposed(storm.world, candidate)
                    || hasNearbySymbiont(candidate)) continue;
            final Source selectedSource = source;
            SupplementalEntities.BlockClusterEntity cluster = new SupplementalEntities.BlockClusterEntity(storm.world);
            cluster.populateWithRadius(candidate, clusterRadius,
                    (world, position, blockState) -> isValidClusterBlock(selectedSource, blockState));
            int size = cluster.getBlocks().size();
            if (size <= 0) continue;
            if (size >= 55 && random.nextInt(3) == 0) cluster.setShouldCrumble(true);
            cluster.setTime(50);
            cluster.setShakeTime(shakeTime);
            if (size >= 2) {
                cluster.playSound(ModSounds.get("block_cluster_shake"), 2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            }
            cluster.setRotationDelta(rotationDeltaX, rotationDeltaY);
            cluster.setRotateClockwise(random.nextBoolean());
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setShouldNotCountToConsumedMass(source == Source.NATURE);
            playBreakSound(source, candidate, state);
            if (storm.world.spawnEntity(cluster)) {
                if (trackForStorm) storm.trackEntityToConsume(cluster);
                return cluster;
            }
            return null;
        }
        return null;
    }

    private boolean canUse(Source source) {
        return source == Source.HUNCHBACK ? storm.getPhase() <= 3
                : source != Source.DEFAULT || storm.getPhase() >= 4;
    }

    private int getClusterCount(Source source) {
        int phase = storm.getPhase();
        switch (source) {
            case SMALL:
                if (phase == 4) return 4;
                if (phase == 5) return 5;
                if (phase == 6) return 6;
                if (phase == 7) return 8;
                return 1;
            case NATURE:
                if (phase == 0) return 4;
                if (phase == 1) return 6;
                if (phase == 2) return 8;
                return 10;
            case HUNCHBACK:
                if (phase == 1) return 3;
                if (phase == 2) return 9;
                if (phase == 3) return 18;
                return 1;
            default:
                return 1;
        }
    }

    private int getPickupInterval(Source source) {
        int phase = storm.getPhase();
        switch (source) {
            case DEFAULT:
                if (shouldSpeedUp()) return Math.max(1, WitherStormConfig.devourerClusterPickupInterval * 4);
                return Math.max(1, phase < 6 ? WitherStormConfig.clusterPickupInterval
                        : WitherStormConfig.devourerClusterPickupInterval);
            case SMALL:
                if (WitherStormConfig.constantBlackhole) return 6;
                switch (phase) {
                    case 0:
                    case 1:
                    case 2:
                    case 3: return 64;
                    case 4: return 30;
                    case 5: return 25;
                    case 6: return 20;
                    case 7: return 15;
                    default: return 100;
                }
            case NATURE:
                if (WitherStormConfig.constantBlackhole) return 1;
                switch (phase) {
                    case 0: return 60;
                    case 1: return 40;
                    case 2: return 20;
                    case 3: return 15;
                    case 4: return 30;
                    case 5: return 24;
                    case 6: return 16;
                    case 7: return 12;
                    default: return 100;
                }
            case HUNCHBACK:
                if (WitherStormConfig.constantBlackhole) return 1;
                return Math.max(1, WitherStormConfig.hunchbackClusterPickupInterval
                        - Math.round(storm.getConsumedMass() * 0.00375F));
            default:
                return 100;
        }
    }

    private float getClusterRadius(Source source) {
        return source == Source.DEFAULT
                ? Math.max(1.0F, storm.getClusterRadius() + WitherStormConfig.clusterSizeModifier)
                : 1.0F;
    }

    private int getSearchRadius(Source source) {
        int phase = storm.getPhase();
        int hunchbackRadius = storm.getHunchbackConsumptionRadius();
        int consumptionRadius = storm.getEntityConsumptionRadius();
        switch (source) {
            case SMALL:
                return consumptionRadius * (phase >= 5 ? 2 : 1);
            case NATURE:
                return phase <= 3 ? hunchbackRadius + 12 : consumptionRadius * (phase == 7 ? 2 : 1);
            case HUNCHBACK:
                return hunchbackRadius;
            default:
                return consumptionRadius;
        }
    }

    private int getShakeTime(Source source, Random random) {
        int phase = storm.getPhase();
        switch (source) {
            case DEFAULT:
                return 20 + random.nextInt(10);
            case SMALL:
                return 0;
            case NATURE:
                if (phase == 0) return 20 + random.nextInt(10);
                if (phase == 1) return 15 + random.nextInt(10);
                if (phase == 2) return 10 + random.nextInt(10);
                if (phase == 3) return 5 + random.nextInt(5);
                return 0;
            case HUNCHBACK:
                int fastThreshold = (int) (15000 * storm.getEvolutionSpeedModifier());
                int mediumThreshold = (int) (10000 * storm.getEvolutionSpeedModifier());
                if (storm.getConsumedMass() >= fastThreshold) return 0;
                return storm.getConsumedMass() >= mediumThreshold
                        ? random.nextInt(10) : random.nextInt(40);
            default:
                return 0;
        }
    }

    private float getRotationDelta(Source source, Random random) {
        if (source == Source.DEFAULT) return random.nextInt(20) * 0.05F;
        int fastRotationThreshold = storm.adjustAmountForEvolutionSpeed(10000);
        float multiplier = storm.getConsumedMass() < fastRotationThreshold ? 0.125F : 0.75F;
        return (random.nextInt(20) - 10) * multiplier;
    }

    private boolean shouldSpeedUp() {
        return storm.shouldSpeedUp();
    }

    private boolean shouldScanUpwards() {
        if (storm.getRNG().nextInt(2) != 0) return false;
        BlockPos cursor = storm.getPosition();
        for (int y = cursor.getY(); y < storm.world.getActualHeight(); y++) {
            BlockPos position = new BlockPos(cursor.getX(), y, cursor.getZ());
            if (!storm.world.isAirBlock(position)) return true;
        }
        return false;
    }

    private boolean isInvalidInitialBlock(Source source, IBlockState state) {
        if (source == Source.SMALL) {
            return UpstreamBlockTags.contains(UpstreamBlockTags.LESS_FAVORABLE_BLOCKS, state)
                    && storm.getRNG().nextDouble() <= 0.9D;
        }
        return source == Source.HUNCHBACK && storm.getPhase() == 3
                && UpstreamBlockTags.contains(UpstreamBlockTags.LESS_FAVORABLE_BLOCKS_HUNCH, state)
                && storm.getRNG().nextDouble() <= 0.995D;
    }

    private boolean isValidClusterBlock(Source source, IBlockState state) {
        if (isBlacklisted(state)) return false;
        if (source == Source.SMALL) {
            return !UpstreamBlockTags.contains(UpstreamBlockTags.SMALL_CLUSTER_BLACKLIST, state);
        }
        if (source == Source.NATURE) {
            return UpstreamBlockTags.contains(UpstreamBlockTags.NATURE_CLUSTER_WHITELIST, state);
        }
        return true;
    }

    private static boolean isBlacklisted(IBlockState state) {
        return !WitherStormBlockRules.canConsume(state);
    }

    private boolean hasNearbySymbiont(BlockPos position) {
        return !storm.world.getEntitiesWithinAABB(SickenedEntities.WitheredSymbiontEntity.class,
                new net.minecraft.util.math.AxisAlignedBB(position).grow(15.0D),
                SickenedMobEntity::isEntityAlive).isEmpty();
    }

    private static boolean isAirOrWater(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AIR || block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

    private void playBreakSound(Source source, BlockPos position, IBlockState state) {
        if (source == Source.DEFAULT) return;
        SoundType sound = state.getBlock().getSoundType(state, storm.world, position, null);
        storm.world.playSound(null, position, sound.getBreakSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }

    private enum Source {
        DEFAULT(256),
        SMALL(1024),
        NATURE(256),
        HUNCHBACK(256);

        private final int maximumAttempts;

        Source(int maximumAttempts) {
            this.maximumAttempts = maximumAttempts;
        }
    }
}
