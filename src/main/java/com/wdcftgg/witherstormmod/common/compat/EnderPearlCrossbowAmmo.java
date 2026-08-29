package com.wdcftgg.witherstormmod.common.compat;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import git.jbredwards.crossbow.api.capability.ICrossbowAmmo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.item.ItemStack;


public class EnderPearlCrossbowAmmo implements ICrossbowAmmo {
    @Override
    public boolean isHeldCrossbowAmmo(
            EntityLivingBase shooter, ItemStack crossbow, ItemStack ammunition) {
        return WitherStormConfig.crossbowsSupportEnderPearls;
    }

    @Override
    public boolean isInventoryCrossbowAmmo(
            EntityLivingBase shooter, ItemStack crossbow, ItemStack ammunition) {
        return false;
    }

    @Override
    public void damageCrossbow(
            EntityLivingBase shooter, ItemStack crossbow, ItemStack ammunition) {
        crossbow.damageItem(WitherStormConfig.crossbowsSupportEnderPearls ? 3 : 1, shooter);
    }

    @Override
    public IProjectile createCrossbowProjectile(
            EntityLivingBase shooter, ItemStack crossbow, ItemStack ammunition) {
        if (!WitherStormConfig.crossbowsSupportEnderPearls) {

            return new EntityTippedArrow(shooter.world, shooter);
        }
        return new EntityEnderPearl(shooter.world, shooter);
    }
}
