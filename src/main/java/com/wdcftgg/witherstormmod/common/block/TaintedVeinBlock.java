package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import net.minecraft.block.BlockVine;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraft.block.properties.PropertyBool;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public class TaintedVeinBlock extends BlockVine {

    public TaintedVeinBlock(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(0.4F);
        setResistance(SimpleBlock.toLegacyResistance(0.4F));
        setSoundType(SoundType.SLIME);
        setTickRandomly(false);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {

    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tileEntity, ItemStack tool) {
        if (!world.isRemote) {
            player.addStat(StatList.getBlockStats(this));
            player.addExhaustion(0.005F);
        }
        if (!world.isRemote && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            int connectedFaces = 0;
            for (PropertyBool face : ALL_FACES) {
                if (state.getValue(face)) connectedFaces++;
            }
            if (connectedFaces > 0) {
                spawnAsEntity(world, pos, new ItemStack(this, connectedFaces));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }
}
