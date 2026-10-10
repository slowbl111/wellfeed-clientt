package dk.wellfeed.module;

import dk.wellfeed.Config;
import net.minecraft.client.MinecraftClient;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int key = -1; // -1 = ingen keybind
    private NumberSetting setting;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public int getKey() { return key; }

    public void setKey(int key) {
        this.key = key;
        Config.save();
    }

    // Kun brugt af Config når den indlæser (ingen gem undervejs).
    public void setKeyQuiet(int key) { this.key = key; }

    public NumberSetting getSetting() { return setting; }

    protected NumberSetting addSetting(NumberSetting s) {
        this.setting = s;
        return s;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (value) onEnable(mc); else onDisable(mc);
        Config.save();
    }

    protected void onEnable(MinecraftClient mc) {}
    protected void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
}
