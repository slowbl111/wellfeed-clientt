package dev.wellfeed;

import dev.wellfeed.gui.ClickGuiScreen;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.modules.AutoTotemModule;
import dev.wellfeed.modules.CinematicModule;
import dev.wellfeed.modules.EspModule;
import dev.wellfeed.modules.FlyModule;
import dev.wellfeed.modules.FreecamModule;
import dev.wellfeed.modules.FullbrightModule;
import dev.wellfeed.util.CameraRig;
import dev.wellfeed.util.Render;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class WellfeedClient implements ClientModInitializer {
    public static final String VERSION = "4.0.0";

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("wellfeed", "main"));

    private static KeyBinding guiKey;
    private static final Map<KeyBinding, Module> BINDS = new LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        guiKey = register("gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
        bind(EspModule.INSTANCE, "esp", GLFW.GLFW_KEY_H);
        bind(FlyModule.INSTANCE, "fly", GLFW.GLFW_KEY_G);
        bind(FullbrightModule.INSTANCE, "fullbright", GLFW.GLFW_KEY_Z);
        bind(AutoTotemModule.INSTANCE, "autototem", GLFW.GLFW_KEY_J);
        bind(FreecamModule.INSTANCE, "freecam", GLFW.GLFW_KEY_N);
        bind(CinematicModule.INSTANCE, "cinematic", GLFW.GLFW_KEY_B);

        ClientTickEvents.START_CLIENT_TICK.register(WellfeedClient::preTick);
        ClientTickEvents.END_CLIENT_TICK.register(WellfeedClient::tick);
        HudRenderCallback.EVENT.register(WellfeedClient::hud);
    }

    /** Bound key of a module's toggle keybind (e.g. "N"), or "" if it has none. */
    public static String keyLabel(Module m) {
        for (Map.Entry<KeyBinding, Module> e : BINDS.entrySet()) {
            if (e.getValue() == m) return e.getKey().getBoundKeyLocalizedText().getString();
        }
        return "";
    }

    private static KeyBinding register(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.wellfeed." + id, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    private static void bind(Module m, String id, int key) {
        BINDS.put(register(id, key), m);
    }

    private static void preTick(MinecraftClient mc) {
        // Left the world or respawned while a detached camera was on: shut it down cleanly.
        if (CameraRig.active() && !CameraRig.valid(mc)) {
            FreecamModule.INSTANCE.setEnabled(false);
            CinematicModule.INSTANCE.setEnabled(false);
            CameraRig.stopNow();
        }
        if (mc.player == null || mc.world == null) return;
        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) m.onPreTick(mc);
        }
    }

    private static void tick(MinecraftClient mc) {
        while (guiKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        }
        for (Map.Entry<KeyBinding, Module> e : BINDS.entrySet()) {
            while (e.getKey().wasPressed()) e.getValue().toggle();
        }

        if (mc.player == null || mc.world == null) return;
        CameraRig.frame();   // fallback if the Camera mixin hook is unavailable (harmless otherwise)
        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }

    private static void hud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        float delta = tickCounter.getTickProgress(false);

        // Watermark + enabled modules list
        ctx.drawText(mc.textRenderer, "Wellfeed v4", 6, 6, Render.CREAM, true);
        int y = 18;
        for (Module m : ModuleManager.MODULES) {
            if (m.listed && m.isEnabled()) {
                ctx.drawText(mc.textRenderer, m.name, 6, y, Render.CREAM_SOFT, true);
                y += 10;
            }
        }

        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) m.onHud(ctx, delta, mc);
        }
    }
}
