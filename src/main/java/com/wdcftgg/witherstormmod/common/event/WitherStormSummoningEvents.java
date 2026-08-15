package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormSummoningEvents {

    private WitherStormSummoningEvents() {
    }

    @SubscribeEvent
    public static void onSkullPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof World) || event.getWorld().isRemote || event.getPlacedBlock().getBlock() != Blocks.SKULL) {
            return;
        }
        World world = (World) event.getWorld();
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL) return;
        BlockPos placedPosition = event.getPos();
        for (EnumFacing.Axis axis : new EnumFacing.Axis[] {EnumFacing.Axis.X, EnumFacing.Axis.Z}) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos centerSkull = axis == EnumFacing.Axis.X ? placedPosition.add(offset, 0, 0) : placedPosition.add(0, 0, offset);
                if (matchesStructure(world, centerSkull, axis)) {
                    spawnStorm(world, centerSkull, axis);
                    return;
                }
            }
        }
    }

    private static boolean matchesStructure(World world, BlockPos centerSkull, EnumFacing.Axis axis) {
        BlockPos sideOne = axis == EnumFacing.Axis.X ? centerSkull.west() : centerSkull.north();
        BlockPos sideTwo = axis == EnumFacing.Axis.X ? centerSkull.east() : centerSkull.south();
        return isWitherSkull(world, sideOne) && isWitherSkull(world, centerSkull) && isWitherSkull(world, sideTwo)
                && isSummonBase(world, sideOne.down())
                && UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_SUMMON_COMMAND_BLOCKS,
                        world.getBlockState(centerSkull.down()))
                && isSummonBase(world, sideTwo.down())
                && isSummonBase(world, centerSkull.down(2));
    }

    private static boolean isSummonBase(World world, BlockPos position) {
        return UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_SUMMON_BASE_BLOCKS,
                world.getBlockState(position));
    }

    private static boolean isWitherSkull(World world, BlockPos position) {
        if (world.getBlockState(position).getBlock() != Blocks.SKULL) {
            return false;
        }
        TileEntity tileEntity = world.getTileEntity(position);
        return tileEntity instanceof TileEntitySkull && ((TileEntitySkull) tileEntity).getSkullType() == 1;
    }

    private static void spawnStorm(World world, BlockPos centerSkull, EnumFacing.Axis axis) {
        BlockPos sideOne = axis == EnumFacing.Axis.X ? centerSkull.west() : centerSkull.north();
        BlockPos sideTwo = axis == EnumFacing.Axis.X ? centerSkull.east() : centerSkull.south();
        BlockPos[] structure = {sideOne, centerSkull, sideTwo, sideOne.down(), centerSkull.down(), sideTwo.down(), centerSkull.down(2)};
        IBlockState[] removedStates = new IBlockState[structure.length];
        for (int index = 0; index < structure.length; index++) {
            BlockPos position = structure[index];
            removedStates[index] = world.getBlockState(position);
            world.setBlockState(position, Blocks.AIR.getDefaultState(), 2);
            world.playEvent(2001, position, Block.getStateId(removedStates[index]));
        }
        WitherStormEntity storm = new WitherStormEntity(world);
        BlockPos spawnPosition = centerSkull.down(2);
        float yaw = axis == EnumFacing.Axis.X ? 0.0F : 90.0F;
        storm.setLocationAndAngles(spawnPosition.getX() + 0.5D, spawnPosition.getY() + 0.55D,
                spawnPosition.getZ() + 0.5D, yaw, 0.0F);
        storm.initializeStructureSummonYaw(yaw);
        storm.ignite();
        if (ModSounds.get("command_block_activates") != null) {
            world.playSound(null, spawnPosition, ModSounds.get("command_block_activates"),
                    SoundCategory.HOSTILE, 4.0F, 1.0F);
        }
        if (world.spawnEntity(storm)) {
            for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class,
                    storm.getEntityBoundingBox().grow(50.0D))) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger(player, storm);
            }
        }
        for (BlockPos position : structure) {
            world.notifyNeighborsRespectDebug(position, Blocks.AIR, false);
        }
    }
}
