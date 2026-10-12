package net.wellfeed.module;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        MOVEMENT("Movement"), WORLD("World"), RENDER("Render");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String name;
    public final String description;
    public final Category category;
    public final List<NumberSetting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (enabled) onEnable(mc);
        else onDisable(mc);
    }

    protected NumberSetting add(NumberSetting s) {
        settings.add(s);
        return s;
    }

    protected void onEnable(MinecraftClient mc) {}

    protected void onDisable(MinecraftClient mc) {}

    /** Called every client tick while enabled and in a world. */
    public void onTick(MinecraftClient mc) {}
}
