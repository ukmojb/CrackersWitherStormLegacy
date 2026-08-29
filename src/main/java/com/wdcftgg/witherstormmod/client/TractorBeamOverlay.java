package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.entity.TractorBeamProvider;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public final class TractorBeamOverlay {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/misc/tractor_beam_outline.png");
    private static final int MAXIMUM_TICKS = 240;
    private static final int FULL_FADE_TICKS = 120;
    private static final double PLAYER_BEAM_RADIUS = 4.0D;

    private static int ticksInTractorBeam;

    private TractorBeamOverlay() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.world == null || minecraft.player == null) {
            reset();
            return;
        }
        if (minecraft.isGamePaused()) return;

        boolean insideBeam = !minecraft.player.isSpectator()
                && isInsideAnyBeam(minecraft.player.getPositionVector(),
                minecraft.world.loadedEntityList);
        if (insideBeam) {
            ticksInTractorBeam = Math.min(MAXIMUM_TICKS, ticksInTractorBeam + 1);
        } else {
            ticksInTractorBeam = Math.max(0, ticksInTractorBeam - 1);
        }
    }

    private static boolean isInsideAnyBeam(Vec3d playerPosition, Iterable<Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof TractorBeamProvider) || entity.isDead) continue;
            TractorBeamProvider provider = (TractorBeamProvider) entity;
            if (provider.isDeadOrPlayingDead()) continue;
            for (int head = 0; head < provider.getTotalHeads(); head++) {
                if (!provider.tractorBeamActive(head)) continue;
                Vec3d origin = provider.getHeadPositionForBeam(head);
                Vec3d direction = provider.getHeadDirectionForBeam(head);
                if (origin == null || direction == null || direction.lengthSquared() <= 0.0001D) continue;
                if (TractorBeamHelper.isInsideTractorBeam(playerPosition, origin,
                        direction.normalize(), provider.getTractorBeamCutoffDistance(head),
                        PLAYER_BEAM_RADIUS)) return true;
            }
        }
        return false;
    }

    public static void render(Minecraft minecraft, ScaledResolution resolution) {
        if (!WitherStormClientConfig.renderTractorBeams
                || !WitherStormClientConfig.renderTractorBeamOverlay
                || ticksInTractorBeam <= 0) return;

        float alpha = Math.min(ticksInTractorBeam, FULL_FADE_TICKS)
                / (float) MAXIMUM_TICKS;
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        minecraft.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.pushAttrib();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F,
                width, height, width, height);
        GlStateManager.popAttrib();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void reset() {
        ticksInTractorBeam = 0;
    }
}
