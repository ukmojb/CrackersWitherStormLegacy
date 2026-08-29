package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.compat.EnderPearlCrossbowAmmo;
import git.jbredwards.crossbow.api.capability.ICrossbowAmmo;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


public final class CrossbowModClientCompatibility {
    private static final ResourceLocation CROSSBOW_ID =
            new ResourceLocation("crossbow", "crossbow");
    private static final ResourceLocation ENDER_PEARL_MODEL =
            new ResourceLocation(Tags.MOD_ID, "crossbow_mod_ender_pearl");
    private static final ModelResourceLocation BAKED_ENDER_PEARL_MODEL =
            new ModelResourceLocation(ENDER_PEARL_MODEL, "inventory");

    private CrossbowModClientCompatibility() {
    }

    public static Object createAmmo() {
        return new ClientEnderPearlCrossbowAmmo();
    }

    public static void registerModels() {
        ICrossbowAmmo.AMMO_MODELS.add(BAKED_ENDER_PEARL_MODEL);
        Item crossbow = ForgeRegistries.ITEMS.getValue(CROSSBOW_ID);
        if (crossbow != null) {
            ModelBakery.registerItemVariants(crossbow, ENDER_PEARL_MODEL);
        }
    }

    private static final class ClientEnderPearlCrossbowAmmo extends EnderPearlCrossbowAmmo {
        @Override
        public ModelResourceLocation getAmmoModelLocation(
                EntityLivingBase shooter, ItemStack crossbow, ItemStack ammunition) {
            return BAKED_ENDER_PEARL_MODEL;
        }
    }
}
