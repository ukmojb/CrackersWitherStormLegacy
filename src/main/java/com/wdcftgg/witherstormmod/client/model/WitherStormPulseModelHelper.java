package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PreciseModelBoxBounds;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;

import java.util.Random;


final class WitherStormPulseModelHelper {
    private static final ModelRenderer PULSE_TEXTURE_OWNER;

    static {
        ModelBase textureModel = new ModelBase() { };
        textureModel.textureWidth = 32;
        textureModel.textureHeight = 32;
        PULSE_TEXTURE_OWNER = new ModelRenderer(textureModel).setTextureSize(32, 32);
    }

    private WitherStormPulseModelHelper() {
    }

    static void render(Entity entity, int phase, int phaseMultiplier, boolean lowResolution,
                       ModelRenderer mass, float partialTicks, float scale,
                       Runnable massTransform) {
        if (mass == null) return;
        if (mass.cubeList == null || mass.cubeList.isEmpty()) return;
        int amount = (int) (phase * phaseMultiplier / 2.0F
                * (lowResolution ? 3.0F : 1.0F));
        if (amount <= 0) return;

        GlStateManager.pushMatrix();
        massTransform.run();
        for (int index = 0; index < amount; index++) {
            float tick = entity.ticksExisted + index + partialTicks;
            long seed = (long) (tick / 20.0F);
            Random random = new Random(seed + (long) (index * Math.PI));
            ModelRenderer part = selectRandomPart(mass, random);
            ModelBox cube = part.cubeList.get(random.nextInt(part.cubeList.size()));
            float[] point = randomSurfacePoint(cube, random);
            int textureU = random.nextInt(28);
            int textureV = Math.max(16, random.nextInt(30));
            float fade = 1.0F - tick % 20.0F * 0.05F;
            if (fade <= 0.0F) continue;

            GlStateManager.pushMatrix();
            applySelectedPartTransform(mass, part, scale);
            drawPulseCube(point, scale, fade, textureU, textureV);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    private static ModelRenderer selectRandomPart(ModelRenderer mass, Random random) {
        if (mass.childModels != null && !mass.childModels.isEmpty()) {
            ModelRenderer candidate = mass.childModels.get(random.nextInt(mass.childModels.size()));
            if (candidate.cubeList != null && !candidate.cubeList.isEmpty()) return candidate;
        }
        return mass;
    }

    private static void applySelectedPartTransform(ModelRenderer mass, ModelRenderer part,
                                                   float scale) {
        float positionX = mass.rotationPointX;
        float positionY = mass.rotationPointY;
        float positionZ = mass.rotationPointZ;
        if (part != mass) {
            positionX += part.rotationPointX;
            positionY += part.rotationPointY;
            positionZ += part.rotationPointZ;
        }
        GlStateManager.translate(positionX * scale, positionY * scale, positionZ * scale);
        if (part.rotateAngleZ != 0.0F) {
            GlStateManager.rotate(part.rotateAngleZ * (180.0F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        }
        if (part.rotateAngleY != 0.0F) {
            GlStateManager.rotate(part.rotateAngleY * (180.0F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }
        if (part.rotateAngleX != 0.0F) {
            GlStateManager.rotate(part.rotateAngleX * (180.0F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }
    }

    private static float[] randomSurfacePoint(ModelBox cube, Random random) {
        float minimumX = cube.posX1;
        float minimumY = cube.posY1;
        float minimumZ = cube.posZ1;
        float maximumX = cube.posX2;
        float maximumY = cube.posY2;
        float maximumZ = cube.posZ2;
        if (cube instanceof PreciseModelBoxBounds) {
            PreciseModelBoxBounds precise = (PreciseModelBoxBounds) cube;
            minimumX = precise.minimumX();
            minimumY = precise.minimumY();
            minimumZ = precise.minimumZ();
            maximumX = precise.maximumX();
            maximumY = precise.maximumY();
            maximumZ = precise.maximumZ();
        }
        float x = minimumX;
        float y = minimumY;
        float z = minimumZ;
        int xSize = (int) (maximumX - minimumX);
        int ySize = (int) (maximumY - minimumY);
        int zSize = (int) (maximumZ - minimumZ);
        if (xSize <= 0 || ySize <= 0 || zSize <= 0) return new float[]{x, y, z};
        int direction = random.nextInt(6);

        if (direction < 2) {
            x += random.nextInt(xSize);
            y = direction == 0 ? minimumY : maximumY - 1.0F;
            z += random.nextInt(zSize);
        } else if (direction < 4) {
            x += random.nextInt(xSize);
            y += random.nextInt(ySize);
            z = direction == 2 ? minimumZ : maximumZ - 1.0F;
        } else {
            y += random.nextInt(ySize);
            z += random.nextInt(zSize);
            x = direction == 4 ? minimumX : maximumX - 1.0F;
        }
        return new float[]{x, y, z};
    }

    private static void drawPulseCube(float[] point, float scale, float alpha,
                                      int textureU, int textureV) {
        ModelBox pulse = new ModelBox(PULSE_TEXTURE_OWNER, textureU, textureV,
                point[0], point[1], point[2], 1, 1, 1, 0.01F, false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

        pulse.render(buffer, scale);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
