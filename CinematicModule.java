package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.CameraRig;

/**
 * Cinematic drone cam: the camera flies around you on its own, gliding between five shots
 * (orbit, rising spiral, figure-eight fly-by, low hero orbit, high crane) and never clips into walls.
 * You keep playing normally the whole time. The opening shot is random.
 */
public final class CinematicModule extends Module {
    public static final CinematicModule INSTANCE = new CinematicModule();

    private final Setting speed = add(Setting.num("Speed", 1.0, 0.25, 3.0, 0.25));
    private final Setting shot = add(Setting.num("Shot (0 = auto)", 0, 0, 5, 1));

    private CinematicModule() {
        super("Cinematic", "Drone cam: the camera flies around you on its own while you play.", false, true);
    }

    public double speed() { return speed.getNum(); }
    public int manualShot() { return (int) shot.getNum(); }

    @Override
    protected void onEnable() {
        FreecamModule.INSTANCE.setEnabled(false);
        if (!CameraRig.start(CameraRig.Mode.CINEMA)) setEnabled(false);
    }

    @Override
    protected void onDisable() {
        CameraRig.stop(CameraRig.Mode.CINEMA);
    }
}
