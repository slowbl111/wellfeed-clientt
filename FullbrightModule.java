package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import org.lwjgl.glfw.GLFW;

/**
 * Full brightness. No logic here: SimpleOptionMixin overrides the gamma value
 * while this module is on, so there is no per-frame cost and no fake potion effect.
 */
public final class FullbrightModule extends Module {
    public static final FullbrightModule INSTANCE = new FullbrightModule();

    private FullbrightModule() {
        super("Fullbright", "Everything is fully lit, even in caves.",
                Category.RENDER, false, true, true, false, GLFW.GLFW_KEY_B);
    }

    public static boolean active() {
        return INSTANCE.isEnabled();
    }
}
