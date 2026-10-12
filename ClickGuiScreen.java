package dev.wellfeed.gui;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Click GUI.
 *  - Left click a module: toggle it.
 *  - Right click a module: expand / collapse its settings.
 *  - Settings: click a toggle; for numbers click the left half to decrease, right half to increase.
 *  - ESC or Right Shift closes.
 *
 * Clicks are read by polling GLFW inside render(), which keeps this screen independent of
 * the mouse-event signatures that changed between recent Minecraft versions.
 */
public final class ClickGuiScreen extends Screen {
    private static final int X = 24, Y = 24, W = 150, HEADER_H = 18, ROW_H = 16, SET_H = 14;

    private Module expanded;
    private boolean prevL, prevR, prevShift;

    public ClickGuiScreen() {
        super(Text.literal("Wellfeed"));
    }

    @Override
    protected void init() {
        long h = client.getWindow().getHandle();
        prevL = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevShift = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        long handle = client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean clickL = l && !prevL;
        boolean clickR = r && !prevR;
        boolean pressShift = shift && !prevShift;
        prevL = l;
        prevR = r;
        prevShift = shift;

        if (pressShift) {
            close();
            return;
        }

        TextRenderer font = client.textRenderer;

        // Header
        Render.roundRect(ctx, X, Y, W, HEADER_H, 0xF0261E17);
        ctx.drawText(font, "Wellfeed", X + 8, Y + 5, Render.CREAM, true);
        ctx.drawText(font, "RMB: settings", X + W - font.getWidth("RMB: settings") - 6, Y + 5,
                Render.CREAM_SOFT, false);

        int cy = Y + HEADER_H + 2;
        String tooltip = null;

        for (Module m : ModuleManager.MODULES) {
            boolean hover = mouseX >= X && mouseX < X + W && mouseY >= cy && mouseY < cy + ROW_H;
            boolean on = m.isEnabled();

            int bg = on ? (hover ? 0xFFF8EDD3 : 0xEEF5E6C8) : (hover ? 0xD0463A2F : Render.PANEL_ROW);
            Render.roundRect(ctx, X, cy, W, ROW_H, bg);
            ctx.drawText(font, m.name, X + 8, cy + 4, on ? Render.ROAST : Render.CREAM, false);
            if (!m.settings.isEmpty()) {
                String arrow = expanded == m ? "v" : ">";
                ctx.drawText(font, arrow, X + W - 12, cy + 4, on ? Render.ROAST : Render.CREAM_SOFT, false);
            }

            if (hover) {
                tooltip = m.description;
                if (clickL) m.toggle();
                if (clickR && !m.settings.isEmpty()) expanded = expanded == m ? null : m;
            }
            cy += ROW_H + 1;

            if (expanded == m) {
                for (Setting s : m.settings) {
                    boolean sh = mouseX >= X && mouseX < X + W && mouseY >= cy && mouseY < cy + SET_H;
                    Render.roundRect(ctx, X + 6, cy, W - 6, SET_H, sh ? 0xC0463A2F : 0xB0221A14);
                    ctx.drawText(font, s.name, X + 12, cy + 3, Render.CREAM, false);

                    String value;
                    int valueColor = Render.CREAM_SOFT;
                    if (s.type == Setting.Type.BOOL) {
                        value = s.getBool() ? "ON" : "OFF";
                        valueColor = s.getBool() ? 0xFF9BD67A : 0xFFE06C6C;
                    } else {
                        value = "< " + fmt(s) + " >";
                    }
                    ctx.drawText(font, value, X + W - font.getWidth(value) - 6, cy + 3, valueColor, false);

                    if (sh && clickL) {
                        if (s.type == Setting.Type.BOOL) {
                            s.toggle();
                        } else {
                            s.nudge(mouseX < X + W / 2 + 10 ? -1 : 1);
                        }
                    }
                    cy += SET_H + 1;
                }
                cy += 2;
            }
        }

        if (tooltip != null) {
            ctx.drawText(font, tooltip, X, cy + 4, Render.CREAM_SOFT, true);
        }
    }

    private static String fmt(Setting s) {
        return s.step() < 1 ? String.format(Locale.ROOT, "%.1f", s.getNum())
                : String.format(Locale.ROOT, "%.0f", s.getNum());
    }
}
