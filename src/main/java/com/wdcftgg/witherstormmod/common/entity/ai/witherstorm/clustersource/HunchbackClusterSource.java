package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Random;


public final class HunchbackClusterSource extends BlockClusterSource {
    public HunchbackClusterSource() {
        super(256);
    }

    @Override
    protected boolean shouldScanUpwards(WitherStormEntity storm) {
        if (storm.getRNG().nextInt(2) != 0) return false;
        BlockPos cursor = storm.getPosition();
        for (int y = cursor.getY(); y < storm.world.getActualHeight(); y++) {
            if (!storm.world.isAirBlock(new BlockPos(cursor.getX(), y, cursor.getZ()))) return true;
        }
        return false;
    }

    @Override
    protected void createCluster(WitherStormEntity storm) {
        int count;
        switch (storm.getPhase()) {
            case 1: count = 3; break;
            case 2: count = 9; break;
            case 3: count = 18; break;
            default: count = 1;
        }
        for (int index = 0; index < count; index++) super.createCluster(storm);
    }

    @Override
    protected int calculateShakeTime(WitherStormEntity storm, Random random) {
        if (storm.getConsumedMass() >= storm.adjustAmountForEvolutionSpeed(15000)) return 0;
        if (storm.getConsumedMass() >= storm.adjustAmountForEvolutionSpeed(10000)) {
            return random.nextInt(10);
        }
        return random.nextInt(40);
    }

    @Override
    protected float getClusterSizeRadius(WitherStormEntity storm) {
        return 1.0F;
    }

    @Override
    protected boolean isInvalidInitialStartBlock(WitherStormEntity storm, IBlockState state) {
        return storm.getPhase() == 3
                && UpstreamBlockTags.contains(UpstreamBlockTags.LESS_FAVORABLE_BLOCKS_HUNCH, state)
                && storm.getRNG().nextDouble() <= 0.995D;
    }

    @Override
    protected int getClusterSearchRadius(WitherStormEntity storm) {
        return storm.getHunchbackConsumptionRadius();
    }

    @Override
    protected int getPickupInterval(WitherStormEntity storm) {
        if (WitherStormConfig.constantBlackhole) return 1;
        return Math.max(1, 60 - Math.round(storm.getConsumedMass() * 0.00375F));
    }

    @Override
    protected boolean canUse(WitherStormEntity storm) {
        return storm.getPhase() <= 3;
    }

    @Override
    protected void onClusterAddedToWorld(WitherStormEntity storm,
                                          SupplementalEntities.BlockClusterEntity cluster,
                                          BlockPos startPos, IBlockState startState) {
        SmallClusterSource.playBreakSound(storm, startPos, startState);
    }
}
