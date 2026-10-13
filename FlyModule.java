package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Velocity-based flight: WASD to move the way you face, Space up, Shift down.
 * With Smooth on, speed eases in and out instead of snapping.
 * Most multiplayer servers kick or ban for this; it is meant for singleplayer and private worlds.
 */
public final class FlyModule extends Module {
    public static final FlyModule INSTANCE = new FlyModule();

    private final Setting speed = add(Setting.num("Speed", 0.8, 0.1, 3.0, 0.1));
    private final Setting vertical = add(Setting.num("Vertical speed", 0.6, 0.1, 3.0, 0.1));
    private final Setting smooth = add(Setting.bool("Smooth", true));

    private FlyModule() {
        super("Fly", "Fly freely. Space = up, Shift = down.",
                Category.MOVEMENT, false, true, false, false, GLFW.GLFW_KEY_G);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.hasVehicle() || p.isSpectator()) return;
        if (FreecamModule.INSTANCE.isEnabled()) { // body hovers in place during freecam
            p.setVelocity(0, 0, 0);
            return;
        }

        var o = mc.options;
        double s = speed.getNum();

        double forward = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double strafe = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);

        double yaw = Math.toRadians(p.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);     // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);    // right

        double mx = fx * forward + rx * strafe;
        double mz = fz * forward + rz * strafe;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 1e-6) {
            mx = mx / len * s;
            mz = mz / len * s;
        }

        double my = 0;
        if (o.jumpKey.isPressed()) my += vertical.getNum();
        if (o.sneakKey.isPressed()) my -= vertical.getNum();

        if (smooth.getBool()) {
            Vec3d v = p.getVelocity();
            double k = 0.28;
            mx = v.x + (mx - v.x) * k;
            my = v.y + (my - v.y) * k;
            mz = v.z + (mz - v.z) * k;
        }
        p.setVelocity(mx, my, mz);
    }
}
