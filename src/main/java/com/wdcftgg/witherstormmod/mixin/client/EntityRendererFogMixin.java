package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import net.minecraft.client.Minecraft;
import com.wdcftgg.witherstormmod.client.render.DistantProjection;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge's FogDensity event runs before EntityRenderer unconditionally enables fog.
 * Disable it after setup so the client option also covers the linear boss-fog path.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererFogMixin {
    @Shadow private Minecraft mc;
    @Shadow private float farPlaneDistance;
    @Shadow private double cameraZoom;
    @Shadow private double cameraYaw;
    @Shadow private double cameraPitch;
    @Unique private float witherstormmod$scopeFovScale = 1.0F;

    @Shadow private native float getFOVModifier(float partialTicks, boolean useFovSetting);

    @Redirect(method = "setupCameraTransform(FI)V", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void witherstormmod$extendFoglessWorldProjection(
            float fovY, float aspect, float nearPlane, float farPlane) {
        Project.gluPerspective(fovY, aspect, nearPlane,
                witherstormmod$optifineShadersActive()
                        ? farPlane : DistantProjection.adjustWorldFarPlane(farPlane));
    }

    /**
     * Keep terrain/entity visibility on the normal field of view, then switch only the
     * rendered projection to the phasometer zoom. Frustum's default constructor performs
     * a second clipping-helper sample, so the switch must happen after construction.
     */
    @Inject(method = "renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/culling/ICamera;setPosition(DDD)V",
                    shift = At.Shift.BEFORE))
    private void witherstormmod$applyPhasometerProjectionAfterCulling(
            int pass, float partialTicks, long finishTimeNano, CallbackInfo callback) {
        if (witherstormmod$optifineShadersActive()) return;
        witherstormmod$scopeFovScale = PhasometerOverlay.getFovScale(partialTicks);
        witherstormmod$setupWorldProjection(pass, partialTicks);
    }

    /** Every later world projection reset must keep the scope zoom while it is active. */
    @Redirect(method = "renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void witherstormmod$renderWorldProjection(
            float fovY, float aspect, float nearPlane, float farPlane) {
        Project.gluPerspective(fovY * witherstormmod$scopeFovScale, aspect, nearPlane,
                witherstormmod$optifineShadersActive()
                        ? farPlane : DistantProjection.adjustWorldFarPlane(farPlane));
    }

    @Redirect(method = "renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void witherstormmod$renderCloudProjection(
            float fovY, float aspect, float nearPlane, float farPlane) {
        witherstormmod$renderWorldProjection(fovY, aspect, nearPlane, farPlane);
    }

    private void witherstormmod$setupWorldProjection(int pass, float partialTicks) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        if (mc.gameSettings.anaglyph) {
            GlStateManager.translate(-(pass * 2 - 1) * 0.07F, 0.0F, 0.0F);
        }
        if (cameraZoom != 1.0D) {
            GlStateManager.translate((float) cameraYaw, (float) -cameraPitch, 0.0F);
            GlStateManager.scale(cameraZoom, cameraZoom, 1.0D);
        }
        float fov = getFOVModifier(partialTicks, true);
        Project.gluPerspective(
                fov * witherstormmod$scopeFovScale,
                (float) mc.displayWidth / (float) mc.displayHeight,
                0.05F,
                DistantProjection.adjustWorldFarPlane(farPlaneDistance * MathHelper.SQRT_2));
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void witherstormmod$disableVanillaFog(int startCoords, float partialTicks,
                                                  CallbackInfo callback) {
        if (!WitherStormClientConfig.disableVanillaFog || witherstormmod$optifineShadersActive()) return;
        GlStateManager.setFog(GlStateManager.FogMode.EXP);
        GlStateManager.setFogDensity(0.0F);
        // Keep the cached and actual GL state aligned even if another renderer used raw calls.
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.0F);
        GlStateManager.disableFog();
    }

    @Unique
    private static boolean witherstormmod$optifineShadersActive() {
        try {
            Class<?> config = Class.forName("optifine.Config");
            try {
                return Boolean.TRUE.equals(config.getMethod("isShaders").invoke(null));
            } catch (ReflectiveOperationException ignored) {
                Class<?> shaders = Class.forName("net.optifine.shaders.Shaders");
                return shaders.getField("shaderPackLoaded").getBoolean(null);
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
