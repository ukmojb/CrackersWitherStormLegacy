package com.wdcftgg.witherstormmod.common.init;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

import javax.annotation.Nullable;


public final class ModDamageSources {
    private static final DamageSource WITHER_SICKNESS =
            new ZeroExhaustionDamageSource("witherSickness")
                    .setDamageBypassesArmor().setDamageIsAbsolute();

    private ModDamageSources() {
    }

    public static DamageSource flamingWitherSkull(Entity projectile, Entity cause) {
        DamageSource source = new EntityDamageSourceIndirect(
                "flamingWitherSkull", projectile, cause).setProjectile();
        return cause instanceof EntityPlayer ? source : source.setDifficultyScaled();
    }

    public static DamageSource witherSickness() {
        return WITHER_SICKNESS;
    }

    public static DamageSource formidibomb(@Nullable Entity cause) {
        DamageSource source = cause == null
                ? new DamageSource("formidibomb")
                : new EntityDamageSource("formidibomb.player", cause);
        return source.setDifficultyScaled().setExplosion();
    }

    public static DamageSource witherStormAttack(EntityLivingBase cause) {
        return new EntityDamageSource("witherStorm", cause).setDifficultyScaled();
    }

    public static DamageSource witherStormAttackMob(EntityLivingBase cause) {
        return new EntityDamageSource("witherStorm", cause)
                .setDifficultyScaled().setDamageBypassesArmor();
    }

    public static DamageSource playerAttackWitherStorm(EntityPlayer cause) {
        return new EntityDamageSource("player", cause).setDamageAllowedInCreativeMode();
    }

    public static DamageSource mobAttackWitherStorm(EntityLivingBase cause) {
        return new EntityDamageSource("mob", cause)
                .setDifficultyScaled().setDamageAllowedInCreativeMode();
    }

    public static DamageSource superTntExplosion() {
        return new DamageSource("super_tnt_explosion").setDifficultyScaled();
    }

    public static DamageSource ironPierced(EntityLivingBase cause) {
        return new EntityDamageSource("ironPierced", cause).setDifficultyScaled()
                .setDamageBypassesArmor().setDamageIsAbsolute();
    }

    private static final class ZeroExhaustionDamageSource extends DamageSource {
        private ZeroExhaustionDamageSource(String damageType) {
            super(damageType);
        }

        @Override
        public float getHungerDamage() {
            return 0.0F;
        }
    }
}
