package com.wellfeed.gui;

import com.wellfeed.Config;

public final class Theme {
    public static final String[] NAMES = {"Amethyst", "Cyan", "Lime", "Rose", "Rainbow"};
    private static final float[] HUES = {276f, 192f, 92f, 338f};

    private Theme() {
    }

    /** Current accent as opaque ARGB. {@code shift} offsets the hue in Rainbow mode. */
    public static int accent(int shift) {
        if (Config.accent >= 4) {
            float h = (System.currentTimeMillis() / 20.0f + shift * 12f) % 360f;
            return hsv(h, 0.6f, 1f);
        }
        return hsv(HUES[Config.accent], 0.62f, 1f);
    }

    /** Colour of the picker swatch for an accent index. */
    public static int swatch(int index) {
        if (index >= 4) {
            return hsv((System.currentTimeMillis() / 20.0f) % 360f, 0.6f, 1f);
        }
        return hsv(HUES[index], 0.62f, 1f);
    }

    public static int alpha(int argb, int a) {
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r;
        float g;
        float b;
        switch (((int) (h / 60f)) % 6) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return 0xFF000000
                | ((int) ((r + m) * 255) << 16)
                | ((int) ((g + m) * 255) << 8)
                | (int) ((b + m) * 255);
    }
}
