package com.wdcftgg.witherstormmod.common.block;

import com.wdcftgg.witherstormmod.common.init.ModCreativeTabs;
import net.minecraft.block.BlockPane;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class TaintedGlassPaneBlock extends BlockPane {

    public TaintedGlassPaneBlock(String name) {
        super(Material.GLASS, false);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(ModCreativeTabs.MAIN);
        setHardness(0.6F);
        setResistance(SimpleBlock.toLegacyResistance(1200.0F));
        setSoundType(SoundType.GLASS);
        setLightOpacity(0);
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public int quantityDropped(Random random) {

        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }
}
