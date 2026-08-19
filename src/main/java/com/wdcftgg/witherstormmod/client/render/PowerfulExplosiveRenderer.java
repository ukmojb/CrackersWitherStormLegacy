package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class PowerfulExplosiveRenderer<T extends PowerfulExplosiveEntity> extends Render<T> {

    private final IBlockState renderedState;

    public PowerfulExplosiveRenderer(RenderManager renderManager, String blockName) {
        super(renderManager);
        shadowSize = 0.5F;
        renderedState = ModBlocks.get(blockName).getDefaultState();
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        boolean formidibomb = entity instanceof PowerfulExplosiveEntity.FormidibombEntity;
        IBlockState state = formidibomb
                ? ((PowerfulExplosiveEntity.FormidibombEntity) entity).getBlockState() : renderedState;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + 0.5F, (float) z);
        float remaining = entity.getFuse() - partialTicks + 1.0F;
        float swellingStart = formidibomb ? 20.0F : 10.0F;
        if (remaining < swellingStart) {
            float swelling = 1.0F - remaining / swellingStart;
            swelling = MathHelper.clamp(swelling, 0.0F, 1.0F);
            swelling *= swelling;
            swelling *= swelling;
            float scale = 1.0F + swelling * (formidibomb ? 20.0F : 5.0F);
            GlStateManager.scale(scale, scale, scale);
        }

        if (formidibomb) {
            renderFormidibombRays((PowerfulExplosiveEntity.FormidibombEntity) entity, partialTicks);
            PowerfulExplosiveEntity.FormidibombEntity bomb =
                    (PowerfulExplosiveEntity.FormidibombEntity) entity;
            if (!bomb.onGround) {
                float airTime = bomb.getAirTime() + partialTicks;
                GlStateManager.rotate(airTime, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(airTime * 0.5F, 1.0F, 0.0F, 0.0F);
            }
        }

        float flashAlpha = formidibomb ? 0.8F
                : MathHelper.clamp((1.0F - remaining / 100.0F) * 0.8F, 0.0F, 0.8F);
        bindEntityTexture(entity);
        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, 0.5F);
        dispatcher.renderBlockBrightness(state, entity.getBrightness());
        GlStateManager.translate(0.0F, 0.0F, 1.0F);

        if (renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(getTeamColor(entity));
            dispatcher.renderBlockBrightness(state, 1.0F);
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        } else if (shouldFlash(entity, formidibomb)) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, flashAlpha);
            GlStateManager.doPolygonOffset(-3.0F, -3.0F);
            GlStateManager.enablePolygonOffset();
            dispatcher.renderBlockBrightness(state, 1.0F);
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GL11.glPopAttrib();
        }
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static boolean shouldFlash(PowerfulExplosiveEntity entity, boolean formidibomb) {
        if (!formidibomb) return entity.getFuse() / 5 % 2 == 0;
        PowerfulExplosiveEntity.FormidibombEntity bomb =
                (PowerfulExplosiveEntity.FormidibombEntity) entity;
        return bomb.getStartFuse() > 0 && bomb.getFuse() > 0
                && bomb.getStartFuse() / bomb.getFuse() % 2 == 0;
    }

    private static void renderFormidibombRays(PowerfulExplosiveEntity.FormidibombEntity entity,
                                                float partialTicks) {
        float fuse = entity.getFuse() - partialTicks;
        if (fuse >= 240.0F) return;
        float ticks = (240.0F - fuse) / 150.0F;
        Random random = new Random(289L);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderHelper.disableStandardItemLighting();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();

        int rayCount = (int) ((ticks + ticks * ticks) * 0.5F * 10.0F);
        for (int ray = 0; ray < rayCount; ray++) {
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F + ticks * 90.0F, 0.0F, 0.0F, 1.0F);
            float length = random.nextFloat()
                    * (float) entity.getEntityBoundingBox().getAverageEdgeLength() * 0.2F
                    + ticks * Math.max(1.0F, (40.0F - fuse) / 6.0F);
            float width = random.nextFloat() * 0.025F + ticks;
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0.0D, 0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            buffer.pos(-0.866D * width, length, -0.5D * width).color(255, 0, 255, 0).endVertex();
            buffer.pos(0.866D * width, length, -0.5D * width).color(255, 0, 255, 0).endVertex();
            buffer.pos(0.0D, length, width).color(255, 0, 255, 0).endVertex();
            buffer.pos(-0.866D * width, length, -0.5D * width).color(255, 0, 255, 0).endVertex();
            tessellator.draw();
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GL11.glPopAttrib();
        RenderHelper.enableStandardItemLighting();
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
