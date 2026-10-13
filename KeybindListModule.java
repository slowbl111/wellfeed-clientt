package dev.wellfeed.modules;

import dev.wellfeed.WellfeedClient;
import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * On-screen keybind list: the menu key plus every module that has a key, showing the key you
 * currently have it bound to (rebinding in Options > Controls updates it live) and whether
 * the module is on.
 */
public final class KeybindListModule extends Module {
    public static final KeybindListModule INSTANCE = new KeybindListModule();

    private final Setting onlyActive = add(Setting.bool("Only enabled", false));

    private static final int ROW_H = 12;

    private KeybindListModule() {
        super("Keybinds", "Shows your module keybinds on screen.",
                Category.HUD, true, false, true, false, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        TextRenderer font = mc.textRenderer;

        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> ons = new ArrayList<>();

        KeyBinding gui = WellfeedClient.guiKey();
        if (gui != null && !gui.isUnbound() && !onlyActive.getBool()) {
            keys.add(gui.getBoundKeyLocalizedText().getString());
            labels.add("Menu");
            ons.add(false);
        }
        for (Module m : ModuleManager.MODULES) {
            if (m.key == null || m.key.isUnbound()) continue;
            if (onlyActive.getBool() && !m.isEnabled()) continue;
            keys.add(m.key.getBoundKeyLocalizedText().getString());
            labels.add(m.name);
            ons.add(m.isEnabled());
        }
        if (labels.isEmpty()) return;

        int keyW = 0, nameW = 0;
        for (int i = 0; i < labels.size(); i++) {
            keyW = Math.max(keyW, font.getWidth(keys.get(i)));
            nameW = Math.max(nameW, font.getWidth(labels.get(i)));
        }
        int capW = keyW + 8;
        int w = capW + nameW + 17;
        int h = labels.size() * ROW_H + 6;
        int x = ctx.getScaledWindowWidth() - w - 8;
        int y = 8;

        Render.shadow(ctx, x, y, w, h, 3, 1f);
        Render.roundRect(ctx, x, y, w, h, 3, Render.PANEL);

        for (int i = 0; i < labels.size(); i++) {
            int ry = y + 3 + i * ROW_H;
            boolean on = ons.get(i);

            Render.roundRect(ctx, x + 5, ry, capW, ROW_H - 2, 2, on ? Render.CREAM : 0xFF3A3028);
            Render.textCentered(ctx, font, keys.get(i), x + 5 + capW / 2, ry + 1,
                    on ? Render.ROAST : Render.CREAM_SOFT, false);
            Render.text(ctx, font, labels.get(i), x + 5 + capW + 6, ry + 1,
                    on ? Render.CREAM : Render.CREAM_DIM, false);
        }
    }
}
