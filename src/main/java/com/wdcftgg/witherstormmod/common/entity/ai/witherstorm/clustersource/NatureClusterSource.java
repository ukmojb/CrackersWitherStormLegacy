package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Random;


public final class NatureClusterSource extends BlockClusterSource {
    public NatureClusterSource() {
        super(256);
    }

    @Override
    protected boolean shouldntCountToConsumedEntities(WitherStormEntity storm) {
        return true;
    }

    @Override
    protected void createCluster(WitherStormEntity storm) {
        int count;
        switch (storm.getPhase()) {
            case 0: count = 4; break;
            case 1: count = 6; break;
            case 2: count = 8; break;
            default: count = 10;
        }
        for (int index = 0; index < count; index++) super.createCluster(storm);
    }

    @Override
    protected float getClusterSizeRadius(WitherStormEntity storm) {
        return 1.0F;
    }

    @Override
    protected int getClusterSearchRadius(WitherStormEntity storm) {
        if (storm.getPhase() <= 3) return storm.getHunchbackConsumptionRadius() + 12;
        return storm.getEntityConsumptionRadius() * (storm.getPhase() == 7 ? 2 : 1);
    }

    @Override
    protected int calculateShakeTime(WitherStormEntity storm, Random random) {
        switch (storm.getPhase()) {
            case 0: return 20 + random.nextInt(10);
            case 1: return 15 + random.nextInt(10);
            case 2: return 10 + random.nextInt(10);
            case 3: return 5 + random.nextInt(5);
            default: return 0;
        }
    }

    @Override
    protected boolean isValidClusterBlock(WitherStormEntity storm, IBlockState state) {
        return super.isValidClusterBlock(storm, state)
                && UpstreamBlockTags.contains(UpstreamBlockTags.NATURE_CLUSTER_WHITELIST, state);
    }

    @Override
    protected int getPickupInterval(WitherStormEntity storm) {
        if (WitherStormConfig.constantBlackhole) return 1;
        switch (storm.getPhase()) {
            case 0: return 60;
            case 1: return 40;
            case 2: return 20;
            case 3: return 15;
            case 4: return 30;
            case 5: return 24;
            case 6: return 16;
            case 7: return 12;
            default: return 100;
        }
    }

    @Override
    protected boolean canUse(WitherStormEntity storm) {
        return true;
    }

    @Override
    protected void onClusterAddedToWorld(WitherStormEntity storm,
                                          SupplementalEntities.BlockClusterEntity cluster,
                                          BlockPos startPos, IBlockState startState) {
        SmallClusterSource.playBreakSound(storm, startPos, startState);
    }
}
