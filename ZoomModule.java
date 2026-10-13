package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import org.lwjgl.glfw.GLFW;

/**
 * Hold the key to zoom, release to zoom back out. The field of view eases smoothly in both
 * directions and mouse sensitivity drops with it so aiming stays controllable.
 * GameRendererMixin applies the multiplier; there is no per-tick work here.
 */
public final class ZoomModule extends Module {
    public static final ZoomModule INSTANCE = new ZoomModule();

    private final Setting level = add(Setting.num("Zoom level", 4.0, 1.5, 12.0, 0.5));
    private final Setting speed = add(Setting.num("Smoothness", 12, 4, 30, 1));
    private final Setting sens = add(Setting.bool("Lower sensitivity", true));

    private float mult = 1f;
    private long lastNano;

    private ZoomModule() {
        super("Zoom", "Hold the key to zoom in smoothly (hold-to-use).",
                Category.RENDER, false, false, false, true, GLFW.GLFW_KEY_C);
    }

    /** Current field-of-view multiplier (1 = no zoom). Call once per frame; eases toward the target. */
    public float multiplier() {
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (now - lastNano) / 1_000_000_000f);
        lastNano = now;
        float target = isEnabled() ? 1f / (float) level.getNum() : 1f;
        mult = Render.approach(mult, target, dt, (float) speed.getNum());
        if (Math.abs(mult - target) < 0.0005f) mult = target;
        return mult;
    }

    /** Multiplier for mouse sensitivity while zoomed. */
    public float sensitivityScale() {
        return sens.getBool() ? Math.max(0.1f, mult) : 1f;
    }

    public boolean zoomed() {
        return mult < 0.999f;
    }
}
