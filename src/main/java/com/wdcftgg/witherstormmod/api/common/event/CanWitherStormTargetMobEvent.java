package com.wdcftgg.witherstormmod.api.common.event;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class CanWitherStormTargetMobEvent extends WitherStormEvent {
    private final EntityLivingBase targetingEntity;
    private final EntityLivingBase potentialTarget;

    public CanWitherStormTargetMobEvent(WitherStormEntity storm, EntityLivingBase potentialTarget) {
        this(storm, storm, potentialTarget);
    }

    public CanWitherStormTargetMobEvent(WitherStormEntity storm,
                                        EntityLivingBase targetingEntity,
                                        EntityLivingBase potentialTarget) {
        super(storm);
        this.targetingEntity = targetingEntity;
        this.potentialTarget = potentialTarget;
    }


    public EntityLivingBase getTargetingEntity() {
        return targetingEntity;
    }

    public EntityLivingBase getPotentialTarget() {
        return potentialTarget;
    }
}
