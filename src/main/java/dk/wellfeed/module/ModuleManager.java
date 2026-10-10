package dk.wellfeed.module;

import dk.wellfeed.module.impl.*;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        // Movement
        MODULES.add(new Fly());
        MODULES.add(new Sprint());
        MODULES.add(new ElytraGlide());
        // Misc
        MODULES.add(new AutoEat());
        MODULES.add(new AutoTool());
        MODULES.add(new AutoFirework());
        MODULES.add(new AutoMine());
        MODULES.add(new AutoLog());
        // Render
        MODULES.add(new Hud());
        MODULES.add(new Fullbright());

        get(Hud.class).setEnabled(true);
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
}
