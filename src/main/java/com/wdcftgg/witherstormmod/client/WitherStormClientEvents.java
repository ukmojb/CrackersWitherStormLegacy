package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModEffects;
import com.wdcftgg.witherstormmod.client.sound.WitherStormLoopSound;
import com.wdcftgg.witherstormmod.client.sound.WitherStormTrembleSound;
import com.wdcftgg.witherstormmod.client.sound.BowelsLoopSound;
import com.wdcftgg.witherstormmod.client.sound.BossThemeManager;
import com.wdcftgg.witherstormmod.client.sound.CommandBlockLoopSound;
import com.wdcftgg.witherstormmod.client.sound.FormidiBladeChargeSound;
import com.wdcftgg.witherstormmod.client.sound.FormidibombFuseSound;
import com.wdcftgg.witherstormmod.client.sound.TractorBeamLoopSound;
import com.wdcftgg.witherstormmod.client.sound.WitheredSymbiontSpellLoopSound;
import com.wdcftgg.witherstormmod.client.sound.WitheredSymbiontHeartbeatSound;
import com.wdcftgg.witherstormmod.client.util.FormidiBladeAnimationHelper;
import com.wdcftgg.witherstormmod.client.shader.PostProcessingShaders;
import com.wdcftgg.witherstormmod.client.particle.CommandBlockParticle;
import com.wdcftgg.witherstormmod.client.particle.PhlegmBlockParticle;
import com.wdcftgg.witherstormmod.client.particle.TractorBeamParticle;
import com.wdcftgg.witherstormmod.client.render.SuperBeaconRenderer;
import com.wdcftgg.witherstormmod.client.render.TractorBeamRenderer;
import com.wdcftgg.witherstormmod.client.render.WitherStormRenderer;
import com.wdcftgg.witherstormmod.client.render.DistantProjection;
import com.wdcftgg.witherstormmod.client.render.DistantStormRenderTracker;
import com.wdcftgg.witherstormmod.client.render.LegacyRenderBufferer;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.SymbiontDragonFireballEntity;
import com.wdcftgg.witherstormmod.common.entity.FormidibombSource;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.BossThemeProvider;
import com.wdcftgg.witherstormmod.common.entity.TractorBeamProvider;
import com.wdcftgg.witherstormmod.common.item.FormidiBladeItem;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.tile.FormidibombTileEntity;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class WitherStormClientEvents {
    private static final Map<Integer, WitherStormLoopSound> WITHER_STORM_LOOPS =
            new HashMap<Integer, WitherStormLoopSound>();
    private static final Map<Integer, String> WITHER_STORM_LOOP_NAMES = new HashMap<Integer, String>();
    private static final Map<Integer, Integer> WITHER_STORM_LOOP_UPDATES =
            new HashMap<Integer, Integer>();
    private static final Set<Integer> WITHER_STORM_LOOP_DISABLED = new HashSet<Integer>();
    private static final Map<Integer, WitherStormTrembleSound> TREMBLE_LOOPS =
            new HashMap<Integer, WitherStormTrembleSound>();
    private static final Map<FormidibombSource, FormidibombFuseSound> FORMIDIBOMB_LOOPS =
            new IdentityHashMap<FormidibombSource, FormidibombFuseSound>();
    private static final Map<SupplementalEntities.CommandBlockEntity, CommandBlockLoopSound> COMMAND_BLOCK_LOOPS =
            new IdentityHashMap<SupplementalEntities.CommandBlockEntity, CommandBlockLoopSound>();
    private static final Map<Entity, TractorBeamLoopSound[]> TRACTOR_BEAM_LOOPS =
            new IdentityHashMap<Entity, TractorBeamLoopSound[]>();
    private static final Map<EntityPlayer, FormidiBladeChargeSound> FORMIDI_BLADE_LOOPS =
            new IdentityHashMap<EntityPlayer, FormidiBladeChargeSound>();
    private static final Map<SickenedEntities.WitheredSymbiontEntity, WitheredSymbiontSpellLoopSound>
            SYMBIONT_SPELL_LOOPS =
            new IdentityHashMap<SickenedEntities.WitheredSymbiontEntity, WitheredSymbiontSpellLoopSound>();
    private static final Map<SickenedEntities.WitheredSymbiontEntity, WitheredSymbiontHeartbeatSound>
            SYMBIONT_HEARTBEAT_LOOPS =
            new IdentityHashMap<SickenedEntities.WitheredSymbiontEntity, WitheredSymbiontHeartbeatSound>();
    private static BowelsLoopSound bowelsLoop;
    private static int bowelsMoodDelay;
    private static int bowelsTrembleTimer = -1;
    private static int bowelsScreamTimer = -1;
    private static final Random BOWELS_RANDOM = new Random();
    private static int witherStormLoopTick;
    private static World witherStormLoopWorld;
    private static boolean creativeStackModelsAudited;
    private static boolean optifineWarningShown;
    private static final Random WITHER_SICKNESS_HEALTH_RANDOM = new Random();
    private static final ResourceLocation WITHER_SICKNESS_ICONS =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/wither_sickness.png");
    private static int sicknessLastHealth;
    private static int sicknessDisplayHealth;
    private static long sicknessLastHealthTime;
    private static long sicknessHealthBlinkTime;
    private static RenderWorldLastEvent lastWorldEffectsEvent;
    private static int lastWorldEffectsDiagnosticTick = Integer.MIN_VALUE;

    private WitherStormClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModBlocks.registerModels();
        ModItems.registerModels();
        PhasometerModelCompatibility.registerModels();
        WitherStormMod.proxy.registerCrossbowModModels();
        WitherStormMod.LOGGER.info("Registered all legacy block and item models during ModelRegistryEvent");
    }

    @SubscribeEvent
    public static void registerParticleSprites(TextureStitchEvent.Pre event) {
        CommandBlockParticle.registerSprites(event.getMap());
        PhlegmBlockParticle.registerSprite(event.getMap());
        TractorBeamParticle.registerSprite(event.getMap());
        SuperBeaconRenderer.registerSprites(event.getMap());
    }

    @SubscribeEvent
    public static void auditBakedModels(ModelBakeEvent event) {
        PhasometerModelCompatibility.bakeModels(event);
        IBakedModel missing = event.getModelManager().getMissingModel();
        int checked = 0;
        int failed = 0;
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation registryName = item.getRegistryName();
            if (registryName == null || !Tags.MOD_ID.equals(registryName.getNamespace())) continue;
            checked++;
            ModelResourceLocation location = new ModelResourceLocation(registryName, "inventory");
            IBakedModel model = event.getModelRegistry().getObject(location);
            List<String> missingTextures = findMissingQuadTextures(model);
            if (model == null || model == missing || !missingTextures.isEmpty()) {
                failed++;
                WitherStormMod.LOGGER.error("Item model audit failed: item={}, model={}, baked={}, missingTextures={}",
                        registryName, location, model == null ? "null" : model.getClass().getName(), missingTextures);
            }
        }
        if (failed == 0) {
            WitherStormMod.LOGGER.info("Item model audit passed for all {} registered items", checked);
        } else {
            WitherStormMod.LOGGER.error("Item model audit found {} failures among {} registered items", failed, checked);
        }
    }

    private static List<String> findMissingQuadTextures(IBakedModel model) {
        List<String> missingTextures = new ArrayList<String>();
        if (model == null) return missingTextures;
        collectMissingQuadTextures(model.getQuads(null, null, 0L), missingTextures);
        for (EnumFacing facing : EnumFacing.values()) {
            collectMissingQuadTextures(model.getQuads(null, facing, 0L), missingTextures);
        }
        return missingTextures;
    }

    private static void collectMissingQuadTextures(List<BakedQuad> quads, List<String> missingTextures) {
        for (BakedQuad quad : quads) {
            String texture = quad.getSprite() == null ? "null" : quad.getSprite().getIconName();
            if (("missingno".equals(texture) || texture.endsWith(":missingno"))
                    && !missingTextures.contains(texture)) {
                missingTextures.add(texture);
            }
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            LegacyRenderBufferer.INSTANCE.tick(minecraft.world);
            ClientEffects.tick(minecraft);
            SkyAmbienceManager.INSTANCE.tick(minecraft);
            return;
        }
        if (event.phase != TickEvent.Phase.END) return;
        BossThemeManager.INSTANCE.tick(minecraft);
        AmuletAnimationHelper.tick(minecraft);
        TractorBeamOverlay.tick(minecraft);
        PhasometerOverlay.tick(minecraft);
        if (!creativeStackModelsAudited) {
            auditCreativeStackModels(minecraft);
            creativeStackModelsAudited = true;
        }
        if (minecraft.world == null || minecraft.player == null) {
            for (WitherStormLoopSound witherStormLoop : WITHER_STORM_LOOPS.values()) {
                witherStormLoop.stopImmediately();
            }
            WITHER_STORM_LOOPS.clear();
            WITHER_STORM_LOOP_NAMES.clear();
            WITHER_STORM_LOOP_UPDATES.clear();
            WITHER_STORM_LOOP_DISABLED.clear();
            witherStormLoopTick = 0;
            witherStormLoopWorld = null;
            if (bowelsLoop != null) bowelsLoop.stop();
            for (WitherStormTrembleSound trembleLoop : TREMBLE_LOOPS.values()) {
                trembleLoop.stopImmediately();
            }
            TREMBLE_LOOPS.clear();
            for (FormidibombFuseSound formidibombLoop : FORMIDIBOMB_LOOPS.values()) {
                formidibombLoop.stop();
            }
            FORMIDIBOMB_LOOPS.clear();
            for (CommandBlockLoopSound commandBlockLoop : COMMAND_BLOCK_LOOPS.values()) {
                commandBlockLoop.stop();
            }
            COMMAND_BLOCK_LOOPS.clear();
            for (TractorBeamLoopSound[] beamLoops : TRACTOR_BEAM_LOOPS.values()) {
                stopTractorBeamLoops(beamLoops);
            }
            TRACTOR_BEAM_LOOPS.clear();
            for (FormidiBladeChargeSound loop : FORMIDI_BLADE_LOOPS.values()) loop.stopImmediately();
            FORMIDI_BLADE_LOOPS.clear();
            for (WitheredSymbiontSpellLoopSound loop : SYMBIONT_SPELL_LOOPS.values()) {
                loop.stopImmediately();
            }
            SYMBIONT_SPELL_LOOPS.clear();
            for (WitheredSymbiontHeartbeatSound loop : SYMBIONT_HEARTBEAT_LOOPS.values()) {
                loop.stopImmediately();
            }
            SYMBIONT_HEARTBEAT_LOOPS.clear();
            bowelsLoop = null;
            bowelsMoodDelay = 0;
            PostProcessingShaders.INSTANCE.setSource(null);
            return;
        }
        if (witherStormLoopWorld != minecraft.world) {
            for (WitherStormLoopSound loop : WITHER_STORM_LOOPS.values()) loop.stopImmediately();
            WITHER_STORM_LOOPS.clear();
            WITHER_STORM_LOOP_NAMES.clear();
            WITHER_STORM_LOOP_UPDATES.clear();
            WITHER_STORM_LOOP_DISABLED.clear();
            witherStormLoopWorld = minecraft.world;
        }
        showOptifineWarning(minecraft);
        ++witherStormLoopTick;
        updateBowelsAmbience(minecraft);
        updateFormidibombLoops(minecraft);
        updateCommandBlockLoops(minecraft);
        updateTractorBeamLoops(minecraft);
        // 粒子是每客户端 tick 补充的短生命周期对象；暂停时游戏 tick 冻结、
        // EffectRenderer 不再老化粒子，但 ClientTickEvent.END 仍会推进，若继续
        // 召唤会在暂停期间无限堆积，恢复后同帧爆发。原版粒子走服务端 tick 网络包，
        // 暂停时服务端不 tick、不发包，因此没有这种堆积。这里对齐该行为。
        if (!minecraft.isGamePaused()) {
            spawnFormidibombParticles(minecraft);
            spawnSymbiontDragonFireballParticles(minecraft);
            spawnWitherStormParticles(minecraft);
            spawnTractorBeamParticles(minecraft);
            spawnCommandBlockItemParticles(minecraft);
        }
        updateFormidiBladeChargeSound(minecraft);
        updateSymbiontSpellLoops(minecraft);
        updateSymbiontHeartbeatLoops(minecraft);
        tickWitherSicknessTrackers(minecraft);
        updateWitherStormTrembleLoops(minecraft);
        updateWitherStormLoops(minecraft);
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LegacyRenderBufferer.INSTANCE.beginRenderFrame();
        }
    }

    /** 对应上游 SoundManagersRefresher：停止并清空全部循环音效，下一帧自动重建。 */
    public static void refreshAllLoopSounds() {
        for (WitherStormLoopSound loop : WITHER_STORM_LOOPS.values()) loop.stopImmediately();
        WITHER_STORM_LOOPS.clear();
        WITHER_STORM_LOOP_NAMES.clear();
        WITHER_STORM_LOOP_UPDATES.clear();
        WITHER_STORM_LOOP_DISABLED.clear();
        witherStormLoopTick = 0;
        for (WitherStormTrembleSound tremble : TREMBLE_LOOPS.values()) tremble.stopImmediately();
        TREMBLE_LOOPS.clear();
        for (FormidibombFuseSound loop : FORMIDIBOMB_LOOPS.values()) loop.stop();
        FORMIDIBOMB_LOOPS.clear();
        for (CommandBlockLoopSound loop : COMMAND_BLOCK_LOOPS.values()) loop.stop();
        COMMAND_BLOCK_LOOPS.clear();
        for (TractorBeamLoopSound[] loops : TRACTOR_BEAM_LOOPS.values()) {
            stopTractorBeamLoops(loops);
        }
        TRACTOR_BEAM_LOOPS.clear();
        for (FormidiBladeChargeSound loop : FORMIDI_BLADE_LOOPS.values()) loop.stopImmediately();
        FORMIDI_BLADE_LOOPS.clear();
        for (WitheredSymbiontSpellLoopSound loop : SYMBIONT_SPELL_LOOPS.values()) {
            loop.stopImmediately();
        }
        SYMBIONT_SPELL_LOOPS.clear();
        for (WitheredSymbiontHeartbeatSound loop : SYMBIONT_HEARTBEAT_LOOPS.values()) {
            loop.stopImmediately();
        }
        SYMBIONT_HEARTBEAT_LOOPS.clear();
        if (bowelsLoop != null) bowelsLoop.stop();
        bowelsLoop = null;
    }

    private static void updateFormidiBladeChargeSound(Minecraft minecraft) {
        SoundEvent sound = ModSounds.get("formidibomb_pulse_loop");
        for (EntityPlayer player : minecraft.world.playerEntities) {
            FormidiBladeChargeSound loop = FORMIDI_BLADE_LOOPS.get(player);
            if (FormidiBladeChargeSound.getPower(player) > 0.05F && sound != null
                    && (loop == null || loop.isDonePlaying())) {
                loop = new FormidiBladeChargeSound(player, sound);
                FORMIDI_BLADE_LOOPS.put(player, loop);
                minecraft.getSoundHandler().playSound(loop);
            }
        }
        Iterator<Map.Entry<EntityPlayer, FormidiBladeChargeSound>> iterator =
                FORMIDI_BLADE_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityPlayer, FormidiBladeChargeSound> entry = iterator.next();
            if (entry.getKey().isDead || entry.getKey().world != minecraft.world) {
                entry.getValue().stopImmediately();
                iterator.remove();
            } else if (FormidiBladeChargeSound.getPower(entry.getKey()) <= 0.05F) {
                entry.getValue().stop();
                iterator.remove();
            } else if (entry.getValue().isDonePlaying()) {
                iterator.remove();
            }
        }
    }

    private static void updateSymbiontSpellLoops(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof SickenedEntities.WitheredSymbiontEntity) || entity.isDead) {
                continue;
            }
            SickenedEntities.WitheredSymbiontEntity symbiont =
                    (SickenedEntities.WitheredSymbiontEntity) entity;
            WitheredSymbiontSpellLoopSound loop = SYMBIONT_SPELL_LOOPS.get(symbiont);
            SpellType spell = symbiont.getSpell();
            SoundEvent loopSound = spell.spellLoop().isPresent() ? spell.spellLoop().get().get() : null;
            if (symbiont.isCastingSpell() && loopSound != null) {
                if (loop == null || loop.isDonePlaying() || !loop.matches(spell)) {
                    if (loop != null) loop.stop();
                    loop = new WitheredSymbiontSpellLoopSound(symbiont, loopSound);
                    SYMBIONT_SPELL_LOOPS.put(symbiont, loop);
                    minecraft.getSoundHandler().playSound(loop);
                }
            } else if (loop != null) {
                loop.stop();
                SYMBIONT_SPELL_LOOPS.remove(symbiont);
            }
        }
        java.util.Iterator<java.util.Map.Entry<SickenedEntities.WitheredSymbiontEntity,
                WitheredSymbiontSpellLoopSound>> iterator = SYMBIONT_SPELL_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<SickenedEntities.WitheredSymbiontEntity,
                    WitheredSymbiontSpellLoopSound> entry = iterator.next();
            if (entry.getKey().isDead) entry.getValue().stopImmediately();
            if (entry.getValue().isDonePlaying()) {
                iterator.remove();
            }
        }
    }

    private static void updateSymbiontHeartbeatLoops(Minecraft minecraft) {
        SoundEvent heartbeat = ModSounds.get("withered_symbiont_heart_beat");
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof SickenedEntities.WitheredSymbiontEntity) || entity.isDead) continue;
            SickenedEntities.WitheredSymbiontEntity symbiont =
                    (SickenedEntities.WitheredSymbiontEntity) entity;
            WitheredSymbiontHeartbeatSound loop = SYMBIONT_HEARTBEAT_LOOPS.get(symbiont);
            if (symbiont.isVulnerable() && heartbeat != null
                    && (loop == null || loop.isDonePlaying())) {
                loop = new WitheredSymbiontHeartbeatSound(symbiont, heartbeat);
                SYMBIONT_HEARTBEAT_LOOPS.put(symbiont, loop);
                minecraft.getSoundHandler().playSound(loop);
            }
        }
        Iterator<Map.Entry<SickenedEntities.WitheredSymbiontEntity,
                WitheredSymbiontHeartbeatSound>> iterator = SYMBIONT_HEARTBEAT_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SickenedEntities.WitheredSymbiontEntity,
                    WitheredSymbiontHeartbeatSound> entry = iterator.next();
            WitheredSymbiontHeartbeatSound loop = entry.getValue();
            if (entry.getKey().isDead) {
                loop.stopImmediately();
                iterator.remove();
            } else if (!entry.getKey().isVulnerable()) {
                loop.stop();
                iterator.remove();
            } else if (loop.isDonePlaying()) {
                iterator.remove();
            }
        }
    }

    private static void spawnTractorBeamParticles(Minecraft minecraft) {
        if (!WitherStormClientConfig.tractorBeamParticles) return;
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof TractorBeamProvider) || entity.isDead) continue;
            TractorBeamParticle.spawnForProvider(entity, (TractorBeamProvider) entity);
        }
    }

    private static void updateWitherStormLoops(Minecraft minecraft) {
        Set<Integer> activeStormIds = new HashSet<Integer>();
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof WitherStormEntity)) continue;
            WitherStormEntity storm = (WitherStormEntity) entity;
            if (storm.isDead || !storm.isEntityAlive() || storm.isSilent() || storm.isDeadOrPlayingDead()) continue;

            int entityId = storm.getEntityId();
            if (WITHER_STORM_LOOP_DISABLED.contains(entityId)) continue;
            activeStormIds.add(entityId);
            updateWitherStormLoop(minecraft, entityId, storm.getPositionVector(),
                    storm.getPhase(), storm, false);
        }

        Iterator<Map.Entry<Integer, WitherStormLoopSound>> iterator =
                WITHER_STORM_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, WitherStormLoopSound> entry = iterator.next();
            Integer lastUpdate = WITHER_STORM_LOOP_UPDATES.get(entry.getKey());
            boolean recentlyUpdated = lastUpdate != null && witherStormLoopTick - lastUpdate <= 40;
            if (entry.getValue().isDonePlaying()
                    || !activeStormIds.contains(entry.getKey()) && !recentlyUpdated) {
                if (!entry.getValue().isDonePlaying()) entry.getValue().requestStop();
                WITHER_STORM_LOOP_NAMES.remove(entry.getKey());
                WITHER_STORM_LOOP_UPDATES.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    public static void handleWitherStormLoop(ModNetwork.WitherStormLoopMessage message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int entityId = message.getEntityId();
        if (!message.isActive()) {
            WITHER_STORM_LOOP_DISABLED.add(entityId);
            WitherStormLoopSound loop = WITHER_STORM_LOOPS.remove(entityId);
            if (loop != null) loop.requestStop();
            WITHER_STORM_LOOP_NAMES.remove(entityId);
            WITHER_STORM_LOOP_UPDATES.remove(entityId);
            return;
        }
        if (minecraft.world == null || minecraft.player == null) return;
        WITHER_STORM_LOOP_DISABLED.remove(entityId);
        Entity loaded = minecraft.world.getEntityByID(entityId);
        WitherStormEntity storm = loaded instanceof WitherStormEntity
                ? (WitherStormEntity) loaded : null;
        updateWitherStormLoop(minecraft, entityId,
                new Vec3d(message.getX(), message.getY(), message.getZ()),
                message.getPhase(), storm, true);
    }

    private static void updateWitherStormLoop(Minecraft minecraft, int entityId, Vec3d position,
                                              int phase, WitherStormEntity storm,
                                              boolean networkUpdate) {
        if (networkUpdate) WITHER_STORM_LOOP_UPDATES.put(entityId, witherStormLoopTick);
        double distanceSquared = minecraft.player.getDistanceSq(position.x, position.y, position.z);
        float distanceFade = Math.max(1.0F, (float) (distanceSquared / 1000.0D) / 32.0F);
        SoundEvent desiredSound = WitherStormEntity.getSoundForLoop(phase, distanceFade);
        String desiredName = desiredSound == null || desiredSound.getRegistryName() == null
                ? "" : desiredSound.getRegistryName().toString();
        WitherStormLoopSound currentLoop = WITHER_STORM_LOOPS.get(entityId);
        if (currentLoop != null && currentLoop.isDonePlaying()) {
            WITHER_STORM_LOOPS.remove(entityId);
            WITHER_STORM_LOOP_NAMES.remove(entityId);
            currentLoop = null;
        }
        String currentName = WITHER_STORM_LOOP_NAMES.get(entityId);
        if (desiredSound == null) {
            if (currentLoop != null) currentLoop.requestStop();
            WITHER_STORM_LOOPS.remove(entityId);
            WITHER_STORM_LOOP_NAMES.remove(entityId);
            return;
        }
        if (currentLoop == null || !desiredName.equals(currentName)) {
            if (currentLoop != null) currentLoop.requestStop();
            WitherStormLoopSound replacement = storm == null
                    ? new WitherStormLoopSound(entityId, position, desiredSound,
                    WitherStormEntity.getSoundLoopAttenuationDistance(phase))
                    : new WitherStormLoopSound(storm, desiredSound,
                    WitherStormEntity.getSoundLoopAttenuationDistance(phase));
            replacement.updatePosition(position);
            WITHER_STORM_LOOPS.put(entityId, replacement);
            WITHER_STORM_LOOP_NAMES.put(entityId, desiredName);
            minecraft.getSoundHandler().playSound(replacement);
        } else {
            currentLoop.bindTo(storm);
            currentLoop.updatePosition(position);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attenuateOccludedWitherStormSound(PlaySoundEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ISound sound = event.getResultSound();
        if (!WitherStormConfig.occludeSoundsUnderground || minecraft.player == null
                || sound == null || WorldUtil.isInAnOpenArea(minecraft.player)
                || !isOccludedWitherStormSound(sound.getSoundLocation())) return;

        float depth = MathHelper.clamp((float) -minecraft.player.posY + 40.0F, 0.0F, 20.0F);
        try {
            float volume = sound.getVolume() * ((20.0F - depth) / 20.0F) * 0.5F;
            event.setResultSound(new PositionedSoundRecord(sound.getSoundLocation(), sound.getCategory(),
                    volume, sound.getPitch(), sound.canRepeat(), sound.getRepeatDelay(),
                    sound.getAttenuationType(), sound.getXPosF(), sound.getYPosF(), sound.getZPosF()));
        } catch (NullPointerException ignored) {
            // Cleanroom can post PlaySoundEvent before PositionedSound resolves its Sound.
            // Keeping the original result lets SoundManager finish resolving and play it safely.
        }
    }

    private static boolean isOccludedWitherStormSound(ResourceLocation sound) {
        return sound != null && WitherStormEntity.isOccludedSound(
                ForgeRegistries.SOUND_EVENTS.getValue(sound));
    }

    @SubscribeEvent
    public static void leftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!checkForPlayingDeadCoreHit(event.getEntityPlayer())) {
            checkForHeadHit(event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (checkForPlayingDeadCoreHit(event.getEntityPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void attackEntity(AttackEntityEvent event) {
        if (event.getTarget() instanceof SupplementalEntities.CommandBlockEntity) return;
        checkForHeadHit(event.getEntityPlayer());
    }

    private static boolean checkForPlayingDeadCoreHit(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == null || player != minecraft.player || player.world == null
                || !player.world.isRemote || player.isSpectator() || minecraft.playerController == null) {
            return false;
        }

        double reach = minecraft.playerController.getBlockReachDistance();
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = new Vec3d(eyes.x + look.x * reach,
                eyes.y + look.y * reach, eyes.z + look.z * reach);
        // Query a wider corridor because the visible ribcage extends several
        // blocks beyond the 1x1 physical core. The final ray test below still
        // selects only the core's interaction envelope.
        AxisAlignedBB search = player.getEntityBoundingBox()
                .expand(look.x * reach, look.y * reach, look.z * reach).grow(3.0D);
        SupplementalEntities.CommandBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (SupplementalEntities.CommandBlockEntity core : player.world.getEntitiesWithinAABB(
                SupplementalEntities.CommandBlockEntity.class, search)) {
            if (!core.isEntityAlive() || core.isIndependentBowelsPart()
                    || core.getCoreState()
                    != SupplementalEntities.CommandBlockEntity.CoreState.PLAYING_DEAD) continue;
            // Keep the physical 1x1 core hitbox unchanged, but use the visible
            // ribcage as a forgiving interaction envelope. The podium and ribs
            // otherwise make the tiny command block impossible to select from
            // the same angles where it is clearly visible.
            RayTraceResult intercept = core.getInteractionBoundingBox()
                    .grow(Math.max(0.0D, core.getCollisionBorderSize()))
                    .calculateIntercept(eyes, end);
            if (intercept == null || intercept.hitVec == null) continue;
            double distance = eyes.squareDistanceTo(intercept.hitVec);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = core;
            }
        }
        if (closest == null) return false;
        ModNetwork.attackPlayingDeadCore(closest);
        return true;
    }

    private static void checkForHeadHit(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == null || player != minecraft.player || player.world == null
                || !player.world.isRemote || player.isSpectator() || minecraft.playerController == null) return;
        double reach = minecraft.playerController.getBlockReachDistance();
        for (WitherStormEntity storm : player.world.getEntitiesWithinAABB(
                WitherStormEntity.class, player.getEntityBoundingBox().grow(50.0D))) {
            for (int head = 0; head < storm.getTotalHeads(); head++) {
                if (!storm.tractorBeamActive(head) || !storm.canPlayerReachHead(player, head, reach)) continue;
                if (!storm.isDeadOrPlayingDead() && !storm.isHeadInjured(head)) {
                    ModNetwork.injureWitherStormHead(storm, head);
                } else {
                    player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
                }
                return;
            }
        }
        for (SupplementalEntities.WitherStormSegmentEntity segment : player.world.getEntitiesWithinAABB(
                SupplementalEntities.WitherStormSegmentEntity.class, player.getEntityBoundingBox().grow(80.0D))) {
            for (int head = 0; head < segment.getTotalHeads(); head++) {
                if (!segment.tractorBeamActive(head) || !segment.canPlayerReachHead(player, head, reach)) continue;
                if (!segment.isInDeathSequence() && !segment.isHeadInjured(head)) {
                    ModNetwork.injureWitherStormHead(segment, head);
                } else {
                    player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void renderAmuletInHand(RenderSpecificHandEvent event) {
        // 新版望远镜观测时会跳过整个第一人称手部阶段，主副手都不应穿过遮罩。
        if (PhasometerOverlay.isFirstPersonScoping(Minecraft.getMinecraft())) {
            event.setCanceled(true);
            return;
        }
        AmuletAnimationHelper.render(event);
        FormidiBladeAnimationHelper.onRenderItemInHand(event);
    }

    @SubscribeEvent
    public static void renderWitherStormWorldEffects(RenderWorldLastEvent event) {
        // 同一次 Forge 世界末尾事件只能提交一批全局效果，避免重复注册时整批叠加。
        if (lastWorldEffectsEvent == event) return;
        lastWorldEffectsEvent = event;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.getRenderViewEntity() == null) return;
        int diagnosticTick = minecraft.player == null
                ? Integer.MIN_VALUE : minecraft.player.ticksExisted;
        boolean logDiagnostics = StormDiagnosticLogger.isEnabled()
                && diagnosticTick != Integer.MIN_VALUE
                && diagnosticTick != lastWorldEffectsDiagnosticTick
                && diagnosticTick % 20 == 0;
        if (logDiagnostics) {
            lastWorldEffectsDiagnosticTick = diagnosticTick;
            StormDiagnosticLogger.info(
                    "[风暴诊断][世界末尾渲染进入] tick={} 世界实例={} 维度={} 观察实体={} GL={}",
                    diagnosticTick, System.identityHashCode(minecraft.world),
                    minecraft.world.provider.getDimension(),
                    minecraft.getRenderViewEntity().getEntityId(), describeCurrentGlState());
        }
        // 原版 RenderWorldLastEvent 在 RenderHelper.disableStandardItemLighting() 之后发布，
        // 之后唯一会触碰 GL 状态的只剩第一人称物品 pass。F1 或观察者模式会跳过物品
        // 绘制，任何泄漏的光照/混合/alpha test 都会进入下一帧并使天空全黑。事件边界
        // 保存并恢复完整 GL 状态，异常路径同样兜底，不再依赖手部渲染碰巧重置。
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int previousAlphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float previousAlphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean defaultTextureEnabled = isTexture2DEnabled(
                OpenGlHelper.defaultTexUnit, previousActiveTexture);
        boolean lightmapTextureEnabled = isTexture2DEnabled(
                OpenGlHelper.lightmapTexUnit, previousActiveTexture);
        forceActiveTexture(previousActiveTexture);
        float previousLightmapX = OpenGlHelper.lastBrightnessX;
        float previousLightmapY = OpenGlHelper.lastBrightnessY;
        int renderableStormCount = 0;
        GlStateManager.pushMatrix();
        try {
            DistantStormRenderTracker.renderMissing(event.getPartialTicks());
            double viewerX = minecraft.getRenderManager().viewerPosX;
            double viewerY = minecraft.getRenderManager().viewerPosY;
            double viewerZ = minecraft.getRenderManager().viewerPosZ;
            // World.loadedEntityList can briefly contain the same tracked entity more than once
            // while a split storm is reattached to a distant chunk. Render its world effects once.
            List<WitherStormEntity> renderableStorms = new ArrayList<WitherStormEntity>();
            Set<java.util.UUID> renderedStormIds = new HashSet<java.util.UUID>();
            for (Entity entity : minecraft.world.loadedEntityList) {
                if (entity instanceof WitherStormEntity && !entity.isDead
                        && renderedStormIds.add(entity.getUniqueID())) {
                    renderableStorms.add((WitherStormEntity) entity);
                }
            }
            renderableStormCount = renderableStorms.size();
            for (WitherStormEntity storm : renderableStorms) {
                boolean extendedProjection = DistantProjection.shouldUse(storm);
                if (extendedProjection) DistantProjection.push();
                try {
                    WitherStormRenderer.renderDebrisRings(storm, event.getPartialTicks(),
                            viewerX, viewerY, viewerZ);
                } finally {
                    if (extendedProjection) DistantProjection.pop();
                }
            }
            TractorBeamRenderer.renderAll(minecraft.world.loadedEntityList,
                    event.getPartialTicks(), viewerX, viewerY, viewerZ);
            for (WitherStormEntity storm : renderableStorms) {
                boolean extendedProjection = DistantProjection.shouldUse(storm);
                if (extendedProjection) DistantProjection.push();
                try {
                    WitherStormRenderer.renderShine(storm, event.getPartialTicks(),
                            viewerX, viewerY, viewerZ);
                } finally {
                    if (extendedProjection) DistantProjection.pop();
                }
            }
            PostProcessingShaders.INSTANCE.render(event.getPartialTicks());
        } finally {
            String beforeRestore = logDiagnostics ? describeCurrentGlState() : null;
            restoreWorldEffectsGlState(lightingEnabled, blendEnabled, alphaTestEnabled,
                    cullEnabled, depthMaskEnabled, fogEnabled, previousDepthFunc,
                    previousAlphaFunc, previousAlphaReference, previousMatrixMode,
                    previousActiveTexture, defaultTextureEnabled, lightmapTextureEnabled,
                    previousLightmapX, previousLightmapY);
            if (logDiagnostics) {
                StormDiagnosticLogger.info(
                        "[风暴诊断][世界末尾渲染退出] tick={} 可渲染主体={} 恢复前GL={} 恢复后GL={}",
                        diagnosticTick, renderableStormCount, beforeRestore,
                        describeCurrentGlState());
            }
        }
    }

    private static String describeCurrentGlState() {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean defaultTextureEnabled = isTexture2DEnabled(
                OpenGlHelper.defaultTexUnit, activeTexture);
        boolean lightmapTextureEnabled = isTexture2DEnabled(
                OpenGlHelper.lightmapTexUnit, activeTexture);
        return "lighting=" + GL11.glIsEnabled(GL11.GL_LIGHTING)
                + ",blend=" + GL11.glIsEnabled(GL11.GL_BLEND)
                + ",alpha=" + GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
                + ",cull=" + GL11.glIsEnabled(GL11.GL_CULL_FACE)
                + ",depth=" + GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                + ",depthMask=" + GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
                + ",fog=" + GL11.glIsEnabled(GL11.GL_FOG)
                + ",depthFunc=" + GL11.glGetInteger(GL11.GL_DEPTH_FUNC)
                + ",matrixMode=" + GL11.glGetInteger(GL11.GL_MATRIX_MODE)
                + ",modelStack=" + GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH)
                + ",projectionStack=" + GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH)
                + ",activeTexture=" + activeTexture
                + ",texture0=" + defaultTextureEnabled
                + ",lightmapTexture=" + lightmapTextureEnabled;
    }

    /** 查询指定纹理单元的二维纹理状态，并保持真实活动单元不变。 */
    private static boolean isTexture2DEnabled(int textureUnit, int activeTexture) {
        OpenGlHelper.setActiveTexture(textureUnit);
        boolean enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(activeTexture);
        return enabled;
    }

    /** 把世界末尾特效批恢复为事件进入前的 GL 状态，保持 1.12 状态缓存与实际 GL 同步。 */
    private static void restoreWorldEffectsGlState(boolean lightingEnabled, boolean blendEnabled,
                                                   boolean alphaTestEnabled, boolean cullEnabled,
                                                   boolean depthMaskEnabled, boolean fogEnabled,
                                                   int previousDepthFunc, int previousAlphaFunc,
                                                   float previousAlphaReference, int previousMatrixMode,
                                                   int previousActiveTexture,
                                                   boolean defaultTextureEnabled,
                                                   boolean lightmapTextureEnabled,
                                                   float previousLightmapX, float previousLightmapY) {
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(previousMatrixMode);
        GlStateManager.depthMask(depthMaskEnabled);
        GlStateManager.depthFunc(previousDepthFunc);
        if (blendEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (alphaTestEnabled) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }
        GlStateManager.alphaFunc(previousAlphaFunc, previousAlphaReference);
        if (cullEnabled) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        if (lightingEnabled) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }
        if (fogEnabled) {
            GlStateManager.enableFog();
        } else {
            GlStateManager.disableFog();
        }
        restoreTexture2DState(OpenGlHelper.defaultTexUnit, defaultTextureEnabled);
        restoreTexture2DState(OpenGlHelper.lightmapTexUnit, lightmapTextureEnabled);
        forceActiveTexture(previousActiveTexture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disablePolygonOffset();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                previousLightmapX, previousLightmapY);
    }

    /** 强制同步指定纹理单元的真实状态与 GlStateManager 缓存。 */
    private static void restoreTexture2DState(int textureUnit, boolean enabled) {
        forceActiveTexture(textureUnit);
        if (enabled) {
            GlStateManager.disableTexture2D();
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.enableTexture2D();
            GlStateManager.disableTexture2D();
        }
    }

    /** 通过一次不同单元的过渡，避免活动纹理缓存错误时跳过真实 GL 调用。 */
    private static void forceActiveTexture(int textureUnit) {
        int alternate = textureUnit == OpenGlHelper.defaultTexUnit
                ? OpenGlHelper.lightmapTexUnit : OpenGlHelper.defaultTexUnit;
        GlStateManager.setActiveTexture(alternate);
        GlStateManager.setActiveTexture(textureUnit);
    }

    private static void spawnFormidibombParticles(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (entity instanceof PowerfulExplosiveEntity.FormidibombEntity && !entity.isDead) {
                CommandBlockParticle.spawnForBomb((PowerfulExplosiveEntity.FormidibombEntity) entity);
            }
        }
    }

    private static void spawnSymbiontDragonFireballParticles(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (entity instanceof SymbiontDragonFireballEntity) {
                CommandBlockParticle.spawnForSymbiontDragonFireball(entity);
            }
            if (entity instanceof SickenedEntities.WitheredSymbiontEntity) {
                CommandBlockParticle.spawnForSymbiont((SickenedEntities.WitheredSymbiontEntity) entity);
            }
        }
    }

    private static void spawnWitherStormParticles(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (entity instanceof WitherStormEntity && !entity.isDead) {
                CommandBlockParticle.spawnForWitherStorm((WitherStormEntity) entity);
            }
        }
    }

    private static void updateWitherStormTrembleLoops(Minecraft minecraft) {
        Set<Integer> loadedStormIds = new HashSet<Integer>();
        SoundEvent trembleSound = ModSounds.get("wither_storm_tremble");
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof WitherStormEntity) || entity.isDead) continue;
            WitherStormEntity storm = (WitherStormEntity) entity;
            int entityId = storm.getEntityId();
            loadedStormIds.add(entityId);
            WitherStormTrembleSound trembleLoop = TREMBLE_LOOPS.get(entityId);
            if (storm.getPlayDeadState() == WitherStormEntity.PlayDeadState.FALLING) {
                if ((trembleLoop == null || trembleLoop.isDonePlaying()) && trembleSound != null) {
                    trembleLoop = new WitherStormTrembleSound(storm, trembleSound);
                    TREMBLE_LOOPS.put(entityId, trembleLoop);
                    minecraft.getSoundHandler().playSound(trembleLoop);
                }
            } else if (trembleLoop != null) {
                trembleLoop.requestStop();
            }
        }

        Iterator<Map.Entry<Integer, WitherStormTrembleSound>> iterator = TREMBLE_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, WitherStormTrembleSound> entry = iterator.next();
            WitherStormTrembleSound trembleLoop = entry.getValue();
            if (!loadedStormIds.contains(entry.getKey())) {
                trembleLoop.stopImmediately();
                iterator.remove();
            } else if (trembleLoop.isDonePlaying()) {
                iterator.remove();
            }
        }
    }

    private static void updateFormidibombLoops(Minecraft minecraft) {
        Set<FormidibombSource> currentSources = java.util.Collections.newSetFromMap(
                new IdentityHashMap<FormidibombSource, Boolean>());
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (entity instanceof PowerfulExplosiveEntity.FormidibombEntity && !entity.isDead) {
                currentSources.add((PowerfulExplosiveEntity.FormidibombEntity) entity);
            }
        }
        for (TileEntity tile : minecraft.world.loadedTileEntityList) {
            if (tile instanceof FormidibombTileEntity
                    && minecraft.player.getEntityBoundingBox().grow(50.0D).contains(
                    new Vec3d(tile.getPos()).add(0.5D, 0.5D, 0.5D))) {
                currentSources.add((FormidibombTileEntity) tile);
            }
        }

        FormidibombSource selectedSource = null;
        float selectedProgress = -1.0F;
        for (FormidibombSource source : currentSources) {
            if (!source.isFormidibombAlive() || source.getStartFuse() <= 0) continue;
            float progress = (source.getStartFuse() - source.getFuseLife())
                    / (float) source.getStartFuse();
            if (selectedSource == null || progress > selectedProgress) {
                selectedSource = source;
                selectedProgress = progress;
            }
        }
        PostProcessingShaders.INSTANCE.setSource(selectedSource);

        SoundEvent pulseSound = ModSounds.get("formidibomb_pulse_loop");
        for (FormidibombSource source : currentSources) {
            FormidibombFuseSound loop = FORMIDIBOMB_LOOPS.get(source);
            if ((loop == null || loop.isDonePlaying()) && pulseSound != null) {
                loop = new FormidibombFuseSound(source, pulseSound);
                FORMIDIBOMB_LOOPS.put(source, loop);
                minecraft.getSoundHandler().playSound(loop);
            }
        }

        Iterator<Map.Entry<FormidibombSource, FormidibombFuseSound>> iterator =
                FORMIDIBOMB_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FormidibombSource, FormidibombFuseSound> entry = iterator.next();
            if (!currentSources.contains(entry.getKey()) || !entry.getKey().isFormidibombAlive()
                    || entry.getValue().isDonePlaying()) {
                entry.getValue().stop();
                iterator.remove();
            }
        }
    }

    private static void updateCommandBlockLoops(Minecraft minecraft) {
        Set<SupplementalEntities.CommandBlockEntity> currentCommandBlocks = java.util.Collections.newSetFromMap(
                new IdentityHashMap<SupplementalEntities.CommandBlockEntity, Boolean>());
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (entity instanceof SupplementalEntities.CommandBlockEntity && !entity.isDead) {
                currentCommandBlocks.add((SupplementalEntities.CommandBlockEntity) entity);
            }
        }

        SoundEvent pulseSound = ModSounds.get("command_block_pulse_loop");
        for (SupplementalEntities.CommandBlockEntity commandBlock : currentCommandBlocks) {
            CommandBlockLoopSound loop = COMMAND_BLOCK_LOOPS.get(commandBlock);
            if ((loop == null || loop.isDonePlaying()) && pulseSound != null) {
                loop = new CommandBlockLoopSound(commandBlock, pulseSound);
                COMMAND_BLOCK_LOOPS.put(commandBlock, loop);
                minecraft.getSoundHandler().playSound(loop);
            }
        }

        Iterator<Map.Entry<SupplementalEntities.CommandBlockEntity, CommandBlockLoopSound>> iterator =
                COMMAND_BLOCK_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SupplementalEntities.CommandBlockEntity, CommandBlockLoopSound> entry = iterator.next();
            if (!currentCommandBlocks.contains(entry.getKey()) || entry.getKey().isDead
                    || entry.getValue().isDonePlaying()) {
                entry.getValue().stop();
                iterator.remove();
            }
        }
    }

    private static void updateTractorBeamLoops(Minecraft minecraft) {
        Set<Entity> currentSources = java.util.Collections.newSetFromMap(
                new IdentityHashMap<Entity, Boolean>());
        SoundEvent beamSound = ModSounds.get("wither_storm_tractor_beam");
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof TractorBeamProvider) || entity.isDead) continue;
            currentSources.add(entity);
            TractorBeamProvider provider = (TractorBeamProvider) entity;
            int headCount = Math.max(0, provider.getTotalHeads());
            TractorBeamLoopSound[] loops = TRACTOR_BEAM_LOOPS.get(entity);
            if (loops == null || loops.length != headCount) {
                stopTractorBeamLoops(loops);
                loops = new TractorBeamLoopSound[headCount];
                TRACTOR_BEAM_LOOPS.put(entity, loops);
            }
            for (int head = 0; head < headCount; head++) {
                if (loops[head] != null && loops[head].isDonePlaying()) loops[head] = null;
                if (beamSound == null || entity.isSilent() || provider.isDeadOrPlayingDead()
                        || !provider.tractorBeamActive(head) || loops[head] != null) continue;
                Vec3d closest = TractorBeamLoopSound.calculateClosestPoint(
                        entity, provider, head, minecraft.player.getPositionVector());
                if (minecraft.player.getDistanceSq(closest.x, closest.y, closest.z)
                        > TractorBeamLoopSound.MAXIMUM_DISTANCE * TractorBeamLoopSound.MAXIMUM_DISTANCE) continue;
                loops[head] = new TractorBeamLoopSound(entity, provider, head, beamSound, closest);
                minecraft.getSoundHandler().playSound(loops[head]);
            }
        }

        Iterator<Map.Entry<Entity, TractorBeamLoopSound[]>> iterator = TRACTOR_BEAM_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, TractorBeamLoopSound[]> entry = iterator.next();
            if (!currentSources.contains(entry.getKey())) {
                stopTractorBeamLoops(entry.getValue());
                iterator.remove();
            }
        }
    }

    private static void stopTractorBeamLoops(TractorBeamLoopSound[] loops) {
        if (loops == null) return;
        for (TractorBeamLoopSound loop : loops) {
            if (loop != null) loop.stop();
        }
    }

    @SubscribeEvent
    public static void setupCamera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!WitherStormClientConfig.cameraShakeEffects
                || minecraft.player == null || event.getEntity() != minecraft.player) return;
        float partialTicks = (float) event.getRenderPartialTicks();
        GlStateManager.translate(ClientEffects.getShakeTranslationX(partialTicks),
                ClientEffects.getShakeTranslationY(partialTicks), 0.0F);
    }

    @SubscribeEvent
    public static void modifyStormFogColors(EntityViewRenderEvent.FogColors event) {
        if (!WitherStormClientConfig.renderSkyAmbienceEffects) return;
        Entity viewEntity =
                Minecraft.getMinecraft().getRenderViewEntity();
        if (viewEntity == null) return;
        Vec3d blended = SkyAmbienceManager.INSTANCE.blendFogColor(viewEntity,
                (float) event.getRenderPartialTicks(),
                new Vec3d(event.getRed(), event.getGreen(), event.getBlue()));
        if (blended != null) {
            event.setRed((float) blended.x);
            event.setGreen((float) blended.y);
            event.setBlue((float) blended.z);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void disableVanillaFog(EntityViewRenderEvent.FogDensity event) {
        if (!WitherStormClientConfig.disableVanillaFog) return;
        // 事件短路原版雾参数；EntityRendererFogMixin 再关闭 setupFog 末尾启用的 GL 状态。
        GlStateManager.setFog(GlStateManager.FogMode.EXP);
        GlStateManager.setFogDensity(0.0F);
        event.setDensity(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderBlindOverlay(RenderGameOverlayEvent.Pre event) {
        // Draw the flash before the HUD pass.  Drawing during HOTBAR covers the
        // slot background and makes the inventory look as if it disappeared.
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        // 上游把白屏注册为 HUD 覆盖层：任何 GuiScreen（物品栏/容器）打开时都不绘制，
        // 避免全屏白矩形进入容器 GUI 的混合状态并吞掉物品栏。
        if (minecraft.currentScreen != null) return;
        float fade = ClientEffects.getBlindFade(event.getPartialTicks());
        if (fade <= 0.0F) return;
        int alpha = MathHelper.clamp(Math.round(fade * 255.0F), 0, 255);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        try {
            // 与上游 renderSolidOverlay 等价：显式管理深度与混合，不依赖
            // Gui.drawRect 的局部恢复，避免残留状态破坏后续 HUD/GUI 渲染。
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            Gui.drawRect(0, 0, event.getResolution().getScaledWidth(),
                    event.getResolution().getScaledHeight(), alpha << 24 | 0xFFFFFF);
        } finally {
            GlStateManager.depthMask(depthMask);
            if (depthEnabled) GlStateManager.enableDepth();
            else GlStateManager.disableDepth();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            if (!blendEnabled) GlStateManager.disableBlend();
            // Gui.drawRect leaves the OpenGL current color at the overlay alpha;
            // reset it before vanilla draws the hotbar and inventory icons.
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void renderTractorBeamOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        TractorBeamOverlay.render(Minecraft.getMinecraft(), event.getResolution());
    }

    @SubscribeEvent
    public static void renderSymbiontThemeWatermark(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        BossThemeProvider provider = BossThemeManager.INSTANCE.getActiveProvider();
        if (!(provider instanceof SickenedEntities.WitheredSymbiontEntity)) return;
        String watermark = I18n.format("witherstormmod.watermark.withered_symbiont_theme");
        if (watermark.isEmpty()) return;
        ScaledResolution resolution = event.getResolution();
        GlStateManager.enableBlend();
        minecraft.fontRenderer.drawStringWithShadow(watermark,
                resolution.getScaledWidth() - minecraft.fontRenderer.getStringWidth(watermark) - 4,
                resolution.getScaledHeight() - 12, 0x66FFFFFF);
        GlStateManager.disableBlend();
    }

    @SubscribeEvent
    public static void renderDebugText(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) return;
        List<String> right = event.getRight();
        right.add("");
        right.add("witherstormmod: " + Tags.VERSION);
        right.add("Buffered Instances: " + LegacyRenderBufferer.INSTANCE.getTotalInstances());
        boolean active = LegacyRenderBufferer.INSTANCE.shouldUse();
        right.add("Render Bufferer Active: "
                + (active ? TextFormatting.GREEN : TextFormatting.RED) + active);
    }

    /** 对应上游 RenderWitherSicknessOverlay：患病时用病化血条替换原版心形血条。 */
    @SubscribeEvent
    public static void renderWitherSicknessHealth(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || minecraft.gameSettings.hideGUI
                || !player.isPotionActive(ModEffects.WITHER_SICKNESS)) return;
        // Classic Bars owns the health overlay when installed; its optional mixin maps
        // Wither Sickness to the configured Withered Colors palette.
        if (Loader.isModLoaded("classicbar")) return;
        event.setCanceled(true);

        int tickCount = minecraft.ingameGUI.getUpdateCounter();
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int health = MathHelper.ceil(player.getHealth());
        boolean highlight = sicknessHealthBlinkTime > tickCount
                && (sicknessHealthBlinkTime - tickCount) / 3L % 2L == 1L;
        if (health < sicknessLastHealth && player.hurtResistantTime > 0) {
            sicknessLastHealthTime = Minecraft.getSystemTime();
            sicknessHealthBlinkTime = tickCount + 20;
        } else if (health > sicknessLastHealth && player.hurtResistantTime > 0) {
            sicknessLastHealthTime = Minecraft.getSystemTime();
            sicknessHealthBlinkTime = tickCount + 10;
        }
        if (Minecraft.getSystemTime() - sicknessLastHealthTime > 1000L) {
            sicknessLastHealth = health;
            sicknessDisplayHealth = health;
            sicknessLastHealthTime = Minecraft.getSystemTime();
        }
        sicknessLastHealth = health;
        int healthLast = sicknessDisplayHealth;
        float healthMax = player.getMaxHealth();
        int absorption = MathHelper.ceil(player.getAbsorptionAmount());
        int healthRows = MathHelper.ceil((healthMax + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);
        WITHER_SICKNESS_HEALTH_RANDOM.setSeed(tickCount * 312871L);
        int left = width / 2 - 91;
        int top = height - 39;
        int regen = -1;
        if (player.isPotionActive(MobEffects.REGENERATION)) {
            regen = tickCount % MathHelper.ceil(healthMax + 5.0F);
        }
        int verticalOffset = player.world.getWorldInfo().isHardcoreModeEnabled() ? 9 : 0;
        int background = highlight ? 25 : 16;
        int margin = 34;
        float absorptionRemaining = absorption;
        minecraft.getTextureManager().bindTexture(WITHER_SICKNESS_ICONS);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (int index = MathHelper.ceil((healthMax + absorption) / 2.0F) - 1;
             index >= 0; --index) {
            int row = MathHelper.ceil((index + 1) / 10.0F) - 1;
            int x = left + index % 10 * 8;
            int y = top - row * rowHeight;
            if (health <= 4) y += WITHER_SICKNESS_HEALTH_RANDOM.nextInt(2);
            if (index == regen) y -= 2;
            Gui.drawModalRectWithCustomSizedTexture(x, y, background, verticalOffset,
                    9, 9, 256, 256);
            if (highlight) {
                if (index * 2 + 1 < healthLast) {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, margin, verticalOffset,
                            9, 9, 256, 256);
                } else if (index * 2 + 1 == healthLast) {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, margin + 9, verticalOffset,
                            9, 9, 256, 256);
                }
            }
            if (absorptionRemaining > 0.0F) {
                if (absorptionRemaining == absorption && absorption % 2.0F == 1.0F) {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, margin + 9, verticalOffset,
                            9, 9, 256, 256);
                    absorptionRemaining -= 1.0F;
                } else {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, margin, verticalOffset,
                            9, 9, 256, 256);
                    absorptionRemaining -= 2.0F;
                }
            } else if (index * 2 + 1 < health) {
                Gui.drawModalRectWithCustomSizedTexture(x, y, margin, verticalOffset,
                        9, 9, 256, 256);
            } else if (index * 2 + 1 == health) {
                Gui.drawModalRectWithCustomSizedTexture(x, y, margin + 9, verticalOffset,
                        9, 9, 256, 256);
            }
        }
        GlStateManager.disableBlend();
    }

    private static void showOptifineWarning(Minecraft minecraft) {
        if (optifineWarningShown || !WitherStormClientConfig.optifineWarning
                || minecraft.player == null) return;
        optifineWarningShown = true;
        if (!OptifineCompat.isLoaded()) return;
        minecraft.player.sendMessage(new TextComponentTranslation(
                "chat.witherstormmod.optifine.notice"));
    }

    @SubscribeEvent
    public static void renderPhasometerOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.HELMET) {
            PhasometerOverlay.render(Minecraft.getMinecraft(), event.getResolution(),
                    event.getPartialTicks());
        }
    }

    private static void updateBowelsAmbience(Minecraft minecraft) {
        if (minecraft.player.dimension != BowelsDimensions.DIMENSION_ID) {
            if (bowelsLoop != null) bowelsLoop.stop();
            bowelsLoop = null;
            bowelsMoodDelay = 0;
            bowelsTrembleTimer = -1;
            bowelsScreamTimer = -1;
            return;
        }
        if (bowelsLoop == null || bowelsLoop.isDonePlaying()) {
            SoundEvent loopSound = ModSounds.get("bowels_loop");
            if (loopSound != null) {
                bowelsLoop = new BowelsLoopSound(loopSound);
                minecraft.getSoundHandler().playSound(bowelsLoop);
            }
        }
        if (--bowelsMoodDelay <= 0) {
            bowelsMoodDelay = 240 + minecraft.world.rand.nextInt(240);
            SoundEvent moodSound = ModSounds.get("bowels_mood");
            if (moodSound != null) {
                BlockPos position = minecraft.player.getPosition().add(
                        minecraft.world.rand.nextInt(17) - 8,
                        minecraft.world.rand.nextInt(9) - 4,
                        minecraft.world.rand.nextInt(17) - 8);
                minecraft.getSoundHandler().playSound(new PositionedSoundRecord(
                        moodSound, SoundCategory.AMBIENT, 1.0F, 1.0F, position));
            }
        }

        SupplementalEntities.CommandBlockEntity lowest = null;
        float lowestHealth = -1.0F;
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof SupplementalEntities.CommandBlockEntity)) continue;
            SupplementalEntities.CommandBlockEntity core =
                    (SupplementalEntities.CommandBlockEntity) entity;
            if (lowestHealth != -1.0F && !(core.getHealth() < lowestHealth)) continue;
            lowest = core;
            lowestHealth = core.getHealth();
        }
        float timeReduction = 1.0F;
        boolean damaged = lowest != null && lowest.getHealth() < lowest.getMaxHealth();
        if (damaged) {
            timeReduction = Math.max(0.05F,
                    lowest.getHealth() / lowest.getMaxHealth() * 0.3F);
            if (bowelsScreamTimer < 0) bowelsScreamTimer = 100 + BOWELS_RANDOM.nextInt(240);
            if (--bowelsScreamTimer == 0) {
                SoundEvent scream = ModSounds.get("bowels_loud_hurt");
                if (scream != null) {
                    minecraft.world.playSound(minecraft.player, minecraft.player.posX,
                            minecraft.player.posY, minecraft.player.posZ,
                            scream, SoundCategory.AMBIENT, 1.0F, 1.0F);
                }
                bowelsScreamTimer = 120 + BOWELS_RANDOM.nextInt(120);
            }
        }
        if (bowelsTrembleTimer < 0) bowelsTrembleTimer = 300 + BOWELS_RANDOM.nextInt(600);
        if (--bowelsTrembleTimer == 0) {
            float extraShakeStrength = damaged ? 4.0F : 0.0F;
            ClientEffects.shake(60.0F, 2.0F + extraShakeStrength);
            SoundEvent tremble = ModSounds.get("bowels_tremble");
            if (tremble != null) {
                minecraft.world.playSound(minecraft.player, minecraft.player.posX,
                        minecraft.player.posY, minecraft.player.posZ,
                        tremble, SoundCategory.AMBIENT, 1.0F, 1.0F);
            }
            bowelsTrembleTimer = (int) ((240 + BOWELS_RANDOM.nextInt(720)) * timeReduction);
        }
    }

    /** 对应上游 ParticleEvents：命令方块书/工具掉落物持续冒出命令方块粒子。 */
    private static void spawnCommandBlockItemParticles(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof EntityItem)) continue;
            EntityItem item = (EntityItem) entity;
            if (item.getItem().isEmpty()) continue;
            if (item.getItem().getItem() != ModItems.get("command_block_book")
                    && !UpstreamItemTags.contains(
                    UpstreamItemTags.COMMAND_BLOCK_TOOLS, item.getItem())) continue;
            CommandBlockParticle.spawnForItemEntity(item);
        }
    }

    /** 对应上游 ClientWitherSicknessEvents：客户端推进病化动画计时。 */
    private static void tickWitherSicknessTrackers(Minecraft minecraft) {
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            WitherSicknessTracker tracker =
                    WitherSicknessCapability.get((EntityLivingBase) entity);
            if (tracker != null) tracker.tickClient();
        }
    }

    private static void auditCreativeStackModels(Minecraft minecraft) {
        IBakedModel missing = minecraft.getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
        int checked = 0;
        int failed = 0;
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation registryName = item.getRegistryName();
            if (registryName == null || !Tags.MOD_ID.equals(registryName.getNamespace())) continue;
            NonNullList<ItemStack> stacks = NonNullList.create();
            if (item.getCreativeTab() != null) {
                item.getSubItems(item.getCreativeTab(), stacks);
            }
            if (stacks.isEmpty()) stacks.add(new ItemStack(item));
            for (ItemStack stack : stacks) {
                checked++;
                IBakedModel model = minecraft.getRenderItem().getItemModelMesher().getItemModel(stack);
                List<String> missingTextures = findMissingQuadTextures(model);
                if (model == null || model == missing || !missingTextures.isEmpty()) {
                    failed++;
                    WitherStormMod.LOGGER.error(
                            "Creative stack model audit failed: item={}, metadata={}, baked={}, missingTextures={}",
                            registryName, stack.getMetadata(), model == null ? "null" : model.getClass().getName(),
                            missingTextures);
                }
            }
        }
        if (failed == 0) {
            WitherStormMod.LOGGER.info("Creative stack model audit passed for all {} item stacks", checked);
        } else {
            WitherStormMod.LOGGER.error("Creative stack model audit found {} failures among {} item stacks",
                    failed, checked);
        }
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getType() == RenderGameOverlayEvent.ElementType.HELMET && minecraft.player != null
                && minecraft.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem()
                == Item.getItemFromBlock(ModBlocks.get("tainted_carved_pumpkin"))) {
            ScaledResolution scaled = event.getResolution();
            minecraft.getTextureManager().bindTexture(new ResourceLocation(Tags.MOD_ID, "textures/misc/tainted_pumpkin_blur.png"));
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, scaled.getScaledWidth(), scaled.getScaledHeight(),
                    scaled.getScaledWidth(), scaled.getScaledHeight());
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
        }
    }
}
