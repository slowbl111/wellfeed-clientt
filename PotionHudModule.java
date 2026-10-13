package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Active potion effects with level, time left and a draining duration bar. */
public final class PotionHudModule extends Module {
    public static final PotionHudModule INSTANCE = new PotionHudModule();

    private final Setting bars = add(Setting.bool("Duration bars", true));

    private static final int ROW_H = 14;

    /** Longest duration seen for each effect, to size the draining bar. */
    private final Map<Object, Integer> longest = new HashMap<>();

    private PotionHudModule() {
        super("PotionHUD", "Active effects with level and time left.",
                Category.HUD, true, false, true, false, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        List<StatusEffectInstance> effects = new ArrayList<>(p.getStatusEffects());
        if (effects.isEmpty()) {
            longest.clear();
            return;
        }
        effects.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        List<String> names = new ArrayList<>(effects.size());
        List<String> times = new ArrayList<>(effects.size());
        int nameW = 0, timeW = 0;
        for (StatusEffectInstance e : effects) {
            String n = nameFor(e);
            String t = e.isInfinite() ? "inf" : formatTicks(e.getDuration());
            names.add(n);
            times.add(t);
            nameW = Math.max(nameW, mc.textRenderer.getWidth(n));
            timeW = Math.max(timeW, mc.textRenderer.getWidth(t));
            Object key = e.getEffectType();
            longest.merge(key, e.getDuration(), Math::max);
        }

        int panelW = nameW + timeW + 26;
        int panelH = effects.size() * ROW_H + 6;
        int x = 8;
        int y = ctx.getScaledWindowHeight() / 2 - panelH / 2;

        Render.shadow(ctx, x, y, panelW, panelH, 3, 1f);
        Render.roundRect(ctx, x, y, panelW, panelH, 3, Render.PANEL);

        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int ry = y + 3 + i * ROW_H;
            boolean good = e.getEffectType().value().isBeneficial();
            int accent = good ? Render.GOOD : Render.BAD;

            ctx.fill(x + 4, ry + 1, x + 6, ry + ROW_H - 3, accent);

            boolean low = !e.isInfinite() && e.getDuration() < 200; // under 10 s
            int color = low ? Render.WARN : Render.CREAM;
            Render.text(ctx, mc.textRenderer, names.get(i), x + 10, ry + 1, color, true);
            Render.textRight(ctx, mc.textRenderer, times.get(i), x + panelW - 6, ry + 1,
                    low ? Render.WARN : Render.CREAM_SOFT, true);

            if (bars.getBool() && !e.isInfinite()) {
                int max = Math.max(1, longest.getOrDefault(e.getEffectType(), e.getDuration()));
                float frac = Math.max(0f, Math.min(1f, e.getDuration() / (float) max));
                int bx = x + 10, bw = panelW - 16;
                ctx.fill(bx, ry + ROW_H - 4, bx + bw, ry + ROW_H - 3, 0x60000000);
                ctx.fill(bx, ry + ROW_H - 4, bx + Math.round(bw * frac), ry + ROW_H - 3, Render.alpha(accent, 0.9f));
            }
        }
    }

    private static String nameFor(StatusEffectInstance e) {
        String name = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        return amp > 0 ? name + " " + roman(amp + 1) : name;
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
