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
    /** 共享的同 tick 活跃风暴索引：多个系统（AI/病化/日晒/强加载）在同一 tick 复用一次全量遍历。 */
    private static World cachedStormIndexWorld;
    private static long cachedStormIndexTick = Long.MIN_VALUE;
    private static List<WitherStormEntity> cachedStormIndex = Collections.emptyList();
    private WorldUtil() {
    }

    /** 返回世界中存活的凋零风暴列表：同一 tick 内全局共享，tick 切换时重建（与上游每 tick 检测一致）。 */
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

    /** 1.12 没有 AxisAlignedBB.getCenter()，这里提供等价计算。 */
    public static Vec3d centerOf(AxisAlignedBB box) {
        return new Vec3d(
                box.minX + (box.maxX - box.minX) / 2.0D,
                box.minY + (box.maxY - box.minY) / 2.0D,
                box.minZ + (box.maxZ - box.minZ) / 2.0D);
    }

    public static double centerYOf(AxisAlignedBB box) {
        return box.minY + (box.maxY - box.minY) / 2.0D;
    }

    /** 判断实体是否位于附近地表高度能够覆盖到的开放区域。 */
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

    /** 判断方块是否有可见外露面，用于风暴光束和坠落碎屑的客户端表现。 */
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

    /** 按上游规则从实体眼位最多检查 300 格方块视线。 */
    public static boolean hasLineOfSight(Entity caster, Entity target) {
        if (caster == null || target == null || caster.world != target.world) return false;
        Vec3d start = caster.getPositionEyes(1.0F);
        Vec3d delta = target.getPositionEyes(1.0F).subtract(start);
        double distance = delta.length();
        Vec3d end = distance > 300.0D ? start.add(delta.scale(300.0D / distance)) : start.add(delta);
        RayTraceResult result = caster.world.rayTraceBlocks(start, end, false, true, false);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    /** 从指定高度向上寻找首个非空气方块；未找到时返回世界高度上限。 */
    public static int getCeilingStartingAt(World world, int startingHeight, int x, int z) {
        int maximumHeight = world.getActualHeight();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x,
                MathHelper.clamp(startingHeight, 0, maximumHeight), z);
        while (position.getY() < maximumHeight && world.isAirBlock(position)) {
            position.setY(position.getY() + 1);
        }
        return position.getY();
    }

    /** 从指定高度向下寻找首个非空气方块；未找到时返回世界底部。 */
    public static int getHeightStartingAt(World world, int startingHeight, int x, int z) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x,
                MathHelper.clamp(startingHeight, 0, world.getActualHeight() - 1), z);
        while (position.getY() > 0 && world.isAirBlock(position)) {
            position.setY(position.getY() - 1);
        }
        return position.getY();
    }

    /** 取得阻挡运动的最高表面，同时忽略树叶，等价于新版的 MOTION_BLOCKING_NO_LEAVES。 */
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

    /** 按上游的立方体外壳顺序向外查找首个匹配方块。 */
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

    /** 保留逐位置搜索顺序，但在服务端只查询一次搜索范围内的已加载区块。 */
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
