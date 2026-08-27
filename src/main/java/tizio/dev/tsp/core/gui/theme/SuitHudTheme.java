package tizio.dev.tsp.core.gui.theme;

import tizio.dev.tsp.core.utils.Color;

public final class SuitHudTheme {

    public static final boolean TEXT_SHADOW = true;

    // Primary Amber / Cyberpunk Sci-Fi Palette (From reference HUD)
    public static final int AMBER_PRIMARY       = Color.iRGBA(255, 126,  54, 255);
    public static final int AMBER_BRIGHT        = Color.iRGBA(255, 174, 110, 255);
    public static final int AMBER_GLOW          = Color.iRGBA(255, 140,  60, 180);
    public static final int AMBER_DIM           = Color.iRGBA(190,  88,  32, 200);
    public static final int AMBER_SUB           = Color.iRGBA(120,  52,  16, 140);
    public static final int AMBER_FAINT         = Color.iRGBA( 70,  30,  10,  80);

    // Alert & Warning Overrides
    public static final int ALERT_RED           = Color.iRGBA(255,  40,  50, 255);
    public static final int ALERT_RED_DIM       = Color.iRGBA(160,  20,  30, 180);
    public static final int ALERT_AMBER         = Color.iRGBA(255, 190,  30, 255);
    public static final int FROST_BLUE          = Color.iRGBA( 90, 210, 255, 255);
    public static final int EMERALD_GREEN       = Color.iRGBA( 60, 245, 150, 255);

    // Text & Headers
    public static final int TEXT_HEADER         = AMBER_BRIGHT;
    public static final int TEXT_NORMAL         = AMBER_PRIMARY;
    public static final int TEXT_MUTED          = AMBER_DIM;
    public static final int TEXT_FAINT          = AMBER_SUB;
    public static final int TEXT_HIGHLIGHT      = Color.iRGBA(255, 230, 210, 255);

    // Gauges & Elements
    public static final int GAUGE_BG_ARC        = Color.iRGBA( 60,  26,  10, 100);
    public static final int GAUGE_ACTIVE_ARC    = AMBER_PRIMARY;
    public static final int GAUGE_TICK          = AMBER_BRIGHT;

    // Corner / Visor Tint
    public static final int VISOR_EDGE_TINT     = Color.iRGBA(255, 100,  30,  35);
    public static final int VISOR_BRACKET       = AMBER_DIM;

    /**
     * Maps normalized temperature [-1.0, 1.0] to Celsius core/body temperature.
     * 0.0 -> 37.0°C (neutral body temp)
     * -1.0 -> 12.0°C (hypothermia)
     * +1.0 -> 62.0°C (hyperthermia)
     */
    public static float toCelsiusBody(float normalizedTemp) {
        return 37.0F + (normalizedTemp * 25.0F);
    }

    /**
     * Maps normalized ambient temperature [-1.0, 1.0] to approximate planetary Celsius.
     * -1.0 (Space Void) -> -270°C
     * 0.0 (Earth Overworld) -> 20°C
     * +1.0 (Extreme Heat) -> 450°C
     */
    public static float toCelsiusAmbient(float normalizedEnvTemp) {
        if (normalizedEnvTemp < 0.0F) {
            return 20.0F + (normalizedEnvTemp * 290.0F);
        } else {
            return 20.0F + (normalizedEnvTemp * 430.0F);
        }
    }

    public static int getOxygenColor(float oxygen) {
        if (oxygen > 0.50F) {
            return AMBER_PRIMARY;
        } else if (oxygen > 0.20F) {
            return ALERT_AMBER;
        } else {
            return ALERT_RED;
        }
    }

    public static int getTemperatureColor(float temp) {
        if (temp <= -0.70F) {
            return FROST_BLUE;
        } else if (temp >= 0.70F) {
            return ALERT_RED;
        } else if (Math.abs(temp) > 0.35F) {
            return ALERT_AMBER;
        } else {
            return AMBER_PRIMARY;
        }
    }

    public static int getHeartRateColor(int bpm) {
        if (bpm < 95) {
            return AMBER_PRIMARY;
        } else if (bpm < 135) {
            return ALERT_AMBER;
        } else {
            return ALERT_RED;
        }
    }
}
