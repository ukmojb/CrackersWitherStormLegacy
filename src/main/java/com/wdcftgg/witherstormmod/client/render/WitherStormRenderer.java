package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.resources.WitherStormResourceConfigManager;
import com.wdcftgg.witherstormmod.client.model.WitherStormPhaseModel;
import com.wdcftgg.witherstormmod.client.model.WitherStormPhaseModel.Form;
import com.wdcftgg.witherstormmod.client.util.SpecialDay;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.DebrisCluster;
import com.wdcftgg.witherstormmod.common.util.DebrisRingSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class WitherStormRenderer extends RenderLiving<WitherStormEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/wither_storm.png");
    private static final ResourceLocation SHINE_TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/shine.png");
    private static final ResourceLocation APRIL_FOOLS_TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/misc/pink_wither_storm.png");
    private static final ResourceLocation EXPLODING_TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/wither_storm_exploding.png");
    private static final boolean APRIL_FOOLS_DATE = SpecialDay.isAprilFoolsDate();
    private final Map<Form, WitherStormPhaseModel> models = new EnumMap<Form, WitherStormPhaseModel>(Form.class);
    private final Map<WitherStormEntity, Map<DebrisCluster, DebrisBufferCache>> debrisBufferCaches =
            new WeakHashMap<WitherStormEntity, Map<DebrisCluster, DebrisBufferCache>>();

    public WitherStormRenderer(RenderManager renderManager) {
        super(renderManager, new WitherStormPhaseModel(Form.COMMAND_BLOCK), 0.0F);
        models.put(Form.COMMAND_BLOCK, (WitherStormPhaseModel) mainModel);
        for (Form form : Form.values()) {
            if (form != Form.COMMAND_BLOCK) models.put(form, new WitherStormPhaseModel(form));
        }
        addLayer(new WitherStormHurtLayer(this));
        addLayer(new WitherStormEmissiveLayer(this));
        addLayer(new WitherStormArmorLayer(this));
        addLayer(new SantaHatLayer(this));
        addLayer(new WitherStormPulseLayer(this));
        LegacyRenderBufferer.INSTANCE.registerInvalidator(this::clearDebrisBufferCaches);
    }

    @Override
    public void doRender(WitherStormEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        DistantStormRenderTracker.markRendered(entity);
        boolean extendedProjection = DistantProjection.shouldUse(entity);
        if (extendedProjection) DistantProjection.push();
        DistantProjection.FogState previousFog = extendedProjection
                ? DistantProjection.pushDistantFog(entity, WitherStormClientConfig.distantFog) : null;
        try {
            mainModel = models.get(fetchForm(entity));
            renderDebrisClusters(entity, x, y, z, partialTicks);
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
            if (entity.getDeathTime() > 0) {
                renderDeathWireframe(entity.getDeathTime(), entity.getUnmodifiedHeight(),
                        x, y, z, partialTicks);
            }
        } finally {
            if (extendedProjection) DistantProjection.restoreFog(previousFog);
            if (extendedProjection) DistantProjection.pop();
        }
    }

    @Override
    public boolean shouldRender(WitherStormEntity entity, ICamera camera,
                                double cameraX, double cameraY, double cameraZ) {
        if (WitherStormClientConfig.distantRenderer) {
            return DistantProjection.isWithinFarPlane(entity.posX, entity.posY, entity.posZ,
                    cameraX, cameraY, cameraZ);
        }
        return super.shouldRender(entity, camera, cameraX, cameraY, cameraZ);
    }

    @Override
    protected ResourceLocation getEntityTexture(WitherStormEntity entity) {
        if (entity.getDeathTime() > 0) return EXPLODING_TEXTURE;
        return getBaseTexture(entity);
    }

    private ResourceLocation getBaseTexture(WitherStormEntity entity) {
        int invulnerableTicks = entity.getInvulnerableTicks();
        if (invulnerableTicks > 0
                && (invulnerableTicks > 80 || invulnerableTicks / 5 % 2 != 1)) {
            return WitherStormResourceConfigManager.INSTANCE
                    .getTextureSetByPhase(entity.getPhase()).getInvulnerable();
        }
        if (APRIL_FOOLS_DATE && WitherStormClientConfig.aprilFools) return APRIL_FOOLS_TEXTURE;
        return WitherStormResourceConfigManager.INSTANCE
                .getTextureSetByPhase(entity.getPhase()).getMain();
    }

    @Override
    protected void renderModel(WitherStormEntity entity, float limbSwing, float limbSwingAmount,
                               float ageInTicks, float netHeadYaw, float headPitch,
                               float scaleFactor) {
        if (entity.getDeathTime() <= 0) {
            super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw,
                    headPitch, scaleFactor);
            return;
        }
        renderDissolvingModel(entity, entity.getDeathTime(), mainModel, getBaseTexture(entity),
                EXPLODING_TEXTURE, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw,
                headPitch, scaleFactor);
    }

    static void renderDissolvingModel(EntityLivingBase entity, int deathTime, ModelBase model,
                                      ResourceLocation baseTexture,
                                      ResourceLocation explodingTexture,
                                      float limbSwing, float limbSwingAmount, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scaleFactor) {
        float dissolve = Math.min(deathTime, 400) / 400.0F;
        Minecraft minecraft = Minecraft.getMinecraft();
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int previousAlphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float previousAlphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        try {
            GlStateManager.enableAlpha();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.alphaFunc(GL11.GL_GEQUAL, dissolve);
            minecraft.getTextureManager().bindTexture(explodingTexture);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                    scaleFactor);

            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            minecraft.getTextureManager().bindTexture(baseTexture);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                    scaleFactor);
        } finally {


            GlStateManager.depthFunc(previousDepthFunc);
            GlStateManager.alphaFunc(previousAlphaFunc, previousAlphaReference);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }


    public static boolean isDistantStorm(Entity entity) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null) return false;
        double distance = minecraft.player.getDistance(entity);
        float renderDistance = minecraft.gameSettings.renderDistanceChunks / 16.0F;
        return distance > 200.0F * renderDistance;
    }

    @Override
    protected void preRenderCallback(WitherStormEntity entity, float partialTickTime) {
        float scale = 2.0F;
        int shrinkingTicks = Math.max(0, entity.getInvulnerableTicks() - 750);
        if (shrinkingTicks > 0) {
            int duration = Math.max(1, entity.getStartingInvulnerableTicks() - 750);
            scale -= (shrinkingTicks - partialTickTime) / duration * 0.5F;
        }
        GlStateManager.scale(scale, scale, scale);
    }

    @Override
    protected void applyRotations(WitherStormEntity entity, float ageInTicks,
                                  float rotationYaw, float partialTicks) {


        GlStateManager.rotate(180.0F - entity.getBodyYRotation(partialTicks), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.getBodyXRotation(partialTicks), 1.0F, 0.0F, 0.0F);
    }

    private void renderDebrisClusters(WitherStormEntity entity, double x, double y, double z,
                                      float partialTicks) {
        if (!WitherStormClientConfig.renderDebrisCloud || entity.getDebrisClusters().isEmpty()) return;
        if (!WitherStormClientConfig.renderDistantDebris && isDistantStorm(entity)) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);


        bindTexture(getBaseTexture(entity));
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        setLightmap(entity);
        Map<DebrisCluster, DebrisBufferCache> entityCaches = debrisBufferCaches.get(entity);
        if (entityCaches == null) {
            entityCaches = new IdentityHashMap<DebrisCluster, DebrisBufferCache>();
            debrisBufferCaches.put(entity, entityCaches);
        }

        for (DebrisCluster cluster : entity.getDebrisClusters()) {
            if (cluster.isDisabled() || cluster.getRenderPhase() > entity.getPhase()) continue;
            boolean glowing = cluster.isForcedGlowing()
                    || cluster.isGlowing() && entity.getPhase() > 5;
            boolean restoreFog = glowing && GL11.glIsEnabled(GL11.GL_FOG);
            float minimumU = glowing ? 0.3125F : 0.9F;
            float minimumV = glowing ? 0.3125F : 0.8F;
            float maximumU = glowing ? 0.375F : 1.0F;
            float maximumV = glowing ? 0.375F : 0.9F;

            GlStateManager.pushMatrix();
            if (glowing) GlStateManager.disableFog();
            GlStateManager.translate(0.0F, cluster.getVerticalOffset(), 0.0F);
            GlStateManager.rotate(cluster.getOrbitalAngle(partialTicks), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(cluster.getRadiusFromCenter(), 0.0F, 0.0F);
            GlStateManager.rotate(cluster.getPitch(partialTicks), 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(cluster.getYaw(partialTicks), 0.0F, 1.0F, 0.0F);
            LegacyRenderBufferer.ManagedVertexBuffer buffered = null;
            if (LegacyRenderBufferer.INSTANCE.shouldUse()) {
                buffered = getBufferedDebris(entityCaches, cluster, glowing,
                        minimumU, minimumV, maximumU, maximumV);
            }
            if (buffered != null) {
                buffered.draw(1.0F, 1.0F, 1.0F, 1.0F, false);
            } else {
                drawDebrisPieces(cluster, minimumU, minimumV, maximumU, maximumV, 1.0F);
            }

            if (glowing) {
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GlStateManager.depthMask(false);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                if (buffered != null) {
                    buffered.draw(1.0F, 1.0F, 1.0F, 1.0F, false);
                } else {
                    drawDebrisPieces(cluster, minimumU, minimumV, maximumU, maximumV, 1.0F);
                }
                GlStateManager.depthMask(true);
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.disableBlend();
                setLightmap(entity);
                GlStateManager.enableLighting();
            }
            if (restoreFog) GlStateManager.enableFog();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    private LegacyRenderBufferer.ManagedVertexBuffer getBufferedDebris(
            Map<DebrisCluster, DebrisBufferCache> entityCaches, DebrisCluster cluster,
            boolean glowing, float minimumU, float minimumV, float maximumU, float maximumV) {
        DebrisBufferCache cache = entityCaches.get(cluster);
        if (cache == null || cache.glowing != glowing || cache.hasClosedBuffer()) {
            if (cache != null) cache.close();
            cache = createDebrisCache(cluster, glowing, minimumU, minimumV, maximumU, maximumV);
            entityCaches.put(cluster, cache);
        }
        return cache.prepareBuffer();
    }

    private static DebrisBufferCache createDebrisCache(DebrisCluster cluster, boolean glowing,
                                                        float minimumU, float minimumV,
                                                        float maximumU, float maximumV) {
        List<DebrisPieceSnapshot> pieces = new ArrayList<DebrisPieceSnapshot>();
        for (DebrisCluster.Piece piece : cluster.getPieces()) {
            pieces.add(new DebrisPieceSnapshot(piece.getX(), piece.getY(), piece.getZ(), piece.getSize()));
        }
        if (LegacyRenderBufferer.INSTANCE.shouldBuildAsynchronously()) {
            Future<LegacyRenderBufferer.CpuBuffer> future = LegacyRenderBufferer.INSTANCE.submit(
                    () -> buildDebrisBuffer(pieces, minimumU, minimumV, maximumU, maximumV));
            return new DebrisBufferCache(glowing, future, null);
        }
        try {
            return new DebrisBufferCache(glowing, null,
                    buildDebrisBuffer(pieces, minimumU, minimumV, maximumU, maximumV));
        } catch (RuntimeException exception) {
            WitherStormMod.LOGGER.error("Failed to build cached Wither Storm debris geometry", exception);
            return new DebrisBufferCache(glowing, null, null, true);
        }
    }

    private static LegacyRenderBufferer.CpuBuffer buildDebrisBuffer(
            List<DebrisPieceSnapshot> pieces, float minimumU, float minimumV,
            float maximumU, float maximumV) {
        BufferBuilder buffer = new BufferBuilder(Math.max(256, pieces.size() * 24 * 7));
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        for (DebrisPieceSnapshot piece : pieces) {
            addDebrisCube(buffer, piece.x, piece.y, piece.z, piece.size,
                    minimumU, minimumV, maximumU, maximumV, 1.0F);
        }
        return LegacyRenderBufferer.finish(buffer);
    }

    private void clearDebrisBufferCaches() {
        for (Map<DebrisCluster, DebrisBufferCache> entityCaches : debrisBufferCaches.values()) {
            for (DebrisBufferCache cache : entityCaches.values()) cache.close();
            entityCaches.clear();
        }
        debrisBufferCaches.clear();
    }

    private static void drawDebrisPieces(DebrisCluster cluster, float minimumU, float minimumV,
                                         float maximumU, float maximumV, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        for (DebrisCluster.Piece piece : cluster.getPieces()) {
            addDebrisCube(buffer, piece, minimumU, minimumV, maximumU, maximumV, alpha);
        }
        tessellator.draw();
    }

    private static void addDebrisCube(BufferBuilder buffer, DebrisCluster.Piece piece,
                                      float minimumU, float minimumV, float maximumU,
                                      float maximumV, float alpha) {
        addDebrisCube(buffer, piece.getX(), piece.getY(), piece.getZ(), piece.getSize(),
                minimumU, minimumV, maximumU, maximumV, alpha);
    }

    private static void addDebrisCube(BufferBuilder buffer, float x, float y, float z, float size,
                                      float minimumU, float minimumV, float maximumU,
                                      float maximumV, float alpha) {
        float minimumX = x - size;
        float maximumX = x + size;
        float minimumY = y - size;
        float maximumY = y + size;
        float minimumZ = z - size;
        float maximumZ = z + size;

        addVertex(buffer, maximumX, maximumY, minimumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, minimumY, minimumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, minimumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, maximumY, minimumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);

        addVertex(buffer, minimumX, maximumY, minimumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, minimumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, maximumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, maximumY, maximumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);

        addVertex(buffer, maximumX, maximumY, maximumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, minimumY, maximumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, minimumY, minimumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, maximumY, minimumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);

        addVertex(buffer, minimumX, maximumY, maximumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, maximumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, minimumY, maximumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, maximumY, maximumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);

        addVertex(buffer, maximumX, maximumY, maximumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, maximumY, minimumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, maximumY, minimumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, maximumY, maximumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);

        addVertex(buffer, maximumX, minimumY, maximumZ, minimumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, maximumZ, minimumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, minimumX, minimumY, minimumZ, maximumU, maximumV, alpha, 0.0F, -1.0F, 0.0F);
        addVertex(buffer, maximumX, minimumY, minimumZ, maximumU, minimumV, alpha, 0.0F, -1.0F, 0.0F);
    }

    private static void addVertex(BufferBuilder buffer, float x, float y, float z,
                                  float textureU, float textureV, float alpha,
                                  float normalX, float normalY, float normalZ) {
        buffer.pos(x, y, z).tex(textureU, textureV).color(1.0F, 1.0F, 1.0F, alpha)
                .normal(normalX, normalY, normalZ).endVertex();
    }

    public static void renderDebrisRings(WitherStormEntity entity, float partialTicks,
                                         double viewerX, double viewerY, double viewerZ) {
        if (!WitherStormClientConfig.renderDebrisRings || entity.getDebrisRings().isEmpty()
                || WitherStormClientConfig.hideDebrisRingsUntilSplit && entity.getPhase() <= 5
                || !WitherStormClientConfig.renderDistantDebris && isDistantStorm(entity)) return;
        double renderX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - viewerX;
        double renderY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - viewerY;
        double renderZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - viewerZ;
        double distance = Minecraft.getMinecraft().getRenderViewEntity() == null ? 0.0D
                : Minecraft.getMinecraft().getRenderViewEntity().getDistance(entity);
        float baseAlpha = MathHelper.clamp((float) (400.0D - distance) * 0.005F, 0.2F, 0.5F);

        boolean manageFog = DistantProjection.shouldUse(entity);
        DistantProjection.FogState previousFog = manageFog
                ? DistantProjection.pushDistantFog(entity, WitherStormClientConfig.distantFog) : null;
        GlStateManager.pushMatrix();
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            GlStateManager.translate(renderX, renderY, renderZ);
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                    WitherStormResourceConfigManager.INSTANCE
                            .getTextureSetByPhase(entity.getPhase()).getDebrisRing());
            GlStateManager.disableLighting();


            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableDepth();

            GlStateManager.depthFunc(GL11.GL_LESS);
            GlStateManager.depthMask(true);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (DebrisRingSettings settings : entity.getDebrisRings()) {
                float alpha = settings.getAlpha() * baseAlpha;
                if (alpha <= 0.0F || entity.getPhase() < settings.getPhaseRequirement()) continue;
                float rotation = (entity.ticksExisted + partialTicks) * settings.getSpeedModifier()
                        * (settings.isClockwise() ? 1.0F : -1.0F);
                for (int segment = 0; segment < settings.getSegments(); segment++) {
                    float step = (float) (Math.PI * 2.0D / settings.getSegments());
                    float firstAngle = step * segment + rotation;
                    float secondAngle = step * (segment + 1) + rotation;
                    float firstX = MathHelper.cos(firstAngle);
                    float firstZ = MathHelper.sin(firstAngle);
                    float secondX = MathHelper.cos(secondAngle);
                    float secondZ = MathHelper.sin(secondAngle);
                    addRingVertex(buffer, firstX * settings.getBottomRadius(), settings.getBottomY(),
                            firstZ * settings.getBottomRadius(), 0.0F, 0.0F, alpha);
                    addRingVertex(buffer, firstX * settings.getTopRadius(), settings.getTopY(),
                            firstZ * settings.getTopRadius(), 0.0F, 1.0F, alpha);
                    addRingVertex(buffer, secondX * settings.getTopRadius(), settings.getTopY(),
                            secondZ * settings.getTopRadius(), 1.0F, 1.0F, alpha);
                    addRingVertex(buffer, secondX * settings.getBottomRadius(), settings.getBottomY(),
                            secondZ * settings.getBottomRadius(), 1.0F, 0.0F, alpha);

                }
            }
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(previousDepthFunc);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();


            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            if (manageFog) DistantProjection.restoreFog(previousFog);
        }
    }

    private static void addRingVertex(BufferBuilder buffer, float x, float y, float z,
                                      float textureU, float textureV, float alpha) {
        buffer.pos(x, y, z).tex(textureU, textureV)
                .color(1.0F, 1.0F, 1.0F, alpha).endVertex();
    }

    public static void renderShine(WitherStormEntity entity, float partialTicks,
                                   double viewerX, double viewerY, double viewerZ) {
        float stateAlpha = entity.getShineAlpha(partialTicks);
        if (!WitherStormClientConfig.renderShine || !entity.shouldShine() || stateAlpha <= 0.0F) return;
        double renderX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - viewerX;
        double renderY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - viewerY;
        double renderZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - viewerZ;
        double distance = Math.sqrt(entity.getDistanceSq(viewerX, viewerY, viewerZ));

        float distanceAlpha = 1.0F;
        if (entity.getPhase() > 5
                || entity.getConsumedMass() >= entity.getConsumptionAmountForPhase(5)) {
            float renderDistanceFactor = Minecraft.getMinecraft().gameSettings.renderDistanceChunks / 8.0F;
            distanceAlpha = MathHelper.clamp(
                    (float) (distance - 200.0F * renderDistanceFactor) * 0.005F, 0.0F, 1.0F);
        }
        float nightAlpha = getNightTimeAlpha(entity, partialTicks);
        float alpha = 75.0F / 255.0F * nightAlpha * distanceAlpha * stateAlpha;
        if (alpha <= 0.0F) return;

        float red = 150.0F / 255.0F;
        float green = 59.0F / 255.0F;
        float blue = 1.0F;
        if (entity.hasCustomName() && "jeb_".equals(entity.getCustomNameTag())) {
            float[] rainbow = getRainbowColor(entity, partialTicks);
            red = rainbow[0];
            green = rainbow[1];
            blue = rainbow[2];
        }

        float scale = entity.getShineScale();
        float width = scale * (entity.getPhase() > 5 ? 1.5F : 1.0F);
        float verticalCenter = entity.getUnmodifiedHeight() * 0.5F;
        Minecraft minecraft = Minecraft.getMinecraft();

        GlStateManager.pushMatrix();
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            GlStateManager.translate(renderX, renderY + verticalCenter, renderZ);
            GlStateManager.rotate(-minecraft.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(minecraft.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, scale * 0.5F);
            minecraft.getTextureManager().bindTexture(SHINE_TEXTURE);
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(false);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(-width * 0.5F, -scale * 0.5F, 0.0D).tex(0.0D, 1.0D)
                    .color(red, green, blue, alpha).endVertex();
            buffer.pos(width * 0.5F, -scale * 0.5F, 0.0D).tex(1.0D, 1.0D)
                    .color(red, green, blue, alpha).endVertex();
            buffer.pos(width * 0.5F, scale * 0.5F, 0.0D).tex(1.0D, 0.0D)
                    .color(red, green, blue, alpha).endVertex();
            buffer.pos(-width * 0.5F, scale * 0.5F, 0.0D).tex(0.0D, 0.0D)
                    .color(red, green, blue, alpha).endVertex();
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(previousDepthFunc);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }


    static void renderDeathWireframe(int deathTime, float unmodifiedHeight, double x, double y,
                                     double z, float partialTicks) {
        float f1 = (deathTime + partialTicks) / 200.0F;
        float f2 = Math.min(f1 > 1.6F ? (f1 - 1.6F) / 0.2F : 0.0F, 1.0F);
        Random random = new Random(382L);
        int count = (int) ((f1 + f1 * f1) / 2.0F * 60.0F);
        if (count <= 0) return;
        float size = unmodifiedHeight;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + unmodifiedHeight / 2.0D, z);
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        for (int index = 0; index < count; index++) {
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F + f1 * 90.0F, 0.0F, 0.0F, 1.0F);
            float f3 = random.nextFloat() * (size / 1.5F) * 2.5F + 5.0F + f2 * 10.0F;
            float f4 = random.nextFloat() * 10.0F + 1.0F + f2 * 2.0F;
            int alpha = (int) (255.0F * (1.0F - f2));
            float halfRootThree = (float) (Math.sqrt(3.0D) / 2.0D) * f4;
            float half = -0.5F * f4;
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, -halfRootThree, f3, half, 0);
            addDeathVertex(buffer, halfRootThree, f3, half, 0);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, halfRootThree, f3, half, 0);
            addDeathVertex(buffer, 0.0F, f3, f4, 0);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, 0.0F, 0.0F, 0.0F, alpha);
            addDeathVertex(buffer, 0.0F, f3, f4, 0);
            addDeathVertex(buffer, -halfRootThree, f3, half, 0);
            tessellator.draw();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void addDeathVertex(BufferBuilder buffer, float x, float y, float z,
                                       int alpha) {
        buffer.pos(x, y, z).color(1.0F, 1.0F, 1.0F, alpha / 255.0F).endVertex();
    }

    private static float getNightTimeAlpha(WitherStormEntity entity, float partialTicks) {
        float daylightWave = MathHelper.cos(entity.world.getCelestialAngle(partialTicks)
                * (float) (Math.PI * 2.0D)) * 2.0F + 0.5F;
        return 1.0F - MathHelper.clamp(daylightWave + 0.5F, 0.0F, 1.0F);
    }

    static float[] getRainbowColor(Entity entity, float partialTicks) {
        return getRainbowColor(entity, 0, partialTicks);
    }

    static float[] getRainbowColor(Entity entity, int offset, float partialTicks) {
        int colorCount = EnumDyeColor.values().length;
        int colorTick = entity.ticksExisted / 25 + entity.getEntityId() + offset;
        int firstIndex = Math.floorMod(colorTick, colorCount);
        int secondIndex = Math.floorMod(colorTick + 1, colorCount);
        float progress = (entity.ticksExisted % 25 + partialTicks) / 25.0F;
        float[] first = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata(firstIndex));
        float[] second = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata(secondIndex));
        return new float[]{
                first[0] * (1.0F - progress) + second[0] * progress,
                first[1] * (1.0F - progress) + second[1] * progress,
                first[2] * (1.0F - progress) + second[2] * progress
        };
    }

    private static Form fetchForm(WitherStormEntity entity) {
        int phase = entity.getPhase();
        int consumed = entity.getConsumedMass();
        if (phase == 1) {
            if (consumed >= entity.adjustAmountForEvolutionSpeed(250)) return Form.HUNCHBACK_1_2;
            if (consumed >= entity.adjustAmountForEvolutionSpeed(150)) return Form.HUNCHBACK_1_1;
            return Form.HUNCHBACK_1;
        }
        if (phase == 2) {
            return consumed >= entity.adjustAmountForEvolutionSpeed(800)
                    ? Form.HUNCHBACK_2_1 : Form.GROWING_HUNCHBACK;
        }
        if (phase == 3) {
            if (consumed >= entity.adjustAmountForEvolutionSpeed(3500)) return Form.HUNCHBACK_3_2;
            if (consumed >= entity.adjustAmountForEvolutionSpeed(2350)) return Form.HUNCHBACK_3_1;
            return Form.PREGNANT_HUNCHBACK;
        }
        if (phase == 4) {
            return consumed <= entity.getSubPhaseRequirement(phase) ? Form.DESTROYER : Form.INTERMEDIATE_EVOLVED_DESTROYER;
        }
        if (phase == 5) {
            if (consumed > entity.getPhaseRequirement()) return Form.DEVOURER;
            return consumed <= entity.getSubPhaseRequirement(phase) ? Form.EVOLVED_DESTROYER : Form.INTERMEDIATE_DEVOURER;
        }
        if (phase == 6) {
            return consumed <= entity.getSubPhaseRequirement(phase) ? Form.DISMANTLED : Form.INTERMEDIATE_EVOLVED_DEVOURER;
        }
        if (phase == 7) return entity.isBeingTornApart() ? Form.TORN_EVOLVED_DEVOURER : Form.EVOLVED_DEVOURER;
        return Form.COMMAND_BLOCK;
    }

    private static final class DebrisPieceSnapshot {
        private final float x;
        private final float y;
        private final float z;
        private final float size;

        private DebrisPieceSnapshot(float x, float y, float z, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
        }
    }

    private static final class DebrisBufferCache {
        private final boolean glowing;
        private Future<LegacyRenderBufferer.CpuBuffer> pending;
        private LegacyRenderBufferer.CpuBuffer built;
        private LegacyRenderBufferer.ManagedVertexBuffer buffer;
        private boolean failed;

        private DebrisBufferCache(boolean glowing, Future<LegacyRenderBufferer.CpuBuffer> pending,
                                  LegacyRenderBufferer.CpuBuffer built) {
            this(glowing, pending, built, false);
        }

        private DebrisBufferCache(boolean glowing, Future<LegacyRenderBufferer.CpuBuffer> pending,
                                  LegacyRenderBufferer.CpuBuffer built, boolean failed) {
            this.glowing = glowing;
            this.pending = pending;
            this.built = built;
            this.failed = failed;
        }

        private LegacyRenderBufferer.ManagedVertexBuffer prepareBuffer() {
            if (failed) return null;
            if (pending != null) {
                if (!pending.isDone()) return null;
                try {
                    built = pending.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failed = true;
                } catch (ExecutionException | CancellationException exception) {
                    WitherStormMod.LOGGER.error("Failed to build cached Wither Storm debris geometry",
                            exception instanceof ExecutionException
                                    ? ((ExecutionException) exception).getCause() : exception);
                    failed = true;
                }
                pending = null;
                if (failed) return null;
            }
            if (built != null) {
                buffer = LegacyRenderBufferer.INSTANCE.upload(built);
                built = null;
            }
            return buffer;
        }

        private boolean hasClosedBuffer() {
            return buffer != null && buffer.isClosed();
        }

        private void close() {
            if (pending != null) pending.cancel(false);
            pending = null;
            built = null;
            if (buffer != null) buffer.close();
            buffer = null;
        }
    }
}
