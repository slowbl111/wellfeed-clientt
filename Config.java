package dev.wellfeed.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.ModuleManager;
import dev.wellfeed.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Saves module settings and GUI panel positions to config/wellfeed-v4.json. Best effort, never throws. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Panel top-left positions by category label, shared with the GUI. */
    public static final Map<String, int[]> PANELS = new HashMap<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("wellfeed-v4.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;

            JsonObject mods = obj(root, "modules");
            if (mods != null) {
                for (Module m : ModuleManager.MODULES) {
                    JsonObject mo = obj(mods, m.name);
                    if (mo == null) continue;
                    if (m.persistEnabled && mo.has("enabled")) m.setEnabledSilently(mo.get("enabled").getAsBoolean());
                    JsonObject so = obj(mo, "settings");
                    if (so == null) continue;
                    for (Setting s : m.settings) {
                        JsonElement e = so.get(s.name);
                        if (e == null) continue;
                        switch (s.type) {
                            case BOOL -> s.setBool(e.getAsBoolean());
                            case NUM -> s.setNum(e.getAsDouble());
                            case MODE -> s.setMode(e.getAsInt());
                        }
                    }
                }
            }
            JsonObject panels = obj(root, "panels");
            if (panels != null) {
                for (Map.Entry<String, JsonElement> e : panels.entrySet()) {
                    var arr = e.getValue().getAsJsonArray();
                    PANELS.put(e.getKey(), new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()});
                }
            }
        } catch (Exception ignored) {
            // A broken config file must never stop the game from starting.
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.MODULES) {
                JsonObject mo = new JsonObject();
                if (m.persistEnabled) mo.addProperty("enabled", m.isEnabled());
                JsonObject so = new JsonObject();
                for (Setting s : m.settings) {
                    switch (s.type) {
                        case BOOL -> so.addProperty(s.name, s.getBool());
                        case NUM -> so.addProperty(s.name, s.getNum());
                        case MODE -> so.addProperty(s.name, s.getMode());
                    }
                }
                mo.add("settings", so);
                mods.add(m.name, mo);
            }
            root.add("modules", mods);

            JsonObject panels = new JsonObject();
            for (Map.Entry<String, int[]> e : PANELS.entrySet()) {
                var arr = new com.google.gson.JsonArray();
                arr.add(e.getValue()[0]);
                arr.add(e.getValue()[1]);
                panels.add(e.getKey(), arr);
            }
            root.add("panels", panels);

            Files.writeString(path(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private Config() {}
}
