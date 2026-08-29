package com.wdcftgg.witherstormmod.client.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.resources.color.ColorSet;
import com.wdcftgg.witherstormmod.client.resources.color.SkyColorSet;
import com.wdcftgg.witherstormmod.client.resources.texture.TextureSet;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;





public final class WitherStormResourceConfigManager {

    public static final WitherStormResourceConfigManager INSTANCE =
            new WitherStormResourceConfigManager();

    private final Map<Integer, ColorSet> colors = new LinkedHashMap<Integer, ColorSet>();
    private final Map<Integer, TextureSet> textures = new LinkedHashMap<Integer, TextureSet>();
    private Color bowelsFogColor;
    private boolean initialized;

    private WitherStormResourceConfigManager() {
    }

    public static synchronized void initialize() {
        INSTANCE.load();
    }

    private void load() {
        if (initialized) return;
        loadColors();
        loadTextures();
        initialized = true;
    }

    private void loadColors() {
        try (InputStream stream = UpstreamResourceArchive.open(
                "assets/witherstormmod/config/colors.json")) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("bowels_fog")) {
                bowelsFogColor = parseColor(root.getAsJsonObject("bowels_fog"));
            }
            JsonObject byPhase = root.has("by_phase")
                    ? root.getAsJsonObject("by_phase") : root;
            for (Map.Entry<String, JsonElement> entry : byPhase.entrySet()) {
                int phase;
                try {
                    phase = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException exception) {
                    continue;
                }
                if (phase < 0 || phase > 7) continue;
                colors.put(phase, parseColorSet(entry.getValue().getAsJsonObject()));
            }
            WitherStormMod.LOGGER.info("Loaded external Wither Storm color sets");
        } catch (Exception exception) {

        }
    }

    private void loadTextures() {
        try (InputStream stream = UpstreamResourceArchive.open(
                "assets/witherstormmod/config/textures.json")) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject byPhase = root.has("by_phase")
                    ? root.getAsJsonObject("by_phase") : root;
            for (Map.Entry<String, JsonElement> entry : byPhase.entrySet()) {
                int phase;
                try {
                    phase = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException exception) {
                    continue;
                }
                if (phase < 0 || phase > 7) continue;
                textures.put(phase, parseTextureSet(entry.getValue().getAsJsonObject()));
            }
            WitherStormMod.LOGGER.info("Loaded external Wither Storm texture sets");
        } catch (Exception exception) {

        }
    }

    public ColorSet getColorSetByPhase(int phase) {
        assertPhaseRange(phase);
        ColorSet set = colors.get(phase);
        return set == null ? ColorSet.DEFAULT : set;
    }

    public TextureSet getTextureSetByPhase(int phase) {
        assertPhaseRange(phase);
        TextureSet set = textures.get(phase);
        return set == null ? TextureSet.DEFAULT : set;
    }

    public Color getBowelsFogColor() {
        return bowelsFogColor;
    }

    private static void assertPhaseRange(int phase) {
        if (phase < 0 || phase > 7) {
            throw new IllegalArgumentException("Phase outside of range: 0 ~ 7");
        }
    }

    private static ColorSet parseColorSet(JsonObject object) {
        Color tractor = ColorSet.DEFAULT.getTractorBeamColor();
        Color tractorNight = ColorSet.DEFAULT.getTractorBeamNightColor();
        Color shine = ColorSet.DEFAULT.getNightShineColor();
        SkyColorSet sky = ColorSet.DEFAULT.getSkyColors();
        if (object.has("tractor_beams")) {
            tractor = parseColor(object.getAsJsonObject("tractor_beams"));
        }
        if (object.has("tractor_beams_night")) {
            tractorNight = parseColor(object.getAsJsonObject("tractor_beams_night"));
        }
        if (object.has("night_shine")) {
            shine = parseColor(object.getAsJsonObject("night_shine"));
        }
        if (object.has("sky_colors")) {
            sky = parseSkyColorSet(object.getAsJsonObject("sky_colors"));
        }
        return new ColorSet(tractor, tractorNight, shine, sky);
    }

    private static SkyColorSet parseSkyColorSet(JsonObject object) {
        Color sky = SkyColorSet.DEFAULT.getSkyColor();
        Color cloud = SkyColorSet.DEFAULT.getCloudColor();
        Color fog = SkyColorSet.DEFAULT.getFogColor();
        Color nightSky = SkyColorSet.DEFAULT.getNightSkyColor();
        Color nightCloud = SkyColorSet.DEFAULT.getNightCloudColor();
        Color nightFog = SkyColorSet.DEFAULT.getNightFogColor();
        if (object.has("day") && object.has("night")) {
            JsonObject day = object.getAsJsonObject("day");
            JsonObject night = object.getAsJsonObject("night");
            if (day.has("sky_darken")) sky = parseColor(day.getAsJsonObject("sky_darken"));
            if (day.has("sky_darken_clouds")) cloud = parseColor(day.getAsJsonObject("sky_darken_clouds"));
            if (day.has("sky_darken_fog")) fog = parseColor(day.getAsJsonObject("sky_darken_fog"));
            if (night.has("sky_darken")) nightSky = parseColor(night.getAsJsonObject("sky_darken"));
            if (night.has("sky_darken_clouds")) nightCloud = parseColor(night.getAsJsonObject("sky_darken_clouds"));
            if (night.has("sky_darken_fog")) nightFog = parseColor(night.getAsJsonObject("sky_darken_fog"));
        } else {
            if (object.has("sky_darken")) sky = parseColor(object.getAsJsonObject("sky_darken"));
            if (object.has("sky_darken_clouds")) cloud = parseColor(object.getAsJsonObject("sky_darken_clouds"));
            if (object.has("sky_darken_fog")) fog = parseColor(object.getAsJsonObject("sky_darken_fog"));
        }
        return new SkyColorSet(sky, cloud, fog, nightSky, nightCloud, nightFog);
    }

    private static TextureSet parseTextureSet(JsonObject object) {
        ResourceLocation invulnerable = TextureSet.DEFAULT.getInvulnerable();
        ResourceLocation main = TextureSet.DEFAULT.getMain();
        ResourceLocation emissiveDecal = TextureSet.DEFAULT.getEmissiveDecal();
        ResourceLocation debrisRing = TextureSet.DEFAULT.getDebrisRing();
        if (object.has("invulnerable")) invulnerable = parseTexture(object, "invulnerable");
        if (object.has("main")) main = parseTexture(object, "main");
        if (object.has("emissive_decal")) emissiveDecal = parseTexture(object, "emissive_decal");
        if (object.has("debris_ring")) debrisRing = parseTexture(object, "debris_ring");
        return new TextureSet(invulnerable, main, emissiveDecal, debrisRing);
    }

    private static ResourceLocation parseTexture(JsonObject object, String key) {
        return new ResourceLocation(object.get(key).getAsString());
    }

    private static Color parseColor(JsonObject object) {
        int red = object.get("red").getAsInt();
        int green = object.get("green").getAsInt();
        int blue = object.get("blue").getAsInt();
        if (object.has("alpha")) {
            return new Color(red, green, blue, object.get("alpha").getAsInt());
        }
        return new Color(red, green, blue);
    }
}
