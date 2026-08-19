package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TaintedMushroomBlock extends BlockBush {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(
            5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
            11.0D / 16.0D, 6.0D / 16.0D, 11.0D / 16.0D);

    public TaintedMushroomBlock(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(0.0F);
        setSoundType(SoundType.PLANT);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (pos.getY() < 0 || pos.getY() >= 256) return false;
        IBlockState soil = world.getBlockState(pos.down());
        Block soilBlock = soil.getBlock();
        // 上游 NonGrowableMushroomBlock.canSurvive：minecraft:mushroom_grow_block
        // 标签（1.20 = 菌丝/灰化土/两种菌岩；1.12 无菌岩，映射为菌丝+灰化土），
        // 否则要求光照 < 13 且土壤 canSustainPlant。
        if (soilBlock == Blocks.MYCELIUM) return true;
        if (soilBlock == Blocks.DIRT
                && soil.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.PODZOL) return true;
        return world.getLightFromNeighbors(pos) < 13
                && soilBlock.canSustainPlant(soil, world, pos.down(), EnumFacing.UP, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
