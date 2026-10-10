package dk.wellfeed;

import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import dk.wellfeed.module.impl.Hud;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

// Gemmer keybinds, indstillinger og om HUD er tændt i
// .minecraft/config/wellfeed-client.properties
public class Config {
    private static boolean loading = false;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("wellfeed-client.properties");
    }

    private static String id(Module m) {
        return m.getName().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static void load() {
        loading = true;
        try {
            Hud hud = ModuleManager.get(Hud.class);
            if (hud != null) hud.setEnabled(true); // standard

            Path file = path();
            if (!Files.exists(file)) return;

            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                return;
            }

            for (Module m : ModuleManager.getModules()) {
                String key = p.getProperty("bind." + id(m));
                if (key != null) {
                    try { m.setKeyQuiet(Integer.parseInt(key.trim())); } catch (NumberFormatException ignored) {}
                }
                String val = p.getProperty("setting." + id(m));
                if (val != null && m.getSetting() != null) {
                    try { m.getSetting().set(Double.parseDouble(val.trim())); } catch (NumberFormatException ignored) {}
                }
            }
            String hudOn = p.getProperty("enabled.hud");
            if (hudOn != null && hud != null) hud.setEnabled(Boolean.parseBoolean(hudOn.trim()));
        } finally {
            loading = false;
        }
    }

    public static void save() {
        if (loading) return;
        try {
            Properties p = new Properties();
            for (Module m : ModuleManager.getModules()) {
                if (m.getKey() != -1) p.setProperty("bind." + id(m), String.valueOf(m.getKey()));
                if (m.getSetting() != null) p.setProperty("setting." + id(m), String.valueOf(m.getSetting().get()));
            }
            Hud hud = ModuleManager.get(Hud.class);
            if (hud != null) p.setProperty("enabled.hud", String.valueOf(hud.isEnabled()));

            try (OutputStream out = Files.newOutputStream(path())) {
                p.store(out, "Wellfeed Client");
            }
        } catch (IOException ignored) {
            // Ikke kritisk hvis det ikke kan gemmes
        }
    }
}
