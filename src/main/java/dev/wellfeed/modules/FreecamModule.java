package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.CameraRig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Freecam: the camera leaves your body and flies wherever you look.
 * WASD to move, Space up, Shift down, sprint key = boost. Your real player stands still
 * and can't hit or place anything while this is on.
 */
public final class FreecamModule extends Module {
    public static final FreecamModule INSTANCE = new FreecamModule();

    private final Setting speed = add(Setting.num("Speed (blocks/s)", 12, 2, 60, 2));
    private final Setting boost = add(Setting.num("Sprint boost", 3, 1, 8, 1));
    private final Setting smoothing = add(Setting.num("Smoothing", 8, 2, 20, 1));

    private FreecamModule() {
        super("Freecam", "Fly the camera around freely. WASD + Space/Shift, sprint = boost.", false, true);
    }

    public double speed() { return speed.getNum(); }
    public double boost() { return boost.getNum(); }
    public double smoothing() { return smoothing.getNum(); }

    @Override
    protected void onEnable() {
        CinematicModule.INSTANCE.setEnabled(false);
        if (!CameraRig.start(CameraRig.Mode.FREE)) setEnabled(false);
    }

    @Override
    protected void onDisable() {
        CameraRig.stop(CameraRig.Mode.FREE);
    }

    /** Runs before the game handles input each tick: swallow clicks so the hidden body can't act. */
    @Override
    public void onPreTick(MinecraftClient mc) {
        GameOptions o = mc.options;
        o.attackKey.setPressed(false);
        o.useKey.setPressed(false);
        while (o.attackKey.wasPressed()) { }
        while (o.useKey.wasPressed()) { }
        while (o.pickItemKey.wasPressed()) { }
    }
}
