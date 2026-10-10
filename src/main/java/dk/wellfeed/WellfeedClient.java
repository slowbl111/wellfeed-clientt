package dk.wellfeed;

import dk.wellfeed.gui.ClickGui;
import dk.wellfeed.gui.HudOverlay;
import dk.wellfeed.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class WellfeedClient implements ClientModInitializer {
    public static final String NAME = "Wellfeed Client";

    // Tast der åbner menuen
    public static final int MENU_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;
    // Panic: slukker alle moduler med det samme
    public static final int PANIC_KEY = GLFW.GLFW_KEY_END;

    private boolean menuWasDown = false;
    private boolean panicWasDown = false;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.of("wellfeedclient", "hud"),
                (context, tickCounter) -> HudOverlay.render(context));
    }

    private void onTick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();

        boolean menuDown = GLFW.glfwGetKey(handle, MENU_KEY) == GLFW.GLFW_PRESS;
        if (menuDown && !menuWasDown && mc.currentScreen == null && mc.player != null) {
            mc.setScreen(new ClickGui());
        }
        menuWasDown = menuDown;

        boolean panicDown = GLFW.glfwGetKey(handle, PANIC_KEY) == GLFW.GLFW_PRESS;
        if (panicDown && !panicWasDown) {
            ModuleManager.disableAll();
            if (mc.currentScreen instanceof ClickGui) mc.setScreen(null);
        }
        panicWasDown = panicDown;

        ModuleManager.pollBinds(mc, mc.currentScreen == null && mc.player != null);
        ModuleManager.tick(mc);
    }
}
