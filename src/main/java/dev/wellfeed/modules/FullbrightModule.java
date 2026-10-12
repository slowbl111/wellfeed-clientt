package dev.wellfeed.modules;

import dev.wellfeed.module.Module;

/**
 * Full brightness. No logic here: SimpleOptionMixin overrides the gamma value
 * while this module is on, so there is no per-frame cost and no fake potion effect.
 */
public final class FullbrightModule extends Module {
    public static final FullbrightModule INSTANCE = new FullbrightModule();

    private FullbrightModule() {
        super("Fullbright", "Everything is fully lit, even in caves.", false, true);
    }

    public static boolean active() {
        return INSTANCE.isEnabled();
    }
}
