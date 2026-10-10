package dk.wellfeed.gui;

import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import dk.wellfeed.module.impl.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class HudOverlay {

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        Hud hud = ModuleManager.get(Hud.class);
        if (hud == null || !hud.isEnabled()) return;

        TextRenderer tr = mc.textRenderer;

        drawWatermark(context, tr, mc);
        drawModuleList(context, tr, hud);
    }

    private static void drawWatermark(DrawContext context, TextRenderer tr, MinecraftClient mc) {
        String name = "Wellfeed";
        String rest = " Client";
        int w = tr.getWidth(name + rest) + 12;

        context.fill(4, 4, 4 + w, 18, Theme.HUD_BG);
        context.fill(4, 4, 4 + w, 6, Theme.accent(0));
        context.drawTextWithShadow(tr, name, 10, 8, Theme.accent(0));
        context.drawTextWithShadow(tr, rest, 10 + tr.getWidth(name), 8, Theme.TEXT);

        String info = "FPS " + mc.getCurrentFps()
                + "   XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        int iw = tr.getWidth(info) + 10;
        context.fill(4, 20, 4 + iw, 32, Theme.HUD_BG);
        context.drawTextWithShadow(tr, info, 9, 22, Theme.TEXT_DIM);
    }

    private static int rowWidth(TextRenderer tr, Module m) {
        int w = tr.getWidth(m.getName());
        if (m.getSetting() != null) w += tr.getWidth(" " + m.getSetting().display());
        return w;
    }

    private static void drawModuleList(DrawContext context, TextRenderer tr, Hud hud) {
        List<Module> active = new ArrayList<>();
        for (Module m : ModuleManager.getModules()) {
            if (m.isEnabled() && m != hud) active.add(m);
        }
        // Bredeste række øverst
        active.sort((a, b) -> rowWidth(tr, b) - rowWidth(tr, a));

        int screenW = context.getScaledWindowWidth();
        int y = 4;
        int i = 0;
        for (Module m : active) {
            int w = rowWidth(tr, m);
            int x = screenW - w - 9;
            int color = Theme.accent(i * 0.5);

            context.fill(x - 3, y, screenW, y + 12, Theme.HUD_BG);
            context.fill(screenW - 2, y, screenW, y + 12, color);
            context.drawTextWithShadow(tr, m.getName(), x, y + 2, color);
            if (m.getSetting() != null) {
                context.drawTextWithShadow(tr, " " + m.getSetting().display(),
                        x + tr.getWidth(m.getName()), y + 2, Theme.TEXT_DIM);
            }

            y += 12;
            i++;
        }
    }
}
