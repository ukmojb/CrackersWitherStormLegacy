package com.wdcftgg.witherstormmod.common.entity.ai;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.MathHelper;







public final class SickenedPillagerCrossbowGoal extends EntityAIBase {
    private static final int CHARGE_TICKS = 25;

    private final SickenedEntities.SickenedPillagerEntity pillager;
    private final int attackInterval;
    private final float maximumAttackDistance;
    private int intervalCountdown;
    private int chargeTicks;
    private int seeTime;

    public SickenedPillagerCrossbowGoal(SickenedEntities.SickenedPillagerEntity pillager,
                                        int attackInterval, float maximumAttackDistance) {
        this.pillager = pillager;
        this.attackInterval = attackInterval;
        this.maximumAttackDistance = maximumAttackDistance;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = pillager.getAttackTarget();
        return target != null && target.isEntityAlive() && pillager.isHoldingCrossbow();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute() || !pillager.getNavigator().noPath();
    }

    @Override
    public void startExecuting() {
        pillager.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = pillager.getAttackTarget();
        if (target == null) return;

        boolean canSeeTarget = pillager.getEntitySenses().canSee(target);
        boolean hadSeenTarget = seeTime > 0;
        if (canSeeTarget != hadSeenTarget) seeTime = 0;
        if (canSeeTarget) ++seeTime;
        else --seeTime;

        double squaredDistance = pillager.getDistanceSq(target);
        if (squaredDistance > maximumAttackDistance * maximumAttackDistance && seeTime < 20) {

            pillager.getNavigator().tryMoveToEntityLiving(target, 1.0D);
        } else {
            pillager.getNavigator().clearPath();
        }
        pillager.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

        if (chargeTicks > 0) {
            --chargeTicks;
            pillager.setCharging(true);
            if (chargeTicks == 0) {

                float distanceFactor = MathHelper.clamp(
                        (float) pillager.getDistance(target) / 10.0F, 0.1F, 1.0F);
                pillager.attackEntityWithRangedAttack(target, distanceFactor);
                pillager.setCharging(false);
                intervalCountdown = attackInterval;
            }
            return;
        }
        if (intervalCountdown > 0) {
            --intervalCountdown;
            return;
        }
        if (canSeeTarget && squaredDistance <= maximumAttackDistance * maximumAttackDistance) {
            chargeTicks = CHARGE_TICKS;
            pillager.setCharging(true);
        }
    }

    @Override
    public void resetTask() {
        pillager.setCharging(false);
        chargeTicks = 0;
        intervalCountdown = 0;
        seeTime = 0;
        pillager.getNavigator().clearPath();
    }
}
