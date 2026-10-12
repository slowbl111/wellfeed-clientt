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
        MODULES.add(FreecamModule.INSTANCE);
        MODULES.add(CinematicModule.INSTANCE);
        // Movement
        MODULES.add(FlyModule.INSTANCE);
        // Combat
        MODULES.add(AutoTotemModule.INSTANCE);
    }

    public static final String[] CATEGORIES = { "HUD", "Render", "Camera", "Movement", "Combat" };

    public static String categoryOf(Module m) {
        switch (m.name) {
            case "Keystrokes": case "ArmorHUD": case "PotionHUD": return "HUD";
            case "ESP": case "Fullbright": return "Render";
            case "Freecam": case "Cinematic": return "Camera";
            case "Fly": return "Movement";
            default: return "Combat";
        }
    }

    private ModuleManager() {}
}
