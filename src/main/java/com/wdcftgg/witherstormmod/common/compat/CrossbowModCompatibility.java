package com.wdcftgg.witherstormmod.common.compat;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import git.jbredwards.crossbow.api.capability.CapabilityCrossbowAmmo;
import git.jbredwards.crossbow.api.capability.ICrossbowAmmo;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/** Connects the 1.12 Crossbow mod's public ammo API to the upstream pearl behavior. */
public final class CrossbowModCompatibility {
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(Tags.MOD_ID, "crossbow_ender_pearl_ammo");
    private static final ICrossbowAmmo COMMON_ENDER_PEARL_AMMO =
            new EnderPearlCrossbowAmmo();

    private CrossbowModCompatibility() {
    }

    @SubscribeEvent
    public static void attachEnderPearlAmmo(AttachCapabilitiesEvent<ItemStack> event) {
        if (event.getObject().getItem() != Items.ENDER_PEARL) return;
        Object sidedAmmo = WitherStormMod.proxy.createCrossbowEnderPearlAmmo();
        final ICrossbowAmmo ammo = sidedAmmo instanceof ICrossbowAmmo
                ? (ICrossbowAmmo) sidedAmmo : COMMON_ENDER_PEARL_AMMO;
        event.addCapability(CAPABILITY_ID, new ICapabilityProvider() {
            @Override
            public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
                return CapabilityCrossbowAmmo.CAPABILITY != null
                        && capability == CapabilityCrossbowAmmo.CAPABILITY;
            }

            @Override
            @Nullable
            public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
                Capability<ICrossbowAmmo> ammoCapability = CapabilityCrossbowAmmo.CAPABILITY;
                return ammoCapability != null && capability == ammoCapability
                        ? ammoCapability.cast(ammo) : null;
            }
        });
    }
}
