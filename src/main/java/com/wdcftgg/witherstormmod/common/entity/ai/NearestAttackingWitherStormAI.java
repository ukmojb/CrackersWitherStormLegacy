package com.wdcftgg.witherstormmod.common.entity.ai;

import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.util.math.AxisAlignedBB;


public final class NearestAttackingWitherStormAI
        extends EntityAINearestAttackableTarget<EntityLivingBase> {
    private final EntityCreature creature;

    public NearestAttackingWitherStormAI(EntityCreature creature, int targetChance) {
        super(creature, EntityLivingBase.class, targetChance, true, false,
                target -> target instanceof WitherStormEntity
                        || target instanceof SupplementalEntities.WitherStormSegmentEntity);
        this.creature = creature;
    }

    @Override
    public boolean shouldExecute() {
        if (!super.shouldExecute()) return false;
        EntityLivingBase storm = targetEntity;
        if (storm == null || getPhase(storm) <= 3 || storm.getDistance(creature) <= 30.0F
                || !isInsideTractorBeam(storm) || creature.onGround) {
            targetEntity = null;
            return false;
        }
        return true;
    }

    @Override
    protected AxisAlignedBB getTargetableArea(double targetDistance) {
        return creature.getEntityBoundingBox().grow(targetDistance, targetDistance * 2.0D, targetDistance);
    }

    @Override
    protected double getTargetDistance() {
        return 100.0D;
    }

    private int getPhase(EntityLivingBase storm) {
        if (storm instanceof WitherStormEntity) return ((WitherStormEntity) storm).getPhase();
        return ((SupplementalEntities.WitherStormSegmentEntity) storm).getPhase();
    }

    private boolean isInsideTractorBeam(EntityLivingBase storm) {
        if (storm instanceof WitherStormEntity) {
            return ((WitherStormEntity) storm).isInsideTractorBeam(creature, 4.0D);
        }
        return ((SupplementalEntities.WitherStormSegmentEntity) storm)
                .isInsideTractorBeam(creature, 4.0D);
    }
}
