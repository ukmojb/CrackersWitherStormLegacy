package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.SkyAmbienceManager;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;


@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void witherstormmod$spawnDistantStormWithoutChunk(
            Entity entity, CallbackInfoReturnable<Boolean> callback) {
        World world = (World) (Object) this;
        if (!world.isRemote || !WitherStormClientConfig.distantRenderer
                || !(entity instanceof DistantStormPart) || !entity.forceSpawn) return;
        int chunkX = MathHelper.floor(entity.posX / 16.0D);
        int chunkZ = MathHelper.floor(entity.posZ / 16.0D);
        if (isChunkLoaded(chunkX, chunkZ, false)) return;


        MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, world));
        entity.addedToChunk = false;
        entity.chunkCoordX = chunkX;
        entity.chunkCoordY = MathHelper.clamp(MathHelper.floor(entity.posY / 16.0D), 0, 15);
        entity.chunkCoordZ = chunkZ;
        world.loadedEntityList.add(entity);
        world.onEntityAdded(entity);
        callback.setReturnValue(true);
    }

    @ModifyVariable(method = "unloadEntities(Ljava/util/Collection;)V",
            at = @At("HEAD"), argsOnly = true)
    private Collection<Entity> witherstormmod$keepDistantStormsLoaded(
            Collection<Entity> entities) {
        World world = (World) (Object) this;
        if (!world.isRemote) return entities;
        Collection<Entity> unloadable = new ArrayList<Entity>(entities.size());
        for (Entity entity : entities) {
            if (WitherStormClientConfig.distantRenderer && entity instanceof DistantStormPart) {
                entity.addedToChunk = false;
            } else {
                unloadable.add(entity);
            }
        }
        return unloadable.size() == entities.size() ? entities : unloadable;
    }

    @Inject(method = "updateEntityWithOptionalForce", at = @At("RETURN"))
    private void witherstormmod$tickDetachedDistantStorm(Entity entity, boolean forceUpdate,
                                                         CallbackInfo callback) {
        World world = (World) (Object) this;
        if (!world.isRemote || !WitherStormClientConfig.distantRenderer || !forceUpdate
                || entity.addedToChunk || entity.isDead || !(entity instanceof DistantStormPart)) {
            return;
        }
        ++entity.ticksExisted;
        if (entity.isRiding()) {
            entity.updateRidden();
        } else if (!entity.updateBlocked) {
            entity.onUpdate();
        }
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void witherstormmod$blendStormSkyColor(
            Entity entity, float partialTicks, CallbackInfoReturnable<Vec3d> callback) {
        Vec3d blended = SkyAmbienceManager.INSTANCE.blendSkyColor(
                entity, partialTicks, callback.getReturnValue());
        if (blended != null) callback.setReturnValue(blended);
    }

    @Inject(method = "getCloudColour", at = @At("RETURN"), cancellable = true)
    private void witherstormmod$blendStormCloudColor(
            float partialTicks, CallbackInfoReturnable<Vec3d> callback) {
        Entity viewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        if (viewEntity == null) return;
        Vec3d blended = SkyAmbienceManager.INSTANCE.blendCloudColor(
                viewEntity, partialTicks, callback.getReturnValue());
        if (blended != null) callback.setReturnValue(blended);
    }

    @Inject(method = "getSunBrightness", at = @At("RETURN"), cancellable = true)
    private void witherstormmod$darkenStormSky(
            float partialTicks, CallbackInfoReturnable<Float> callback) {
        World world = (World) (Object) this;
        if (!world.isRemote) return;
        callback.setReturnValue(SkyAmbienceManager.INSTANCE.modifySkyDarken(
                callback.getReturnValue(), 0.0F));
    }
}
