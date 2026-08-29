package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.CommandBlockCoreModel;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.Random;

public final class CommandBlockRenderer
        extends StormPartRenderer<SupplementalEntities.CommandBlockEntity> {
    public CommandBlockRenderer(RenderManager manager) {
        super(manager, new CommandBlockCoreModel(), 1.0F,
                "textures/entity/command_block/ribcage.png", 1.0F);
        shadowSize = 0.0F;
    }

    @Override
    public void doRender(SupplementalEntities.CommandBlockEntity entity,
                         double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        renderCommandBlock(entity, x, y, z, partialTicks);
        renderGlare(entity, x, y, z, partialTicks);
    }

    @Override
    protected void applyRotations(SupplementalEntities.CommandBlockEntity entity,
                                  float ageInTicks, float rotationYaw, float partialTicks) {



        float bodyYaw = interpolateRotation(entity.prevRenderYawOffset,
                entity.renderYawOffset, partialTicks);
        GlStateManager.rotate(90.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
    }

    private static void renderCommandBlock(SupplementalEntities.CommandBlockEntity entity,
                                           double x, double y, double z, float partialTicks) {
        int horizontalIndex = MathHelper.floor(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        EnumFacing facing = EnumFacing.byHorizontalIndex(horizontalIndex);
        IBlockState state = Blocks.COMMAND_BLOCK.getDefaultState()
                .withProperty(BlockCommandBlock.FACING, facing);
        Minecraft minecraft = Minecraft.getMinecraft();
        BlockRendererDispatcher dispatcher = minecraft.getBlockRendererDispatcher();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        float protectionYOffset = entity.getProtectionYOffset(partialTicks);
        GlStateManager.translate(x - 0.5D, y + protectionYOffset, z + 0.5D);
        dispatcher.renderBlockBrightness(state, 1.0F);
        GlStateManager.popMatrix();
        renderDamageOverlay(entity, state, dispatcher, x, y, z, protectionYOffset);
    }

    private static void renderDamageOverlay(SupplementalEntities.CommandBlockEntity entity,
                                            IBlockState state, BlockRendererDispatcher dispatcher,
                                            double x, double y, double z, float protectionYOffset) {
        int healthQuarter = MathHelper.floor((entity.getMaxHealth() - entity.getHealth())
                / (entity.getMaxHealth() / 4.0F));
        if (healthQuarter <= 0 || healthQuarter >= 5) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite destroySprite = minecraft.getTextureMapBlocks().getAtlasSprite(
                "minecraft:blocks/destroy_stage_" + healthQuarter * 2);
        IBakedModel model = dispatcher.getModelForState(state);
        IBakedModel damageModel = ForgeHooksClient.getDamageModel(model, destroySprite, state,
                entity.world, entity.getPosition());

        GlStateManager.pushMatrix();
        GlStateManager.translate(x - 0.5D, y + protectionYOffset, z + 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR,
                GlStateManager.DestFactor.SRC_COLOR);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.doPolygonOffset(-1.0F, -10.0F);
        GlStateManager.enablePolygonOffset();
        dispatcher.getBlockModelRenderer().renderModelBrightness(damageModel, state, 1.0F, true);
        GlStateManager.disablePolygonOffset();
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void renderGlare(SupplementalEntities.CommandBlockEntity entity,
                                    double x, double y, double z, float partialTicks) {
        int deathTime = entity.getSpecialDeathTime();
        int hitGlareTime = entity.getHitGlareTime();
        if (deathTime <= 0 && hitGlareTime <= 0) return;

        float progression;
        float alpha;
        if (deathTime > 0) {
            progression = (deathTime + partialTicks) / 20.0F;
            alpha = 1.0F;
        } else {
            progression = 2.0F + (hitGlareTime - partialTicks) / 30.0F;
            alpha = MathHelper.clamp((hitGlareTime - partialTicks) / 60.0F, 0.0F, 1.0F);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Random random = new Random(122L);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(7425);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height * 0.5D, z);

        float rayCount = (progression + progression * progression) * 0.5F;
        for (int index = 0; index < rayCount; index++) {
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(random.nextFloat() * 360.0F + progression * 90.0F,
                    0.0F, 0.0F, 1.0F);
            float length = random.nextFloat() * 0.2F + progression;
            float width = (random.nextFloat() + 0.2F) * 0.05F * progression;
            int centerAlpha = (int) (255.0F * alpha);
            buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0.0D, 0.0D, 0.0D)
                    .color(255, 255, 255, centerAlpha).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width)
                    .color(255, 123, 0, 0).endVertex();
            buffer.pos(0.866D * width, length, -0.5F * width)
                    .color(255, 123, 0, 0).endVertex();
            buffer.pos(0.0D, length, width)
                    .color(255, 123, 0, 0).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width)
                    .color(255, 123, 0, 0).endVertex();
            tessellator.draw();
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(7424);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        RenderHelper.enableStandardItemLighting();
    }
}
