package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.OptifineCompat;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;


public final class DistantProjection {
    private static final float FALLBACK_NEAR_PLANE = 0.05F;
    private static final float FAR_PLANE_MULTIPLIER = 180.0F;
    private static final FloatBuffer PROJECTION_MATRIX = BufferUtils.createFloatBuffer(16);

    private DistantProjection() {
    }


    public static boolean shouldUse(Entity entity) {
        return WitherStormClientConfig.distantRenderer
                && entity instanceof DistantStormPart;
    }

    public static void push() {


        if (OptifineCompat.areShadersActive()) return;
        PROJECTION_MATRIX.clear();
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX);

        float matrixDepth = PROJECTION_MATRIX.get(10);
        float matrixTranslation = PROJECTION_MATRIX.get(14);
        float nearPlane = matrixTranslation / (matrixDepth - 1.0F);
        if (!Float.isFinite(nearPlane) || nearPlane <= 0.0F) {
            nearPlane = FALLBACK_NEAR_PLANE;
        }

        float farPlane = getFarPlane();
        PROJECTION_MATRIX.put(10, -((farPlane + nearPlane) / (farPlane - nearPlane)));
        PROJECTION_MATRIX.put(14, -(2.0F * farPlane * nearPlane / (farPlane - nearPlane)));
        PROJECTION_MATRIX.position(0);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.multMatrix(PROJECTION_MATRIX);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    public static boolean isWithinFarPlane(double entityX, double entityY, double entityZ,
                                           double cameraX, double cameraY, double cameraZ) {
        double distance = getFarPlane() + 320.0D;
        double deltaX = entityX - cameraX;
        double deltaY = entityY - cameraY;
        double deltaZ = entityZ - cameraZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= distance * distance;
    }

    public static FogState pushDistantFog(Entity entity, boolean enabled) {


        if (OptifineCompat.areShadersActive()) return null;
        FogState previous = new FogState(GL11.glIsEnabled(GL11.GL_FOG),
                GL11.glGetInteger(GL11.GL_FOG_MODE),
                GL11.glGetFloat(GL11.GL_FOG_START),
                GL11.glGetFloat(GL11.GL_FOG_END),
                GL11.glGetFloat(GL11.GL_FOG_DENSITY));
        if (WitherStormClientConfig.disableVanillaFog) {
            GlStateManager.disableFog();
            return previous;
        }
        if (entity.world != null && !entity.world.isBlockLoaded(entity.getPosition(), false)) {
            if (enabled) {
                float farPlane = getFarPlane();
                GlStateManager.enableFog();
                GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
                GlStateManager.setFogStart(farPlane * 0.75F);
                GlStateManager.setFogEnd(farPlane * 0.95F);
            } else {
                GlStateManager.disableFog();
            }
        }
        return previous;
    }

    public static void restoreFog(FogState state) {
        if (state == null) return;
        if (WitherStormClientConfig.disableVanillaFog) {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
            GlStateManager.setFogDensity(0.0F);
            GlStateManager.disableFog();
            return;
        }
        GlStateManager.setFog(toFogMode(state.mode));
        GlStateManager.setFogStart(state.start);
        GlStateManager.setFogEnd(state.end);
        GlStateManager.setFogDensity(state.density);
        if (state.enabled) GlStateManager.enableFog();
        else GlStateManager.disableFog();
    }

    private static GlStateManager.FogMode toFogMode(int mode) {
        if (mode == GL11.GL_EXP) return GlStateManager.FogMode.EXP;
        if (mode == GL11.GL_EXP2) return GlStateManager.FogMode.EXP2;
        return GlStateManager.FogMode.LINEAR;
    }

    public static final class FogState {
        private final boolean enabled;
        private final int mode;
        private final float start;
        private final float end;
        private final float density;

        private FogState(boolean enabled, int mode, float start, float end, float density) {
            this.enabled = enabled;
            this.mode = mode;
            this.start = start;
            this.end = end;
            this.density = density;
        }
    }

    private static float getFarPlane() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return Math.max(1024.0F,
                minecraft.gameSettings.renderDistanceChunks * 16.0F * FAR_PLANE_MULTIPLIER);
    }

    public static void pop() {

        if (OptifineCompat.areShadersActive()) return;
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }
}
