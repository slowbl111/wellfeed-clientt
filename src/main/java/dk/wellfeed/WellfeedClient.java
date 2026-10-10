package dk.wellfeed;

import dk.wellfeed.gui.ClickGui;
import dk.wellfeed.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class WellfeedClient implements ClientModInitializer {
    public static final String NAME = "Wellfeed Client";
    // Tast der åbner menuen. Skift f.eks. til GLFW.GLFW_KEY_INSERT hvis du vil.
    public static final int MENU_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private boolean keyWasDown = false;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;

        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), MENU_KEY) == GLFW.GLFW_PRESS;
        if (down && !keyWasDown && mc.currentScreen == null && mc.player != null) {
            mc.setScreen(new ClickGui());
        }
        keyWasDown = down;

        ModuleManager.tick(mc);
    }
}
