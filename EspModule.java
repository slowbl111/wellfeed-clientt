package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.CamState;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player ESP.
 *
 * Draws a true 3D box (all 12 edges of the player's hitbox, projected from the real camera and
 * clipped against the near plane so it never pops when you walk through it), optional tracers,
 * a name tag with distance, and a health bar that animates. Positions are interpolated every
 * frame, so everything glides at your full frame rate. Works with Freecam and third person.
 * The vanilla glow outline (MinecraftClientMixin) adds a smooth in-world silhouette on top.
 */
public final class EspModule extends Module {
    public static final EspModule INSTANCE = new EspModule();

    private final Setting boxes = add(Setting.bool("3D boxes", true));
    private final Setting tracers = add(Setting.bool("Tracers", false));
    private final Setting names = add(Setting.bool("Names + distance", true));
    private final Setting health = add(Setting.bool("Health bar", true));
    private final Setting glow = add(Setting.bool("Glow outline", true));
    private final Setting colorMode = add(Setting.mode("Color", 1, "Cream", "Distance", "Health"));
    private final Setting thickness = add(Setting.num("Line thickness", 1, 1, 3, 1));
    private final Setting maxDist = add(Setting.num("Max distance", 128, 16, 512, 16));

    private static final double NEAR = 0.05;

    /** Per-player animation state so bars and fades are smooth. */
    private static final class State {
        float alpha;
        float hp;
        boolean seen;
    }

    private final Map<UUID, State> states = new HashMap<>();
    private long lastNano;

    private EspModule() {
        super("ESP", "3D boxes, tracers, names and health for players, through walls.",
                Category.RENDER, false, true, false, false, GLFW.GLFW_KEY_H);
    }

    public boolean glowActive() {
        return isEnabled() && glow.getBool();
    }

    @Override
    protected void onDisable() {
        states.clear();
    }

    // ---- camera basis for the current frame
    private double cx, cy, cz;
    private double fx, fy, fz, rx, ry, rz, ux, uy, uz;
    private double scale;
    private int sw, sh;

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !CamState.valid) return;

        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (now - lastNano) / 1_000_000_000f);
        lastNano = now;

        sw = ctx.getScaledWindowWidth();
        sh = ctx.getScaledWindowHeight();

        cx = CamState.x;
        cy = CamState.y;
        cz = CamState.z;
        double yaw = Math.toRadians(CamState.yaw);
        double pitch = Math.toRadians(CamState.pitch);
        double cyw = Math.cos(yaw), syw = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        fx = -syw * cp; fy = -sp; fz = cyw * cp;          // forward
        rx = -cyw;      ry = 0;   rz = -syw;              // right
        ux = ry * fz - rz * fy;                           // up = right x forward
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;

        double fovDeg = CamState.fov > 1f ? CamState.fov : mc.options.getFov().getValue();
        scale = (sh / 2.0) / Math.tan(Math.toRadians(fovDeg) / 2.0);

        boolean freecam = FreecamModule.INSTANCE.active();
        double max = maxDist.getNum();

        List<AbstractClientPlayerEntity> list = new ArrayList<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !freecam) continue;
            if (p.isSpectator() || !p.isAlive()) continue;
            Vec3d pos = p.getLerpedPos(tickDelta);
            double dx = pos.x - cx, dy = pos.y - cy, dz = pos.z - cz;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > max) continue;
            list.add(p);
        }
        // Nearest 24 only, to keep the per-frame line count bounded.
        list.sort((a, b) -> Double.compare(a.squaredDistanceTo(cx, cy, cz), b.squaredDistanceTo(cx, cy, cz)));
        if (list.size() > 24) list = list.subList(0, 24);

        for (State s : states.values()) s.seen = false;

        for (AbstractClientPlayerEntity p : list) {
            drawPlayer(ctx, mc, p, tickDelta, dt);
        }

        // Fade out and forget players that left range.
        var it = states.values().iterator();
        while (it.hasNext()) {
            State s = it.next();
            if (s.seen) continue;
            s.alpha = Render.approach(s.alpha, 0f, dt, 10f);
            if (s.alpha < 0.02f) it.remove();
        }
    }

    private void drawPlayer(DrawContext ctx, MinecraftClient mc, AbstractClientPlayerEntity p, float tickDelta, float dt) {
        State st = states.computeIfAbsent(p.getUuid(), k -> {
            State s = new State();
            s.hp = p.getHealth();
            return s;
        });
        st.seen = true;
        st.alpha = Render.approach(st.alpha, 1f, dt, 10f);
        float maxHp = Math.max(1f, p.getMaxHealth());
        st.hp = Render.approach(st.hp, p.getHealth(), dt, 9f);
        if (st.alpha < 0.03f) return;

        Vec3d pos = p.getLerpedPos(tickDelta);
        double hw = p.getWidth() / 2.0;
        double h = p.getHeight();

        double dx = pos.x - cx, dy = pos.y + h / 2 - cy, dz = pos.z - cz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        int base = switch (colorMode.getMode()) {
            case 1 -> Render.redToGreen((float) (dist / Math.max(16.0, maxDist.getNum() * 0.5)));
            case 2 -> Render.redToGreen(st.hp / maxHp);
            default -> Render.CREAM;
        };
        int color = Render.alpha(base, st.alpha);
        int th = (int) thickness.getNum();

        // Screen-space bounds of everything drawn, for the name tag and health bar.
        double[] bb = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

        double x0 = pos.x - hw, x1 = pos.x + hw, z0 = pos.z - hw, z1 = pos.z + hw;
        double y0 = pos.y, y1 = pos.y + h;

        if (boxes.getBool()) {
            // bottom square
            edge(ctx, x0, y0, z0, x1, y0, z0, color, th, bb);
            edge(ctx, x1, y0, z0, x1, y0, z1, color, th, bb);
            edge(ctx, x1, y0, z1, x0, y0, z1, color, th, bb);
            edge(ctx, x0, y0, z1, x0, y0, z0, color, th, bb);
            // top square
            edge(ctx, x0, y1, z0, x1, y1, z0, color, th, bb);
            edge(ctx, x1, y1, z0, x1, y1, z1, color, th, bb);
            edge(ctx, x1, y1, z1, x0, y1, z1, color, th, bb);
            edge(ctx, x0, y1, z1, x0, y1, z0, color, th, bb);
            // verticals
            edge(ctx, x0, y0, z0, x0, y1, z0, color, th, bb);
            edge(ctx, x1, y0, z0, x1, y1, z0, color, th, bb);
            edge(ctx, x1, y0, z1, x1, y1, z1, color, th, bb);
            edge(ctx, x0, y0, z1, x0, y1, z1, color, th, bb);
        } else {
            // Still need bounds for tags: project the box without drawing it.
            for (int i = 0; i < 8; i++) {
                double px = (i & 1) == 0 ? x0 : x1, py = (i & 2) == 0 ? y0 : y1, pz = (i & 4) == 0 ? z0 : z1;
                double[] s = project(px, py, pz);
                if (s != null) grow(bb, s[0], s[1]);
            }
        }

        if (tracers.getBool()) {
            double[] feet = project(pos.x, pos.y, pos.z);
            if (feet != null) {
                Render.line(ctx, sw / 2.0, sh, feet[0], feet[1], Render.alpha(base, st.alpha * 0.7f), 1, sw, sh);
            }
        }

        if (bb[0] == Double.MAX_VALUE) return; // fully behind the camera

        int left = (int) Math.round(bb[0]), top = (int) Math.round(bb[1]);
        int right = (int) Math.round(bb[2]), bottom = (int) Math.round(bb[3]);

        if (health.getBool()) {
            float frac = Math.max(0f, Math.min(1f, st.hp / maxHp));
            int bh = Math.max(4, bottom - top);
            int bx = left - 5;
            ctx.fill(bx, top - 1, bx + 3, bottom + 1, Render.alpha(0xFF000000, 0.55f * st.alpha));
            int fh = Math.round(bh * frac);
            ctx.fill(bx + 1, bottom - fh, bx + 2, bottom, Render.alpha(Render.redToGreen(frac), st.alpha));
        }

        if (names.getBool()) {
            String label = p.getName().getString() + "  " + Math.round(dist) + "m";
            int tw = mc.textRenderer.getWidth(label);
            int cxl = (left + right) / 2;
            int ty = top - 14;
            Render.roundRect(ctx, cxl - tw / 2 - 4, ty - 1, tw + 8, 11, 3, Render.alpha(0xFF14100C, 0.6f * st.alpha));
            Render.textCentered(ctx, mc.textRenderer, label, cxl, ty, Render.alpha(Render.CREAM, st.alpha), false);
        }
    }

    private void grow(double[] bb, double x, double y) {
        if (x < bb[0]) bb[0] = x;
        if (y < bb[1]) bb[1] = y;
        if (x > bb[2]) bb[2] = x;
        if (y > bb[3]) bb[3] = y;
    }

    /** World point -> screen x,y, or null if it is behind the camera. */
    private double[] project(double wx, double wy, double wz) {
        double dx = wx - cx, dy = wy - cy, dz = wz - cz;
        double z = dx * fx + dy * fy + dz * fz;
        if (z < NEAR) return null;
        double x = dx * rx + dy * ry + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        return new double[]{sw / 2.0 + x / z * scale, sh / 2.0 - y / z * scale};
    }

    /** One 3D edge, clipped to the camera's near plane, then drawn and added to the bounds. */
    private void edge(DrawContext ctx, double ax, double ay, double az, double bx, double by, double bz,
                      int color, int th, double[] bb) {
        double adx = ax - cx, ady = ay - cy, adz = az - cz;
        double bdx = bx - cx, bdy = by - cy, bdz = bz - cz;
        double za = adx * fx + ady * fy + adz * fz;
        double zb = bdx * fx + bdy * fy + bdz * fz;
        if (za < NEAR && zb < NEAR) return;

        if (za < NEAR) {
            double t = (NEAR - za) / (zb - za);
            adx += (bdx - adx) * t; ady += (bdy - ady) * t; adz += (bdz - adz) * t;
            za = NEAR;
        } else if (zb < NEAR) {
            double t = (NEAR - zb) / (za - zb);
            bdx += (adx - bdx) * t; bdy += (ady - bdy) * t; bdz += (adz - bdz) * t;
            zb = NEAR;
        }

        double sx0 = sw / 2.0 + (adx * rx + ady * ry + adz * rz) / za * scale;
        double sy0 = sh / 2.0 - (adx * ux + ady * uy + adz * uz) / za * scale;
        double sx1 = sw / 2.0 + (bdx * rx + bdy * ry + bdz * rz) / zb * scale;
        double sy1 = sh / 2.0 - (bdx * ux + bdy * uy + bdz * uz) / zb * scale;

        grow(bb, sx0, sy0);
        grow(bb, sx1, sy1);
        Render.line(ctx, sx0, sy0, sx1, sy1, color, th, sw, sh);
    }
}
