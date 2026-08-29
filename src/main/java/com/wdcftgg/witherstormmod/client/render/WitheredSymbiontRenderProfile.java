package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.util.ResourceLocation;


final class WitheredSymbiontRenderProfile {
    static final float MODEL_SCALE = 1.8F;
    static final float SHADOW_SIZE = 0.8F;
    static final float WALK_SPEED_THRESHOLD = 0.01F;
    static final ResourceLocation BASE_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/withered_symbiont/withered_symbiont.png");
    static final ResourceLocation EASTER_EGG_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/misc/crackers.png");
    static final ResourceLocation EMISSIVE_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/withered_symbiont/withered_symbiont_emissive.png");
    static final ResourceLocation TEAR_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/withered_symbiont/withered_symbiont_tear.png");

    private WitheredSymbiontRenderProfile() {
    }

    static boolean usesEasterEggTexture(String customName) {
        return "nonamecrackers2".equals(customName);
    }

    static float getWalkWobbleDegrees(float walkPosition, float walkSpeed) {
        if (walkSpeed < WALK_SPEED_THRESHOLD) return 0.0F;
        float cycle = (walkPosition + 6.0F) % 13.0F;
        float wave = (Math.abs(cycle - 6.5F) - 3.25F) / 3.25F;
        return 6.5F * wave;
    }
}
