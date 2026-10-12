package dev.wellfeed.gui;

import dev.wellfeed.WellfeedClient;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wellfeed v4 menu: centered animated panel, category tabs, animated switches, draggable sliders,
 * keybind chips and a slow drift of cream dust in the background.
 *
 *  - Left click a module: toggle.   - Right click a module: show / hide its settings.
 *  - Drag a slider, click a switch.  - ESC or Right Shift closes.
 *
 * Mouse buttons are read by polling GLFW inside render(), which keeps this screen independent of
 * the mouse-event signatures that changed between recent Minecraft versions.
 */
public final class ClickGuiScreen extends Screen {
    private static final int W = 360, H = 236, SIDE = 84, TOP = 30, PAD = 8;
    private static final int ROW_H = 20, SET_H = 16;

    private static final int BG        = 0xF01B1510;
    private static final int SIDEBAR   = 0xF0150F0B;
    private static final int ROW       = 0xC02A2119;
    private static final int ROW_HOVER = 0xC03A2E23;
    private static final int OFF_TRACK = 0xFF4A3D31;
    private static final int ORANGE    = 0xFFE59A4B;

    private static String selected = ModuleManager.CATEGORIES[0];   // remembered between openings
    private Module expanded;
    private Setting dragging;
    private boolean prevL, prevR, prevShift;
    private long openedAt;
    private final Map<Object, Float> anim = new HashMap<>();
    private long lastNano;

    public ClickGuiScreen() {
        super(Text.literal("Wellfeed"));
    }

    @Override
    protected void init() {
        long h = client.getWindow().getHandle();
        prevL = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevShift = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (openedAt == 0) openedAt = System.nanoTime();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private float ease(Object key, boolean on, float k) {
        float v = anim.getOrDefault(key, on ? 1f : 0f);
        v += ((on ? 1f : 0f) - v) * k;
        anim.put(key, v);
        return v;
    }

    @Override
    public void render(DrawContext ctx, int rawMouseX, int rawMouseY, float delta) {
        super.render(ctx, rawMouseX, rawMouseY, delta);

        long handle = client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean clickL = l && !prevL, clickR = r && !prevR, pressShift = shift && !prevShift;
        prevL = l; prevR = r; prevShift = shift;
        if (!l) dragging = null;
        if (pressShift) { close(); return; }

        long nowN = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (nowN - lastNano) / 1_000_000_000f);
        lastNano = nowN;
        float k = 1f - (float) Math.exp(-dt * 16f);
        double secs = (nowN - openedAt) / 1_000_000_000.0;

        // Open animation: scale + fade, easeOutBack-ish.
        float t = (float) Math.min(1, secs / 0.28);
        float eo = 1f - (float) Math.pow(1 - t, 3);
        float scale = 0.92f + 0.08f * eo;
        float alpha = eo;
        boolean interactive = t >= 1f;

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int px = (sw - W) / 2, py = (sh - H) / 2;
        float cx = sw / 2f, cy = sh / 2f;
        int mouseX = (int) ((rawMouseX - cx) / scale + cx);
        int mouseY = (int) ((rawMouseY - cy) / scale + cy);

        TextRenderer font = client.textRenderer;

        // Dust drifting across the whole screen.
        for (int i = 0; i < 36; i++) {
            double sx = frac(Math.sin(i * 91.7) * 43758.5), sy = frac(Math.sin(i * 17.3 + 4.0) * 24634.6);
            double sp = 3 + 7 * frac(Math.sin(i * 7.1) * 9631.2), ph = i * 1.7;
            int dx = (int) (sx * sw + Math.sin(secs * 0.5 + ph) * 12);
            int dy = (int) (sh - ((secs * sp + sy * sh) % sh));
            int size = i % 5 == 0 ? 2 : 1;
            float tw = 0.35f + 0.35f * (float) Math.sin(secs * 1.3 + ph);
            ctx.fill(dx, dy, dx + size, dy + size, Render.fade(Render.CREAM, tw * alpha));
        }

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        m.translate(-cx, -cy);

        // Panel
        Render.roundRect(ctx, px - 1, py - 1, W + 2, H + 2, Render.fade(0xFF000000, 0.5f * alpha));
        Render.roundRect(ctx, px, py, W, H, Render.fade(BG, alpha));
        ctx.fill(px, py + TOP, px + SIDE, py + H, Render.fade(SIDEBAR, alpha));

        // Header with a slowly shifting accent line
        ctx.drawText(font, "Wellfeed", px + PAD + 2, py + 10, Render.fade(Render.CREAM, alpha), true);
        ctx.drawText(font, "v4", px + PAD + 2 + font.getWidth("Wellfeed") + 4, py + 10,
                Render.fade(ORANGE, alpha), true);
        for (int x = 0; x < W; x += 4) {
            float u = (float) (0.5 + 0.5 * Math.sin(x * 0.03 - secs * 1.6));
            ctx.fill(px + x, py + TOP - 2, px + x + 4, py + TOP - 1,
                    Render.fade(Render.lerpColor(Render.CREAM, ORANGE, u), 0.85f * alpha));
        }

        // Category tabs
        int ty = py + TOP + 8;
        for (String cat : ModuleManager.CATEGORIES) {
            boolean sel = cat.equals(selected);
            boolean hover = inside(mouseX, mouseY, px + 6, ty, SIDE - 12, 20);
            float a = ease("tab:" + cat, sel, k);
            float hv = ease("tabh:" + cat, hover, k);
            int bg = Render.lerpColor(Render.fade(ROW, 0f), Render.CREAM, a);
            if (a < 0.02f && hv > 0.02f) bg = Render.fade(ROW_HOVER, hv);
            Render.roundRect(ctx, px + 6, ty, SIDE - 12, 20, Render.fade(bg, alpha));
            int tc = Render.lerpColor(Render.CREAM, Render.ROAST, a);
            ctx.drawText(font, cat, px + 14 + (int) (3 * a), ty + 6, Render.fade(tc, alpha), false);
            if (interactive && hover && clickL) { selected = cat; expanded = null; }
            ty += 24;
        }

        // Footer hint
        ctx.drawText(font, "RMB: settings", px + 10, py + H - 24, Render.fade(Render.CREAM_SOFT, 0.7f * alpha), false);
        ctx.drawText(font, "R-Shift: close", px + 10, py + H - 14, Render.fade(Render.CREAM_SOFT, 0.7f * alpha), false);

        // Module list
        int lx = px + SIDE + PAD, lw = W - SIDE - PAD * 2;
        int cy2 = py + TOP + 8;
        String tooltip = null;

        for (Module mod : ModuleManager.MODULES) {
            if (!ModuleManager.categoryOf(mod).equals(selected)) continue;

            boolean hover = inside(mouseX, mouseY, lx, cy2, lw, ROW_H);
            boolean on = mod.isEnabled();
            float a = ease(mod, on, k);
            float hv = ease("h:" + mod.name, hover, k);

            int bg = Render.lerpColor(ROW, ROW_HOVER, hv);
            Render.roundRect(ctx, lx, cy2, lw, ROW_H, Render.fade(bg, alpha));
            // accent bar on enabled modules
            ctx.fill(lx, cy2 + 3, lx + 2, cy2 + ROW_H - 3, Render.fade(ORANGE, a * alpha));

            ctx.drawText(font, mod.name, lx + 9, cy2 + 6,
                    Render.fade(Render.lerpColor(Render.CREAM_SOFT, Render.CREAM, a), alpha), false);

            // switch
            sw(ctx, lx + lw - 30, cy2 + 5, a, alpha);

            // keybind chip
            String key = WellfeedClient.keyLabel(mod);
            if (!key.isEmpty()) {
                int kw = font.getWidth(key) + 8;
                int kx = lx + lw - 38 - kw;
                Render.roundRect(ctx, kx, cy2 + 4, kw, 12, Render.fade(0xFF3A2F25, alpha));
                ctx.drawText(font, key, kx + 4, cy2 + 6, Render.fade(Render.CREAM_SOFT, alpha), false);
            }
            if (!mod.settings.isEmpty()) {
                String arrow = expanded == mod ? "v" : ">";
                ctx.drawText(font, arrow, lx + 9 + font.getWidth(mod.name) + 6, cy2 + 6,
                        Render.fade(Render.CREAM_SOFT, 0.6f * alpha), false);
            }

            if (hover) {
                tooltip = mod.description;
                if (interactive && clickL) mod.toggle();
                if (interactive && clickR && !mod.settings.isEmpty()) expanded = expanded == mod ? null : mod;
            }
            cy2 += ROW_H + 2;

            if (expanded == mod) {
                for (Setting s : mod.settings) {
                    boolean sh2 = inside(mouseX, mouseY, lx + 8, cy2, lw - 8, SET_H);
                    Render.roundRect(ctx, lx + 8, cy2, lw - 8, SET_H,
                            Render.fade(sh2 ? 0xC0322820 : 0xB0221A14, alpha));
                    ctx.drawText(font, s.name, lx + 14, cy2 + 4, Render.fade(Render.CREAM, alpha), false);

                    if (s.type == Setting.Type.BOOL) {
                        float sa = ease(s, s.getBool(), k);
                        sw(ctx, lx + lw - 30, cy2 + 3, sa, alpha);
                        if (interactive && sh2 && clickL) s.toggle();
                    } else {
                        // slider on the right half
                        int sx = lx + lw - 112, sww = 74, sy = cy2 + 7;
                        boolean onSlider = inside(mouseX, mouseY, sx - 4, cy2, sww + 8, SET_H);
                        if (interactive && onSlider && clickL) dragging = s;
                        if (dragging == s) s.setFraction((mouseX - sx) / (double) sww);
                        float target = (float) s.fraction();
                        float cur = anim.getOrDefault(s, target);
                        cur += (target - cur) * k;
                        anim.put(s, cur);
                        ctx.fill(sx, sy, sx + sww, sy + 2, Render.fade(OFF_TRACK, alpha));
                        ctx.fill(sx, sy, sx + (int) (sww * cur), sy + 2, Render.fade(ORANGE, alpha));
                        int kx = sx + (int) (sww * cur);
                        Render.roundRect(ctx, kx - 2, sy - 3, 5, 8, Render.fade(Render.CREAM, alpha));
                        String v = fmt(s);
                        ctx.drawText(font, v, lx + lw - 6 - font.getWidth(v), cy2 + 4,
                                Render.fade(Render.CREAM_SOFT, alpha), false);
                    }
                    cy2 += SET_H + 1;
                }
                cy2 += 3;
            }
        }

        if (tooltip != null) {
            ctx.drawText(font, tooltip, lx, py + H - 14, Render.fade(Render.CREAM_SOFT, alpha), true);
        }

        m.popMatrix();
    }

    /** Animated toggle switch; a = 0 (off) .. 1 (on). */
    private static void sw(DrawContext ctx, int x, int y, float a, float alpha) {
        Render.roundRect(ctx, x, y, 22, 10, Render.fade(Render.lerpColor(OFF_TRACK, ORANGE, a), alpha));
        int kx = x + 1 + Math.round(12 * a);
        Render.roundRect(ctx, kx, y + 1, 8, 8, Render.fade(Render.lerpColor(Render.CREAM_SOFT, Render.CREAM, a), alpha));
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private static String fmt(Setting s) {
        return s.step() < 1 ? String.format(Locale.ROOT, "%.2f", s.getNum()).replaceAll("0$", "")
                : String.format(Locale.ROOT, "%.0f", s.getNum());
    }
}
