package dk.wellfeed.module;

import dk.wellfeed.module.impl.Fly;
import dk.wellfeed.module.impl.Fullbright;
import dk.wellfeed.module.impl.Sprint;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        // Tilføj nye moduler her:
        MODULES.add(new Fly());
        MODULES.add(new Sprint());
        MODULES.add(new Fullbright());
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }
}
