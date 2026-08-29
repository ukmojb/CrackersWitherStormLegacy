package com.wdcftgg.witherstormmod.api.common.ai.witherstorm;

import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.clustersource.BlockClusterSource;
import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.pullbehavior.WitherStormPullBehavior;
import com.wdcftgg.witherstormmod.common.event.WitherStormModClusterInteractionEvents;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;




public final class WitherStormWorldInteractions {

    private static WitherStormWorldInteractions instance;

    private final Map<Class<? extends Entity>, WitherStormPullBehavior<?>> pullBehaviors;
    private final List<BlockClusterSource> sources;

    private WitherStormWorldInteractions(
            Map<Class<? extends Entity>, WitherStormPullBehavior<?>> pullBehaviors,
            List<BlockClusterSource> sources) {
        this.pullBehaviors = Collections.unmodifiableMap(
                new LinkedHashMap<Class<? extends Entity>, WitherStormPullBehavior<?>>(pullBehaviors));
        this.sources = Collections.unmodifiableList(
                new ArrayList<BlockClusterSource>(sources));
    }

    public static synchronized void initialize() {
        if (instance != null) {
            throw new IllegalStateException("Cluster interactions have already been initialized!");
        }
        RegisterWorldInteractionsEvent defaults = new RegisterWorldInteractionsEvent();
        WitherStormModClusterInteractionEvents.registerClusterInteractions(defaults);


        RegisterWorldInteractionsEvent external = new RegisterWorldInteractionsEvent();
        MinecraftForge.EVENT_BUS.post(external);
        Map<Class<? extends Entity>, WitherStormPullBehavior<?>> pullBehaviors =
                new LinkedHashMap<Class<? extends Entity>, WitherStormPullBehavior<?>>(defaults.pullBehaviors);
        pullBehaviors.putAll(external.pullBehaviors);
        List<BlockClusterSource> sources = new ArrayList<BlockClusterSource>(defaults.sources);
        sources.addAll(external.sources);
        instance = new WitherStormWorldInteractions(pullBehaviors, sources);
    }

    public static WitherStormWorldInteractions getInstance() {
        return Objects.requireNonNull(instance,
                "Cluster interactions have not been initialized yet");
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> WitherStormPullBehavior<T> getPullBehavior(
            Class<? extends Entity> type) {
        return (WitherStormPullBehavior<T>) Objects.requireNonNull(
                pullBehaviors.get(Objects.requireNonNull(type, "type")),
                "Pull behavior does not exist for entity!");
    }

    public <T extends Entity> WitherStormPullBehavior<T> getPullBehavior(Entity entity) {
        return getPullBehavior(Objects.requireNonNull(entity, "entity").getClass());
    }

    public boolean hasPullBehavior(Class<? extends Entity> type) {
        return type != null && pullBehaviors.containsKey(type);
    }

    public boolean hasPullBehavior(Entity entity) {
        return entity != null && hasPullBehavior(entity.getClass());
    }

    public List<BlockClusterSource> getClusterSources() {
        return sources;
    }
}
