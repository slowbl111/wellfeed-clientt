package com.wellfeed.module;

import com.wellfeed.gui.Toasts;
import com.wellfeed.module.impl.AutoBuild;
import com.wellfeed.module.impl.AutoEat;
import com.wellfeed.module.impl.AutoFarm;
import com.wellfeed.module.impl.AutoFish;
import com.wellfeed.module.impl.AutoMine;
import com.wellfeed.module.impl.AutoRespawn;
import com.wellfeed.module.impl.AutoTool;
import com.wellfeed.module.impl.AutoWalk;
import com.wellfeed.module.impl.Fullbright;
import com.wellfeed.module.impl.Info;
import com.wellfeed.module.impl.MobESP;
import com.wellfeed.module.impl.Sneak;
import com.wellfeed.module.impl.Sprint;
import com.wellfeed.module.impl.Zoom;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> all = new ArrayList<>();

    public ModuleManager() {
        // Movement
        all.add(new Sprint());
        all.add(new AutoWalk());
        all.add(new Sneak());
        // Render
        all.add(new Fullbright());
        all.add(new Zoom());
        all.add(new MobESP());
        all.add(new Info());
        // Automation
        all.add(new AutoMine());
        all.add(new AutoBuild());
        all.add(new AutoFarm());
        all.add(new AutoFish());
        all.add(new AutoEat());
        all.add(new AutoTool());
        // Misc
        all.add(new AutoRespawn());
    }

    public List<Module> all() {
        return all;
    }

    public List<Module> in(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) {
            if (m.category == category) {
                out.add(m);
            }
        }
        return out;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : all) {
            if (type.isInstance(m)) {
                return type.cast(m);
            }
        }
        return null;
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Left the world: let modules clean up, they re-enable when you rejoin.
        if (mc.player == null || mc.world == null) {
            for (Module m : all) {
                if (m.active) {
                    try {
                        m.onDisable();
                    } catch (Throwable ignored) {
                    }
                    m.active = false;
                }
            }
            return;
        }

        for (Module m : all) {
            if (m.enabled && !m.active) {
                m.active = true;
                m.onEnable();
            } else if (!m.enabled && m.active) {
                m.active = false;
                m.onDisable();
            }
            if (m.active) {
                m.onTick();
            }
        }

        // Module hotkeys (polled so no input mixin is needed).
        long handle = mc.getWindow().getHandle();
        for (Module m : all) {
            if (m.key < 0) {
                continue;
            }
            boolean down = GLFW.glfwGetKey(handle, m.key) == GLFW.GLFW_PRESS;
            if (down && !m.keyDown && mc.currentScreen == null) {
                m.toggle();
                Toasts.push(m.name + (m.enabled ? " enabled" : " disabled"));
            }
            m.keyDown = down;
        }
    }

    /** Called when the game closes. */
    public void shutdown() {
        for (Module m : all) {
            if (m.active) {
                try {
                    m.onDisable();
                } catch (Throwable ignored) {
                }
                m.active = false;
            }
        }
    }
}
