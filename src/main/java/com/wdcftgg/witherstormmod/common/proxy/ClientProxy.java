package com.wdcftgg.witherstormmod.common.proxy;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.ClientEffects;
import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.WitherStormClientEvents;
import com.wdcftgg.witherstormmod.client.render.CommandBlockRenderer;
import com.wdcftgg.witherstormmod.client.render.LegacyRenderBufferer;
import com.wdcftgg.witherstormmod.client.render.WitherStormRenderer;
import com.wdcftgg.witherstormmod.client.render.WitherStormHeadRenderer;
import com.wdcftgg.witherstormmod.client.render.WitherStormSegmentRenderer;
import com.wdcftgg.witherstormmod.client.render.WitherStormPaintingRenderer;
import com.wdcftgg.witherstormmod.client.render.WitherSicknessLayerInstaller;
import com.wdcftgg.witherstormmod.client.render.TaintedSignRenderer;
import com.wdcftgg.witherstormmod.client.render.WitheredPhlegmRenderer;
import com.wdcftgg.witherstormmod.client.particle.PhlegmBlockParticle;
import com.wdcftgg.witherstormmod.client.particle.CommandBlockParticle;
import com.wdcftgg.witherstormmod.client.sound.BossThemeManager;
import com.wdcftgg.witherstormmod.client.gui.WitheredPhlegmScreen;
import com.wdcftgg.witherstormmod.client.gui.SuperBeaconScreen;
import com.wdcftgg.witherstormmod.common.inventory.WitheredPhlegmContainer;
import com.wdcftgg.witherstormmod.common.inventory.SuperBeaconContainer;
import com.wdcftgg.witherstormmod.client.render.SickenedRendererRegistry;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.SymbiontDragonFireballEntity;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.item.SpawnEggItem;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourcePackInstaller;
import com.wdcftgg.witherstormmod.client.resources.WitherStormResourceConfigManager;
import com.wdcftgg.witherstormmod.client.shader.PostProcessingShaders;
import com.wdcftgg.witherstormmod.common.tile.TaintedSignTileEntity;
import com.wdcftgg.witherstormmod.common.tile.WitheredPhlegmTileEntity;
import com.wdcftgg.witherstormmod.common.tile.AbstractSuperBeaconTileEntity;
import com.wdcftgg.witherstormmod.common.tile.SuperBeaconTileEntity;
import com.wdcftgg.witherstormmod.common.tile.SuperSupportBeaconTileEntity;
import com.wdcftgg.witherstormmod.client.render.SuperBeaconRenderer;
import com.wdcftgg.witherstormmod.client.DistantSuperBeaconRenderer;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.block.WitheredPhlegmBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedDustBlock;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelWither;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import com.wdcftgg.witherstormmod.client.render.SickenedMobRenderer;
import com.wdcftgg.witherstormmod.client.render.PowerfulExplosiveRenderer;
import com.wdcftgg.witherstormmod.client.render.FlamingWitherSkullRenderer;
import net.minecraft.client.renderer.entity.RenderDragonFireball;
import net.minecraft.entity.item.EntityPainting;
import com.wdcftgg.witherstormmod.client.render.TentacleSpikeRenderer;
import com.wdcftgg.witherstormmod.client.render.BlockClusterRenderer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.Set;

public class ClientProxy extends CommonProxy {
    private static final String CROSSBOW_CLIENT_COMPATIBILITY =
            "com.wdcftgg.witherstormmod.client.CrossbowModClientCompatibility";
    private Object crossbowEnderPearlAmmo;
    private boolean crossbowAmmoCreationAttempted;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        UpstreamResourcePackInstaller.install();
        PostProcessingShaders.INSTANCE.initialize();
        WitherStormResourceConfigManager.initialize();
        LegacyRenderBufferer.INSTANCE.initialize();
        RenderingRegistry.registerEntityRenderingHandler(WitherStormEntity.class, WitherStormRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(PowerfulExplosiveEntity.SuperTntEntity.class,
                manager -> new PowerfulExplosiveRenderer<PowerfulExplosiveEntity.SuperTntEntity>(manager, "super_tnt"));
        RenderingRegistry.registerEntityRenderingHandler(PowerfulExplosiveEntity.FormidibombEntity.class,
                manager -> new PowerfulExplosiveRenderer<PowerfulExplosiveEntity.FormidibombEntity>(manager, "formidibomb"));
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.FlamingWitherSkullEntity.class,
                manager -> new FlamingWitherSkullRenderer<SupplementalEntities.FlamingWitherSkullEntity>(manager, "flaming_wither_skull"));
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.BlueFlamingWitherSkullEntity.class,
                manager -> new FlamingWitherSkullRenderer<SupplementalEntities.BlueFlamingWitherSkullEntity>(manager, "blue_flaming_wither_skull"));
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.TentacleSpikeEntity.class, TentacleSpikeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.BlockClusterEntity.class, BlockClusterRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(SymbiontDragonFireballEntity.class,
                RenderDragonFireball::new);
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.CommandBlockEntity.class,
                CommandBlockRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.WitherStormHeadEntity.class,
                WitherStormHeadRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(SupplementalEntities.WitherStormSegmentEntity.class,
                WitherStormSegmentRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPainting.class,
                WitherStormPaintingRenderer::new);
        SickenedRendererRegistry.register();
        ClientRegistry.bindTileEntitySpecialRenderer(TaintedSignTileEntity.class, new TaintedSignRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(WitheredPhlegmTileEntity.class, new WitheredPhlegmRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(SuperBeaconTileEntity.class, new SuperBeaconRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(SuperSupportBeaconTileEntity.class, new SuperBeaconRenderer());
        ModelLoader.setCustomStateMapper(ModBlocks.get("tainted_door"), new StateMap.Builder().ignore(BlockDoor.POWERED).build());
        ModelLoader.setCustomStateMapper(ModBlocks.get("tainted_fence_gate"), new StateMap.Builder().ignore(BlockFenceGate.POWERED).build());
        ModelLoader.setCustomStateMapper(ModBlocks.get("tainted_sign"),
                new StateMap.Builder().ignore(BlockStandingSign.ROTATION).build());
        ModelLoader.setCustomStateMapper(ModBlocks.get("tainted_wall_sign"),
                new StateMap.Builder().ignore(BlockWallSign.FACING).build());
        ModelLoader.setCustomStateMapper(ModBlocks.get("withered_phlegm_block"),
                new StateMap.Builder().ignore(WitheredPhlegmBlock.POWERED).build());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        WitherSicknessLayerInstaller.install();
        TaintedDustBlock taintedDust = (TaintedDustBlock) ModBlocks.get("tainted_dust");
        Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
                (state, world, position, tintIndex) -> TaintedDustBlock.getColor(), taintedDust);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> TaintedDustBlock.getColor(), Item.getItemFromBlock(taintedDust));
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item instanceof SpawnEggItem) {
                SpawnEggItem egg = (SpawnEggItem) item;
                Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                        (stack, tintIndex) -> egg.getColor(tintIndex), egg);
            }
        }
    }

    @Override
    public Object createCrossbowEnderPearlAmmo() {
        if (!Loader.isModLoaded("crossbow")) return null;
        if (!crossbowAmmoCreationAttempted) {
            crossbowAmmoCreationAttempted = true;
            try {
                Method factory = Class.forName(CROSSBOW_CLIENT_COMPATIBILITY)
                        .getMethod("createAmmo");
                crossbowEnderPearlAmmo = factory.invoke(null);
            } catch (ReflectiveOperationException | LinkageError exception) {
                WitherStormMod.LOGGER.error(
                        "Unable to create the Crossbow ender pearl ammo model bridge", exception);
            }
        }
        return crossbowEnderPearlAmmo;
    }

    @Override
    public void registerCrossbowModModels() {
        if (!Loader.isModLoaded("crossbow")) return;
        try {
            Class.forName(CROSSBOW_CLIENT_COMPATIBILITY)
                    .getMethod("registerModels")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException(
                    "Unable to register Crossbow ender pearl models", exception);
        }
    }

    @Override
    public void handleShakeScreen(float duration, float power) {
        if (!WitherStormClientConfig.cameraShakeEffects) return;
        Minecraft.getMinecraft().addScheduledTask(() -> ClientEffects.shake(duration, power));
    }

    @Override
    public void handleBlindScreen(int duration, int fadeInDuration, int fadeOutDuration) {
        if (!WitherStormClientConfig.blindingEffects) return;
        Minecraft.getMinecraft().addScheduledTask(
                () -> ClientEffects.blind(duration, fadeInDuration, fadeOutDuration));
    }

    @Override
    public void handleGlobalSound(ResourceLocation sound, float volume, float pitch) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> ClientEffects.playGlobalSound(sound, volume, pitch));
    }

    @Override
    public void handleStopSound(ResourceLocation sound, net.minecraft.util.SoundCategory category) {
        Minecraft.getMinecraft().addScheduledTask(() ->
                Minecraft.getMinecraft().getSoundHandler().stop(sound.toString(), category));
    }

    @Override
    public void handlePhasometerObservation(EnumHand hand, int dimension,
                                            int remainingUseTicks,
                                            NBTTagCompound observation) {
        final NBTTagCompound snapshot = observation == null
                ? new NBTTagCompound() : observation.copy();
        Minecraft.getMinecraft().addScheduledTask(() ->
                PhasometerOverlay.acceptObservation(hand, dimension,
                        remainingUseTicks, snapshot));
    }

    @Override
    public void handleFormidibombExplosion(int sourceEntityId, double x, double y, double z,
                                           int radius, int squish) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> {
                    ClientEffects.spawnFormidibombExplosion(x, y, z);
                    BossThemeManager.INSTANCE.forceStop();
                    World world = Minecraft.getMinecraft().world;
                    if (world == null) return;
                    SoundEvent explosion = ModSounds.get(WitherStormClientConfig.earRingingEffects
                            ? "formidibomb_explosion" : "formidibomb_explosion_quiet");
                    if (explosion != null) {
                        world.playSound(null, x, y, z, explosion, SoundCategory.BLOCKS,
                                16.0F, 1.0F);
                    }
                });
    }

    @Override
    public void spawnWitheredPhlegmParticles(World world, BlockPos pos, boolean powered,
                                             java.util.Random random) {
        PhlegmBlockParticle.spawnForBlock(world, pos, powered, random);
    }

    @Override
    public void spawnPhlegmParticle(World world, double x, double y, double z,
                                    double motionX, double motionY, double motionZ) {
        PhlegmBlockParticle.spawn(world, x, y, z, motionX, motionY, motionZ);
    }

    @Override
    public void spawnSuperBeaconResummonParticle(World world, BlockPos pos,
                                                  java.util.Random random) {
        CommandBlockParticle.spawnForSuperBeacon(world, pos, random);
    }

    @Override
    public void handleSuperBeaconParticles(BlockPos pos, int type) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> CommandBlockParticle.spawnSuperBeaconBurst(pos, type));
    }

    @Override
    public void handleSuperBeaconValidEffects(Set<Potion> effects) {
        final Set<Potion> snapshot = new java.util.HashSet<Potion>(effects);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().currentScreen instanceof SuperBeaconScreen) {
                ((SuperBeaconScreen) Minecraft.getMinecraft().currentScreen)
                        .setValidEffects(snapshot);
            }
        });
    }

    @Override
    public void handleCommandBlockParticles(ModNetwork.CommandBlockParticlesMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> CommandBlockParticle.spawnBurst(
                message.getPosition(), message.getCount(), message.getSpreadX(),
                message.getSpreadY(), message.getSpreadZ(), message.getSpeed(),
                message.getDistribution()));
    }

    @Override
    public void handleCommandBlockTickParticles(ModNetwork.CommandBlockTickParticlesMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            Entity entity = world.getEntityByID(message.getEntityId());
            if (entity instanceof SupplementalEntities.CommandBlockEntity && !entity.isDead) {
                CommandBlockParticle.spawnForCommandBlock(
                        (SupplementalEntities.CommandBlockEntity) entity,
                        message.getParticleSpeed(), message.getLuringPlayerId());
            }
        });
    }

    @Override
    public Object createWitheredPhlegmGui(EntityPlayer player, WitheredPhlegmTileEntity tile) {
        return new WitheredPhlegmScreen(new WitheredPhlegmContainer(player.inventory, tile), tile);
    }

    @Override
    public Object createSuperBeaconGui(EntityPlayer player, AbstractSuperBeaconTileEntity tile) {
        return new SuperBeaconScreen(new SuperBeaconContainer(tile), tile);
    }

    @Override
    public void handleDistantSuperBeacon(ModNetwork.DistantSuperBeaconMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> DistantSuperBeaconRenderer.update(message));
    }

    @Override
    public void handleBossThemeAccess(int entityId, boolean allowed) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> BossThemeManager.INSTANCE.setStormAccess(entityId, allowed));
    }

    @Override
    public void handleCreateDebris(int entityId, boolean hidden) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            Entity entity = world.getEntityByID(entityId);
            if (entity instanceof WitherStormEntity) {
                ((WitherStormEntity) entity).ensureDebrisInitialized(hidden);
            }
        });
    }

    @Override
    public void handleWitherStormLoop(ModNetwork.WitherStormLoopMessage message) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> WitherStormClientEvents.handleWitherStormLoop(message));
    }

    @Override
    public void handleWitherSicknessSync(int entityId, NBTTagCompound data) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            Entity entity = world.getEntityByID(entityId);
            if (!(entity instanceof EntityLivingBase)) return;
            WitherSicknessTracker tracker = WitherSicknessCapability.get((EntityLivingBase) entity);
            if (tracker != null) tracker.read(data);
        });
    }

    @Override
    public void handleDamagingProjectileSync(int entityId, double accelerationX,
                                              double accelerationY, double accelerationZ) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            Entity entity = world.getEntityByID(entityId);
            if (!(entity instanceof EntityFireball)) return;
            EntityFireball projectile = (EntityFireball) entity;
            projectile.accelerationX = accelerationX;
            projectile.accelerationY = accelerationY;
            projectile.accelerationZ = accelerationZ;
        });
    }

    @Override
    public void handleHeadAttacked(int entityId, int head) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            Entity entity = world.getEntityByID(entityId);
            if (entity instanceof WitherStormEntity) {
                ((WitherStormEntity) entity).handleHeadAttackedOnClient(head);
            } else if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
                ((SupplementalEntities.WitherStormSegmentEntity) entity)
                        .handleHeadAttackedOnClient(head);
            }
        });
    }
}
