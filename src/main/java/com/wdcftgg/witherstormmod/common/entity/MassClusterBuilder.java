package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


final class MassClusterBuilder {
    private static final ResourceLocation BOWELS_LOOT =
            new ResourceLocation(Tags.MOD_ID, "chests/bowels_general");
    private static final List<WeightedBlock> OUTSIDE_BLOCKS = weightedBlocks(
            weightedBlock("tainted_flesh_block", 20),
            weightedBlock("infected_flesh_block", 3),
            weightedBlock("tainted_planks", 1),
            weightedBlock("tainted_cobblestone", 1));
    private static final List<WeightedBlock> INSIDE_BLOCKS = weightedBlocks(
            weightedBlock("tainted_flesh_block", 20),
            weightedBlock("tainted_sand", 15),
            weightedBlock("tainted_dirt", 10),
            weightedBlock("tainted_cobblestone", 10),
            weightedBlock("tainted_stone", 10),
            weightedBlock("tainted_planks", 8),
            weightedBlock("tainted_log", 5),
            weightedBlock("tainted_leaves", 2),
            weightedBlock("infected_flesh_block", 5));
    private static final List<WeightedBlock> JUNK_BLOCKS = weightedBlocks(
            weightedBlock("tainted_flesh_block", 8),
            weightedBlock("tainted_sand", 5),
            weightedBlock("tainted_sandstone", 3),
            weightedBlock("tainted_dirt", 4),
            weightedBlock("tainted_cobblestone", 6),
            weightedBlock("tainted_stone", 6),
            weightedBlock("tainted_dust_block", 2),
            weightedBlock("tainted_glass", 2),
            weightedBlock("tainted_pumpkin", 1),
            weightedBlock("tainted_zombie_lying", 1),
            weightedBlock("tainted_bone_pile", 1),
            weightedBlock("tainted_wood", 4),
            weightedBlock("tainted_planks", 6),
            weightedBlock("tainted_log", 6),
            weightedBlock("tainted_leaves", 4));
    private static final List<WeightedBlock> DECORATION_BLOCKS = weightedBlocks(
            weightedBlock("tainted_dust", 10),
            weightedBlock("tainted_mushroom", 5),
            weightedBlock("tainted_bone_pile", 1),
            weightedBlock("tainted_zombie_sitting", 1));

    private MassClusterBuilder() {
    }

    static SupplementalEntities.BlockClusterEntity buildLargeDeathCluster(
            World world, Random random, int requestedRadius) {
        int radius = Math.max(1, requestedRadius) + random.nextInt(2);
        Map<BlockPos, IBlockState> states = new LinkedHashMap<BlockPos, IBlockState>();
        for (int lobe = 0; lobe < 4; lobe++) {
            float xStretch = 3.0F * Math.max(0.5F, random.nextFloat());
            float zStretch = 3.0F * Math.max(0.5F, random.nextFloat());
            int horizontalOffsetRadius = radius - 1;
            BlockPos offset = new BlockPos(
                    nextCenteredInt(random, horizontalOffsetRadius),
                    nextCenteredInt(random, radius),
                    nextCenteredInt(random, radius));
            for (int x = -radius; x < radius; x++) {
                for (int y = -radius; y < radius; y++) {
                    for (int z = -radius; z < radius; z++) {
                        int distance = (int) Math.sqrt(x * x * xStretch + 1.0F
                                + y * y * 3.0F + 1.0F + z * z * zStretch + 1.0F);
                        if (distance > radius) continue;
                        List<WeightedBlock> palette = distance < radius - 1
                                ? INSIDE_BLOCKS : OUTSIDE_BLOCKS;
                        int verticalThickness = random.nextInt(2);
                        for (int layer = -verticalThickness; layer <= verticalThickness; layer++) {
                            BlockPos position = new BlockPos(x, y - layer, z).add(offset);
                            if (!states.containsKey(position)) {
                                states.put(position, random.nextFloat() < 0.999F
                                        ? randomState(random, palette) : phlegmState());
                            }
                        }
                    }
                }
            }
        }

        Map<BlockPos, IBlockState> decorations = new LinkedHashMap<BlockPos, IBlockState>();
        for (BlockPos position : states.keySet()) {
            BlockPos above = position.up();
            if (!states.containsKey(above) && random.nextInt(3) == 0) {
                decorations.put(above, randomState(random, DECORATION_BLOCKS));
            }
        }
        states.putAll(decorations);
        return createCluster(world, states);
    }

    static SupplementalEntities.BlockClusterEntity buildSmallDeathCluster(
            World world, Random random, int requestedRadius) {
        int radius = Math.max(1, requestedRadius);
        Map<BlockPos, IBlockState> states = new LinkedHashMap<BlockPos, IBlockState>();
        for (int lobe = 0; lobe < 4; lobe++) {
            BlockPos offset = new BlockPos(random.nextInt(radius) - radius,
                    random.nextInt(radius) - radius, random.nextInt(radius) - radius);
            for (int x = -radius; x < radius; x++) {
                for (int y = -radius; y < radius; y++) {
                    for (int z = -radius; z < radius; z++) {
                        int distance = (int) Math.sqrt(x * x + 1.0F + y * y + 1.0F + z * z + 1.0F);
                        if (distance > radius) continue;
                        BlockPos position = new BlockPos(x, y, z).add(offset);
                        if (!states.containsKey(position)) {
                            states.put(position, random.nextFloat() < 0.975F
                                    ? randomState(random, JUNK_BLOCKS) : phlegmState());
                        }
                    }
                }
            }
        }
        return createCluster(world, states);
    }

    private static SupplementalEntities.BlockClusterEntity createCluster(
            World world, Map<BlockPos, IBlockState> states) {
        SupplementalEntities.BlockClusterEntity cluster =
                new SupplementalEntities.BlockClusterEntity(world);
        cluster.setBlocks(states);
        Block phlegm = ModBlocks.get("withered_phlegm_block");
        for (Map.Entry<BlockPos, IBlockState> entry : states.entrySet()) {
            if (entry.getValue().getBlock() != phlegm) continue;
            BlockPos tilePosition = cluster.getStartPos().add(entry.getKey());
            NBTTagCompound tileData = new NBTTagCompound();
            tileData.setString("id", Tags.MOD_ID + ":withered_phlegm");
            tileData.setInteger("x", tilePosition.getX());
            tileData.setInteger("y", tilePosition.getY());
            tileData.setInteger("z", tilePosition.getZ());
            tileData.setString("LootTable", BOWELS_LOOT.toString());
            cluster.addTileData(tileData);
        }
        return cluster;
    }

    private static int nextCenteredInt(Random random, int radius) {
        return radius <= 0 ? 0 : random.nextInt(radius * 2) - radius;
    }

    private static IBlockState randomState(Random random, List<WeightedBlock> choices) {
        WeightedBlock selected = WeightedRandom.getRandomItem(random, choices);
        Block block = ModBlocks.get(selected.blockName);
        if (block == null) {
            throw new IllegalStateException("Missing mass cluster block " + selected.blockName);
        }
        return block.getDefaultState();
    }

    private static IBlockState phlegmState() {
        Block block = ModBlocks.get("withered_phlegm_block");
        if (block == null) throw new IllegalStateException("Missing withered phlegm block");
        return block.getDefaultState();
    }

    private static WeightedBlock weightedBlock(String blockName, int weight) {
        return new WeightedBlock(blockName, weight);
    }

    private static List<WeightedBlock> weightedBlocks(WeightedBlock... blocks) {
        return new ArrayList<WeightedBlock>(Arrays.asList(blocks));
    }

    private static final class WeightedBlock extends WeightedRandom.Item {
        private final String blockName;

        private WeightedBlock(String blockName, int weight) {
            super(weight);
            this.blockName = blockName;
        }
    }
}
