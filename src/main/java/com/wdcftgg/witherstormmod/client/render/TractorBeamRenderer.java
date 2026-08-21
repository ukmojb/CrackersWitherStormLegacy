package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.resources.WitherStormResourceConfigManager;
import com.wdcftgg.witherstormmod.client.resources.color.ColorSet;
import com.wdcftgg.witherstormmod.client.util.SpecialDay;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.TractorBeamProvider;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.Color;


/** 绘制主风暴、分裂体和独立头共用的发光方形牵引光束。 */
@SideOnly(Side.CLIENT)
public final class TractorBeamRenderer {
    private static final double MINIMUM_DIRECTION_LENGTH = 0.0001D;
    private static final float MAXIMUM_ATTACHED_HEAD_YAW = 80.0F;
    private static final float DEFAULT_RED = 128.0F / 255.0F;
    private static final float DEFAULT_GREEN = 77.0F / 255.0F;
    private static final float DEFAULT_BLUE = 204.0F / 255.0F;
    private static final float HALLOWEEN_RED = 135.0F / 255.0F;
    private static final float HALLOWEEN_GREEN = 82.0F / 255.0F;
    private static final float HALLOWEEN_BLUE = 28.0F / 255.0F;
    private static final SpecialDay SPECIAL_DAY = SpecialDay.getForCurrentDate();
    private static final double[][] LARGE_STORM_HEAD_ROOTS = {
            {0.0D, -32.0D, -23.0D},
            {32.0D, -60.0D, -24.0D},
            {-22.0D, -65.0D, -40.0D}
    };
    private static final double[][] SEGMENT_HEAD_ROOTS = {
            {0.0D, -23.0D, -35.0D},
            {-16.0D, -20.0D, -30.0D},
            {16.0D, -20.0D, -30.0D}
    };
    private static final double[] ZERO_HEAD_ROOT = {0.0D, 0.0D, 0.0D};

    private static final BeamShape EARLY_STORM = new BeamShape(
            0.7F, 90.0F, 0.1F, 5.0F, -1.0F, 34.0F, -19.0F, 1.85F);
    private static final BeamShape LARGE_STORM = new BeamShape(
            3.0F, 90.0F, 0.1F, 5.0F, -1.0F, 8.0F, -4.0F, 0.325F);
    private static final BeamShape INDEPENDENT_HEAD = new BeamShape(
            3.0F, 20.0F, 0.1F, 2.0F, 1.0F, 8.0F, -4.0F, 0.325F);

    private TractorBeamRenderer() {
    }

    public static void renderAll(Iterable<Entity> entities, float partialTicks,
                                 double viewerX, double viewerY, double viewerZ) {
        if (!WitherStormClientConfig.renderTractorBeams
                || !containsRenderableBeam(entities, false)
                && !containsRenderableBeam(entities, true)) return;

        renderPass(entities, partialTicks, viewerX, viewerY, viewerZ, false);
        if (containsRenderableBeam(entities, true)) {
            DistantProjection.push();
            try {
                renderPass(entities, partialTicks, viewerX, viewerY, viewerZ, true);
            } finally {
                DistantProjection.pop();
            }
        }
    }

    private static void renderPass(Iterable<Entity> entities, float partialTicks,
                                   double viewerX, double viewerY, double viewerZ,
                                   boolean distantPass) {
        if (!containsRenderableBeam(entities, distantPass)) return;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        // 光束是发光效果，不应被原版雾按视角远近压暗；显式保存/恢复，
        // 避免只依赖 pushAttrib 时 GlStateManager 的雾缓存与实际 GL 状态脱节。
        boolean fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        try {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.disableFog();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (Entity entity : entities) {
                if (!(entity instanceof TractorBeamProvider)
                        || DistantProjection.shouldUse(entity) != distantPass) continue;
                appendEntityBeams(buffer, entity, (TractorBeamProvider) entity,
                        partialTicks, viewerX, viewerY, viewerZ);
            }
            tessellator.draw();
        } finally {
            if (fogEnabled) GlStateManager.enableFog();
            GlStateManager.popAttrib();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static boolean containsRenderableBeam(Iterable<Entity> entities,
                                                  boolean distantPass) {
        for (Entity entity : entities) {
            if (!(entity instanceof TractorBeamProvider) || entity.isDead
                    || DistantProjection.shouldUse(entity) != distantPass) continue;
            TractorBeamProvider provider = (TractorBeamProvider) entity;
            if (provider.isDeadOrPlayingDead()) continue;
            for (int head = 0; head < provider.getTotalHeads(); head++) {
                if (provider.tractorBeamActive(head)) return true;
            }
        }
        return false;
    }

    private static void appendEntityBeams(BufferBuilder buffer, Entity entity,
                                          TractorBeamProvider provider, float partialTicks,
                                          double viewerX, double viewerY, double viewerZ) {
        if (entity.isDead || provider.isDeadOrPlayingDead()) return;
        BeamShape shape = getShape(entity);
        if (shape == null) return;

        float endAlpha = shape.fixedEndAlpha >= 0.0F
                ? shape.fixedEndAlpha : 0.5F * (1.0F - getDistanceFade(entity, partialTicks,
                viewerX, viewerY, viewerZ));
        for (int head = 0; head < provider.getTotalHeads(); head++) {
            if (!provider.tractorBeamActive(head)) continue;
            float[] color = applyDistantFog(entity,
                    getColor(entity, partialTicks, head), partialTicks);
            BeamPose pose = calculateModelBeamPose(entity, provider, head, partialTicks, shape);
            if (pose == null) continue;
            Vec3d origin = pose.origin;
            Vec3d direction = pose.direction;

            float modelScale = shape.headScale;
            double cutoff = provider.getTractorBeamCutoffDistance(head, partialTicks);
            double length = cutoff >= 0.0D
                    ? cutoff + 10.0D * modelScale : shape.distance * modelScale;
            if (length <= 0.01D) continue;
            double startRadius = shape.startSize * modelScale;
            double endRadius = length * shape.endSize / shape.distance;
            appendBeam(buffer, origin, direction, length, startRadius, endRadius,
                    color[0], color[1], color[2], 0.5F, endAlpha,
                    viewerX, viewerY, viewerZ);
        }
    }

    /** Replays HeadModel.renderTractorBeam's complete pose stack in world space. */
    private static BeamPose calculateModelBeamPose(Entity entity, TractorBeamProvider provider,
                                                    int head, float partialTicks,
                                                    BeamShape shape) {
        Vec3d look = provider.getHeadDirectionForBeam(head, partialTicks);
        if (look == null || look.lengthSquared() <= MINIMUM_DIRECTION_LENGTH) return null;
        look = look.normalize();

        if (!(entity instanceof EntityLivingBase)) return null;
        EntityLivingBase living = (EntityLivingBase) entity;
        float bodyYaw = interpolateRotation(living.prevRenderYawOffset,
                living.renderYawOffset, partialTicks);
        float bodyPitch = getBodyPitch(entity, partialTicks);
        float headYaw = (float) (MathHelper.atan2(-look.x, look.z) * 180.0D / Math.PI);
        float headPitch = (float) (Math.asin(MathHelper.clamp(-look.y, -1.0D, 1.0D))
                * 180.0D / Math.PI);
        float relativeHeadYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);
        if (shouldConstrainAttachedHeadYaw(entity)) {
            relativeHeadYaw = MathHelper.clamp(relativeHeadYaw,
                    -MAXIMUM_ATTACHED_HEAD_YAW, MAXIMUM_ATTACHED_HEAD_YAW);
        }
        double modelYaw = Math.PI + Math.toRadians(relativeHeadYaw);
        double modelPitch = -Math.toRadians(headPitch);
        double modelRoll = getHeadRoll(entity, head, partialTicks);
        double[] root = getHeadRoot(entity, head);

        double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        Vec3d entityPosition = new Vec3d(x, y, z);
        Vec3d origin = transformBeamPoint(Vec3d.ZERO, entityPosition, root, shape,
                bodyYaw, bodyPitch, modelYaw, modelPitch, modelRoll);
        Vec3d forwardPoint = transformBeamPoint(new Vec3d(-1.0D, 0.0D, 0.0D),
                entityPosition, root, shape, bodyYaw, bodyPitch,
                modelYaw, modelPitch, modelRoll);
        Vec3d direction = forwardPoint.subtract(origin);
        return direction.lengthSquared() <= MINIMUM_DIRECTION_LENGTH
                ? null : new BeamPose(origin, direction.normalize());
    }

    private static Vec3d transformBeamPoint(Vec3d localPoint, Vec3d entityPosition,
                                            double[] root, BeamShape shape,
                                            float bodyYaw, float bodyPitch,
                                            double modelYaw, double modelPitch,
                                            double modelRoll) {
        double scale = shape.headScale;
        Vec3d point = localPoint.add(shape.pivotOffsetX * scale / 8.0D,
                shape.pivotOffsetY * scale / 8.0D, 0.0D);
        point = rotateZ(point, -modelPitch);
        point = rotateY(point, -modelYaw - Math.PI / 2.0D);
        point = rotateZ(point, -modelRoll).scale(scale);
        point = point.add(root[0] * scale / 8.0D,
                -(root[1] - shape.tractorBeamYOffset) * scale / 8.0D,
                -root[2] * scale / 8.0D);
        point = rotateX(point, -Math.toRadians(bodyPitch));
        point = rotateY(point, -Math.toRadians(bodyYaw));
        return entityPosition.add(point);
    }

    private static double[] getHeadRoot(Entity entity, int head) {
        int index = MathHelper.clamp(head, 0, 2);
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return SEGMENT_HEAD_ROOTS[index];
        }
        if (entity instanceof WitherStormEntity
                && ((WitherStormEntity) entity).getPhase() > 3) {
            return LARGE_STORM_HEAD_ROOTS[index];
        }
        return ZERO_HEAD_ROOT;
    }

    private static boolean shouldConstrainAttachedHeadYaw(Entity entity) {
        return entity instanceof SupplementalEntities.WitherStormSegmentEntity
                || entity instanceof WitherStormEntity
                && ((WitherStormEntity) entity).getPhase() > 3;
    }

    private static float getBodyPitch(Entity entity, float partialTicks) {
        if (entity instanceof WitherStormEntity) {
            return ((WitherStormEntity) entity).getBodyXRotation(partialTicks);
        }
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return ((SupplementalEntities.WitherStormSegmentEntity) entity)
                    .getBodyXRotation(partialTicks);
        }
        return 0.0F;
    }

    private static float getHeadRoll(Entity entity, int head, float partialTicks) {
        if (entity instanceof WitherStormEntity) {
            return ((WitherStormEntity) entity).getHeadShakeAnimation(head, partialTicks);
        }
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return ((SupplementalEntities.WitherStormSegmentEntity) entity)
                    .getHeadShakeAnimation(head, partialTicks);
        }
        if (entity instanceof SupplementalEntities.WitherStormHeadEntity) {
            return ((SupplementalEntities.WitherStormHeadEntity) entity)
                    .getHeadShakeAnimation(partialTicks);
        }
        return 0.0F;
    }

    private static float interpolateRotation(float start, float end, float partialTicks) {
        return start + MathHelper.wrapDegrees(end - start) * partialTicks;
    }

    private static Vec3d rotateX(Vec3d vector, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Vec3d(vector.x, vector.y * cos - vector.z * sin,
                vector.y * sin + vector.z * cos);
    }

    private static Vec3d rotateY(Vec3d vector, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Vec3d(vector.x * cos + vector.z * sin, vector.y,
                -vector.x * sin + vector.z * cos);
    }

    private static Vec3d rotateZ(Vec3d vector, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Vec3d(vector.x * cos - vector.y * sin,
                vector.x * sin + vector.y * cos, vector.z);
    }

    private static void appendBeam(BufferBuilder buffer, Vec3d origin, Vec3d direction,
                                   double length, double startRadius, double endRadius,
                                   float red, float green, float blue,
                                   float startAlpha, float endAlpha,
                                   double viewerX, double viewerY, double viewerZ) {
        Vec3d reference = Math.abs(direction.y) > 0.99D
                ? new Vec3d(1.0D, 0.0D, 0.0D) : new Vec3d(0.0D, 1.0D, 0.0D);
        Vec3d horizontal = direction.crossProduct(reference).normalize();
        Vec3d vertical = horizontal.crossProduct(direction).normalize();
        Vec3d end = origin.add(direction.scale(length));

        Vec3d[] startCorners = createCorners(origin, horizontal, vertical, startRadius);
        Vec3d[] endCorners = createCorners(end, horizontal, vertical, endRadius);
        appendSide(buffer, startCorners[0], startCorners[1], endCorners[1], endCorners[0],
                red, green, blue, startAlpha, endAlpha, viewerX, viewerY, viewerZ);
        appendSide(buffer, startCorners[3], startCorners[2], endCorners[2], endCorners[3],
                red, green, blue, startAlpha, endAlpha, viewerX, viewerY, viewerZ);
        appendSide(buffer, startCorners[3], startCorners[0], endCorners[0], endCorners[3],
                red, green, blue, startAlpha, endAlpha, viewerX, viewerY, viewerZ);
        appendSide(buffer, startCorners[1], startCorners[2], endCorners[2], endCorners[1],
                red, green, blue, startAlpha, endAlpha, viewerX, viewerY, viewerZ);
        appendCap(buffer, startCorners, red, green, blue, startAlpha,
                viewerX, viewerY, viewerZ);
    }

    private static Vec3d[] createCorners(Vec3d center, Vec3d horizontal,
                                         Vec3d vertical, double radius) {
        Vec3d horizontalOffset = horizontal.scale(radius);
        Vec3d verticalOffset = vertical.scale(radius);
        return new Vec3d[]{
                center.add(horizontalOffset).add(verticalOffset),
                center.subtract(horizontalOffset).add(verticalOffset),
                center.subtract(horizontalOffset).subtract(verticalOffset),
                center.add(horizontalOffset).subtract(verticalOffset)
        };
    }

    private static void appendSide(BufferBuilder buffer, Vec3d firstStart, Vec3d secondStart,
                                   Vec3d secondEnd, Vec3d firstEnd,
                                   float red, float green, float blue,
                                   float startAlpha, float endAlpha,
                                   double viewerX, double viewerY, double viewerZ) {
        appendVertex(buffer, firstStart, red, green, blue, startAlpha, viewerX, viewerY, viewerZ);
        appendVertex(buffer, secondStart, red, green, blue, startAlpha, viewerX, viewerY, viewerZ);
        appendVertex(buffer, secondEnd, red, green, blue, endAlpha, viewerX, viewerY, viewerZ);
        appendVertex(buffer, firstEnd, red, green, blue, endAlpha, viewerX, viewerY, viewerZ);
    }

    private static void appendCap(BufferBuilder buffer, Vec3d[] corners,
                                  float red, float green, float blue, float alpha,
                                  double viewerX, double viewerY, double viewerZ) {
        for (Vec3d corner : corners) {
            appendVertex(buffer, corner, red, green, blue, alpha, viewerX, viewerY, viewerZ);
        }
    }

    private static void appendVertex(BufferBuilder buffer, Vec3d position,
                                     float red, float green, float blue, float alpha,
                                     double viewerX, double viewerY, double viewerZ) {
        buffer.pos(position.x - viewerX, position.y - viewerY, position.z - viewerZ)
                .color(red, green, blue, alpha).endVertex();
    }

    private static BeamShape getShape(Entity entity) {
        if (entity instanceof SupplementalEntities.WitherStormHeadEntity) return INDEPENDENT_HEAD;
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) return LARGE_STORM;
        if (entity instanceof WitherStormEntity) {
            return ((WitherStormEntity) entity).getPhase() > 3 ? LARGE_STORM : EARLY_STORM;
        }
        return null;
    }

    private static float[] getColor(Entity entity, float partialTicks, int head) {
        if (entity instanceof SupplementalEntities.WitherStormHeadEntity) {
            if (SPECIAL_DAY == SpecialDay.HALLOWEEN) {
                return new float[]{HALLOWEEN_RED, HALLOWEEN_GREEN, HALLOWEEN_BLUE};
            }
            Color color = WitherStormResourceConfigManager.INSTANCE
                    .getColorSetByPhase(4).getTractorBeamColor();
            return toFloats(color);
        }

        WitherStormEntity colorSource = entity instanceof WitherStormEntity
                ? (WitherStormEntity) entity : null;
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            WitherStormEntity parent = ((SupplementalEntities.WitherStormSegmentEntity) entity)
                    .getParentStorm();
            if (parent != null) colorSource = parent;
        }
        boolean rainbow = entity.hasCustomName() && "jeb_".equals(entity.getCustomNameTag())
                || colorSource != null && colorSource.hasCustomName()
                && "jeb_".equals(colorSource.getCustomNameTag());
        if (rainbow) {
            return WitherStormRenderer.getRainbowColor(entity, head, partialTicks);
        }
        if (SPECIAL_DAY != null) return toFloats(SPECIAL_DAY.getColor(entity, partialTicks, head));
        if (colorSource != null) return colorFromSet(colorSource, partialTicks);
        return new float[]{DEFAULT_RED, DEFAULT_GREEN, DEFAULT_BLUE};
    }

    private static float[] applyDistantFog(Entity entity, float[] color, float partialTicks) {
        if (WitherStormClientConfig.disableVanillaFog
                || !WitherStormClientConfig.distantFog) return color;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) return color;
        double distance = minecraft.player.getDistance(entity);
        float renderDistance = minecraft.gameSettings.renderDistanceChunks / 16.0F;
        float distanceLerp = MathHelper.clamp(
                (float) ((distance - 200.0F * renderDistance) * 0.005D), 0.0F, 1.0F);
        float daylightWave = MathHelper.cos(entity.world.getCelestialAngle(partialTicks)
                * (float) (Math.PI * 2.0D)) * 2.0F + 0.5F;
        float nightLerp = 1.0F - MathHelper.clamp(daylightWave + 0.5F, 0.0F, 1.0F);
        float alpha = distanceLerp * (1.0F - nightLerp);
        if (alpha <= 0.0F) return color;
        return new float[]{
                color[0] + (0.3F - color[0]) * alpha,
                color[1] + (0.3F - color[1]) * alpha,
                color[2] + (0.3F - color[2]) * alpha
        };
    }

    private static float[] colorFromSet(WitherStormEntity storm, float partialTicks) {
        ColorSet set = WitherStormResourceConfigManager.INSTANCE
                .getColorSetByPhase(storm.getPhase());
        Color day = set.getTractorBeamColor();
        Color night = set.getTractorBeamNightColor();
        float nightLerp = getNightLerp(storm, partialTicks);
        return new float[]{
                (day.getRed() + (night.getRed() - day.getRed()) * nightLerp) / 255.0F,
                (day.getGreen() + (night.getGreen() - day.getGreen()) * nightLerp) / 255.0F,
                (day.getBlue() + (night.getBlue() - day.getBlue()) * nightLerp) / 255.0F
        };
    }

    private static float getNightLerp(Entity entity, float partialTicks) {
        float daylightWave = MathHelper.cos(entity.world.getCelestialAngle(partialTicks)
                * (float) (Math.PI * 2.0D)) * 2.0F + 0.5F;
        return 1.0F - MathHelper.clamp(daylightWave + 0.5F, 0.0F, 1.0F);
    }

    private static float[] toFloats(Color color) {
        return new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F,
                color.getBlue() / 255.0F};
    }

    private static float getDistanceFade(Entity entity, float partialTicks,
                                         double viewerX, double viewerY, double viewerZ) {
        double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        double distance = Math.sqrt((x - viewerX) * (x - viewerX)
                + (y - viewerY) * (y - viewerY) + (z - viewerZ) * (z - viewerZ));
        float renderDistanceScale = Minecraft.getMinecraft().gameSettings.renderDistanceChunks / 16.0F;
        return MathHelper.clamp((float) (distance - 200.0F * renderDistanceScale) * 0.005F,
                0.0F, 1.0F);
    }

    private static final class BeamShape {
        final float headScale;
        final float distance;
        final float startSize;
        final float endSize;
        final float fixedEndAlpha;
        final float tractorBeamYOffset;
        final float pivotOffsetX;
        final float pivotOffsetY;

        BeamShape(float headScale, float distance, float startSize,
                  float endSize, float fixedEndAlpha, float tractorBeamYOffset,
                  float pivotOffsetX, float pivotOffsetY) {
            this.headScale = headScale;
            this.distance = distance;
            this.startSize = startSize;
            this.endSize = endSize;
            this.fixedEndAlpha = fixedEndAlpha;
            this.tractorBeamYOffset = tractorBeamYOffset;
            this.pivotOffsetX = pivotOffsetX;
            this.pivotOffsetY = pivotOffsetY;
        }
    }

    private static final class BeamPose {
        final Vec3d origin;
        final Vec3d direction;

        BeamPose(Vec3d origin, Vec3d direction) {
            this.origin = origin;
            this.direction = direction;
        }
    }
}
