package dk.wellfeed.gui;

import java.awt.Color;

public class Theme {
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFAAAAAA;
    public static final int PANEL = 0xE0141420;
    public static final int PANEL_HEADER = 0xF01A1A2A;
    public static final int HUD_BG = 0x90101018;

    // Langsomt skiftende lilla/pink accent. offset forskyder farven pr. element.
    public static int accent(double offset) {
        double t = System.currentTimeMillis() / 1000.0;
        float hue = (float) (0.76 + 0.08 * Math.sin(t * 1.5 + offset));
        return Color.HSBtoRGB(hue, 0.65f, 1.0f);
    }
}
