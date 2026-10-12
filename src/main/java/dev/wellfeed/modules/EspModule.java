package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.CameraRig;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player ESP v4: a real 3D wireframe box (all 8 corners projected through the camera, clipped
 * against the near plane so it never pops), translucent fill, distance-scaled name tag, smoothly
 * animated health bar, optional tracers, and fades in/out instead of popping. Works with the
 * Freecam and Cinematic cameras. Vanilla glow outline is added through MinecraftClientMixin.
 */
public final class EspModule extends Module {
    public static final EspModule INSTANCE = new EspModule();

    private final Setting boxes   = add(Setting.bool("3D boxes", true));
    private final Setting fill    = add(Setting.bool("Fill", true));
    private final Setting names   = add(Setting.bool("Names + distance", true));
    private final Setting health  = add(Setting.bool("Health bar", true));
    private final Setting tracers = add(Setting.bool("Tracers", false));
    private final Setting byDist  = add(Setting.bool("Color by distance", true));
    private final Setting glow    = add(Setting.bool("Glow outline", true));
    private final Setting maxDist = add(Setting.num("Max distance", 256, 16, 512, 16));

    private static final int RED    = 0xFFFF5555;
    private static final int YELLOW = 0xFFFFE066;
    private static final int GREEN  = 0xFF66FF88;
    private static final double NEAR = 0.05;

    private static final class State {
        float alpha;          // fade-in
        float health = -1;    // eased health
        long frame;           // last frame seen
    }

    private final Map<UUID, State> states = new HashMap<>();
    private long frameId;
    private long lastNano;

    // scratch (single-threaded render)
    private final double[] cx = new double[8], cy = new double[8], cz = new double[8];
    private float bMinX, bMinY, bMaxX, bMaxY;
    private boolean bValid;

    private EspModule() {
        super("ESP", "3D boxes, names, health and distance for players, through walls.", false, true);
    }

    public boolean glowActive() {
        return isEnabled() && glow.getBool();
    }

    @Override
    protected void onDisable() {
        states.clear();
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || !cam.isReady()) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        long nowN = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (nowN - lastNano) / 1_000_000_000f);
        lastNano = nowN;
        float kFade = 1f - (float) Math.exp(-dt * 9f);
        float kHp = 1f - (float) Math.exp(-dt * 7f);
        frameId++;

        // Camera basis from yaw/pitch (Minecraft convention).
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cyw = Math.cos(yaw), syw = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double fx = -syw * cp, fy = -sp, fz = cyw * cp;      // forward
        double rx = -cyw,      ry = 0,   rz = -syw;          // right
        double ux = ry * fz - rz * fy;                       // up = right x forward
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double scale = (h / 2.0) / Math.tan(fov / 2.0);

        boolean rig = CameraRig.active();
        Vec3d eye = rig ? CameraRig.eye() : mc.player.getCameraPosVec(tickDelta);
        double max = maxDist.getNum();
        TextRenderer font = mc.textRenderer;

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if ((p == mc.player && !rig) || p.isSpectator() || !p.isAlive()) continue;

            Vec3d pos = p.getLerpedPos(tickDelta);
            double dist = eye.distanceTo(pos);
            if (dist > max) continue;

            State st = states.computeIfAbsent(p.getUuid(), id -> new State());
            st.frame = frameId;
            st.alpha += (1f - st.alpha) * kFade;
            float distFade = (float) Math.max(0, Math.min(1, (max - dist) / (max * 0.12)));
            float a = st.alpha * distFade;
            if (a < 0.02f) continue;

            int color = byDist.getBool() ? colorFor(dist) : Render.CREAM;

            // 8 corners of the player's box, in camera space.
            double hw = p.getWidth() / 2.0 + 0.05;
            double bh = p.getHeight() + 0.1;
            double ox = pos.x - eye.x, oy = pos.y - 0.05 - eye.y, oz = pos.z - eye.z;
            for (int i = 0; i < 8; i++) {
                double dx = ox + ((i & 1) != 0 ? hw : -hw);
                double dy = oy + ((i & 2) != 0 ? bh : 0);
                double dz = oz + ((i & 4) != 0 ? hw : -hw);
                cx[i] = dx * rx + dy * ry + dz * rz;
                cy[i] = dx * ux + dy * uy + dz * uz;
                cz[i] = dx * fx + dy * fy + dz * fz;
            }

            bValid = false;
            bMinX = bMinY = Float.MAX_VALUE;
            bMaxX = bMaxY = -Float.MAX_VALUE;

            // Pass 1: project edges (also builds the 2D bounds). Drawing waits for the fill.
            int edgeColor = Render.fade(color, a * 0.95f);
            boolean drawBox = boxes.getBool();
            if (fill.getBool()) {
                // bounds first
                for (int i = 0; i < 8; i++) for (int bit = 1; bit <= 4; bit <<= 1)
                    if ((i & bit) == 0) edge(null, i, i | bit, scale, w, h, 0, 0f);
                if (bValid) {
                    int x0 = (int) Math.max(0, bMinX), y0 = (int) Math.max(0, bMinY);
                    int x1 = (int) Math.min(w, bMaxX), y1 = (int) Math.min(h, bMaxY);
                    if (x1 > x0 && y1 > y0) ctx.fill(x0, y0, x1, y1, Render.fade(color, a * 0.10f));
                }
            }
            if (drawBox) {
                bValid = false;
                bMinX = bMinY = Float.MAX_VALUE;
                bMaxX = bMaxY = -Float.MAX_VALUE;
                for (int i = 0; i < 8; i++) for (int bit = 1; bit <= 4; bit <<= 1)
                    if ((i & bit) == 0) edge(ctx, i, i | bit, scale, w, h, edgeColor, bit == 2 ? 1.0f : 0.8f);
            } else if (!fill.getBool()) {
                for (int i = 0; i < 8; i++) for (int bit = 1; bit <= 4; bit <<= 1)
                    if ((i & bit) == 0) edge(null, i, i | bit, scale, w, h, 0, 0f);
            }
            if (!bValid) continue;

            float midX = (bMinX + bMaxX) / 2f;

            if (tracers.getBool()) {
                Render.line(ctx, w / 2f, h, midX, bMaxY, Render.fade(color, a * 0.55f), 0.8f);
            }

            if (health.getBool() && bMaxY - bMinY > 14) {
                float maxHp = Math.max(1f, p.getMaxHealth());
                float hp = Math.max(0f, p.getHealth());
                st.health = st.health < 0 ? hp : st.health + (hp - st.health) * kHp;
                float frac = Math.max(0f, Math.min(1f, st.health / maxHp));
                int bx = (int) bMinX - 4;
                int top = (int) bMinY, bot = (int) bMaxY;
                int fh = Math.round((bot - top) * frac);
                ctx.fill(bx, top, bx + 2, bot, Render.fade(0xFF000000, a * 0.55f));
                ctx.fill(bx, bot - fh, bx + 2, bot, Render.fade(Render.lerpColor(RED, GREEN, frac), a));
            }

            if (names.getBool()) {
                String text = p.getName().getString() + "  " + Math.round(dist) + "m";
                float s = (float) Math.max(0.75, Math.min(1.15, 1.25 - dist / 90.0));
                float nx = Math.max(20, Math.min(w - 20, midX));
                float ny = Math.max(14, Math.min(h - 4, bMinY - 3));
                int tw = font.getWidth(text);

                Matrix3x2fStack m = ctx.getMatrices();
                m.pushMatrix();
                m.translate(nx, ny);
                m.scale(s, s);
                Render.roundRect(ctx, -tw / 2 - 3, -11, tw + 6, 11, Render.fade(Render.PANEL, a));
                ctx.drawText(font, text, -tw / 2, -9, Render.fade(color, a), true);
                m.popMatrix();
            }
        }

        states.values().removeIf(s -> s.frame != frameId);
    }

    /**
     * One box edge: clipped against the near plane, projected, folded into the 2D bounds,
     * and drawn when ctx != null.
     */
    private void edge(DrawContext ctx, int i, int j, double scale, int w, int h, int color, float thickness) {
        double ax = cx[i], ay = cy[i], az = cz[i];
        double bx = cx[j], by = cy[j], bz = cz[j];
        if (az < NEAR && bz < NEAR) return;
        if (az < NEAR) {
            double t = (NEAR - az) / (bz - az);
            ax += (bx - ax) * t; ay += (by - ay) * t; az = NEAR;
        } else if (bz < NEAR) {
            double t = (NEAR - bz) / (az - bz);
            bx += (ax - bx) * t; by += (ay - by) * t; bz = NEAR;
        }
        float x1 = (float) (w / 2.0 + ax / az * scale), y1 = (float) (h / 2.0 - ay / az * scale);
        float x2 = (float) (w / 2.0 + bx / bz * scale), y2 = (float) (h / 2.0 - by / bz * scale);
        if (Math.abs(x1) > 30000 || Math.abs(y1) > 30000 || Math.abs(x2) > 30000 || Math.abs(y2) > 30000) return;

        bValid = true;
        bMinX = Math.min(bMinX, Math.min(x1, x2));
        bMaxX = Math.max(bMaxX, Math.max(x1, x2));
        bMinY = Math.min(bMinY, Math.min(y1, y2));
        bMaxY = Math.max(bMaxY, Math.max(y1, y2));

        if (ctx != null) Render.line(ctx, x1, y1, x2, y2, color, thickness);
    }

    /** Smooth red -> yellow -> green by distance (red under ~10 m, green past ~90 m). */
    private static int colorFor(double dist) {
        float t = (float) Math.max(0, Math.min(1, (dist - 10) / 80.0));
        return t < 0.5f ? Render.lerpColor(RED, YELLOW, t * 2f) : Render.lerpColor(YELLOW, GREEN, (t - 0.5f) * 2f);
    }
}
