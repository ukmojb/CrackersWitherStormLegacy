package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PottedTaintedMushroomBlock extends Block {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(
            5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
            11.0D / 16.0D, 6.0D / 16.0D, 11.0D / 16.0D);

    public PottedTaintedMushroomBlock(String name) {
        super(Material.CIRCUITS);
        setRegistryName(name);
        setTranslationKey(name);
        setHardness(0.0F);
        setSoundType(SoundType.STONE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && hasPotSupport(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos) {
        if (!hasPotSupport(world, pos)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    private static boolean hasPotSupport(World world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState support = world.getBlockState(below);
        return support.isTopSolid()
                || support.getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
        drops.add(new ItemStack(Items.FLOWER_POT));
        drops.add(new ItemStack(ModBlocks.get("tainted_mushroom")));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
