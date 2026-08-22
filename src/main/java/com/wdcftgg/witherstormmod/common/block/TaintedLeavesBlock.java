package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 1.12 leaf lifecycle with the upstream stick-only loot and fire behavior. */
public final class TaintedLeavesBlock extends BlockLeaves {
    private static final float[] STICK_CHANCES = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    public TaintedLeavesBlock(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(0.2F);
        setResistance(SimpleBlock.toLegacyResistance(0.2F));
        setSoundType(SoundType.PLANT);
        setDefaultState(blockState.getBaseState()
                .withProperty(DECAYABLE, Boolean.TRUE)
                .withProperty(CHECK_DECAY, Boolean.FALSE));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int metadata,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(DECAYABLE, Boolean.FALSE);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState()
                .withProperty(DECAYABLE, (metadata & 4) == 0)
                .withProperty(CHECK_DECAY, (metadata & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int metadata = state.getValue(DECAYABLE) ? 0 : 4;
        return state.getValue(CHECK_DECAY) ? metadata | 8 : metadata;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DECAYABLE, CHECK_DECAY);
    }

    @Override
    public BlockPlanks.EnumType getWoodType(int metadata) {
        return BlockPlanks.EnumType.OAK;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : new Random();
        float chance = STICK_CHANCES[Math.min(Math.max(fortune, 0), STICK_CHANCES.length - 1)];
        if (random.nextFloat() < chance) {
            drops.add(new ItemStack(Items.STICK, 1 + random.nextInt(2)));
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.STICK;
    }

    @Override
    public int quantityDropped(Random random) {
        return random.nextFloat() < STICK_CHANCES[0] ? 1 + random.nextInt(2) : 0;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        float chance = STICK_CHANCES[Math.min(Math.max(fortune, 0), STICK_CHANCES.length - 1)];
        return random.nextFloat() < chance ? 1 + random.nextInt(2) : 0;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return true;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 5;
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
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        IBlockState adjacentState = blockAccess.getBlockState(pos.offset(side));
        return adjacentState.getBlock() != this;
    }
}
