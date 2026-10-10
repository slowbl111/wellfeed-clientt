package dk.wellfeed.gui;

import dk.wellfeed.WellfeedClient;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import dk.wellfeed.module.impl.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class HudOverlay {
    private static final int ACCENT = 0xFF7B5CFF;
    private static final int WHITE = 0xFFFFFFFF;

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        Hud hud = ModuleManager.get(Hud.class);
        if (hud == null || !hud.isEnabled()) return;

        int y = 4;
        context.drawTextWithShadow(mc.textRenderer, WellfeedClient.NAME, 4, y, ACCENT);
        y += 11;

        for (Module m : ModuleManager.getModules()) {
            if (m.isEnabled() && m != hud) {
                context.drawTextWithShadow(mc.textRenderer, m.getName(), 4, y, WHITE);
                y += 10;
            }
        }
    }
}
