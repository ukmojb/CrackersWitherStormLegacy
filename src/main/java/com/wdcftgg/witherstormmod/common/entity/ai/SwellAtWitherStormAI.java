package com.wdcftgg.witherstormmod.common.entity.ai;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.Vec3d;


public final class SwellAtWitherStormAI extends EntityAIBase {
    private final EntityCreeper creeper;
    private WitherStormEntity storm;
    private int head;

    public SwellAtWitherStormAI(EntityCreeper creeper) {
        this.creeper = creeper;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!(creeper.getAttackTarget() instanceof WitherStormEntity)) return false;
        WitherStormEntity candidate = (WitherStormEntity) creeper.getAttackTarget();
        int candidateHead = candidate.findContainingTractorBeamHead(creeper, 4.0D);
        if (candidateHead < 0) return false;
        Vec3d headPosition = candidate.getHeadPositionForBeam(candidateHead);
        if (creeper.getDistanceSq(headPosition.x, headPosition.y, headPosition.z) >= 144.0D
                || creeper.getRNG().nextInt(3) != 0) return false;
        storm = candidate;
        head = candidateHead;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (storm == null || creeper.getAttackTarget() != storm) return false;
        int currentHead = storm.findContainingTractorBeamHead(creeper, 4.0D);
        if (currentHead < 0) return false;
        head = currentHead;
        return true;
    }

    @Override
    public void startExecuting() {
        creeper.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        creeper.setCreeperState(-1);
        storm = null;
        head = -1;
    }

    @Override
    public void updateTask() {
        if (storm == null) {
            creeper.setCreeperState(-1);
            return;
        }
        int currentHead = storm.findContainingTractorBeamHead(creeper, 4.0D);
        if (currentHead < 0) {
            creeper.setCreeperState(-1);
            return;
        }
        head = currentHead;
        Vec3d headPosition = storm.getHeadPositionForBeam(head);
        creeper.setCreeperState(creeper.getDistanceSq(headPosition.x, headPosition.y, headPosition.z)
                > 16.0D ? -1 : 1);
    }
}
