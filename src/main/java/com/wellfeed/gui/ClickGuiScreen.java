package com.wellfeed.gui;

import com.wellfeed.Config;
import com.wellfeed.WellfeedClient;
import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.module.ModuleManager;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.setting.ModeSetting;
import com.wellfeed.setting.MultiSetting;
import com.wellfeed.setting.NumSetting;
import com.wellfeed.setting.Setting;
import com.wellfeed.util.KeyNames;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Wellfeed click GUI. Mouse and keyboard are polled from GLFW inside render(),
 * which keeps the screen independent of the input-method signatures that change
 * between Minecraft versions.
 */
public class ClickGuiScreen extends Screen {
    private enum Kind { PANEL, HEADER, MODULE, KEY, BOOL, NUM, MODE, MULTI, SWATCH, SEARCH }

    private record Item(Kind kind, int x, int y, int w, int h,
                        Category cat, Module mod, Setting set, String opt, int idx) {
        boolean hit(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final int PW = 124;
    private static final int HH = 18;
    private static final int MH = 15;
    private static final int SH = 13;
    private static final int GAP = 10;

    // Kept between openings so panels stay where you put them.
    private static final Map<Category, int[]> POS = new EnumMap<>(Category.class);
    private static final List<Category> ORDER = new ArrayList<>(List.of(Category.values()));
    private static final Set<Module> EXPANDED = new HashSet<>();

    private final ModuleManager mm;

    private final boolean[] kNow = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] kPrev = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private boolean lNow;
    private boolean lPrev;
    private boolean rNow;
    private boolean rPrev;
    private boolean first = true;

    private Module binding;
    private Item slider;
    private Category dragCat;
    private int dragOX;
    private int dragOY;
    private int dragSX;
    private int dragSY;
    private boolean dragMoved;
    private String search = "";
    private boolean searchFocus;

    public ClickGuiScreen(ModuleManager mm) {
        super(Text.literal("Wellfeed"));
        this.mm = mm;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return binding == null && !searchFocus;
    }

    @Override
    public void close() {
        Config.save(mm);
        WellfeedClient.markClosed();
        super.close();
    }

    // ------------------------------------------------------------------ input polling

    private void poll() {
        long h = client.getWindow().getHandle();
        System.arraycopy(kNow, 0, kPrev, 0, kNow.length);
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            kNow[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
        }
        lPrev = lNow;
        rPrev = rNow;
        lNow = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        rNow = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (first) {
            // The key that opened the menu is still down; do not count it as a new press.
            System.arraycopy(kNow, 0, kPrev, 0, kNow.length);
            lPrev = lNow;
            rPrev = rNow;
        }
    }

    private boolean pressed(int key) {
        return kNow[key] && !kPrev[key];
    }

    private void handleKeys() {
        int menuCode = KeyBindingHelper.getBoundKeyOf(WellfeedClient.menuKey).getCode();

        if (binding != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (!pressed(k)) {
                    continue;
                }
                if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                    binding.key = -1;
                } else if (k != GLFW.GLFW_KEY_ESCAPE && k != menuCode) {
                    binding.key = k;
                }
                binding = null;
                Config.save(mm);
                return;
            }
            return;
        }

        if (searchFocus) {
            if (pressed(GLFW.GLFW_KEY_ESCAPE) || pressed(GLFW.GLFW_KEY_ENTER)) {
                searchFocus = false;
            } else if (pressed(GLFW.GLFW_KEY_BACKSPACE) && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
            }
            for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
                if (pressed(k) && search.length() < 16) {
                    search += (char) ('a' + (k - GLFW.GLFW_KEY_A));
                }
            }
            for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
                if (pressed(k) && search.length() < 16) {
                    search += (char) k;
                }
            }
            if (pressed(GLFW.GLFW_KEY_SPACE) && search.length() < 16) {
                search += ' ';
            }
            return;
        }

        if (pressed(menuCode)) {
            close();
        }
    }

    // ------------------------------------------------------------------ layout

    private List<Item> layout() {
        List<Item> out = new ArrayList<>();

        out.add(new Item(Kind.SEARCH, width - 146, 8, 132, 16, null, null, null, null, 0));
        int sx = 14 + textRenderer.getWidth("WELLFEED") + 14;
        for (int i = 0; i < Theme.NAMES.length; i++) {
            out.add(new Item(Kind.SWATCH, sx + i * 14, 11, 10, 10, null, null, null, null, i));
        }

        String q = search.toLowerCase(Locale.ROOT);
        for (Category c : ORDER) {
            List<Module> list = new ArrayList<>();
            for (Module m : mm.in(c)) {
                if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q)) {
                    list.add(m);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            int[] pos = POS.computeIfAbsent(c, k -> new int[]{14 + k.ordinal() * (PW + GAP), 40, 0});
            pos[0] = MathHelper.clamp(pos[0], 0, Math.max(0, width - PW));
            pos[1] = MathHelper.clamp(pos[1], 0, Math.max(0, height - HH));
            int x = pos[0];
            int y = pos[1];

            List<Item> body = new ArrayList<>();
            body.add(new Item(Kind.HEADER, x, y, PW, HH, c, null, null, null, 0));
            int cy = y + HH;
            if (pos[2] == 0) {
                for (Module m : list) {
                    body.add(new Item(Kind.MODULE, x, cy, PW, MH, c, m, null, null, 0));
                    cy += MH;
                    if (EXPANDED.contains(m)) {
                        body.add(new Item(Kind.KEY, x, cy, PW, SH, c, m, null, null, 0));
                        cy += SH;
                        for (Setting s : m.settings) {
                            if (s instanceof BoolSetting) {
                                body.add(new Item(Kind.BOOL, x, cy, PW, SH, c, m, s, null, 0));
                                cy += SH;
                            } else if (s instanceof NumSetting) {
                                body.add(new Item(Kind.NUM, x, cy, PW, SH, c, m, s, null, 0));
                                cy += SH;
                            } else if (s instanceof ModeSetting) {
                                body.add(new Item(Kind.MODE, x, cy, PW, SH, c, m, s, null, 0));
                                cy += SH;
                            } else if (s instanceof MultiSetting ms) {
                                for (String o : ms.options) {
                                    body.add(new Item(Kind.MULTI, x, cy, PW, SH, c, m, s, o, 0));
                                    cy += SH;
                                }
                            }
                        }
                    }
                }
                cy += 3;
            }
            out.add(new Item(Kind.PANEL, x, y, PW, cy - y, c, null, null, null, 0));
            out.addAll(body);
        }
        return out;
    }

    // ------------------------------------------------------------------ input actions

    private void setSlider(Item it, int mx) {
        NumSetting ns = (NumSetting) it.set;
        double f = (mx - (it.x + 3)) / (double) (it.w - 6);
        ns.set(ns.min + MathHelper.clamp(f, 0.0, 1.0) * (ns.max - ns.min));
    }

    private void input(List<Item> items, int mx, int my) {
        boolean lc = lNow && !lPrev;
        boolean rc = rNow && !rPrev;

        if (slider != null) {
            if (lNow) {
                setSlider(slider, mx);
            } else {
                slider = null;
                Config.save(mm);
            }
        }
        if (dragCat != null) {
            int[] p = POS.get(dragCat);
            if (lNow) {
                p[0] = mx - dragOX;
                p[1] = my - dragOY;
                if (Math.abs(mx - dragSX) + Math.abs(my - dragSY) > 4) {
                    dragMoved = true;
                }
            } else {
                if (!dragMoved) {
                    p[2] = 1 - p[2];
                }
                dragCat = null;
                Config.save(mm);
            }
        }
        if (!lc && !rc) {
            return;
        }
        if (dragCat != null || slider != null) {
            return;
        }

        Item hit = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).hit(mx, my)) {
                hit = items.get(i);
                break;
            }
        }
        if (lc && (hit == null || hit.kind != Kind.SEARCH)) {
            searchFocus = false;
        }
        if (hit == null || hit.kind == Kind.PANEL) {
            return;
        }

        switch (hit.kind) {
            case SEARCH -> {
                if (lc) {
                    searchFocus = true;
                }
            }
            case SWATCH -> {
                if (lc) {
                    Config.accent = hit.idx;
                    Config.save(mm);
                }
            }
            case HEADER -> {
                if (lc) {
                    int[] p = POS.get(hit.cat);
                    dragCat = hit.cat;
                    dragOX = mx - p[0];
                    dragOY = my - p[1];
                    dragSX = mx;
                    dragSY = my;
                    dragMoved = false;
                    ORDER.remove(hit.cat);
                    ORDER.add(hit.cat);
                }
            }
            case MODULE -> {
                if (lc) {
                    hit.mod.toggle();
                    Config.save(mm);
                } else if (rc) {
                    if (!EXPANDED.remove(hit.mod)) {
                        EXPANDED.add(hit.mod);
                    }
                }
            }
            case KEY -> {
                if (lc) {
                    binding = hit.mod;
                } else if (rc) {
                    hit.mod.key = -1;
                    Config.save(mm);
                }
            }
            case BOOL -> {
                if (lc) {
                    BoolSetting b = (BoolSetting) hit.set;
                    b.value = !b.value;
                    Config.save(mm);
                }
            }
            case NUM -> {
                if (lc) {
                    slider = hit;
                    setSlider(hit, mx);
                }
            }
            case MODE -> {
                ModeSetting ms = (ModeSetting) hit.set;
                if (lc) {
                    ms.next();
                } else {
                    ms.prev();
                }
                Config.save(mm);
            }
            case MULTI -> {
                if (lc) {
                    ((MultiSetting) hit.set).toggle(hit.opt);
                    Config.save(mm);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, s, x, y, color);
    }

    private void textRight(DrawContext ctx, String s, int right, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, s, right - textRenderer.getWidth(s), y, color);
    }

    private void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        poll();
        if (!first) {
            handleKeys();
        }
        if (!first) {
            input(layout(), mx, my);
        }
        first = false;

        List<Item> items = layout();
        Item top = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).hit(mx, my)) {
                top = items.get(i);
                break;
            }
        }

        ctx.fillGradient(0, 0, width, height, 0xB0090614, 0xD0120A24);
        text(ctx, "WELLFEED", 14, 12, Theme.accent(0));

        String hoverText = null;
        for (Item it : items) {
            boolean hov = it == top;
            switch (it.kind) {
                case PANEL -> {
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, 0xD4100B1C);
                }
                case HEADER -> {
                    int ac = Theme.accent(it.cat.ordinal() * 2);
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0xF01C1330 : 0xF0160F28);
                    ctx.fill(it.x, it.y + it.h - 1, it.x + it.w, it.y + it.h, ac);
                    text(ctx, it.cat.label, it.x + 7, it.y + 5, ac);
                    int on = 0;
                    List<Module> all = mm.in(it.cat);
                    for (Module m : all) {
                        if (m.enabled) {
                            on++;
                        }
                    }
                    textRight(ctx, on + "/" + all.size(), it.x + it.w - 6, it.y + 5, 0xFF8F89A9);
                }
                case MODULE -> {
                    Module m = it.mod;
                    int ac = Theme.accent(it.y / 12);
                    if (hov) {
                        ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, 0x22FFFFFF);
                        hoverText = m.name + " - " + m.description;
                    }
                    if (m.enabled) {
                        ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, Theme.alpha(ac, 0x34));
                        ctx.fill(it.x, it.y, it.x + 2, it.y + it.h, ac);
                    }
                    text(ctx, m.name, it.x + 7, it.y + 4, m.enabled ? 0xFFFFFFFF : 0xFFC9C5DA);
                    String marker = EXPANDED.contains(m) ? "-" : "+";
                    textRight(ctx, marker, it.x + it.w - 6, it.y + 4, 0xFF6F6A88);
                    if (m.key >= 0) {
                        textRight(ctx, "[" + KeyNames.of(m.key) + "]", it.x + it.w - 16, it.y + 4, 0xFF8F89A9);
                    }
                }
                case KEY -> {
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0x30FFFFFF : 0x18FFFFFF);
                    text(ctx, "Keybind", it.x + 10, it.y + 3, 0xFFCFCAE3);
                    boolean waiting = binding == it.mod;
                    String v = waiting ? "press a key..." : KeyNames.of(it.mod.key);
                    textRight(ctx, v, it.x + it.w - 6, it.y + 3, waiting ? Theme.accent(0) : 0xFF8F89A9);
                }
                case BOOL -> {
                    BoolSetting b = (BoolSetting) it.set;
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0x30FFFFFF : 0x18FFFFFF);
                    text(ctx, b.name, it.x + 10, it.y + 3, 0xFFCFCAE3);
                    int bx = it.x + it.w - 20;
                    ctx.fill(bx, it.y + 3, bx + 14, it.y + 10, b.value ? Theme.accent(1) : 0xFF3A3552);
                    ctx.fill(b.value ? bx + 8 : bx + 1, it.y + 4, b.value ? bx + 13 : bx + 6, it.y + 9, 0xFFFFFFFF);
                }
                case NUM -> {
                    NumSetting n = (NumSetting) it.set;
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0x30FFFFFF : 0x18FFFFFF);
                    double f = (n.value - n.min) / (n.max - n.min);
                    ctx.fill(it.x + 3, it.y + 1, it.x + 3 + (int) ((it.w - 6) * f), it.y + it.h - 1,
                            Theme.alpha(Theme.accent(2), 0x70));
                    text(ctx, n.name, it.x + 10, it.y + 3, 0xFFEFECFA);
                    textRight(ctx, n.display(), it.x + it.w - 6, it.y + 3, 0xFFFFFFFF);
                }
                case MODE -> {
                    ModeSetting ms = (ModeSetting) it.set;
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0x30FFFFFF : 0x18FFFFFF);
                    text(ctx, ms.name, it.x + 10, it.y + 3, 0xFFCFCAE3);
                    textRight(ctx, ms.get(), it.x + it.w - 6, it.y + 3, Theme.accent(1));
                }
                case MULTI -> {
                    MultiSetting ms = (MultiSetting) it.set;
                    boolean on = ms.has(it.opt);
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, hov ? 0x30FFFFFF : 0x18FFFFFF);
                    ctx.fill(it.x + 10, it.y + 4, it.x + 16, it.y + 10, on ? Theme.accent(1) : 0xFF3A3552);
                    text(ctx, it.opt, it.x + 21, it.y + 3, on ? 0xFFFFFFFF : 0xFF9C97B4);
                }
                case SWATCH -> {
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, Theme.swatch(it.idx));
                    if (Config.accent == it.idx) {
                        border(ctx, it.x - 2, it.y - 2, it.w + 4, it.h + 4, 0xFFFFFFFF);
                    } else if (hov) {
                        border(ctx, it.x - 1, it.y - 1, it.w + 2, it.h + 2, 0x88FFFFFF);
                    }
                    if (hov) {
                        hoverText = "Accent: " + Theme.NAMES[it.idx];
                    }
                }
                case SEARCH -> {
                    ctx.fill(it.x, it.y, it.x + it.w, it.y + it.h, 0xD0100B1C);
                    border(ctx, it.x, it.y, it.w, it.h, searchFocus ? Theme.accent(0) : 0xFF2B2542);
                    boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
                    if (search.isEmpty() && !searchFocus) {
                        text(ctx, "Search modules...", it.x + 6, it.y + 4, 0xFF6F6A88);
                    } else {
                        text(ctx, search + (searchFocus && blink ? "_" : ""), it.x + 6, it.y + 4, 0xFFFFFFFF);
                    }
                }
            }
        }

        String hint = hoverText != null ? hoverText
                : "Click: toggle   Right-click: settings   Drag header: move   R-Shift: close";
        text(ctx, hint, 14, height - 14, 0xFF8F89A9);
    }
}
