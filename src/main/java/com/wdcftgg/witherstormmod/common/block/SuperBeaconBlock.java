package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import com.wdcftgg.witherstormmod.common.init.ModStats;
import com.wdcftgg.witherstormmod.common.tile.SuperBeaconTileEntity;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.gui.ModGuiHandler;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SuperBeaconBlock extends BlockContainer {

    public SuperBeaconBlock(String name) {
        super(Material.GLASS);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(3.0F);
        setResistance(SimpleBlock.toLegacyResistance(3.0F));
        setLightLevel(12.0F / 15.0F);
        setSoundType(SoundType.GLASS);
        setLightOpacity(0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new SuperBeaconTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos position, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(position);
        if (!(tileEntity instanceof SuperBeaconTileEntity)) {
            return false;
        }
        SuperBeaconTileEntity beacon = (SuperBeaconTileEntity) tileEntity;
        if (beacon.isDoingResummonAnimation()) {
            return true;
        }
        if (!beacon.canPlayerUseItems(player)) return true;
        ItemStack heldItem = player.getHeldItem(hand);
        if (!player.isSneaking() && heldItem.isEmpty() && !beacon.isEmpty()) {
            ItemStack returned = beacon.takeItem();
            if (!returned.isEmpty() && !player.addItemStackToInventory(returned)) {
                player.dropItem(returned, false);
            }
        } else if (!heldItem.isEmpty()) {
            ItemStack inserted = heldItem.copy();
            inserted.setCount(1);
            if (beacon.addItem(inserted)) {
                if (!player.capabilities.isCreativeMode) heldItem.shrink(1);
                world.playSound(null, position, SoundEvents.ENTITY_ITEM_PICKUP,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            player.openGui(WitherStormMod.INSTANCE, ModGuiHandler.SUPER_BEACON, world,
                    position.getX(), position.getY(), position.getZ());
            player.addStat(ModStats.INTERACT_WITH_SUPER_BEACON);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos position, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        if (!stack.hasDisplayName()) return;
        TileEntity tile = world.getTileEntity(position);
        if (tile instanceof SuperBeaconTileEntity) {
            ((SuperBeaconTileEntity) tile).setCustomName(stack.getDisplayName());
        }
    }

    @Override
    public void breakBlock(World world, BlockPos position, IBlockState state) {
        TileEntity tile = world.getTileEntity(position);
        if (tile instanceof SuperBeaconTileEntity) InventoryHelper.dropInventoryItems(world, position, (SuperBeaconTileEntity) tile);
        super.breakBlock(world, position, state);
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world,
                                    BlockPos pos, Entity entity) {
        return !SimpleBlock.isDestructiveBoss(entity)
                && super.canEntityDestroy(state, world, pos, entity);
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if (!world.isRemote) dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}
