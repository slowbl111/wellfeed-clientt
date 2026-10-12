package net.wellfeed.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wellfeed.module.Module;
import net.wellfeed.module.ModuleManager;
import net.wellfeed.module.NumberSetting;
import net.wellfeed.module.impl.AutoMiner;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;

/**
 * Click GUI with layered "depth" panels: every panel shifts with the mouse by a different amount
 * (parallax), casts a soft shadow and sits over an animated perspective grid floor.
 *
 * Left click a module = toggle. Right click a module = show its settings.
 * Drag a panel header to move it. Drag a slider to change a value.
 */
public class ClickGuiScreen extends Screen {
    private static final int ACCENT_A = 0xFF7C5CFF;
    private static final int ACCENT_B = 0xFF35D0FF;
    private static final int BG = 0xEC0E0E16;
    private static final int ROW = 0xFF171724;
    private static final int ROW_HOVER = 0xFF23233A;
    private static final int ON_TOP = 0xFF7C5CFF;
    private static final int ON_BOTTOM = 0xFF4B35C9;
    private static final int TEXT = 0xFFEDEDF7;
    private static final int DIM = 0xFF8A8AA3;
    private static final int GOOD = 0xFF5DFFA0;
    private static final int BAD = 0xFFFF5D7A;

    private static final int HEADER_H = 20;
    private static final int ROW_H = 16;
    private static final int SET_H = 14;

    private static final List<Panel> PANELS = new ArrayList<>();
    private static final Set<Module> EXPANDED = new HashSet<>();

    private final List<Hit> hits = new ArrayList<>();
    private Hit activeSlider;
    private Panel dragging;

    public ClickGuiScreen() {
        super(Text.literal("Wellfeed"));
    }

    @Override
    protected void init() {
        if (PANELS.isEmpty()) {
            int w = 150;
            int gap = 14;
            int regionW = 216;
            int total = w * 3 + regionW + gap * 3;
            int x = Math.max(8, (width - total) / 2);
            int y = 44;
            PANELS.add(new Panel("Movement", Module.Category.MOVEMENT, x, y, w, 1.0f));
            PANELS.add(new Panel("World", Module.Category.WORLD, x + (w + gap), y, w, 1.6f));
            PANELS.add(new Panel("Render", Module.Category.RENDER, x + 2 * (w + gap), y, w, 1.0f));
            PANELS.add(new Panel("AutoMiner Region", null, x + 3 * (w + gap), y, regionW, 2.2f));
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        float px = (mouseX - width / 2f) / (width / 2f);
        float py = (mouseY - height / 2f) / (height / 2f);

        drawBackdrop(ctx, px);

        // title floats highest above everything
        String title = "WELLFEED";
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(width / 2f + (-px * 14), 10 + (-py * 6));
        ctx.getMatrices().scale(2f, 2f);
        ctx.drawText(textRenderer, title, -textRenderer.getWidth(title) / 2, 0, ACCENT_B, true);
        ctx.getMatrices().popMatrix();
        String hint = "Right-click a module for settings  |  /wf pos1 x y z   /wf pos2 x y z   /wf start";
        ctx.drawText(textRenderer, hint, width / 2 - textRenderer.getWidth(hint) / 2 + (int) (-px * 6), 30 + (int) (-py * 3), DIM, false);

        for (Panel p : PANELS) {
            drawPanel(ctx, p, mouseX, mouseY, px, py);
        }
    }

    private void drawBackdrop(DrawContext ctx, float px) {
        ctx.fillGradient(0, 0, width, height, 0xE0050510, 0xF00B0A24);

        // animated perspective grid floor
        int horizon = (int) (height * 0.60f);
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        int cx = width / 2 + (int) (-px * 30);
        for (int y = horizon; y < height; y++) {
            float t = (y - horizon) / (float) (height - horizon);
            int alpha = (int) (20 + 120 * t);
            float v = 1f / (t + 0.06f) - time * 1.2f;
            float frac = v - (float) Math.floor(v);
            if (frac < 0.07f) {
                ctx.fill(0, y, width, y + 1, (alpha << 24) | 0x5B45E0);
            }
            int col = (alpha << 24) | 0x5B45E0;
            for (int i = -14; i <= 14; i++) {
                int x = cx + (int) (i * t * width * 0.09f);
                if (x >= 0 && x < width) ctx.fill(x, y, x + 1, y + 1, col);
            }
        }
        ctx.fill(0, horizon, width, horizon + 1, 0x8035D0FF);
    }

    private int panelHeight(Panel p) {
        if (p.category == null) return HEADER_H + 6 + 3 * 22 + 2 * 20 + 6 + 20 + 6;
        int h = HEADER_H;
        for (Module m : ModuleManager.INSTANCE.byCategory(p.category)) {
            h += ROW_H;
            if (EXPANDED.contains(m)) h += SET_H * m.settings.size();
        }
        return h + 4;
    }

    private void drawPanel(DrawContext ctx, Panel p, int mouseX, int mouseY, float px, float py) {
        int ox = (int) (-px * p.depth * 8);
        int oy = (int) (-py * p.depth * 5);
        int x = p.x + ox;
        int y = p.y + oy;
        int w = p.w;
        int h = panelHeight(p);

        // soft layered shadow, offset opposite to the parallax shift => feels like height
        int sx = -ox / 2 + 3;
        int sy = -oy / 2 + 5;
        for (int i = 4; i >= 1; i--) {
            ctx.fill(x + sx - i, y + sy - i, x + w + sx + i, y + h + sy + i, (0x16 << 24));
        }
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A2A44);
        ctx.fill(x, y, x + w, y + h, BG);

        // header
        ctx.fillGradient(x, y, x + w, y + HEADER_H - 2, ACCENT_A, 0xFF4B35C9);
        ctx.fill(x, y + HEADER_H - 2, x + w, y + HEADER_H - 1, ACCENT_B);
        ctx.drawText(textRenderer, p.title, x + 6, y + 6, TEXT, true);
        Hit header = new Hit(x, y, x + w, y + HEADER_H);
        header.panel = p;
        hits.add(header);

        if (p.category == null) {
            drawRegion(ctx, x, y, w, mouseX, mouseY);
            return;
        }

        int cy = y + HEADER_H;
        for (Module m : ModuleManager.INSTANCE.byCategory(p.category)) {
            boolean hover = inside(mouseX, mouseY, x, cy, x + w, cy + ROW_H);
            if (m.isEnabled()) {
                ctx.fillGradient(x + 2, cy, x + w - 2, cy + ROW_H, ON_TOP, ON_BOTTOM);
            } else {
                ctx.fill(x + 2, cy, x + w - 2, cy + ROW_H, hover ? ROW_HOVER : ROW);
            }
            ctx.drawText(textRenderer, m.name, x + 8, cy + 4, TEXT, false);
            if (!m.settings.isEmpty()) {
                ctx.drawText(textRenderer, EXPANDED.contains(m) ? "-" : "+", x + w - 12, cy + 4, m.isEnabled() ? TEXT : DIM, false);
            }
            Hit row = new Hit(x + 2, cy, x + w - 2, cy + ROW_H);
            row.left = m::toggle;
            row.right = () -> {
                if (m.settings.isEmpty()) return;
                if (!EXPANDED.remove(m)) EXPANDED.add(m);
            };
            hits.add(row);
            cy += ROW_H;

            if (EXPANDED.contains(m)) {
                for (NumberSetting s : m.settings) {
                    drawSlider(ctx, s, x + 2, cy, w - 4);
                    cy += SET_H;
                }
            }
        }

    }

    private void drawSlider(DrawContext ctx, NumberSetting s, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + SET_H, 0xFF12121C);
        int barX1 = x + 6;
        int barX2 = x + w - 6;
        int fillX = barX1 + (int) ((barX2 - barX1) * s.progress());
        ctx.fill(barX1, y + SET_H - 4, barX2, y + SET_H - 2, 0xFF2B2B44);
        ctx.fillGradient(barX1, y + SET_H - 4, fillX, y + SET_H - 2, ACCENT_A, ACCENT_B);
        ctx.fill(fillX - 1, y + SET_H - 6, fillX + 1, y + SET_H, TEXT);
        ctx.drawText(textRenderer, s.name, barX1, y + 1, DIM, false);
        String val = String.format("%.1f", s.get());
        ctx.drawText(textRenderer, val, barX2 - textRenderer.getWidth(val), y + 1, TEXT, false);
        Hit hit = new Hit(barX1 - 2, y, barX2 + 2, y + SET_H);
        final int x1 = barX1, x2 = barX2;
        hit.slider = mx -> {
            double f = MathHelper.clamp((mx - x1) / (double) (x2 - x1), 0.0, 1.0);
            s.set(s.min + (s.max - s.min) * f);
        };
        hits.add(hit);
    }

    private void drawRegion(DrawContext ctx, int x, int y, int w, int mouseX, int mouseY) {
        AutoMiner m = ModuleManager.INSTANCE.autoMiner;
        int cy = y + HEADER_H + 6;

        for (int c = 0; c < 2; c++) {
            boolean first = c == 0;
            BlockPos pos = first ? m.pos1 : m.pos2;
            ctx.drawText(textRenderer, first ? "Corner 1" : "Corner 2", x + 8, cy, first ? ACCENT_B : ACCENT_A, false);
            ctx.drawText(textRenderer, "L-click +-1   R-click +-10", x + w - 8 - textRenderer.getWidth("L-click +-1   R-click +-10"), cy, DIM, false);
            cy += 11;
            int cellW = (w - 16) / 3;
            for (int axis = 0; axis < 3; axis++) {
                int cx = x + 8 + axis * cellW;
                String val = pos == null ? "-" : String.valueOf(axis == 0 ? pos.getX() : axis == 1 ? pos.getY() : pos.getZ());
                String lab = axis == 0 ? "X" : axis == 1 ? "Y" : "Z";
                stepper(ctx, cx, cy, cellW - 3, lab + " " + val, first, axis, mouseX, mouseY);
            }
            cy += 22;
        }

        int bw = (w - 20) / 2;
        button(ctx, x + 8, cy, bw, 16, "Corner 1 = me", mouseX, mouseY, () -> setHere(true), null, false);
        button(ctx, x + 12 + bw, cy, bw, 16, "Corner 2 = me", mouseX, mouseY, () -> setHere(false), null, false);
        cy += 20;
        button(ctx, x + 8, cy, bw, 18, m.isEnabled() ? "STOP" : "START", mouseX, mouseY, m::toggle, null, m.isEnabled());
        button(ctx, x + 12 + bw, cy, bw, 18, "Clear", mouseX, mouseY, () -> {
            m.setEnabled(false);
            m.pos1 = null;
            m.pos2 = null;
        }, null, false);
        cy += 24;

        String status;
        int color;
        if (!m.hasRegion()) {
            status = "Set both corners";
            color = BAD;
        } else if (m.isEnabled()) {
            status = "Mining... " + m.remaining() + " left";
            color = GOOD;
        } else {
            status = "Region: " + m.regionVolume() + " blocks";
            color = TEXT;
        }
        ctx.drawText(textRenderer, status, x + 8, cy, color, false);
    }

    private void stepper(DrawContext ctx, int x, int y, int w, String label, boolean first, int axis, int mx, int my) {
        ctx.fill(x, y, x + w, y + 16, ROW);
        int bw = 11;
        button(ctx, x, y, bw, 16, "-", mx, my, () -> nudge(first, axis, -1), () -> nudge(first, axis, -10), false);
        button(ctx, x + w - bw, y, bw, 16, "+", mx, my, () -> nudge(first, axis, 1), () -> nudge(first, axis, 10), false);
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, label, x + (w - tw) / 2, y + 4, TEXT, false);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my,
                        Runnable left, Runnable right, boolean active) {
        boolean hover = inside(mx, my, x, y, x + w, y + h);
        if (active) ctx.fillGradient(x, y, x + w, y + h, 0xFFFF5D7A, 0xFFB52B48);
        else ctx.fillGradient(x, y, x + w, y + h, hover ? 0xFF3A3A5E : 0xFF26263F, hover ? 0xFF2C2C4A : 0xFF1C1C30);
        ctx.fill(x, y, x + w, y + 1, hover ? ACCENT_B : 0xFF33334F);
        ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + (h - 8) / 2 + 1, TEXT, false);
        Hit hit = new Hit(x, y, x + w, y + h);
        hit.left = left;
        hit.right = right;
        hits.add(hit);
    }

    // ------------------------------------------------------------------ region logic

    private void nudge(boolean first, int axis, int delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AutoMiner m = ModuleManager.INSTANCE.autoMiner;
        BlockPos cur = first ? m.pos1 : m.pos2;
        if (cur == null && mc.player != null) cur = mc.player.getBlockPos();
        if (cur == null) return;
        BlockPos next = switch (axis) {
            case 0 -> cur.add(delta, 0, 0);
            case 1 -> cur.add(0, delta, 0);
            default -> cur.add(0, 0, delta);
        };
        if (first) m.pos1 = next;
        else m.pos2 = next;
    }

    private void setHere(boolean first) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        BlockPos p = mc.player.getBlockPos().down(); // block you are standing on
        if (first) ModuleManager.INSTANCE.autoMiner.pos1 = p;
        else ModuleManager.INSTANCE.autoMiner.pos2 = p;
    }

    // ------------------------------------------------------------------ input
    // NOTE: these signatures follow the 1.21.9+ input API (Click / KeyInput records).

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my)) continue;
            if (h.panel != null && button == 0) {
                dragging = h.panel;
                // bring to front
                PANELS.remove(h.panel);
                PANELS.add(h.panel);
                return true;
            }
            if (h.slider != null && button == 0) {
                activeSlider = h;
                h.slider.accept(mx);
                return true;
            }
            if (button == 0 && h.left != null) {
                h.left.run();
                return true;
            }
            if (button == 1 && h.right != null) {
                h.right.run();
                return true;
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (activeSlider != null) {
            activeSlider.slider.accept(click.x());
            return true;
        }
        if (dragging != null) {
            dragging.x += (int) Math.round(offsetX);
            dragging.y += (int) Math.round(offsetY);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        activeSlider = null;
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    private static class Panel {
        final String title;
        final Module.Category category;
        int x, y, w;
        final float depth;

        Panel(String title, Module.Category category, int x, int y, int w, float depth) {
            this.title = title;
            this.category = category;
            this.x = x;
            this.y = y;
            this.w = w;
            this.depth = depth;
        }
    }

    private static class Hit {
        final int x1, y1, x2, y2;
        Runnable left, right;
        DoubleConsumer slider;
        Panel panel;

        Hit(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        boolean contains(double mx, double my) {
            return inside(mx, my, x1, y1, x2, y2);
        }
    }
}
