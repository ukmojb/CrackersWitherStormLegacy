package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.util.Contributors;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class PlayerCosmeticsEvents {
    private PlayerCosmeticsEvents() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = event.getEntityPlayer();
        if (player.isSpectator()
                || (!WitherStormClientConfig.patronCosmetic && player == minecraft.player)) {
            return;
        }

        String name = player.getGameProfile().getName();
        boolean developer = Contributors.isDeveloper(name);
        boolean patron = Contributors.isPatron(name);
        boolean kofi = Contributors.isKofi(name);
        if (!developer && !patron && !kofi) return;

        BlockRendererDispatcher dispatcher = minecraft.getBlockRendererDispatcher();
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();
        try {
            double renderY = event.getY() - (player.isSneaking() ? 0.125D : 0.0D);
            GlStateManager.translate(event.getX(), renderY, event.getZ());
            GlStateManager.enableRescaleNormal();
            minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            float ticks = player.ticksExisted + event.getPartialRenderTick();
            boolean patronFlash = patron && player.ticksExisted / 20 % 5 == 0;
            if (patron) {
                renderOrbitingBlock(dispatcher, ModBlocks.get("formidibomb").getDefaultState(),
                        ticks, 0.6F, player.height + 0.2F, patronFlash);
            }
            if (developer) {
                renderOrbitingBlock(dispatcher, Blocks.COMMAND_BLOCK.getDefaultState(),
                        ticks, -0.6F, player.height + 0.2F, patronFlash);
            }
            if (kofi) {
                renderKofiBlock(dispatcher, ModBlocks.get("tainted_zombie_lying").getDefaultState(),
                        ticks, player.height + 0.2F);
            }
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableRescaleNormal();
            GlStateManager.bindTexture(previousTexture);
            GlStateManager.popMatrix();
        }
    }

    private static void renderOrbitingBlock(BlockRendererDispatcher dispatcher, IBlockState state,
                                             float ticks, float orbitX, float orbitY,
                                             boolean flash) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(ticks, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(orbitX, orbitY, 0.0F);
        float scale = (MathHelper.sin(ticks * 0.1F) + 10.0F) * 0.025F;
        GlStateManager.rotate(ticks, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(ticks * 4.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        dispatcher.renderBlockBrightness(state, 1.0F);
        if (flash) renderDamageFlash(dispatcher, state);
        GlStateManager.popMatrix();
    }

    private static void renderKofiBlock(BlockRendererDispatcher dispatcher, IBlockState state,
                                        float ticks, float orbitY) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-ticks, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.6F, orbitY, 1.0F);
        GlStateManager.rotate(-ticks * 6.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-ticks * 6.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(0.2727F, 0.2727F, 0.2727F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        dispatcher.renderBlockBrightness(state, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void renderDamageFlash(BlockRendererDispatcher dispatcher, IBlockState state) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 0.0F, 0.0F, 0.45F);
        GlStateManager.doPolygonOffset(-3.0F, -3.0F);
        GlStateManager.enablePolygonOffset();
        dispatcher.renderBlockBrightness(state, 1.0F);
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
