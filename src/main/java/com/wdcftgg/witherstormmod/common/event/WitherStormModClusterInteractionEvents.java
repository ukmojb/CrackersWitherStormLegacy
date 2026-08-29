package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.RegisterWorldInteractionsEvent;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource.DefaultClusterSource;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource.HunchbackClusterSource;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource.NatureClusterSource;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.clustersource.SmallClusterSource;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.pullbehavior.BlockClusterPullBehavior;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.pullbehavior.ItemPullBehavior;
import com.wdcftgg.witherstormmod.common.entity.ai.witherstorm.pullbehavior.SlimePullBehavior;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySlime;


public final class WitherStormModClusterInteractionEvents {
    private WitherStormModClusterInteractionEvents() {
    }

    public static void registerClusterInteractions(RegisterWorldInteractionsEvent event) {
        event.registerPullBehavior(EntitySlime.class, new SlimePullBehavior());
        event.registerPullBehavior(EntityItem.class, new ItemPullBehavior());
        event.registerPullBehavior(SupplementalEntities.BlockClusterEntity.class,
                new BlockClusterPullBehavior());
        event.registerBlockClusterSource(new DefaultClusterSource());
        event.registerBlockClusterSource(new HunchbackClusterSource());
        event.registerBlockClusterSource(new SmallClusterSource());
        event.registerBlockClusterSource(new NatureClusterSource());
    }
}
