package dev.wellfeed.module;

import dev.wellfeed.modules.*;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        // HUD
        MODULES.add(KeystrokesModule.INSTANCE);
        MODULES.add(ArmorHudModule.INSTANCE);
        MODULES.add(PotionHudModule.INSTANCE);
        // Render
        MODULES.add(EspModule.INSTANCE);
        MODULES.add(FullbrightModule.INSTANCE);
        // Movement
        MODULES.add(FlyModule.INSTANCE);
        // Combat
        MODULES.add(AutoTotemModule.INSTANCE);
    }

    private ModuleManager() {}
}
