package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartDefinition;
import com.wdcftgg.witherstormmod.client.model.witherstorm.WitherStormModelDefinitions;
import com.wdcftgg.witherstormmod.client.render.WitherStormRenderer;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;


public final class WitherStormSegmentModel extends ModelBase {
    private static final int[] GEOMETRY_TO_HEAD = {2, 0, 1};
    private static final float[] HEAD_ANIMATION_OFFSETS = {0.0F, 175.0F, 100.0F};
    private static final float MAXIMUM_HEAD_YAW = 80.0F;
    private final PartDefinition rootDefinition;
    private final ModelRenderer mass;
    private final ModelRenderer lowResMass;
    private final List<ModelRenderer> heads = new ArrayList<ModelRenderer>();
    private final List<TentacleParts> tentacles = new ArrayList<TentacleParts>();

    public WitherStormSegmentModel() {
        textureWidth = 160;
        textureHeight = 160;
        ModelRenderer root = new ModelRenderer(this);
        rootDefinition = new PartDefinition(this, root);
        WitherStormModelDefinitions.initializeRoot(rootDefinition);
        WitherStormModelDefinitions.buildSegment(rootDefinition);
        mass = renderer(rootDefinition.child("mass"));
        lowResMass = renderer(rootDefinition.child("lowResMass"));
        collectHeads();
        collectTentacles();
        configureTentacles();
    }

    private void collectHeads() {
        PartDefinition definition = rootDefinition.child("heads");
        if (definition == null) return;
        for (PartDefinition child : definition.children().values()) heads.add(child.renderer());
    }

    private void collectTentacles() {
        PartDefinition definition = rootDefinition.child("tentacles");
        if (definition == null) return;
        for (Map.Entry<String, PartDefinition> entry : definition.children().entrySet()) {
            PartDefinition base = entry.getValue().child("base");
            if (base != null) tentacles.add(new TentacleParts(base));
        }
    }

    private void configureTentacles() {
        float[] scales = {3.0F, 2.0F, 2.5F, 3.0F, 2.0F};
        float[] speeds = {0.2F, 0.5F, 0.2F, 0.2F, 0.2F};
        float[] xOffsets = {1.3963F, 1.0472F, 0.0F, -1.9199F, 0.3491F};
        float[] yOffsets = {-1.5708F, 1.3963F, 1.3963F, 1.0472F, 4.7124F};
        float[] xAngular = {-0.2617F, -0.3925F, -0.1963F, -0.2617F, -0.2617F};
        float[] yAngular = {-0.2617F, -0.2617F, -0.2617F, -0.1744F, 0.1744F};
        float[] reach = {2.0F, 1.0F, 3.0F, 2.0F, 2.0F};
        for (int i = 0; i < tentacles.size() && i < scales.length; i++) {
            TentacleParts part = tentacles.get(i);
            part.scale = scales[i];
            part.animationSpeed = speeds[i];
            part.xRotationalOffset = xOffsets[i];
            part.yRotationalOffset = yOffsets[i];
            part.xAngularOffset = xAngular[i];
            part.yAngularOffset = yAngular[i];
            part.reach = reach[i];
        }
    }

    private static ModelRenderer renderer(PartDefinition definition) {
        return definition == null ? null : definition.renderer();
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age,
                       float yaw, float pitch, float scale) {
        SupplementalEntities.WitherStormSegmentEntity segment =
                (SupplementalEntities.WitherStormSegmentEntity) entity;
        float partialTicks = MathHelper.clamp(age - segment.ticksExisted, 0.0F, 1.0F);
        animate(segment, age, partialTicks);
        renderHeads(segment, scale, head -> true);
        renderMass(segment, scale);
        GlStateManager.pushMatrix();
        GlStateManager.scale(segment.isMirrored() ? -1.0F : 1.0F, 1.0F, 1.0F);
        for (TentacleParts tentacle : tentacles) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(tentacle.scale, tentacle.scale, tentacle.scale);
            tentacle.base.render(scale);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    public void renderHeads(SupplementalEntities.WitherStormSegmentEntity segment,
                            float scale, IntPredicate predicate) {
        for (int index = 0; index < heads.size(); index++) {
            int headIndex = logicalHead(index);
            if (!predicate.test(headIndex)
                    || segment.areOtherHeadsDisabled() && headIndex != 0) continue;
            GlStateManager.pushMatrix();
            GlStateManager.scale(3.0F, 3.0F, 3.0F);
            heads.get(index).render(scale);
            GlStateManager.popMatrix();
        }
    }

    public void renderMass(SupplementalEntities.WitherStormSegmentEntity segment, float scale) {
        if (mass == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.scale(segment.isMirrored() ? -1.0F : 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(10.0F, 10.0F, 10.0F);
        (shouldUseLowResMass(segment) && lowResMass != null ? lowResMass : mass).render(scale);
        GlStateManager.popMatrix();
    }

    public void renderSantaHats(SupplementalEntities.WitherStormSegmentEntity segment,
                                SantaHatModel santaHat, float scale) {
        for (int index = 0; index < heads.size(); index++) {
            if (segment.areOtherHeadsDisabled() && logicalHead(index) != 0) continue;
            GlStateManager.pushMatrix();
            GlStateManager.scale(3.0F, 3.0F, 3.0F);
            heads.get(index).postRender(scale);
            santaHat.render(segment, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, scale);
            GlStateManager.popMatrix();
        }
    }

    public void renderPulse(SupplementalEntities.WitherStormSegmentEntity segment,
                            float partialTicks, float scale) {
        if (mass == null) return;
        ModelRenderer renderedMass = shouldUseLowResMass(segment) && lowResMass != null
                ? lowResMass : mass;
        WitherStormPulseModelHelper.render(segment, segment.getPhase(), 5,
                WitherStormClientConfig.lowResModels, renderedMass, partialTicks, scale, () -> {
                    GlStateManager.scale(segment.isMirrored() ? -1.0F : 1.0F, 1.0F, 1.0F);
                    GlStateManager.scale(10.0F, 10.0F, 10.0F);
                });
    }

    private void animate(SupplementalEntities.WitherStormSegmentEntity segment,
                         float age, float partialTicks) {
        float bodyYaw = segment.prevRenderYawOffset
                + MathHelper.wrapDegrees(segment.renderYawOffset
                - segment.prevRenderYawOffset) * partialTicks;
        for (int index = 0; index < heads.size(); index++) {
            ModelRenderer head = heads.get(index);
            int headIndex = logicalHead(index);
            float relativeYaw = MathHelper.clamp(MathHelper.wrapDegrees(
                    segment.getHeadYaw(headIndex, partialTicks) - bodyYaw),
                    -MAXIMUM_HEAD_YAW, MAXIMUM_HEAD_YAW);
            head.rotateAngleY = relativeYaw * 0.017453292F + (float) Math.PI;
            head.rotateAngleX = -segment.getHeadPitch(headIndex, partialTicks) * 0.017453292F;
            head.rotateAngleZ = segment.getHeadShakeAnimation(headIndex, partialTicks);
            PartDefinition definition = rootDefinition.child("heads").child("head" + index);
            ModelRenderer lower = renderer(definition == null ? null : definition.child("lowerJaw"));
            if (lower != null) {
                float ticks = segment.isDeadOrPlayingDead() ? 0.0F : age;
                lower.rotateAngleX = WitherStormHeadAnimation.jawPitch(
                        segment.getMouthAnimation(headIndex, partialTicks), ticks,
                        HEAD_ANIMATION_OFFSETS[MathHelper.clamp(headIndex, 0,
                                HEAD_ANIMATION_OFFSETS.length - 1)]);
                lower.rotateAngleZ = WitherStormHeadAnimation.brokenJawRoll(segment, headIndex,
                        segment.getBrokenJawAnimation(headIndex, partialTicks));
            }
        }
        boolean mirrored = segment.isMirrored();
        for (int index = 0; index < tentacles.size(); index++) {
            TentacleParts tentacle = tentacles.get(index);
            tentacle.animationOffset = mirrored
                    ? new float[]{0.0F, 5.0F, 10.0F, 15.0F, 25.0F}[Math.min(index, 4)]
                    : new float[]{10.0F, 15.0F, 29.0F, 32.0F, 57.0F}[Math.min(index, 4)];
            tentacle.animate(segment.getTentacleAnimation(partialTicks));
        }
    }

    private static int logicalHead(int geometryIndex) {
        return GEOMETRY_TO_HEAD[Math.min(geometryIndex, GEOMETRY_TO_HEAD.length - 1)];
    }


    private boolean shouldUseLowResMass(SupplementalEntities.WitherStormSegmentEntity segment) {
        return lowResMass != null && (WitherStormClientConfig.lowResModels
                || WitherStormClientConfig.witherStormLOD
                && WitherStormRenderer.isDistantStorm(segment));
    }

    private static final class TentacleParts {
        final ModelRenderer base;
        final ModelRenderer[] segments = new ModelRenderer[6];
        float scale = 1.0F;
        float xRotationalOffset;
        float yRotationalOffset;
        float animationSpeed = 1.0F;
        float yAngularOffset;
        float xAngularOffset;
        float animationOffset;
        float reach = 1.0F;

        TentacleParts(PartDefinition definition) {
            base = definition.renderer();
            PartDefinition current = definition;
            for (int index = 0; index < segments.length; index++) {
                current = current.child("segment" + (index + 1));
                if (current == null) break;
                segments[index] = current.renderer();
            }
        }

        void animate(float age) {
            float f = MathHelper.cos((age + animationOffset * 10.0F) * animationSpeed * 0.1F) * reach;
            float s = MathHelper.sin((age + animationOffset * 10.0F) * animationSpeed * 0.05F) * reach;
            base.rotateAngleY = s * f * 0.05F + yRotationalOffset;
            base.rotateAngleX = f * s * 0.05F + xRotationalOffset;
            if (segments[0] != null) segments[0].rotateAngleX = f * -0.1F;
            if (segments[1] != null) segments[1].rotateAngleX = f * 0.1F + xAngularOffset;
            if (segments[2] != null) segments[2].rotateAngleX = f * 0.075F + xAngularOffset;
            if (segments[3] != null) segments[3].rotateAngleX = f * 0.05F + xAngularOffset;
            if (segments[4] != null) segments[4].rotateAngleX = f * 0.1F + xAngularOffset;
            if (segments[5] != null) segments[5].rotateAngleX = f * 0.1F + xAngularOffset;
            for (int index = 1; index < segments.length; index++) {
                if (segments[index] != null) segments[index].rotateAngleY = yAngularOffset;
            }
        }
    }
}
