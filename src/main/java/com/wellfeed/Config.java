package com.wellfeed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wellfeed.module.Module;
import com.wellfeed.module.ModuleManager;
import com.wellfeed.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wellfeed.json");

    /** Index into Theme.NAMES. */
    public static int accent = 0;

    private Config() {
    }

    public static void load(ModuleManager mm) {
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("accent")) {
                accent = Math.max(0, Math.min(4, root.get("accent").getAsInt()));
            }
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods == null) {
                return;
            }
            for (Module m : mm.all()) {
                if (!mods.has(m.name)) {
                    continue;
                }
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) {
                    m.enabled = o.get("enabled").getAsBoolean();
                }
                if (o.has("key")) {
                    m.key = o.get("key").getAsInt();
                }
                JsonObject ss = o.getAsJsonObject("settings");
                if (ss != null) {
                    for (Setting s : m.settings) {
                        if (ss.has(s.name)) {
                            try {
                                s.load(ss.get(s.name));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            WellfeedClient.LOGGER.warn("Could not read wellfeed.json, using defaults", e);
        }
    }

    public static void save(ModuleManager mm) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("accent", accent);
            JsonObject mods = new JsonObject();
            for (Module m : mm.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("key", m.key);
                JsonObject ss = new JsonObject();
                for (Setting s : m.settings) {
                    ss.add(s.name, s.save());
                }
                o.add("settings", ss);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) {
            WellfeedClient.LOGGER.warn("Could not save wellfeed.json", e);
        }
    }
}
