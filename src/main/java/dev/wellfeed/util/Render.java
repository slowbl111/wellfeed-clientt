package dev.wellfeed.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Render {
    // Cream palette
    public static final int CREAM      = 0xFFF5E6C8;
    public static final int CREAM_SOFT = 0xFFE8D5AE;
    public static final int ROAST      = 0xFF3B2F26;
    public static final int PANEL      = 0xC0261E17;
    public static final int PANEL_ROW  = 0xB0332A22;

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        int oa = (int) (aa + (ba - aa) * t);
        int or = (int) (ar + (br - ar) * t);
        int og = (int) (ag + (bg - ag) * t);
        int ob = (int) (ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    /** Rectangle with 1px clipped corners. Regions don't overlap, so translucent colors stay even. */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void textCentered(DrawContext ctx, TextRenderer f, String s, int cx, int y, int color, boolean shadow) {
        ctx.drawText(f, s, cx - f.getWidth(s) / 2, y, color, shadow);
    }

    private Render() {}
}
