package dev.wellfeed;

import dev.wellfeed.gui.ClickGuiScreen;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.util.Config;
import dev.wellfeed.util.Render;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class WellfeedClient implements ClientModInitializer {
    public static final String VERSION = "4.0";

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("wellfeed", "main"));

    private static KeyBinding guiKey;

    public static KeyBinding guiKey() {
        return guiKey;
    }

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        guiKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.wellfeed.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        for (Module m : ModuleManager.MODULES) {
            if (m.defaultKey == GLFW.GLFW_KEY_UNKNOWN) continue;
            m.key = KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.wellfeed." + m.name.toLowerCase(), m.defaultKey, CATEGORY));
        }

        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            dev.wellfeed.modules.FreecamModule.INSTANCE.suppressInput(mc);
        });
        ClientTickEvents.END_CLIENT_TICK.register(WellfeedClient::tick);
        HudRenderCallback.EVENT.register(WellfeedClient::hud);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Config.save());
    }

    private static void tick(MinecraftClient mc) {
        while (guiKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        }

        boolean inWorld = mc.player != null && mc.world != null;

        for (Module m : ModuleManager.MODULES) {
            if (m.key == null) continue;
            if (m.holdToUse) {
                while (m.key.wasPressed()) { /* hold modules follow the key state, see hud() */ }
                continue;
            }
            while (m.key.wasPressed()) {
                if (inWorld) m.toggle();
            }
        }

        if (!inWorld) return;
        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }

    private static void hud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean inWorld = mc.player != null && mc.world != null;

        // Hold-to-use modules (Zoom) follow the physical key every frame for instant response.
        for (Module m : ModuleManager.MODULES) {
            if (m.holdToUse && m.key != null) {
                m.setEnabled(inWorld && mc.currentScreen == null && m.key.isPressed());
            }
        }

        if (!inWorld || mc.options.hudHidden) return;

        float delta = tickCounter.getTickProgress(false);

        drawWatermark(ctx, mc);

        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) m.onHud(ctx, delta, mc);
        }
    }

    /** Wellfeed tag plus a compact list of the modules that are currently on. */
    private static void drawWatermark(DrawContext ctx, MinecraftClient mc) {
        var font = mc.textRenderer;
        String title = "Wellfeed";
        int tw = font.getWidth(title);
        int w = tw + font.getWidth(" v" + VERSION) + 14;
        Render.shadow(ctx, 6, 6, w, 15, 3, 1f);
        Render.roundRect(ctx, 6, 6, w, 15, 3, Render.PANEL);
        ctx.fill(8, 9, 10, 18, Render.CREAM);
        ctx.drawText(font, title, 14, 10, Render.CREAM, true);
        ctx.drawText(font, " v" + VERSION, 14 + tw, 10, Render.CREAM_DIM, false);

        int y = 24;
        for (Module m : ModuleManager.MODULES) {
            if (!m.listed || !m.isEnabled()) continue;
            int mw = font.getWidth(m.name) + 12;
            Render.roundRect(ctx, 6, y, mw, 12, 2, Render.alpha(Render.PANEL, 0.85f));
            ctx.fill(8, y + 3, 9, y + 9, Render.CREAM_SOFT);
            ctx.drawText(font, m.name, 12, y + 2, Render.CREAM_SOFT, false);
            y += 13;
        }
    }
}
