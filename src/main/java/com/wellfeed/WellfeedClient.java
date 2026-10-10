package com.wellfeed;

import com.wellfeed.gui.ClickGuiScreen;
import com.wellfeed.gui.HudRenderer;
import com.wellfeed.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WellfeedClient implements ClientModInitializer {
    public static final String ID = "wellfeed";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    public static KeyBinding menuKey;
    public static ModuleManager modules;

    private static int tickCounter;
    private static int lastClosed = -100;

    /** Called by the menu when it closes, so the same key press cannot reopen it. */
    public static void markClosed() {
        lastClosed = tickCounter;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        Config.load(modules);

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wellfeed.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of(ID, "main"))
        ));

        HudRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            modules.tick();
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null && tickCounter - lastClosed > 6) {
                    client.setScreen(new ClickGuiScreen(modules));
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            modules.shutdown();
            Config.save(modules);
        });

        LOGGER.info("Wellfeed loaded");
    }
}
