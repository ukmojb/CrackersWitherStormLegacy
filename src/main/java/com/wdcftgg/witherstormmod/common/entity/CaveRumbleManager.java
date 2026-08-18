package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumFacing;

import java.util.Random;

public final class CaveRumbleManager {
    private static final int CAVE_IN_RADIUS = 64;
    private static final int REDSTONE_RADIUS = 16;

    private CaveRumbleManager() {
    }

    public static void trigger(WorldServer world, EntityPlayerMP player,
                               double configuredIntensity, Random random) {
        double intensity = MathHelper.clamp(configuredIntensity, 0.0D, 1.0D);
        double intensityRoot = Math.sqrt(intensity);
        int effectChance = Math.max(0, (int) (10.0D * (1.0D - intensityRoot)));
        int caveInChance = Math.max(0, (int) (12.0D * (1.0D - intensityRoot)));

        ModNetwork.shakePlayer(player, 180.0F, (float) (12.0D * intensityRoot));
        SoundEvent sound = ModSounds.get("earth_rumble");
        if (sound != null) {
            player.connection.sendPacket(new SPacketSoundEffect(sound, SoundCategory.AMBIENT,
                    player.posX, player.posY, player.posZ, 1.0F, 1.0F));
        }

        int ceiling = WorldUtil.getCeilingStartingAt(world, player.getPosition().getY(),
                player.getPosition().getX(), player.getPosition().getZ());
        if (ceiling < world.getActualHeight()) {
            disturbCeiling(world, player, random, effectChance, caveInChance);
        }
        disturbRedstone(world, player, random, effectChance);
    }

    private static void disturbCeiling(WorldServer world, EntityPlayerMP player, Random random,
                                       int effectChance, int caveInChance) {
        BlockPos playerPosition = player.getPosition();
        for (int offsetX = -CAVE_IN_RADIUS; offsetX < CAVE_IN_RADIUS; offsetX++) {
            for (int offsetZ = -CAVE_IN_RADIUS; offsetZ < CAVE_IN_RADIUS; offsetZ++) {
                if (!passesChance(random, effectChance)) continue;
                int x = playerPosition.getX() + offsetX;
                int z = playerPosition.getZ() + offsetZ;
                int y = WorldUtil.getCeilingStartingAt(world, playerPosition.getY() + 4, x, z);
                if (y >= world.getActualHeight()) continue;

                BlockPos ceilingPosition = new BlockPos(x, y, z);
                if (!world.isBlockLoaded(ceilingPosition)) continue;
                IBlockState ceilingState = world.getBlockState(ceilingPosition);
                releaseFallingCeilingBlock(world, ceilingPosition);

                if (passesChance(random, caveInChance)
                        && !UpstreamBlockTags.contains(UpstreamBlockTags.CAVE_IN_BLACKLIST, ceilingState)) {
                    spawnCaveInCluster(world, ceilingPosition, random);
                }
                spawnCeilingParticles(world, ceilingPosition, ceilingState);
            }
        }
    }

    private static void releaseFallingCeilingBlock(WorldServer world, BlockPos ceilingPosition) {
        if (!world.isBlockLoaded(ceilingPosition) || ceilingPosition.getY() <= 0) return;
        IBlockState state = world.getBlockState(ceilingPosition);
        if (!(state.getBlock() instanceof BlockFalling)
                || !BlockFalling.canFallThrough(world.getBlockState(ceilingPosition.down()))) return;
        EntityFallingBlock fallingBlock = new EntityFallingBlock(world,
                ceilingPosition.getX() + 0.5D, ceilingPosition.getY(),
                ceilingPosition.getZ() + 0.5D, state);
        ((BlockFalling) state.getBlock()).onStartFalling(fallingBlock);
        world.spawnEntity(fallingBlock);
    }

    private static void spawnCaveInCluster(WorldServer world, BlockPos position, Random random) {
        SupplementalEntities.BlockClusterEntity cluster =
                new SupplementalEntities.BlockClusterEntity(world);
        cluster.populateWithRadius(position, 1.0F,
                (clusterWorld, clusterPosition, state) ->
                        !UpstreamBlockTags.contains(UpstreamBlockTags.CAVE_IN_BLACKLIST, state));
        if (cluster.getBlocks().isEmpty()) return;
        int rotationDelta = random.nextInt(257) - 128;
        cluster.setTime(50);
        cluster.setShouldCrumble(false);
        cluster.setRotationDelta(rotationDelta * 0.0625F, rotationDelta * 0.0625F);
        cluster.setNoGravity(false);
        cluster.motionX = 0.0D;
        cluster.motionY = random.nextDouble() * 0.25D;
        cluster.motionZ = 0.0D;
        cluster.setPhysics(true);
        world.spawnEntity(cluster);
    }

    private static void spawnCeilingParticles(WorldServer world, BlockPos position, IBlockState state) {
        if (!isBlockExposed(world, position, state)) return;
        int stateId = Block.getStateId(state);
        world.spawnParticle(EnumParticleTypes.BLOCK_DUST,
                position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                2, 0.5D, 0.5D, 0.5D, 0.0D, stateId);
        world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                4, 0.5D, 0.5D, 0.5D, 1.0D, stateId);
    }

    private static boolean isBlockExposed(WorldServer world, BlockPos position, IBlockState state) {
        if (state.getMaterial().isReplaceable()) return false;
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = position.offset(facing);
            if (world.isAirBlock(neighbor) || world.getBlockState(neighbor).getMaterial().isReplaceable()) return true;
        }
        return false;
    }

    private static void disturbRedstone(WorldServer world, EntityPlayerMP player, Random random,
                                        int effectChance) {
        BlockPos center = player.getPosition();
        for (int offsetX = -REDSTONE_RADIUS; offsetX < REDSTONE_RADIUS; offsetX++) {
            for (int offsetY = -REDSTONE_RADIUS; offsetY < REDSTONE_RADIUS; offsetY++) {
                for (int offsetZ = -REDSTONE_RADIUS; offsetZ < REDSTONE_RADIUS; offsetZ++) {
                    if (!passesChance(random, effectChance)) continue;
                    BlockPos position = center.add(offsetX, offsetY, offsetZ);
                    if (position.getY() < 0 || position.getY() >= world.getActualHeight()
                            || !world.isBlockLoaded(position)) continue;
                    disturbBlock(world, position, world.getBlockState(position), random);
                }
            }
        }
    }

    private static void disturbBlock(WorldServer world, BlockPos position, IBlockState state, Random random) {
        Block block = state.getBlock();
        if (block == Blocks.REDSTONE_LAMP && WitherStormConfig.caveRumblesMessWithRedstone) {
            world.setBlockState(position, Blocks.LIT_REDSTONE_LAMP.getDefaultState(), 2);
            world.scheduleUpdate(position, Blocks.LIT_REDSTONE_LAMP, 20 + random.nextInt(10));
        } else if (block instanceof BlockTrapDoor) {
            boolean open = state.getValue(BlockTrapDoor.OPEN);
            boolean bottom = state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.BOTTOM;
            if (open && bottom) {
                world.setBlockState(position, state.withProperty(BlockTrapDoor.OPEN, false), 2);
            } else if (!open && !bottom) {
                world.setBlockState(position, state.withProperty(BlockTrapDoor.OPEN, true), 2);
            }
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.LIT_REDSTONE_ORE) {
            world.setBlockState(position, Blocks.LIT_REDSTONE_ORE.getDefaultState(), 2);
            world.scheduleUpdate(position, Blocks.LIT_REDSTONE_ORE, 10 + random.nextInt(70));
        } else if (block instanceof BlockLever && WitherStormConfig.caveRumblesMessWithRedstone) {
            world.setBlockState(position, state.cycleProperty(BlockLever.POWERED), 3);
        } else if (block instanceof BlockButton && WitherStormConfig.caveRumblesMessWithRedstone) {
            if (!state.getValue(BlockButton.POWERED)) {
                world.setBlockState(position, state.withProperty(BlockButton.POWERED, true), 3);
                world.scheduleUpdate(position, block, block.tickRate(world));
            }
        } else if (block instanceof BlockPressurePlate && WitherStormConfig.caveRumblesMessWithRedstone) {
            world.setBlockState(position, state.withProperty(BlockPressurePlate.POWERED, true), 3);
            world.scheduleUpdate(position, block, random.nextInt(60));
        } else if (block instanceof BlockPressurePlateWeighted
                && WitherStormConfig.caveRumblesMessWithRedstone) {
            world.setBlockState(position, state.withProperty(BlockPressurePlateWeighted.POWER, 15), 3);
            world.scheduleUpdate(position, block, random.nextInt(60));
        } else if (block == Blocks.NOTEBLOCK && WitherStormConfig.caveRumblesMessWithRedstone) {
            TileEntity tileEntity = world.getTileEntity(position);
            if (tileEntity instanceof TileEntityNote) {
                ((TileEntityNote) tileEntity).changePitch();
            }
        }
    }

    private static boolean passesChance(Random random, int chance) {
        return chance < 1 || random.nextInt(chance) == 0;
    }
}
