package dev.wellfeed.module;

import dev.wellfeed.modules.*;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        // Render
        MODULES.add(EspModule.INSTANCE);
        MODULES.add(FreecamModule.INSTANCE);
        MODULES.add(FullbrightModule.INSTANCE);
        MODULES.add(ZoomModule.INSTANCE);
        // Movement
        MODULES.add(FlyModule.INSTANCE);
        // HUD
        MODULES.add(KeystrokesModule.INSTANCE);
        MODULES.add(KeybindListModule.INSTANCE);
        MODULES.add(ArmorHudModule.INSTANCE);
        MODULES.add(PotionHudModule.INSTANCE);
        MODULES.add(InfoHudModule.INSTANCE);
    }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    private ModuleManager() {}
}
