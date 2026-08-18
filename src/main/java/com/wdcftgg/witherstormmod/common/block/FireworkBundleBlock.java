package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import com.wdcftgg.witherstormmod.common.tile.FireworkBundleTileEntity;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.Block;

public class FireworkBundleBlock extends BlockContainer {

    public FireworkBundleBlock(String name) {
        super(Material.TNT);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(2.5F);
        setResistance(SimpleBlock.toLegacyResistance(2.5F));
        setSoundType(SoundType.PLANT);
    }

    @Override public TileEntity createNewTileEntity(World world, int metadata) { return new FireworkBundleTileEntity(); }

    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (world.isBlockPowered(pos)) beginFuse(world, pos);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() != Items.FLINT_AND_STEEL && stack.getItem() != Items.FIRE_CHARGE) return false;
        Item usedItem = stack.getItem();
        beginFuse(world, pos);
        if (!player.capabilities.isCreativeMode) {
            if (stack.getItem() == Items.FLINT_AND_STEEL) stack.damageItem(1, player); else stack.shrink(1);
        }
        player.addStat(StatList.getObjectUseStats(usedItem));
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (world.isBlockPowered(pos)) beginFuse(world, pos);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof FireworkBundleTileEntity
                && ((FireworkBundleTileEntity) tile).isActivated() ? 4 : 0;
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    private static void beginFuse(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof FireworkBundleTileEntity) ((FireworkBundleTileEntity) tile).beginFuse();
    }
}
