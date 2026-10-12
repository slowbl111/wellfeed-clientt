package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * "Creamy" keystrokes: WASD, mouse buttons with CPS, and space. Each key fades
 * smoothly between a roasted-brown idle color and cream when pressed.
 */
public final class KeystrokesModule extends Module {
    public static final KeystrokesModule INSTANCE = new KeystrokesModule();

    private final Setting showCps = add(Setting.bool("Show CPS", true));
    private final Setting showSpace = add(Setting.bool("Show space bar", true));

    private static final int IDLE_BG  = 0xA0332A22;
    private static final int CREAM_BG = 0xF5F5E6C8;

    // 0..1 animation per key
    private float aW, aA, aS, aD, aL, aR, aSpace;
    private boolean prevL, prevR;
    private final Deque<Long> lClicks = new ArrayDeque<>();
    private final Deque<Long> rClicks = new ArrayDeque<>();
    private long lastNano;

    private KeystrokesModule() {
        super("Keystrokes", "Smooth cream-colored WASD, mouse and space display.", true, false);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        GameOptions o = mc.options;
        TextRenderer font = mc.textRenderer;

        long nowN = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (nowN - lastNano) / 1_000_000_000f);
        lastNano = nowN;
        float k = 1f - (float) Math.exp(-dt * 22f); // smooth ease toward target

        boolean atk = o.attackKey.isPressed();
        boolean use = o.useKey.isPressed();

        long ms = System.currentTimeMillis();
        if (atk && !prevL) lClicks.addLast(ms);
        if (use && !prevR) rClicks.addLast(ms);
        prevL = atk;
        prevR = use;
        while (!lClicks.isEmpty() && ms - lClicks.peekFirst() > 1000) lClicks.pollFirst();
        while (!rClicks.isEmpty() && ms - rClicks.peekFirst() > 1000) rClicks.pollFirst();

        aW += ((o.forwardKey.isPressed() ? 1f : 0f) - aW) * k;
        aA += ((o.leftKey.isPressed() ? 1f : 0f) - aA) * k;
        aS += ((o.backKey.isPressed() ? 1f : 0f) - aS) * k;
        aD += ((o.rightKey.isPressed() ? 1f : 0f) - aD) * k;
        aL += ((atk ? 1f : 0f) - aL) * k;
        aR += ((use ? 1f : 0f) - aR) * k;
        aSpace += ((o.jumpKey.isPressed() ? 1f : 0f) - aSpace) * k;

        int x0 = ctx.getScaledWindowWidth() - 8 - 70;
        int y0 = ctx.getScaledWindowHeight() / 2 - 44;

        key(ctx, font, x0 + 24, y0,      22, 22, "W", null, aW);
        key(ctx, font, x0,      y0 + 24, 22, 22, "A", null, aA);
        key(ctx, font, x0 + 24, y0 + 24, 22, 22, "S", null, aS);
        key(ctx, font, x0 + 48, y0 + 24, 22, 22, "D", null, aD);

        boolean cps = showCps.getBool();
        key(ctx, font, x0,      y0 + 48, 34, 24, "LMB", cps ? lClicks.size() + " CPS" : null, aL);
        key(ctx, font, x0 + 36, y0 + 48, 34, 24, "RMB", cps ? rClicks.size() + " CPS" : null, aR);

        if (showSpace.getBool()) {
            key(ctx, font, x0, y0 + 74, 70, 14, "SPACE", null, aSpace);
        }
    }

    private static void key(DrawContext ctx, TextRenderer font, int x, int y, int w, int h,
                            String label, String sub, float anim) {
        Render.roundRect(ctx, x, y, w, h, Render.lerpColor(IDLE_BG, CREAM_BG, anim));

        int main = Render.lerpColor(Render.CREAM, Render.ROAST, anim);
        int cx = x + w / 2;
        if (sub == null) {
            Render.textCentered(ctx, font, label, cx, y + (h - 8) / 2, main, false);
        } else {
            Render.textCentered(ctx, font, label, cx, y + 4, main, false);
            int subColor = Render.lerpColor(Render.CREAM_SOFT, Render.ROAST, anim);
            Render.textCentered(ctx, font, sub, cx, y + 13, subColor, false);
        }
    }
}
