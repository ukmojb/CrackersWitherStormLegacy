package com.wdcftgg.witherstormmod.client.resources.color;

import java.awt.Color;


public final class ColorSet {

    public static final ColorSet DEFAULT = new ColorSet(
            new Color(128, 77, 204), new Color(128, 77, 204),
            new Color(150, 59, 255, 75), SkyColorSet.DEFAULT);

    private final Color tractorBeamColor;
    private final Color tractorBeamNightColor;
    private final Color nightShineColor;
    private final SkyColorSet skyColors;

    public ColorSet(Color tractorBeamColor, Color tractorBeamNightColor,
                    Color nightShineColor, SkyColorSet skyColors) {
        this.tractorBeamColor = tractorBeamColor;
        this.tractorBeamNightColor = tractorBeamNightColor;
        this.nightShineColor = nightShineColor;
        this.skyColors = skyColors;
    }

    public Color getTractorBeamColor() {
        return tractorBeamColor;
    }

    public Color getTractorBeamNightColor() {
        return tractorBeamNightColor;
    }

    public Color getNightShineColor() {
        return nightShineColor;
    }

    public SkyColorSet getSkyColors() {
        return skyColors;
    }
}
