package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Surprise #2: a slim info bar at the top: coordinates, facing, speed, FPS and ping. */
public final class InfoHudModule extends Module {
    public static final InfoHudModule INSTANCE = new InfoHudModule();

    private final Setting coords = add(Setting.bool("Coordinates", true));
    private final Setting facing = add(Setting.bool("Facing", true));
    private final Setting speed = add(Setting.bool("Speed (blocks/s)", true));
    private final Setting fps = add(Setting.bool("FPS", true));
    private final Setting ping = add(Setting.bool("Ping", true));

    private double lastX, lastZ;
    private boolean haveLast;
    private float bps;

    private InfoHudModule() {
        super("InfoHUD", "Coordinates, facing, speed, FPS and ping in a slim bar.",
                Category.HUD, true, false, true, false, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (haveLast) {
            double dx = p.getX() - lastX, dz = p.getZ() - lastZ;
            float inst = (float) Math.sqrt(dx * dx + dz * dz) * 20f;
            bps += (inst - bps) * 0.2f;
        }
        lastX = p.getX();
        lastZ = p.getZ();
        haveLast = true;
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        List<String> parts = new ArrayList<>();
        if (coords.getBool()) {
            parts.add(String.format(Locale.ROOT, "%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ()));
        }
        if (facing.getBool()) parts.add(cardinal(p.getYaw()));
        if (speed.getBool()) parts.add(String.format(Locale.ROOT, "%.1f b/s", bps));
        if (fps.getBool()) parts.add(mc.getCurrentFps() + " fps");
        if (ping.getBool()) {
            int ms = -1;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (e != null) ms = e.getLatency();
            }
            if (ms >= 0) parts.add(ms + " ms");
        }
        if (parts.isEmpty()) return;

        String text = String.join("   |   ", parts);
        int w = mc.textRenderer.getWidth(text) + 16;
        int x = ctx.getScaledWindowWidth() / 2 - w / 2;
        int y = 6;
        Render.shadow(ctx, x, y, w, 15, 3, 1f);
        Render.roundRect(ctx, x, y, w, 15, 3, Render.PANEL);
        Render.textCentered(ctx, mc.textRenderer, text, x + w / 2, y + 4, Render.CREAM, false);
    }

    private static String cardinal(float yaw) {
        float y = ((yaw % 360f) + 360f) % 360f;
        String[] d = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return d[Math.round(y / 45f) % 8];
    }
}
