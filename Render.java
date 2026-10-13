package dev.wellfeed.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Palette and small 2D drawing helpers shared by the GUI and HUD. */
public final class Render {
    // Cream / roast palette
    public static final int CREAM      = 0xFFF5E6C8;
    public static final int CREAM_SOFT = 0xFFE8D5AE;
    public static final int CREAM_DIM  = 0xFFA89A80;
    public static final int ROAST      = 0xFF3B2F26;
    public static final int PANEL      = 0xD0201914;
    public static final int PANEL_ROW  = 0xB0332A22;
    public static final int GOOD       = 0xFF9BD67A;
    public static final int BAD        = 0xFFE06C6C;
    public static final int WARN       = 0xFFE8A87C;

    public static int alpha(int color, float a) {
        int oa = Math.round(((color >>> 24) & 255) * Math.max(0f, Math.min(1f, a)));
        return (oa << 24) | (color & 0xFFFFFF);
    }

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

    /** Red (0) through yellow to green (1). */
    public static int redToGreen(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t < 0.5f ? lerpColor(0xFFE06C6C, 0xFFF2D36B, t * 2f) : lerpColor(0xFFF2D36B, 0xFF9BD67A, (t - 0.5f) * 2f);
    }

    /** Frame-rate independent exponential smoothing toward a target. */
    public static float approach(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-dt * speed));
    }

    public static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Rounded rectangle with a stepped quarter-circle corner of radius r. */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * (double) r - dy * dy));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        roundRect(ctx, x, y, w, h, 2, color);
    }

    /** Soft drop shadow made of a few widening translucent rounded rects. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r, float a) {
        for (int i = 4; i >= 1; i--) {
            roundRect(ctx, x - i, y - i + 1, w + i * 2, h + i * 2, r + i, alpha(0xFF000000, a * 0.07f));
        }
    }

    public static void text(DrawContext ctx, TextRenderer f, String s, int x, int y, int color, boolean shadow) {
        ctx.drawText(f, s, x, y, color, shadow);
    }

    public static void textCentered(DrawContext ctx, TextRenderer f, String s, int cx, int y, int color, boolean shadow) {
        ctx.drawText(f, s, cx - f.getWidth(s) / 2, y, color, shadow);
    }

    public static void textRight(DrawContext ctx, TextRenderer f, String s, int rx, int y, int color, boolean shadow) {
        ctx.drawText(f, s, rx - f.getWidth(s), y, color, shadow);
    }

    /**
     * Straight line made of 1-pixel-tall (or wide) runs, so a long diagonal costs a few fills
     * instead of one fill per pixel. Clipped to the screen first.
     */
    public static void line(DrawContext ctx, double x0, double y0, double x1, double y1, int color, int thick,
                            int screenW, int screenH) {
        // Liang-Barsky clip against the screen with a small margin.
        double xmin = -2, ymin = -2, xmax = screenW + 2, ymax = screenH + 2;
        double dx = x1 - x0, dy = y1 - y0;
        double t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - xmin, xmax - x0, y0 - ymin, ymax - y0};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) return;
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) {
                    if (t > t1) return;
                    if (t > t0) t0 = t;
                } else {
                    if (t < t0) return;
                    if (t < t1) t1 = t;
                }
            }
        }
        double ax = x0 + dx * t0, ay = y0 + dy * t0, bx = x0 + dx * t1, by = y0 + dy * t1;
        drawClipped(ctx, ax, ay, bx, by, color, Math.max(1, thick));
    }

    private static void drawClipped(DrawContext ctx, double ax, double ay, double bx, double by, int color, int th) {
        double dx = bx - ax, dy = by - ay;
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (bx < ax) { double t = ax; ax = bx; bx = t; t = ay; ay = by; by = t; dx = -dx; dy = -dy; }
            int n = (int) Math.round(bx) - (int) Math.round(ax);
            int runX = (int) Math.round(ax);
            int runY = (int) Math.round(ay);
            for (int i = 1; i <= n; i++) {
                double t = (double) i / n;
                int px = (int) Math.round(ax) + i;
                int py = (int) Math.round(ay + dy * t);
                if (py != runY) {
                    ctx.fill(runX, runY, px, runY + th, color);
                    runX = px;
                    runY = py;
                }
            }
            ctx.fill(runX, runY, (int) Math.round(bx) + 1, runY + th, color);
        } else {
            if (by < ay) { double t = ax; ax = bx; bx = t; t = ay; ay = by; by = t; dx = -dx; dy = -dy; }
            int n = (int) Math.round(by) - (int) Math.round(ay);
            int runX = (int) Math.round(ax);
            int runY = (int) Math.round(ay);
            for (int i = 1; i <= n; i++) {
                double t = (double) i / n;
                int py = (int) Math.round(ay) + i;
                int px = (int) Math.round(ax + dx * t);
                if (px != runX) {
                    ctx.fill(runX, runY, runX + th, py, color);
                    runX = px;
                    runY = py;
                }
            }
            ctx.fill(runX, runY, runX + th, (int) Math.round(by) + 1, color);
        }
    }

    private Render() {}
}
