package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public final class TaintedStatueBlock extends BlockHorizontal {

    private static final float UPSTREAM_HARDNESS = 1.0F;
    private static final float LEGACY_RESISTANCE_FOR_UPSTREAM_SIX = 10.0F;

    public TaintedStatueBlock(String name, StatueMaterial statueMaterial) {
        super(statueMaterial.material);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(UPSTREAM_HARDNESS);
        setResistance(LEGACY_RESISTANCE_FOR_UPSTREAM_SIX);
        setSoundType(statueMaterial.soundType);
        if (statueMaterial == StatueMaterial.TAINTED_ZOMBIE) {
            setDefaultSlipperiness(0.8F);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos position, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int metadata, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(metadata));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos position) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos position) {
        return true;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos position,
                                            EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos position,
                         IBlockState state, int fortune) {

        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        boolean zombie = "tainted_zombie_sitting".equals(name)
                || "tainted_zombie_wall".equals(name) || "tainted_zombie_lying".equals(name);
        Random random = world instanceof World ? ((World) world).rand : new Random();
        for (int roll = 0; roll < 2; roll++) {
            if (zombie) {
                if (random.nextBoolean()) {
                    drops.add(new ItemStack(ModItems.get("withered_flesh"), 1 + random.nextInt(2)));
                } else {
                    drops.add(new ItemStack(ModItems.get("withered_bone")));
                }
            } else if (random.nextBoolean()) {
                drops.add(new ItemStack(ModBlocks.get("tainted_dust")));
            } else {
                drops.add(new ItemStack(ModItems.get("withered_bone"), 1 + random.nextInt(2)));
            }
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    public enum StatueMaterial {
        TAINTED_ZOMBIE(Material.CLAY, SoundType.SLIME),
        TAINTED_BONE(Material.ROCK, SoundType.STONE);

        private final Material material;
        private final SoundType soundType;

        StatueMaterial(Material material, SoundType soundType) {
            this.material = material;
            this.soundType = soundType;
        }
    }
}
