package dk.wellfeed.module;

import dk.wellfeed.Config;
import dk.wellfeed.module.impl.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final boolean[] BIND_PREV = new boolean[GLFW.GLFW_KEY_LAST + 1];

    public static void init() {
        // Movement
        MODULES.add(new Fly());
        MODULES.add(new Sprint());
        MODULES.add(new ElytraGlide());
        MODULES.add(new AutoWalk());
        // Misc
        MODULES.add(new AutoEat());
        MODULES.add(new AutoTool());
        MODULES.add(new AutoFirework());
        MODULES.add(new AutoMine());
        MODULES.add(new AutoLog());
        MODULES.add(new AutoRespawn());
        MODULES.add(new AntiAfk());
        // Render
        MODULES.add(new Hud());
        MODULES.add(new Fullbright());

        Config.load();
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.getCategory() == category) out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) {
            if (type.isInstance(m)) return (T) m;
        }
        return null;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }

    // Tjekker keybinds. allowTrigger er false når en menu/chat er åben.
    public static void pollBinds(MinecraftClient mc, boolean allowTrigger) {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();

        boolean[] down = new boolean[MODULES.size()];
        for (int i = 0; i < MODULES.size(); i++) {
            int k = MODULES.get(i).getKey();
            if (k < GLFW.GLFW_KEY_SPACE || k > GLFW.GLFW_KEY_LAST) continue;
            down[i] = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
            if (allowTrigger && down[i] && !BIND_PREV[k]) {
                MODULES.get(i).toggle();
            }
        }
        for (int i = 0; i < MODULES.size(); i++) {
            int k = MODULES.get(i).getKey();
            if (k < GLFW.GLFW_KEY_SPACE || k > GLFW.GLFW_KEY_LAST) continue;
            BIND_PREV[k] = down[i];
        }
    }

    // Panic: slukker alle moduler (inkl. HUD). Skjuler eller sletter ingenting.
    public static void disableAll() {
        for (Module m : MODULES) {
            m.setEnabled(false);
        }
    }
}
