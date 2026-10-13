package dev.wellfeed.gui;

import dev.wellfeed.WellfeedClient;
import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Config;
import dev.wellfeed.util.Render;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wellfeed click GUI: one draggable panel per category.
 *
 *  - Left click a module: toggle it.  Right click: expand its settings.
 *  - Toggles: click.  Sliders: click or drag.  Choice lists: click (right click goes back).
 *  - Drag a panel by its header; right click the header to collapse it.
 *  - Right Shift (or whatever the menu key is bound to) or Esc closes.
 *
 * Mouse buttons are polled from GLFW inside render(), which keeps this screen independent of
 * the mouse-event signatures that changed between recent Minecraft versions.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PW = 138, HEAD = 22, ROW = 18, GAP = 1;
    private static final int H_BOOL = 15, H_MODE = 15, H_NUM = 25;

    private final List<Category> order = new ArrayList<>();
    private final Map<Category, Boolean> collapsed = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Module, Float> onAnim = new HashMap<>();
    private final Map<Module, Float> openAnim = new HashMap<>();
    private final Map<Module, Boolean> open = new HashMap<>();
    private final Map<Setting, Float> boolAnim = new HashMap<>();

    private boolean prevL, prevR, prevKey;
    private long lastNano;
    private float fade;

    private Category dragCat;
    private int dragDx, dragDy;
    private Setting dragSlider;
    private int sliderX, sliderW;

    public ClickGuiScreen() {
        super(Text.literal("Wellfeed"));
        for (Category c : Category.values()) {
            if (!ModuleManager.in(c).isEmpty()) order.add(c);
        }
    }

    @Override
    protected void init() {
        long h = client.getWindow().getHandle();
        prevL = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevKey = menuKeyDown();
        lastNano = 0;

        // Default layout: panels side by side, each remembered once moved.
        int x = 24;
        for (Category c : order) {
            if (!Config.PANELS.containsKey(c.label)) Config.PANELS.put(c.label, new int[]{x, 44});
            x += PW + 12;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    private boolean menuKeyDown() {
        KeyBinding kb = WellfeedClient.guiKey();
        if (kb == null) return false;
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(kb);
        if (key.getCategory() != InputUtil.Type.KEYSYM || key.getCode() == GLFW.GLFW_KEY_UNKNOWN) return false;
        return InputUtil.isKeyPressed(client.getWindow(), key.getCode());
    }

    // ---------------------------------------------------------------- layout

    private static int settingH(Setting s) {
        return switch (s.type) {
            case NUM -> H_NUM;
            case MODE -> H_MODE;
            default -> H_BOOL;
        };
    }

    private static int settingsTotal(Module m) {
        int t = 0;
        for (Setting s : m.settings) t += settingH(s) + 1;
        return t + (m.settings.isEmpty() ? 0 : 3);
    }

    private static float ease(float p) {
        return p * p * (3f - 2f * p);
    }

    private int panelHeight(Category c) {
        if (collapsed.getOrDefault(c, false)) return HEAD;
        int h = HEAD + 4;
        for (Module m : ModuleManager.in(c)) {
            h += ROW + GAP;
            h += Math.round(settingsTotal(m) * ease(openAnim.getOrDefault(m, 0f)));
        }
        return h + 2;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (now - lastNano) / 1_000_000_000f);
        lastNano = now;
        fade = Render.approach(fade, 1f, dt, 14f);
        if (fade > 0.995f) fade = 1f;

        // ---- input (polled)
        long handle = client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean keyNow = menuKeyDown();
        boolean clickL = l && !prevL;
        boolean clickR = r && !prevR;
        boolean pressKey = keyNow && !prevKey;
        prevL = l;
        prevR = r;
        prevKey = keyNow;

        if (pressKey) {
            close();
            return;
        }
        if (!l) {
            dragCat = null;
            dragSlider = null;
        }

        TextRenderer font = client.textRenderer;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        // ---- title pill
        String title = "Wellfeed";
        String ver = " v" + WellfeedClient.VERSION;
        String hint = "LMB toggle   RMB settings   drag headers   Esc close";
        int tw = font.getWidth(title) + font.getWidth(ver) + 20;
        int tx = sw / 2 - tw / 2;
        Render.shadow(ctx, tx, 10, tw, 20, 4, fade);
        Render.roundRect(ctx, tx, 10, tw, 20, 4, Render.alpha(Render.PANEL, fade));
        ctx.fill(tx + 6, 15, tx + 8, 25, Render.alpha(Render.CREAM, fade));
        Render.text(ctx, font, title, tx + 13, 16, Render.alpha(Render.CREAM, fade), true);
        Render.text(ctx, font, ver, tx + 13 + font.getWidth(title), 16, Render.alpha(Render.CREAM_DIM, fade), false);
        Render.textCentered(ctx, font, hint, sw / 2, 34, Render.alpha(Render.CREAM_DIM, fade), true);

        // ---- which panel is under the cursor (topmost wins)
        Category hoverCat = null;
        for (Category c : order) {
            int[] pos = Config.PANELS.get(c.label);
            if (Render.inside(mouseX, mouseY, pos[0], pos[1], PW, panelHeight(c))) hoverCat = c;
        }

        // ---- drag panel
        if (dragCat != null && l) {
            int[] pos = Config.PANELS.get(dragCat.label);
            pos[0] = Math.max(0, Math.min(sw - PW, mouseX - dragDx));
            pos[1] = Math.max(0, Math.min(sh - HEAD, mouseY - dragDy));
        }

        String tooltip = null;
        int tipX = 0, tipY = 0;

        // ---- panels, back to front
        for (Category c : new ArrayList<>(order)) {
            int[] pos = Config.PANELS.get(c.label);
            int px = pos[0], py = pos[1];
            int ph = panelHeight(c);
            boolean isHover = c == hoverCat;

            Render.shadow(ctx, px, py, PW, ph, 4, fade);
            Render.roundRect(ctx, px, py, PW, ph, 4, Render.alpha(Render.PANEL, fade));

            // header
            boolean coll = collapsed.getOrDefault(c, false);
            Render.roundRect(ctx, px, py, PW, HEAD, 4, Render.alpha(0xFF2B221B, fade));
            if (!coll) ctx.fill(px, py + HEAD - 1, px + PW, py + HEAD, Render.alpha(Render.CREAM, fade * 0.8f));
            Render.text(ctx, font, c.label, px + 9, py + 7, Render.alpha(Render.CREAM, fade), true);
            int enabled = 0;
            List<Module> mods = ModuleManager.in(c);
            for (Module m : mods) if (m.isEnabled()) enabled++;
            Render.textRight(ctx, font, enabled + "/" + mods.size(), px + PW - 9, py + 7,
                    Render.alpha(Render.CREAM_DIM, fade), false);

            boolean onHeader = isHover && Render.inside(mouseX, mouseY, px, py, PW, HEAD);
            if (onHeader && clickL && dragCat == null && dragSlider == null) {
                dragCat = c;
                dragDx = mouseX - px;
                dragDy = mouseY - py;
                order.remove(c);   // bring to front
                order.add(c);
            }
            if (onHeader && clickR) collapsed.put(c, !coll);
            if (coll) continue;

            int cy = py + HEAD + 3;
            for (Module m : mods) {
                boolean hover = isHover && dragCat == null && Render.inside(mouseX, mouseY, px + 3, cy, PW - 6, ROW);
                boolean on = m.isEnabled();

                float ha = Render.approach(hoverAnim.getOrDefault(m, 0f), hover ? 1f : 0f, dt, 18f);
                float oa = Render.approach(onAnim.getOrDefault(m, on ? 1f : 0f), on ? 1f : 0f, dt, 16f);
                hoverAnim.put(m, ha);
                onAnim.put(m, oa);

                int rowBg = Render.lerpColor(0xA0332A22, 0xE0463A2F, ha);
                rowBg = Render.lerpColor(rowBg, 0xF0F5E6C8, oa * 0.92f);
                Render.roundRect(ctx, px + 3, cy, PW - 6, ROW, 3, Render.alpha(rowBg, fade));

                int nameColor = Render.lerpColor(Render.CREAM, Render.ROAST, oa);
                Render.text(ctx, font, m.name, px + 10, cy + 5, Render.alpha(nameColor, fade), false);
                int nameEnd = px + 10 + font.getWidth(m.name);
                if (!m.settings.isEmpty()) {
                    boolean isOpen = open.getOrDefault(m, false);
                    Render.text(ctx, font, isOpen ? "-" : "+", nameEnd + 4, cy + 5,
                            Render.alpha(Render.lerpColor(Render.CREAM_DIM, 0xFF7A6A55, oa), fade), false);
                }

                // toggle pill
                int pillX = px + PW - 6 - 22, pillY = cy + 5;
                pill(ctx, pillX, pillY, oa, fade, true);

                // bound key
                if (m.key != null && !m.key.isUnbound()) {
                    String k = m.key.getBoundKeyLocalizedText().getString();
                    Render.textRight(ctx, font, k, pillX - 5, cy + 5,
                            Render.alpha(Render.lerpColor(Render.CREAM_DIM, 0xFF7A6A55, oa), fade), false);
                }

                if (hover) {
                    tooltip = m.description;
                    tipX = mouseX + 10;
                    tipY = mouseY + 12;
                    if (clickL) {
                        if (m.holdToUse) {
                            // hold modules are driven by their key; clicking just shows the settings
                            open.put(m, !open.getOrDefault(m, false));
                        } else {
                            m.toggle();
                        }
                    }
                    if (clickR && !m.settings.isEmpty()) open.put(m, !open.getOrDefault(m, false));
                }
                cy += ROW + GAP;

                // ---- animated settings area
                float p = Render.approach(openAnim.getOrDefault(m, 0f), open.getOrDefault(m, false) ? 1f : 0f, dt, 14f);
                if (Math.abs(p - (open.getOrDefault(m, false) ? 1f : 0f)) < 0.004f) p = open.getOrDefault(m, false) ? 1f : 0f;
                openAnim.put(m, p);
                int areaH = Math.round(settingsTotal(m) * ease(p));
                if (areaH > 0) {
                    ctx.enableScissor(px, cy, px + PW, cy + areaH);
                    int sy = cy + 1;
                    for (Setting s : m.settings) {
                        int sh2 = settingH(s);
                        drawSetting(ctx, font, m, s, px + 8, sy, PW - 16, sh2,
                                isHover && dragCat == null && areaH >= settingsTotal(m) - 1, mouseX, mouseY, clickL, clickR, l, dt);
                        sy += sh2 + 1;
                    }
                    ctx.disableScissor();
                    cy += areaH;
                }
            }
        }

        if (tooltip != null) {
            int w = font.getWidth(tooltip) + 10;
            int x = Math.min(tipX, sw - w - 4);
            int y = Math.min(tipY, sh - 18);
            Render.shadow(ctx, x, y, w, 14, 3, 1f);
            Render.roundRect(ctx, x, y, w, 14, 3, 0xF014100C);
            Render.text(ctx, font, tooltip, x + 5, y + 3, Render.CREAM, false);
        }
    }

    private void drawSetting(DrawContext ctx, TextRenderer font, Module m, Setting s, int x, int y, int w, int h,
                             boolean interactive, int mx, int my, boolean clickL, boolean clickR, boolean heldL, float dt) {
        boolean hover = interactive && Render.inside(mx, my, x, y, w, h);
        Render.roundRect(ctx, x, y, w, h, 2, Render.alpha(hover ? 0xC02E251E : 0x90221A14, fade));
        ctx.fill(x + 1, y + 2, x + 2, y + h - 2, Render.alpha(Render.CREAM_DIM, fade * 0.7f));

        switch (s.type) {
            case BOOL -> {
                Render.text(ctx, font, s.name, x + 6, y + 4, Render.alpha(Render.CREAM, fade), false);
                float a = Render.approach(boolAnim.getOrDefault(s, s.getBool() ? 1f : 0f), s.getBool() ? 1f : 0f, dt, 16f);
                boolAnim.put(s, a);
                pill(ctx, x + w - 22, y + 3, a, fade, false);
                if (hover && clickL) s.toggle();
            }
            case MODE -> {
                Render.text(ctx, font, s.name, x + 6, y + 4, Render.alpha(Render.CREAM, fade), false);
                String v = s.getModeName();
                Render.textRight(ctx, font, v, x + w - 6, y + 4, Render.alpha(Render.CREAM_SOFT, fade), false);
                if (hover && clickL) s.cycle(1);
                if (hover && clickR) s.cycle(-1);
            }
            case NUM -> {
                Render.text(ctx, font, s.name, x + 6, y + 3, Render.alpha(Render.CREAM, fade), false);
                String v = fmt(s);
                Render.textRight(ctx, font, v, x + w - 6, y + 3, Render.alpha(Render.CREAM_SOFT, fade), false);

                int trackX = x + 6, trackW = w - 12, trackY = y + 16;
                ctx.fill(trackX, trackY, trackX + trackW, trackY + 3, Render.alpha(0xFF14100C, fade));
                int fillW = (int) Math.round(trackW * s.fraction());
                ctx.fill(trackX, trackY, trackX + fillW, trackY + 3, Render.alpha(Render.CREAM, fade));
                ctx.fill(trackX + fillW - 1, trackY - 2, trackX + fillW + 2, trackY + 5, Render.alpha(0xFFFFF4DC, fade));

                if (hover && clickL && dragSlider == null && dragCat == null) {
                    dragSlider = s;
                    sliderX = trackX;
                    sliderW = trackW;
                }
                if (dragSlider == s && heldL) {
                    s.setFraction((mx - sliderX) / (double) sliderW);
                }
            }
        }
    }

    /** Animated on/off pill. big = module toggle (22x9), otherwise a small setting toggle. */
    private static void pill(DrawContext ctx, int x, int y, float a, float fade, boolean big) {
        int w = big ? 22 : 18, h = big ? 9 : 9;
        int track = Render.lerpColor(0xFF4A3D32, big ? 0xFF8A7556 : Render.CREAM, a);
        Render.roundRect(ctx, x, y, w, h, 4, Render.alpha(track, fade));
        int knob = 7;
        int kx = x + 1 + Math.round((w - knob - 2) * a);
        Render.roundRect(ctx, kx, y + 1, knob, knob, 3,
                Render.alpha(Render.lerpColor(0xFFA89A80, 0xFFFFF4DC, a), fade));
    }

    private static String fmt(Setting s) {
        return s.step() < 1 ? String.format(Locale.ROOT, "%.1f", s.getNum())
                : String.format(Locale.ROOT, "%.0f", s.getNum());
    }
}
