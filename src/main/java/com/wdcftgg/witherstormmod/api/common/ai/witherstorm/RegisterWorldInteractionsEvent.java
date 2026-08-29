package com.wdcftgg.witherstormmod.api.common.ai.witherstorm;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior.WitherStormPullBehavior;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;




public class RegisterWorldInteractionsEvent extends Event {

    protected final Map<Class<? extends Entity>, WitherStormPullBehavior<?>> pullBehaviors;
    protected final List<BlockClusterSource> sources;

    public RegisterWorldInteractionsEvent() {
        this.pullBehaviors = new LinkedHashMap<Class<? extends Entity>, WitherStormPullBehavior<?>>();
        this.sources = new ArrayList<BlockClusterSource>();
    }

    public <T extends Entity> void registerPullBehavior(
            Class<? extends T> entityClass, WitherStormPullBehavior<T> behavior) {
        if (pullBehaviors.containsKey(entityClass)) {
            throw new IllegalArgumentException("Type '" + entityClass + "' is already registered");
        }
        pullBehaviors.put(entityClass, behavior);
    }

    public void registerBlockClusterSource(BlockClusterSource source) {
        sources.add(source);
    }
}
