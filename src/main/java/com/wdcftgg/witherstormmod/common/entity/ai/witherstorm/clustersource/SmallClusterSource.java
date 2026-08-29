package com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.Random;


public final class SmallClusterSource extends BlockClusterSource {
    public SmallClusterSource() {
        super(1024);
    }

    @Override
    protected void createCluster(WitherStormEntity storm) {
        int count;
        switch (storm.getPhase()) {
            case 4: count = 4; break;
            case 5: count = 5; break;
            case 6: count = 6; break;
            case 7: count = 8; break;
            default: count = 1;
        }
        for (int index = 0; index < count; index++) super.createCluster(storm);
    }

    @Override
    protected float getClusterSizeRadius(WitherStormEntity storm) {
        return 1.0F;
    }

    @Override
    protected int getClusterSearchRadius(WitherStormEntity storm) {
        return storm.getEntityConsumptionRadius() * (storm.getPhase() >= 5 ? 2 : 1);
    }

    @Override
    protected int calculateShakeTime(WitherStormEntity storm, Random random) {
        return 0;
    }

    @Override
    protected boolean isValidClusterBlock(WitherStormEntity storm, IBlockState state) {
        return super.isValidClusterBlock(storm, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.SMALL_CLUSTER_BLACKLIST, state);
    }

    @Override
    protected boolean isInvalidInitialStartBlock(WitherStormEntity storm, IBlockState state) {
        return UpstreamBlockTags.contains(UpstreamBlockTags.LESS_FAVORABLE_BLOCKS, state)
                && storm.getRNG().nextDouble() <= 0.9D;
    }

    @Override
    protected int getPickupInterval(WitherStormEntity storm) {
        if (WitherStormConfig.constantBlackhole) return 6;
        switch (storm.getPhase()) {
            case 0:
            case 1:
            case 2:
            case 3: return 64;
            case 4: return 30;
            case 5: return 25;
            case 6: return 20;
            case 7: return 15;
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
        playBreakSound(storm, startPos, startState);
    }

    static void playBreakSound(WitherStormEntity storm, BlockPos position, IBlockState state) {
        SoundType sound = state.getBlock().getSoundType(state, storm.world, position, null);
        storm.world.playSound(null, position, sound.getBreakSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
}
