package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * Player ESP: name + distance (meters) + box, visible through walls.
 * 2D HUD overlay (cheap), plus vanilla glow outline via MinecraftClientMixin.
 */
public final class EspModule extends Module {
    public static final EspModule INSTANCE = new EspModule();

    private final Setting boxes = add(Setting.bool("Boxes", true));
    private final Setting names = add(Setting.bool("Names + distance", true));
    private final Setting glow = add(Setting.bool("Glow outline", true));
    private final Setting maxDist = add(Setting.num("Max distance", 256, 16, 512, 16));

    private static final int COLOR_NEAR = 0xFFFF5555;   // < 20 m
    private static final int COLOR_MID  = 0xFFFFFF55;   // < 60 m
    private static final int COLOR_FAR  = 0xFF55FF55;   // rest

    private EspModule() {
        super("ESP", "Names, distance in meters and boxes for players, through walls.", false, true);
    }

    public boolean glowActive() {
        return isEnabled() && glow.getBool();
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || !cam.isReady()) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // Camera basis from yaw/pitch (Minecraft convention).
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double fx = -sy * cp, fy = -sp, fz = cy * cp;      // forward
        double rx = -cy,      ry = 0,   rz = -sy;          // right
        double ux = ry * fz - rz * fy;                     // up = right x forward
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double scale = (h / 2.0) / Math.tan(fov / 2.0);

        Vec3d eye = mc.player.getCameraPosVec(tickDelta);
        double max = maxDist.getNum();

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator() || !p.isAlive()) continue;

            Vec3d pos = p.getLerpedPos(tickDelta);
            double dist = eye.distanceTo(pos);
            if (dist > max) continue;

            double headY = pos.y + p.getHeight() + 0.25;
            double[] top = project(pos.x - eye.x, headY - eye.y, pos.z - eye.z,
                    fx, fy, fz, rx, ry, rz, ux, uy, uz, scale, w, h);
            double[] bottom = project(pos.x - eye.x, pos.y - eye.y, pos.z - eye.z,
                    fx, fy, fz, rx, ry, rz, ux, uy, uz, scale, w, h);
            if (top == null || bottom == null) continue; // behind camera

            int color = dist < 20 ? COLOR_NEAR : dist < 60 ? COLOR_MID : COLOR_FAR;

            double boxH = bottom[1] - top[1];
            double boxW = boxH * 0.5;
            int x1 = (int) (top[0] - boxW / 2);
            int y1 = (int) top[1];

            if (boxes.getBool()) {
                ctx.drawBorder(x1, y1, (int) boxW, (int) boxH, color);
            }
            if (names.getBool()) {
                String label = p.getName().getString() + " [" + Math.round(dist) + "m]";
                ctx.drawCenteredTextWithShadow(mc.textRenderer, label, (int) top[0], y1 - 10, color);
            }
        }
    }

    /** World offset (relative to camera) -> screen x,y. Returns null if behind camera. */
    private static double[] project(double dx, double dy, double dz,
                                    double fx, double fy, double fz,
                                    double rx, double ry, double rz,
                                    double ux, double uy, double uz,
                                    double scale, int w, int h) {
        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.05) return null;
        double x = dx * rx + dy * ry + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        return new double[]{ w / 2.0 + x / z * scale, h / 2.0 - y / z * scale };
    }
}
