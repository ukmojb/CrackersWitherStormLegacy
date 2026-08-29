package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.util.math.Vec3d;


public interface TractorBeamProvider {
    int getTotalHeads();

    boolean tractorBeamActive(int head);

    boolean isDeadOrPlayingDead();

    Vec3d getHeadPositionForBeam(int head);

    default Vec3d getHeadPositionForBeam(int head, float partialTicks) {
        return getHeadPositionForBeam(head);
    }

    Vec3d getHeadDirectionForBeam(int head);

    default Vec3d getHeadDirectionForBeam(int head, float partialTicks) {
        return getHeadDirectionForBeam(head);
    }


    default double getTractorBeamCutoffDistance(int head) {
        return -1.0D;
    }

    default double getTractorBeamCutoffDistance(int head, float partialTicks) {
        return getTractorBeamCutoffDistance(head);
    }
}
