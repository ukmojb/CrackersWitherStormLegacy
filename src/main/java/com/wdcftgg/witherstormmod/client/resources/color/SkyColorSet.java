package com.wdcftgg.witherstormmod.client.resources.color;

import java.awt.Color;


public final class SkyColorSet {

    public static final SkyColorSet DEFAULT =
            new SkyColorSet(new Color(20, 0, 19), new Color(28, 10, 27),
                    new Color(133, 69, 62), null, null, null);

    private final Color skyColor;
    private final Color cloudColor;
    private final Color fogColor;
    private final Color nightSkyColor;
    private final Color nightCloudColor;
    private final Color nightFogColor;

    public SkyColorSet(Color skyColor, Color cloudColor, Color fogColor) {
        this(skyColor, cloudColor, fogColor, null, null, null);
    }

    public SkyColorSet(Color skyColor, Color cloudColor, Color fogColor,
                       Color nightSkyColor, Color nightCloudColor, Color nightFogColor) {
        this.skyColor = skyColor;
        this.cloudColor = cloudColor;
        this.fogColor = fogColor;
        this.nightSkyColor = nightSkyColor;
        this.nightCloudColor = nightCloudColor;
        this.nightFogColor = nightFogColor;
    }

    public Color getSkyColor() {
        return skyColor;
    }

    public Color getCloudColor() {
        return cloudColor;
    }

    public Color getFogColor() {
        return fogColor;
    }

    public Color getNightSkyColor() {
        return nightSkyColor;
    }

    public Color getNightCloudColor() {
        return nightCloudColor;
    }

    public Color getNightFogColor() {
        return nightFogColor;
    }
}
