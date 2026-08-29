package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityDragonFireball;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.List;


public class SymbiontDragonFireballEntity extends EntityDragonFireball
        implements IEntityAdditionalSpawnData {
    public SymbiontDragonFireballEntity(World world) {
        super(world);
        getEntityData().setBoolean("CreatedBySymbiont", true);
    }

    public SymbiontDragonFireballEntity(World world, EntityLivingBase shooter,
                                       double accelerationX, double accelerationY, double accelerationZ) {
        super(world, shooter, accelerationX, accelerationY, accelerationZ);
        getEntityData().setBoolean("CreatedBySymbiont", true);
    }


    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeDouble(accelerationX);
        buffer.writeDouble(accelerationY);
        buffer.writeDouble(accelerationZ);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        accelerationX = buffer.readDouble();
        accelerationY = buffer.readDouble();
        accelerationZ = buffer.readDouble();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null && result.entityHit.isEntityEqual(shootingEntity)) return;
        if (world.isRemote) return;

        List<EntityLivingBase> livingEntities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                getEntityBoundingBox().grow(4.0D, 2.0D, 4.0D));
        EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(world, posX, posY, posZ);
        cloud.setOwner(shootingEntity);
        cloud.setParticle(EnumParticleTypes.DRAGON_BREATH);
        cloud.setRadius(2.5F);
        cloud.setDuration(120);
        cloud.addEffect(new PotionEffect(MobEffects.WITHER, 60, 2));
        cloud.setRadiusPerTick((10.0F - cloud.getRadius()) / (float) cloud.getDuration());

        for (EntityLivingBase living : livingEntities) {
            if (getDistanceSq(living) < 16.0D) {
                cloud.setPosition(living.posX, living.posY, living.posZ);
                break;
            }
        }
        world.playEvent(2006, new BlockPos(posX, posY, posZ), 0);
        world.spawnEntity(cloud);
        setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (getEntityData().getBoolean("CreatedBySymbiont")) {
            compound.setBoolean("CreatedBySymbiont", true);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("CreatedBySymbiont", 1)) {
            getEntityData().setBoolean("CreatedBySymbiont", compound.getBoolean("CreatedBySymbiont"));
        }
    }
}
