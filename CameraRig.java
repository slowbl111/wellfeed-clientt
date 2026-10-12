package dev.wellfeed.util;

import dev.wellfeed.modules.CinematicModule;
import dev.wellfeed.modules.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Random;

/**
 * Detached camera shared by Freecam and Cinematic.
 *
 * How it works: a client-only armor stand (never added to the world) becomes the game's camera
 * entity, and {@link #frame()} moves it once per rendered frame (called from CameraMixin right
 * before the camera reads it). Position is integrated per frame, not per tick, so it is smooth at
 * any FPS.
 *
 *  - FREE:   your real player keeps standing where it is. Its movement input is swapped for an
 *            empty one, and mouse movement is read off the player's yaw/pitch and moved to the
 *            camera instead.
 *  - CINEMA: you keep playing normally while the camera glides along drone shots around you.
 */
public final class CameraRig {
    public enum Mode { NONE, FREE, CINEMA }

    private static final int SHOTS = 5;
    private static final double SHOT_SECONDS = 11.0;

    private static Mode mode = Mode.NONE;
    private static ArmorStandEntity cam;
    private static ClientPlayerEntity owner;
    private static Input savedInput;
    private static double eyeOffset;
    private static long lastNano;

    // Camera state
    private static double x, y, z, vx, vy, vz;
    private static float yaw, pitch;           // state
    private static float outYaw, outPitch;     // what the camera shows (state + sway)
    private static float savedYaw, savedPitch; // real player's rotation, restored every frame (FREE)

    // Cinematic state
    private static double time, blend, fx, fy, fz;
    private static int shot, prevShot, startShot;

    public static Mode mode() { return mode; }
    public static boolean active() { return mode != Mode.NONE; }
    public static Vec3d eye() { return new Vec3d(x, y, z); }

    // ---------------------------------------------------------------- lifecycle

    public static boolean start(Mode m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (m == Mode.NONE || p == null || mc.world == null) return false;
        if (mode != Mode.NONE) stopNow();

        mode = m;
        owner = p;
        cam = new ArmorStandEntity(EntityType.ARMOR_STAND, mc.world);
        cam.refreshPositionAndAngles(0, 0, 0, 0, 0);
        eyeOffset = cam.getEyeY() - cam.getY();

        Vec3d eye = p.getEyePos();
        x = eye.x; y = eye.y; z = eye.z;
        vx = vy = vz = 0;
        yaw = outYaw = savedYaw = p.getYaw();
        pitch = outPitch = savedPitch = p.getPitch();
        lastNano = 0;

        if (m == Mode.FREE) {
            savedInput = p.input;
            p.input = new Input();
        } else {
            Vec3d pp = p.getPos();
            fx = pp.x; fy = pp.y + p.getHeight() * 0.6; fz = pp.z;
            time = 0;
            startShot = new Random().nextInt(SHOTS);   // surprise: a different opening every time
            shot = prevShot = startShot;
            blend = 1;
        }

        cam.refreshPositionAndAngles(x, y - eyeOffset, z, outYaw, outPitch);
        mc.setCameraEntity(cam);
        return true;
    }

    public static void stop(Mode m) {
        if (mode == m) stopNow();
    }

    public static void stopNow() {
        if (mode == Mode.NONE) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mode == Mode.FREE && owner != null && savedInput != null && owner == mc.player) {
            owner.input = savedInput;
        }
        if (mc.player != null && mc.getCameraEntity() == cam) {
            mc.setCameraEntity(mc.player);
        }
        mode = Mode.NONE;
        cam = null;
        owner = null;
        savedInput = null;
        lastNano = 0;
    }

    /** False when the world or player the rig was started in is gone (respawn, disconnect). */
    public static boolean valid(MinecraftClient mc) {
        return mode == Mode.NONE
                || (mc.player != null && mc.world != null && mc.player == owner);
    }

    // ---------------------------------------------------------------- per frame

    public static void frame() {
        if (mode == Mode.NONE) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || p != owner || cam == null) return;

        long now = System.nanoTime();
        double dt = lastNano == 0 ? 0.016 : Math.min(0.1, (now - lastNano) / 1_000_000_000.0);
        lastNano = now;
        if (dt <= 0) return;

        if (mode == Mode.FREE) free(mc, p, dt); else cinema(mc, p, dt);

        cam.refreshPositionAndAngles(x, y - eyeOffset, z, outYaw, outPitch);
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    private static void free(MinecraftClient mc, ClientPlayerEntity p, double dt) {
        FreecamModule fm = FreecamModule.INSTANCE;

        // Mouse: vanilla just turned the real player. Take that turn for the camera, put the player back.
        float dyaw = p.getYaw() - savedYaw;
        float dpitch = p.getPitch() - savedPitch;
        if (Math.abs(dyaw) < 90f) yaw += dyaw;
        pitch = MathHelper.clamp(pitch + dpitch, -90f, 90f);
        p.setYaw(savedYaw);
        p.setPitch(savedPitch);
        outYaw = yaw;
        outPitch = pitch;

        GameOptions o = mc.options;
        double f = 0, s = 0, u = 0;
        if (mc.currentScreen == null) {
            f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
            s = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
            u = (o.jumpKey.isPressed() ? 1 : 0) - (o.sneakKey.isPressed() ? 1 : 0);
        }

        double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
        double cp = Math.cos(rp);
        double fdx = -Math.sin(ry) * cp, fdy = -Math.sin(rp), fdz = Math.cos(ry) * cp;   // forward
        double rdx = -Math.cos(ry), rdz = -Math.sin(ry);                                 // right

        double mx = fdx * f + rdx * s;
        double my = fdy * f + u;
        double mz = fdz * f + rdz * s;
        double len = Math.sqrt(mx * mx + my * my + mz * mz);
        if (len > 1) { mx /= len; my /= len; mz /= len; }

        double speed = fm.speed() * (o.sprintKey.isPressed() ? fm.boost() : 1.0);
        double k = 1 - Math.exp(-dt * fm.smoothing());   // ease toward target velocity
        vx += (mx * speed - vx) * k;
        vy += (my * speed - vy) * k;
        vz += (mz * speed - vz) * k;

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
    }

    private static void cinema(MinecraftClient mc, ClientPlayerEntity p, double dt) {
        CinematicModule cm = CinematicModule.INSTANCE;
        time += dt * cm.speed();

        // Focus point: your chest, eased so footsteps never jerk the camera.
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d pp = p.getLerpedPos(tp);
        double kf = 1 - Math.exp(-dt * 8);
        fx += (pp.x - fx) * kf;
        fy += (pp.y + p.getHeight() * 0.6 - fy) * kf;
        fz += (pp.z - fz) * kf;

        // Pick the shot, crossfade between shots over 2.5 s.
        int manual = cm.manualShot();
        int target = manual > 0 ? (manual - 1) % SHOTS : ((int) (time / SHOT_SECONDS) + startShot) % SHOTS;
        if (target != shot) {
            prevShot = shot;
            shot = target;
            blend = 0;
        }
        blend = Math.min(1, blend + dt / 2.5);
        double e = blend * blend * (3 - 2 * blend);

        double[] a = offset(prevShot, time);
        double[] b = offset(shot, time);
        double tx = fx + a[0] + (b[0] - a[0]) * e;
        double ty = fy + a[1] + (b[1] - a[1]) * e;
        double tz = fz + a[2] + (b[2] - a[2]) * e;

        // Don't film from inside a wall: pull the camera in front of whatever blocks the line of sight.
        Vec3d from = new Vec3d(fx, fy, fz);
        Vec3d to = new Vec3d(tx, ty, tz);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) {
            double full = to.distanceTo(from);
            double d = Math.max(0.8, hit.getPos().distanceTo(from) - 0.4);
            if (full > 1e-6 && d < full) {
                double r = d / full;
                tx = fx + (tx - fx) * r;
                ty = fy + (ty - fy) * r;
                tz = fz + (tz - fz) * r;
            }
        }

        double kp = 1 - Math.exp(-dt * 4);
        x += (tx - x) * kp;
        y += (ty - y) * kp;
        z += (tz - z) * kp;

        // Look at the focus point, eased, plus a tiny handheld sway.
        double dx = fx - x, dy = fy - y, dz = fz - z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        double kr = 1 - Math.exp(-dt * 5);
        yaw += (float) (MathHelper.wrapDegrees(wantYaw - yaw) * kr);
        pitch += (float) ((wantPitch - pitch) * kr);
        pitch = MathHelper.clamp(pitch, -90f, 90f);
        outYaw = yaw + (float) (Math.sin(time * 0.9) * 0.35);
        outPitch = MathHelper.clamp(pitch + (float) (Math.sin(time * 1.3 + 1.7) * 0.25), -90f, 90f);
    }

    /** Camera position relative to the focus point for a shot at time t. */
    private static double[] offset(int shot, double t) {
        switch (shot) {
            case 0: { // slow orbit
                double a = t * 0.35, r = 6.5 + 1.5 * Math.sin(t * 0.4);
                return new double[]{ r * Math.cos(a), 2.2 + Math.sin(t * 0.55), r * Math.sin(a) };
            }
            case 1: { // rising spiral
                double a = t * 0.55 + 1.0, r = 4.0 + 2.5 * (0.5 - 0.5 * Math.cos(t * 0.3));
                return new double[]{ r * Math.cos(a), 0.8 + 7.0 * (0.5 - 0.5 * Math.cos(t * 0.25)), r * Math.sin(a) };
            }
            case 2: { // figure-eight fly-by
                double a = t * 0.4, d = 1 + Math.sin(a) * Math.sin(a);
                return new double[]{ 8.0 * Math.cos(a) / d, 2.5 + 1.5 * Math.sin(t * 0.7), 12.0 * Math.sin(a) * Math.cos(a) / d };
            }
            case 3: { // low hero orbit, the other way round
                double a = -t * 0.5 + 2.0, r = 3.6 + 0.6 * Math.sin(t * 0.9);
                return new double[]{ r * Math.cos(a), 0.4 + 0.5 * Math.sin(t * 0.6), r * Math.sin(a) };
            }
            default: { // high crane
                double a = t * 0.2, r = 2.5;
                return new double[]{ r * Math.cos(a), 11.0 - 3.0 * Math.sin(t * 0.3), r * Math.sin(a) };
            }
        }
    }

    private CameraRig() {}
}
