package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.stats.StatList;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public class SimpleBlock extends Block {

    public SimpleBlock(String name, Material material, float hardness, float resistance) {
        this(name, material, hardness, resistance, SoundType.STONE);
    }

    public SimpleBlock(String name, Material material, float hardness, float resistance, SoundType soundType) {
        super(material);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(hardness);
        setResistance(toLegacyResistance(resistance));
        setSoundType(soundType);
    }


    public static float toLegacyResistance(float modernResistance) {
        return modernResistance * (5.0F / 3.0F);
    }

    @Override
    public int quantityDropped(Random random) {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        if ("tainted_glass".equals(name)) return 0;
        if ("tainted_leaves".equals(name)) return random.nextFloat() < 0.02F ? 1 + random.nextInt(2) : 0;
        return super.quantityDropped(random);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (getRegistryName() != null && "tainted_leaves".equals(getRegistryName().getPath())) {
            float[] chances = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};
            float chance = chances[Math.min(Math.max(fortune, 0), chances.length - 1)];
            return random.nextFloat() < chance ? 1 + random.nextInt(2) : 0;
        }
        return super.quantityDroppedWithBonus(fortune, random);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        if ("tainted_stone".equals(name)) return Item.getItemFromBlock(ModBlocks.get("tainted_cobblestone"));
        if ("tainted_leaves".equals(name)) return Items.STICK;
        return super.getItemDropped(state, random, fortune);
    }

    @Override
    protected boolean canSilkHarvest() {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        return "tainted_glass".equals(name) || "tainted_leaves".equals(name)
                || "tainted_stone".equals(name) || super.canSilkHarvest();
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tileEntity, ItemStack tool) {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        if ("tainted_leaves".equals(name) && tool.getItem() == Items.SHEARS) {
            player.addStat(StatList.getBlockStats(this));
            player.addExhaustion(0.005F);
            if (!world.isRemote) spawnAsEntity(world, pos, new ItemStack(this));
            return;
        }
        super.harvestBlock(world, player, pos, state, tileEntity, tool);
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world,
                                    BlockPos pos, Entity entity) {
        return !(isBossImmuneBlock() && isDestructiveBoss(entity))
                && super.canEntityDestroy(state, world, pos, entity);
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return !hasGuaranteedExplosionDrop() && super.canDropFromExplosion(explosion);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if (hasGuaranteedExplosionDrop() && !world.isRemote) {
            dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
        }
        super.onBlockExploded(world, pos, explosion);
    }

    public static boolean isDestructiveBoss(Entity entity) {
        return entity instanceof EntityWither || entity instanceof EntityDragon;
    }

    private boolean isBossImmuneBlock() {
        return getRegistryName() != null && "hardened_flesh_block".equals(getRegistryName().getPath());
    }

    private boolean hasGuaranteedExplosionDrop() {
        return getRegistryName() != null && "infected_flesh_block".equals(getRegistryName().getPath());
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        return state.getMaterial() != Material.GLASS && !"tainted_leaves".equals(name)
                && !"withered_phlegm_block".equals(name) && super.isOpaqueCube(state);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        return state.getMaterial() != Material.GLASS && !"tainted_leaves".equals(name)
                && !"withered_phlegm_block".equals(name) && super.isFullCube(state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        String name = getRegistryName() == null ? "" : getRegistryName().getPath();
        if ("tainted_glass".equals(name) || "withered_phlegm_block".equals(name)) {
            return BlockRenderLayer.TRANSLUCENT;
        }
        if ("tainted_leaves".equals(name) || "tainted_mushroom".equals(name)) {
            return BlockRenderLayer.CUTOUT;
        }
        return super.getRenderLayer();
    }
}
