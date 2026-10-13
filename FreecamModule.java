package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam: the camera detaches from your player and flies around on its own, with smooth
 * acceleration. Your body stays where it is and ignores movement keys while this is on.
 *
 * Look direction is taken from the player's yaw/pitch (so the mouse works as normal) and is
 * restored when you turn Freecam off. CameraMixin applies the position every frame.
 */
public final class FreecamModule extends Module {
    public static final FreecamModule INSTANCE = new FreecamModule();

    private final Setting speed = add(Setting.num("Speed", 1.0, 0.2, 5.0, 0.2));
    private final Setting smooth = add(Setting.num("Smoothing", 8, 2, 30, 1));
    private final Setting body = add(Setting.bool("Show own body", true));

    private double x, y, z;
    private double vx, vy, vz;
    private float savedYaw, savedPitch;
    private long lastNano;

    private FreecamModule() {
        super("Freecam", "Detach the camera and fly around. Your body stays put.",
                Category.RENDER, false, true, false, false, GLFW.GLFW_KEY_Z);
    }

    /** True while the camera should be overridden. */
    public boolean active() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return isEnabled() && mc.player != null && mc.world != null;
    }

    public boolean showBody() {
        return body.getBool();
    }

    public Vec3d position() {
        return new Vec3d(x, y, z);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) {
            setEnabledSilently(false);
            return;
        }
        Vec3d eye = p.getEyePos();
        x = eye.x;
        y = eye.y;
        z = eye.z;
        vx = vy = vz = 0;
        savedYaw = p.getYaw();
        savedPitch = p.getPitch();
        lastNano = 0;
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p != null) {
            p.setYaw(savedYaw);
            p.setPitch(savedPitch);
        }
        // Give the keys back to the game in whatever state they are physically in.
        GameOptions o = mc.options;
        KeyBinding[] ks = {o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sneakKey, o.sprintKey};
        for (KeyBinding kb : ks) kb.setPressed(down(mc, kb));
    }

    /**
     * Runs at the start of every client tick, before vanilla reads the keys:
     * stops the player body from walking, jumping, attacking or using items.
     */
    public void suppressInput(MinecraftClient mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) {
            setEnabled(false);
            return;
        }
        GameOptions o = mc.options;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sneakKey.setPressed(false);
        o.sprintKey.setPressed(false);
        o.attackKey.setPressed(false);
        o.useKey.setPressed(false);
        while (o.attackKey.wasPressed()) { /* drain */ }
        while (o.useKey.wasPressed()) { /* drain */ }
        while (o.pickItemKey.wasPressed()) { /* drain */ }
    }

    /**
     * Advances the camera by one rendered frame (called from CameraMixin).
     * Returns {x, y, z, yaw, pitch}.
     */
    public double[] step(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (now - lastNano) / 1_000_000_000f);
        lastNano = now;

        float yaw = p.getYaw();
        float pitch = p.getPitch();

        double fwd = 0, side = 0, up = 0;
        boolean boost = false;
        if (mc.currentScreen == null) {
            GameOptions o = mc.options;
            fwd = (down(mc, o.forwardKey) ? 1 : 0) - (down(mc, o.backKey) ? 1 : 0);
            side = (down(mc, o.rightKey) ? 1 : 0) - (down(mc, o.leftKey) ? 1 : 0);
            up = (down(mc, o.jumpKey) ? 1 : 0) - (down(mc, o.sneakKey) ? 1 : 0);
            boost = down(mc, o.sprintKey);
        }

        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr), rz = -Math.sin(yr);

        double tx = fx * fwd + rx * side;
        double ty = fy * fwd + up;
        double tz = fz * fwd + rz * side;
        double len = Math.sqrt(tx * tx + ty * ty + tz * tz);
        double top = 10.0 * speed.getNum() * (boost ? 2.5 : 1.0);
        if (len > 1e-6) {
            tx = tx / len * top;
            ty = ty / len * top;
            tz = tz / len * top;
        }

        float k = 1f - (float) Math.exp(-dt * smooth.getNum());
        vx += (tx - vx) * k;
        vy += (ty - vy) * k;
        vz += (tz - vz) * k;
        x += vx * dt;
        y += vy * dt;
        z += vz * dt;

        return new double[]{x, y, z, yaw, pitch};
    }

    /** Reads the physical key state, since the game's own pressed flags are being held at false. */
    private static boolean down(MinecraftClient mc, KeyBinding kb) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(kb);
        if (key.getCategory() != InputUtil.Type.KEYSYM || key.getCode() == GLFW.GLFW_KEY_UNKNOWN) return false;
        return InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        String s = "FREECAM";
        int w = mc.textRenderer.getWidth(s) + 14;
        int x0 = ctx.getScaledWindowWidth() / 2 - w / 2;
        int y0 = 28;
        Render.roundRect(ctx, x0, y0, w, 14, 3, Render.PANEL);
        Render.textCentered(ctx, mc.textRenderer, s, x0 + w / 2, y0 + 3, Render.CREAM, false);
    }
}
