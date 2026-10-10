package com.wellfeed.gui;

import com.wellfeed.WellfeedClient;
import com.wellfeed.module.Module;
import com.wellfeed.module.ModuleManager;
import com.wellfeed.module.impl.Info;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static final String[] FACING = {"South", "West", "North", "East"};

    private HudRenderer() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.of(WellfeedClient.ID, "overlay"), HudRenderer::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        ModuleManager mm = WellfeedClient.modules;
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        // Enabled-module list, longest first.
        List<String> lines = new ArrayList<>();
        for (Module m : mm.all()) {
            if (m.enabled) {
                String tag = m.tag();
                lines.add(tag.isEmpty() ? m.name : m.name + " " + tag);
            }
        }
        lines.sort((a, b) -> tr.getWidth(b) - tr.getWidth(a));
        int y = mc.player.getStatusEffects().isEmpty() ? 3 : 30;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int w = tr.getWidth(s);
            int x = sw - w - 7;
            int accent = Theme.accent(i);
            ctx.fill(x - 3, y - 1, sw - 3, y + 10, 0x78000000);
            ctx.fill(sw - 3, y - 1, sw - 1, y + 10, accent);
            ctx.drawTextWithShadow(tr, s, x, y + 1, accent);
            y += 11;
        }

        // Info overlay (top left).
        Info info = mm.get(Info.class);
        if (info != null && info.enabled) {
            int iy = 3;
            if (info.coords.value) {
                String s = String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f",
                        mc.player.getX(), mc.player.getY(), mc.player.getZ());
                ctx.drawTextWithShadow(tr, s, 4, iy, 0xFFFFFFFF);
                iy += 11;
            }
            if (info.direction.value) {
                int idx = ((int) Math.floor(mc.player.getYaw() / 90.0 + 0.5)) & 3;
                ctx.drawTextWithShadow(tr, "Facing  " + FACING[idx], 4, iy, 0xFFFFFFFF);
                iy += 11;
            }
            if (info.fps.value) {
                ctx.drawTextWithShadow(tr, mc.getCurrentFps() + " FPS", 4, iy, 0xFFFFFFFF);
            }
        }

        // Toast near the hotbar.
        String toast = Toasts.current();
        if (toast != null) {
            int w = tr.getWidth(toast);
            int x = (sw - w) / 2;
            int ty = sh - 62;
            ctx.fill(x - 6, ty - 3, x + w + 6, ty + 11, 0xB0100B1C);
            ctx.fill(x - 6, ty + 10, x + w + 6, ty + 11, Theme.accent(0));
            ctx.drawTextWithShadow(tr, toast, x, ty, 0xFFFFFFFF);
        }
    }
}
