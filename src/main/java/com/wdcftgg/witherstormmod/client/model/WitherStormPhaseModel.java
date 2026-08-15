package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.model.witherstorm.CommandBlockGeometry;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.CubeDeformation;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartDefinition;
import com.wdcftgg.witherstormmod.client.model.witherstorm.WitherStormModelDefinitions;
import com.wdcftgg.witherstormmod.client.render.WitherStormRenderer;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public class WitherStormPhaseModel extends ModelBase {
    private static final int[] GEOMETRY_TO_HEAD = {2, 0, 1};
    private static final float[] HEAD_ANIMATION_OFFSETS = {0.0F, 175.0F, 100.0F};
    private static final float MAXIMUM_LATE_HEAD_YAW = 80.0F;

    public enum Form {
        COMMAND_BLOCK,
        HUNCHBACK_1,
        HUNCHBACK_1_1,
        HUNCHBACK_1_2,
        GROWING_HUNCHBACK,
        HUNCHBACK_2_1,
        PREGNANT_HUNCHBACK,
        HUNCHBACK_3_1,
        HUNCHBACK_3_2,
        DESTROYER,
        INTERMEDIATE_EVOLVED_DESTROYER,
        EVOLVED_DESTROYER,
        INTERMEDIATE_DEVOURER,
        DEVOURER,
        DISMANTLED,
        INTERMEDIATE_EVOLVED_DEVOURER,
        EVOLVED_DEVOURER,
        TORN_EVOLVED_DEVOURER
    }

    private final Form form;
    private final PartDefinition rootDefinition;
    private final ModelRenderer mass;
    private final ModelRenderer lowResMass;
    private final ModelRenderer commandBlockBase;
    private final List<ModelRenderer> heads = new ArrayList<ModelRenderer>();
    private final List<TentacleParts> tentacles = new ArrayList<TentacleParts>();

    public WitherStormPhaseModel(Form form) {
        this.form = form;
        textureWidth = 160;
        textureHeight = 160;
        ModelRenderer root = new ModelRenderer(this);
        rootDefinition = new PartDefinition(this, root);
        WitherStormModelDefinitions.initializeRoot(rootDefinition);
        buildDefinition(form);
        mass = renderer(rootDefinition.child("mass"));
        lowResMass = renderer(rootDefinition.child("lowResMass"));
        commandBlockBase = renderer(rootDefinition.child("witherBase"));
        collectHeads();
        collectTentacles();
        WitherStormTentacleConfig.apply(form, tentacles);
    }

    private void buildDefinition(Form selected) {
        CubeDeformation def = CubeDeformation.f_171458_;
        switch (selected) {
            case COMMAND_BLOCK:
                CommandBlockGeometry.populateBase(rootDefinition, def, true, true, true);
                break;
            case HUNCHBACK_1:
                WitherStormModelDefinitions.buildHunchback(rootDefinition, def);
                break;
            case HUNCHBACK_1_1:
                WitherStormModelDefinitions.buildHunchback1_1(rootDefinition, def);
                break;
            case HUNCHBACK_1_2:
                WitherStormModelDefinitions.buildHunchback1_2(rootDefinition, def);
                break;
            case GROWING_HUNCHBACK:
                WitherStormModelDefinitions.buildGrowingHunchback(rootDefinition, def);
                break;
            case HUNCHBACK_2_1:
                WitherStormModelDefinitions.buildHunchback2_1(rootDefinition, def);
                break;
            case PREGNANT_HUNCHBACK:
                WitherStormModelDefinitions.buildPregnantHunchback(rootDefinition, def);
                break;
            case HUNCHBACK_3_1:
                WitherStormModelDefinitions.buildHunchback3_1(rootDefinition, def);
                break;
            case HUNCHBACK_3_2:
                WitherStormModelDefinitions.buildHunchback3_2(rootDefinition, def);
                break;
            case DESTROYER:
                WitherStormModelDefinitions.buildDestroyer(rootDefinition);
                break;
            case INTERMEDIATE_EVOLVED_DESTROYER:
                WitherStormModelDefinitions.buildIntermediateEvolvedDestroyer(rootDefinition);
                break;
            case EVOLVED_DESTROYER:
                WitherStormModelDefinitions.buildEvolvedDestroyer(rootDefinition);
                break;
            case INTERMEDIATE_DEVOURER:
                WitherStormModelDefinitions.buildIntermediateDevourer(rootDefinition);
                break;
            case DEVOURER:
                WitherStormModelDefinitions.buildDevourer(rootDefinition);
                break;
            case DISMANTLED:
                WitherStormModelDefinitions.buildDismantled(rootDefinition);
                break;
            case INTERMEDIATE_EVOLVED_DEVOURER:
                WitherStormModelDefinitions.buildIntermediateEvolvedDevourer(rootDefinition);
                break;
            case EVOLVED_DEVOURER:
                WitherStormModelDefinitions.buildEvolvedDevourer(rootDefinition);
                break;
            case TORN_EVOLVED_DEVOURER:
                WitherStormModelDefinitions.buildTornEvolvedDevourer(rootDefinition);
                break;
            default:
                throw new IllegalArgumentException(selected.name());
        }
    }

    private void collectHeads() {
        PartDefinition root = rootDefinition.child("heads");
        if (root == null) return;
        for (PartDefinition definition : root.children().values()) {
            heads.add(definition.renderer());
        }
    }

    private void collectTentacles() {
        PartDefinition root = rootDefinition.child("tentacles");
        if (root == null) return;
        for (Map.Entry<String, PartDefinition> entry : root.children().entrySet()) {
            PartDefinition base = entry.getValue().child("base");
            if (base != null) tentacles.add(new TentacleParts(base));
        }
    }

    private static ModelRenderer renderer(PartDefinition definition) {
        return definition == null ? null : definition.renderer();
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch, float scale) {
        WitherStormEntity storm = (WitherStormEntity) entity;
        animate(storm, age, yaw, pitch);
        if (commandBlockBase != null) {
            GlStateManager.pushMatrix();
            applyMirroredTransform(storm);
            commandBlockBase.render(scale);
            GlStateManager.popMatrix();
        }

        renderMass(storm, scale);
        renderHeads(storm, scale, head -> true);

        for (TentacleParts tentacle : tentacles) {
            GlStateManager.pushMatrix();
            applyMirroredTransform(storm);
            float tentacleScale = tentacle.scale;
            GlStateManager.scale(tentacleScale, tentacleScale, tentacleScale);
            if (rotatesEarlyMass()) GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
            tentacle.base.render(scale);
            GlStateManager.popMatrix();
        }

        renderTornEntrance(storm);
    }

    /** Upstream TornEvolvedDevourer mass decal that visually closes the bowels entrance. */
    private void renderTornEntrance(WitherStormEntity storm) {
        if (form != Form.TORN_EVOLVED_DEVOURER || storm.getHealth() <= 0.0F) return;

        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.pushMatrix();
        try {
            applyMirroredTransform(storm);
            applyMassTransform();

            float topZOffset = 0.4F;
            float stretch = 1.1F;
            if (shouldUseLowResMass(storm)) {
                topZOffset = 0.45F;
                stretch = 1.4F;
                GlStateManager.translate(-0.12F, -2.0F, -0.8F);
            } else {
                GlStateManager.translate(-0.12F, -2.0F, -0.9F);
            }

            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            // RenderType.lightning() enables face culling. RenderLivingBase disables it while
            // rendering models, which otherwise draws both coplanar windings additively.
            GlStateManager.enableCull();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);

            float size = 0.35F;
            float halfWidth = size * stretch;
            float red = 0.5F;
            float green = 0.3F;
            float blue = 0.8F;
            float alpha = 0.2F;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            entranceVertex(buffer, halfWidth, size, 0.0F, red, green, blue, alpha);
            entranceVertex(buffer, halfWidth, -size, -topZOffset, red, green, blue, alpha);
            entranceVertex(buffer, -halfWidth, -size, -topZOffset, red, green, blue, alpha);
            entranceVertex(buffer, -halfWidth, size, 0.0F, red, green, blue, alpha);
            entranceVertex(buffer, -halfWidth, size, 0.0F, red, green, blue, alpha);
            entranceVertex(buffer, -halfWidth, -size, -topZOffset, red, green, blue, alpha);
            entranceVertex(buffer, halfWidth, -size, -topZOffset, red, green, blue, alpha);
            entranceVertex(buffer, halfWidth, size, 0.0F, red, green, blue, alpha);
            tessellator.draw();
        } finally {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            if (!blendEnabled) GlStateManager.disableBlend();
            if (!cullEnabled) GlStateManager.disableCull();
            if (lightingEnabled) GlStateManager.enableLighting();
            if (textureEnabled) GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static void entranceVertex(BufferBuilder buffer, float x, float y, float z,
                                       float red, float green, float blue, float alpha) {
        buffer.pos(x, y, z).color(red, green, blue, alpha).endVertex();
    }

    public void renderHeads(WitherStormEntity storm, float scale, IntPredicate predicate) {
        float scaleForHead = headScale();
        for (int geometryIndex = 0; geometryIndex < heads.size(); geometryIndex++) {
            int head = logicalHead(geometryIndex);
            if (!predicate.test(head) || storm.areOtherHeadsDisabled() && head != 0) continue;
            GlStateManager.pushMatrix();
            GlStateManager.scale(scaleForHead, scaleForHead, scaleForHead);
            heads.get(geometryIndex).render(scale);
            GlStateManager.popMatrix();
        }
    }

    public void renderMass(WitherStormEntity storm, float scale) {
        if (mass == null) return;
        GlStateManager.pushMatrix();
        applyMirroredTransform(storm);
        applyMassTransform();
        (shouldUseLowResMass(storm) && lowResMass != null ? lowResMass : mass).render(scale);
        GlStateManager.popMatrix();
    }

    private int logicalHead(int geometryIndex) {
        return heads.size() == 3
                ? GEOMETRY_TO_HEAD[Math.min(geometryIndex, GEOMETRY_TO_HEAD.length - 1)] : 0;
    }

    public void renderSantaHats(WitherStormEntity storm, SantaHatModel santaHat, float scale) {
        float scaleForHead = headScale();
        for (int i = 0; i < heads.size(); i++) {
            if (storm.areOtherHeadsDisabled() && logicalHead(i) != 0) continue;
            GlStateManager.pushMatrix();
            GlStateManager.scale(scaleForHead, scaleForHead, scaleForHead);
            heads.get(i).postRender(scale);
            santaHat.render(storm, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, scale);
            GlStateManager.popMatrix();
        }
    }

    private static void applyMirroredTransform(WitherStormEntity storm) {
        GlStateManager.scale(storm.isMirrored() ? -1.0F : 1.0F, 1.0F, 1.0F);
    }

    private void animate(WitherStormEntity storm, float age, float yaw, float pitch) {
        float partialTicks = MathHelper.clamp(age - storm.ticksExisted, 0.0F, 1.0F);
        if (commandBlockBase != null) {
            PartDefinition base = rootDefinition.child("witherBase");
            ModelRenderer ribcage = renderer(base.child("ribcage"));
            ModelRenderer tail = renderer(base.child("tail"));
            float wave = MathHelper.cos(age * 0.1F);
            if (ribcage != null) ribcage.rotateAngleX = (0.065F + 0.05F * wave) * (float) Math.PI;
            if (tail != null && ribcage != null) {
                tail.setRotationPoint(-2.0F, 6.9F + MathHelper.cos(ribcage.rotateAngleX) * 10.0F,
                        -0.5F + MathHelper.sin(ribcage.rotateAngleX) * 10.0F);
                tail.rotateAngleX = (0.265F + 0.1F * wave) * (float) Math.PI;
            }
            ModelRenderer center = renderer(base.child("center_head"));
            if (center != null) {
                center.rotateAngleY = yaw * ((float) Math.PI / 180.0F);
                center.rotateAngleX = pitch * ((float) Math.PI / 180.0F);
            }
            animateVanillaSideHead(storm, renderer(base.child("left_head")), 1, partialTicks);
            animateVanillaSideHead(storm, renderer(base.child("right_head")), 2, partialTicks);
        }

        for (int i = 0; i < heads.size(); i++) {
            ModelRenderer head = heads.get(i);
            int headIndex = logicalHead(i);
            animateStormHead(storm, head, headIndex, partialTicks);
            PartDefinition headDefinition = rootDefinition.child("heads").child("head" + i);
            ModelRenderer lower = renderer(headDefinition == null ? null : headDefinition.child("lowerJaw"));
            if (lower != null) {
                float ticks = storm.isDeadOrPlayingDead() ? 0.0F : age;
                lower.rotateAngleX = WitherStormHeadAnimation.jawPitch(
                        storm.getMouthAnimation(headIndex, partialTicks), ticks,
                        HEAD_ANIMATION_OFFSETS[MathHelper.clamp(headIndex, 0,
                                HEAD_ANIMATION_OFFSETS.length - 1)]);
                lower.rotateAngleZ = WitherStormHeadAnimation.brokenJawRoll(storm, headIndex,
                        storm.getBrokenJawAnimation(headIndex, partialTicks));
                head.rotateAngleZ = storm.getHeadShakeAnimation(headIndex, partialTicks);
            }
        }

        for (int i = 0; i < tentacles.size(); i++) {
            tentacles.get(i).animate(storm.getTentacleAnimation(partialTicks));
        }
    }

    private static void animateStormHead(WitherStormEntity storm, ModelRenderer head,
                                         int index, float partialTicks) {
        if (head == null) return;
        float relativeYaw = MathHelper.wrapDegrees(storm.getHeadYRotation(index, partialTicks)
                - interpolateBodyYaw(storm, partialTicks));
        if (storm.getPhase() > 3 && !storm.isDeadOrPlayingDead()) {
            relativeYaw = MathHelper.clamp(relativeYaw,
                    -MAXIMUM_LATE_HEAD_YAW, MAXIMUM_LATE_HEAD_YAW);
        }
        head.rotateAngleY = relativeYaw * ((float) Math.PI / 180.0F) + (float) Math.PI;
        head.rotateAngleX = -storm.getHeadXRotation(index, partialTicks)
                * ((float) Math.PI / 180.0F);
    }

    private static void animateVanillaSideHead(WitherStormEntity storm, ModelRenderer head,
                                               int sideIndex, float partialTicks) {
        if (head == null) return;
        head.rotateAngleY = MathHelper.wrapDegrees(
                storm.getHeadYRotation(sideIndex, partialTicks)
                        - interpolateBodyYaw(storm, partialTicks))
                * ((float) Math.PI / 180.0F) + (float) Math.PI;
        head.rotateAngleX = -storm.getHeadXRotation(sideIndex, partialTicks)
                * ((float) Math.PI / 180.0F);
    }

    private static float interpolateBodyYaw(WitherStormEntity storm, float partialTicks) {
        return storm.prevRenderYawOffset
                + MathHelper.wrapDegrees(storm.renderYawOffset - storm.prevRenderYawOffset)
                * partialTicks;
    }

    private void applyMassTransform() {
        if (isLateForm()) {
            GlStateManager.scale(10.0F, 10.0F, 10.0F);
        } else if (form == Form.GROWING_HUNCHBACK || form == Form.HUNCHBACK_2_1) {
            GlStateManager.scale(1.001F, 1.001F, 1.001F);
        }
        if (rotatesEarlyMass()) GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
    }

    private boolean rotatesEarlyMass() {
        return form == Form.PREGNANT_HUNCHBACK || form == Form.HUNCHBACK_3_1 || form == Form.HUNCHBACK_3_2;
    }

    private boolean isLateForm() {
        return form.ordinal() >= Form.DESTROYER.ordinal();
    }

    private float headScale() {
        if (isLateForm()) return 3.0F;
        if (form == Form.GROWING_HUNCHBACK || form == Form.HUNCHBACK_2_1 ||
                form == Form.PREGNANT_HUNCHBACK || form == Form.HUNCHBACK_3_1 || form == Form.HUNCHBACK_3_2) return 0.7F;
        return 1.0F;
    }

    public ModelRenderer getLowResMass() {
        return lowResMass;
    }

    /** 上游 lowResModelsEnabled：低分辨率质量模型开关与远距离 LOD。 */
    public boolean shouldUseLowResMass(WitherStormEntity storm) {
        return lowResMass != null && (WitherStormClientConfig.lowResModels
                || WitherStormClientConfig.witherStormLOD
                && WitherStormRenderer.isDistantStorm(storm));
    }

    /** 风暴被撕裂时的脉冲方块（上游 WitherStormPulseLayer 在 4.2.1 的等效实现）。 */
    public void renderPulse(WitherStormEntity storm, float partialTicks, float scale) {
        if (mass == null) return;
        ModelRenderer renderedMass = shouldUseLowResMass(storm) ? lowResMass : mass;
        WitherStormPulseModelHelper.render(storm, storm.getPhase(), 15,
                WitherStormClientConfig.lowResModels, renderedMass, partialTicks, scale, () -> {
                    applyMirroredTransform(storm);
                    applyMassTransform();
                });
    }

    static final class TentacleParts {
        final ModelRenderer base;
        final ModelRenderer[] segments = new ModelRenderer[6];
        float scale = 0.7F;
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
            for (int i = 0; i < segments.length; i++) {
                current = current.child("segment" + (i + 1));
                if (current == null) break;
                segments[i] = current.renderer();
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
            for (int i = 1; i < segments.length; i++) {
                if (segments[i] != null) segments[i].rotateAngleY = yAngularOffset;
            }
        }
    }
}
