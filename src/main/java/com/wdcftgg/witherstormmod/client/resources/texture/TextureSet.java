package com.wdcftgg.witherstormmod.client.resources.texture;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.util.ResourceLocation;


public final class TextureSet {

    public static final TextureSet DEFAULT = new TextureSet(
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/wither_storm_invulnerable.png"),
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/wither_storm.png"),
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/wither_storm_emissive_decal.png"),
            new ResourceLocation(Tags.MOD_ID, "textures/entity/wither_storm/debris.png"));

    private final ResourceLocation invulnerable;
    private final ResourceLocation main;
    private final ResourceLocation emissiveDecal;
    private final ResourceLocation debrisRing;

    public TextureSet(ResourceLocation invulnerable, ResourceLocation main,
                      ResourceLocation emissiveDecal, ResourceLocation debrisRing) {
        this.invulnerable = invulnerable;
        this.main = main;
        this.emissiveDecal = emissiveDecal;
        this.debrisRing = debrisRing;
    }

    public ResourceLocation getInvulnerable() {
        return invulnerable;
    }

    public ResourceLocation getMain() {
        return main;
    }

    public ResourceLocation getEmissiveDecal() {
        return emissiveDecal;
    }

    public ResourceLocation getDebrisRing() {
        return debrisRing;
    }
}
