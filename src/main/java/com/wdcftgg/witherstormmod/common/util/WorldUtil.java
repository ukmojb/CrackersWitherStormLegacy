package com.wdcftgg.witherstormmod.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import java.util.function.Predicate;

public final class WorldUtil {

    private static World cachedStormIndexWorld;
    private static long cachedStormIndexTick = Long.MIN_VALUE;
    private static List<WitherStormEntity> cachedStormIndex = Collections.emptyList();
    private WorldUtil() {
    }


    public static List<WitherStormEntity> getCachedStorms(World world) {
        long tick = world.getTotalWorldTime();
        if (cachedStormIndexWorld != world || cachedStormIndexTick != tick) {
            cachedStormIndexWorld = world;
            cachedStormIndexTick = tick;
            List<WitherStormEntity> storms = new ArrayList<WitherStormEntity>();
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof WitherStormEntity && entity.isEntityAlive()) {
                    storms.add((WitherStormEntity) entity);
                }
            }
            cachedStormIndex = storms;
        }
        return cachedStormIndex;
    }


    public static Vec3d centerOf(AxisAlignedBB box) {
        return new Vec3d(
                box.minX + (box.maxX - box.minX) / 2.0D,
                box.minY + (box.maxY - box.minY) / 2.0D,
                box.minZ + (box.maxZ - box.minZ) / 2.0D);
    }

    public static double centerYOf(AxisAlignedBB box) {
        return box.minY + (box.maxY - box.minY) / 2.0D;
    }


    public static boolean isInAnOpenArea(Entity entity) {
        int lowestHeight = Integer.MAX_VALUE;
        BlockPos center = entity.getPosition();
        for (int offsetX = -5; offsetX < 5; offsetX++) {
            for (int offsetZ = -5; offsetZ < 5; offsetZ++) {
                int height = entity.world.getHeight(center.add(offsetX, 0, offsetZ)).getY();
                lowestHeight = Math.min(lowestHeight, height);
            }
        }
        return entity.posY >= lowestHeight - 10.0D;
    }


    public static boolean isBlockExposed(World world, BlockPos position) {
        if (world == null || position == null) return false;
        IBlockState state = world.getBlockState(position);
        if (isAirOrWater(state)) return false;
        for (EnumFacing facing : EnumFacing.values()) {
            if (isAirOrWater(world.getBlockState(position.offset(facing)))) return true;
        }
        return false;
    }

    private static boolean isAirOrWater(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AIR || block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }


    public static boolean hasLineOfSight(Entity caster, Entity target) {
        if (caster == null || target == null || caster.world != target.world) return false;
        Vec3d start = caster.getPositionEyes(1.0F);
        Vec3d delta = target.getPositionEyes(1.0F).subtract(start);
        double distance = delta.length();
        Vec3d end = distance > 300.0D ? start.add(delta.scale(300.0D / distance)) : start.add(delta);
        RayTraceResult result = caster.world.rayTraceBlocks(start, end, false, true, false);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }


    public static int getCeilingStartingAt(World world, int startingHeight, int x, int z) {
        int maximumHeight = world.getActualHeight();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x,
                MathHelper.clamp(startingHeight, 0, maximumHeight), z);
        while (position.getY() < maximumHeight && world.isAirBlock(position)) {
            position.setY(position.getY() + 1);
        }
        return position.getY();
    }


    public static int getHeightStartingAt(World world, int startingHeight, int x, int z) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x,
                MathHelper.clamp(startingHeight, 0, world.getActualHeight() - 1), z);
        while (position.getY() > 0 && world.isAirBlock(position)) {
            position.setY(position.getY() - 1);
        }
        return position.getY();
    }


    public static int getMotionBlockingHeightIgnoringLeaves(World world, int x, int z) {
        int startingHeight = MathHelper.clamp(
                world.getHeight(new BlockPos(x, 0, z)).getY() - 1,
                0, world.getActualHeight() - 1);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x, startingHeight, z);
        while (position.getY() >= 0) {
            IBlockState state = world.getBlockState(position);
            boolean blocksMotion = state.getMaterial().blocksMovement()
                    || state.getMaterial().isLiquid();
            if (blocksMotion && !state.getBlock().isLeaves(state, world, position)) {
                return position.getY() + 1;
            }
            if (position.getY() == 0) break;
            position.setY(position.getY() - 1);
        }
        return 0;
    }


    @Nullable
    public static BlockPos findBlockSpiralOutwards(BlockPos starting, int radius,
                                                    Predicate<BlockPos> predicate) {
        if (predicate.test(starting)) return starting;
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        for (int distance = 0; distance <= radius; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    if (matchesAt(predicate, candidate, starting, x, distance, z)) {
                        return candidate.toImmutable();
                    }
                    if (matchesAt(predicate, candidate, starting, x, -distance, z)) {
                        return candidate.toImmutable();
                    }
                }
            }
            for (int y = -(distance - 1); y <= distance - 1; y++) {
                for (int x = -distance; x <= distance; x++) {
                    if (matchesAt(predicate, candidate, starting, x, y, distance)) {
                        return candidate.toImmutable();
                    }
                    if (matchesAt(predicate, candidate, starting, x, y, -distance)) {
                        return candidate.toImmutable();
                    }
                }
            }
            for (int y = -(distance - 1); y <= distance - 1; y++) {
                for (int z = -(distance - 1); z <= distance - 1; z++) {
                    if (matchesAt(predicate, candidate, starting, distance, y, z)) {
                        return candidate.toImmutable();
                    }
                    if (matchesAt(predicate, candidate, starting, -distance, -y, z)) {
                        return candidate.toImmutable();
                    }
                }
            }
        }
        return null;
    }


    @Nullable
    public static BlockPos findLoadedBlockSpiralOutwards(World world, BlockPos starting, int radius,
                                                          Predicate<IBlockState> predicate) {
        if (!(world instanceof WorldServer)) {
            return findBlockSpiralOutwards(starting, radius,
                    position -> world.isBlockLoaded(position) && predicate.test(world.getBlockState(position)));
        }
        int minimumChunkX = (starting.getX() - radius) >> 4;
        int maximumChunkX = (starting.getX() + radius) >> 4;
        int minimumChunkZ = (starting.getZ() - radius) >> 4;
        int maximumChunkZ = (starting.getZ() + radius) >> 4;
        Chunk[][] loadedChunks = new Chunk[maximumChunkX - minimumChunkX + 1]
                [maximumChunkZ - minimumChunkZ + 1];
        ChunkProviderServer provider = ((WorldServer) world).getChunkProvider();
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                loadedChunks[chunkX - minimumChunkX][chunkZ - minimumChunkZ] =
                        provider.getLoadedChunk(chunkX, chunkZ);
            }
        }
        return findBlockSpiralOutwards(starting, radius, position -> {
            Chunk chunk = loadedChunks[(position.getX() >> 4) - minimumChunkX]
                    [(position.getZ() >> 4) - minimumChunkZ];
            return chunk != null && predicate.test(chunk.getBlockState(position));
        });
    }

    private static boolean matchesAt(Predicate<BlockPos> predicate, BlockPos.MutableBlockPos candidate,
                                     BlockPos origin, int offsetX, int offsetY, int offsetZ) {
        candidate.setPos(origin.getX() + offsetX, origin.getY() + offsetY, origin.getZ() + offsetZ);
        return predicate.test(candidate);
    }
}
