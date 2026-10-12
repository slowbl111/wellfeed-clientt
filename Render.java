package dev.wellfeed.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

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

    /** Same color with its alpha multiplied by f (0..1). */
    public static int fade(int color, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int a = (int) (((color >>> 24) & 255) * f);
        return (a << 24) | (color & 0xFFFFFF);
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

    /**
     * Straight line at any angle and any (fractional) thickness, drawn as one rotated, scaled
     * rectangle. Thickness is in GUI pixels.
     */
    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, int color, float thickness) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate((float) Math.atan2(dy, dx));
        m.translate(0f, -thickness / 2f);
        m.scale(1f, thickness);
        ctx.fill(0, 0, (int) Math.ceil(len), 1, color);
        m.popMatrix();
    }

    private Render() {}
}
