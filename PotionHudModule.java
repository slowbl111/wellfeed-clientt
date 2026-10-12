package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Active potion effects with level and time left, in a compact cream panel. */
public final class PotionHudModule extends Module {
    public static final PotionHudModule INSTANCE = new PotionHudModule();

    private static final int ROW_H = 11;

    private PotionHudModule() {
        super("PotionHUD", "Active effects with level and time left.", true, false);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        List<StatusEffectInstance> effects = new ArrayList<>(p.getStatusEffects());
        if (effects.isEmpty()) return;
        effects.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        List<String> lines = new ArrayList<>(effects.size());
        int width = 0;
        for (StatusEffectInstance e : effects) {
            String line = lineFor(e);
            lines.add(line);
            width = Math.max(width, mc.textRenderer.getWidth(line));
        }

        int panelW = width + 12;
        int panelH = effects.size() * ROW_H + 4;
        int x = 6;
        int y = ctx.getScaledWindowHeight() / 2 - panelH / 2;

        Render.roundRect(ctx, x, y, panelW, panelH, Render.PANEL);

        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int ry = y + 2 + i * ROW_H;
            boolean good = e.getEffectType().value().isBeneficial();
            // accent bar
            ctx.fill(x + 2, ry + 1, x + 4, ry + ROW_H - 1, good ? 0xFF9BD67A : 0xFFE06C6C);

            boolean low = !e.isInfinite() && e.getDuration() < 200; // < 10 s
            int color = low ? 0xFFE8A87C : Render.CREAM;
            ctx.drawText(mc.textRenderer, lines.get(i), x + 8, ry + 1, color, true);
        }
    }

    private static String lineFor(StatusEffectInstance e) {
        String name = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        String level = amp > 0 ? " " + roman(amp + 1) : "";
        String time = e.isInfinite() ? "inf" : formatTicks(e.getDuration());
        return name + level + "  " + time;
    }

    private static String formatTicks(int ticks) {
        int total = Math.max(0, ticks / 20);
        int m = total / 60, s = total % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    private static String roman(int n) {
        if (n <= 0 || n > 39) return Integer.toString(n);
        String[] t = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        String[] x = {"", "X", "XX", "XXX"};
        return x[n / 10] + t[n % 10];
    }
}
