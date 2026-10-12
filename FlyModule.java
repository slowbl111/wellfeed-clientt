package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Velocity-based flight: WASD to move where you face (horizontal), Space up, Shift down.
 * Note: vanilla survival servers kick for this ("Flying is not enabled on this server").
 */
public final class FlyModule extends Module {
    public static final FlyModule INSTANCE = new FlyModule();

    private final Setting speed = add(Setting.num("Speed", 0.8, 0.1, 3.0, 0.1));

    private FlyModule() {
        super("Fly", "Fly freely. Space = up, Shift = down.", false, true);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.hasVehicle() || p.isSpectator()) return;

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
        if (o.jumpKey.isPressed()) my += s;
        if (o.sneakKey.isPressed()) my -= s;

        p.setVelocity(mx, my, mz);
    }
}
