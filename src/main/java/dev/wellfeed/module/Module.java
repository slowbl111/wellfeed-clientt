package dev.wellfeed.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    /** Shown in the on-screen enabled-modules list. */
    public final boolean listed;
    public final List<Setting> settings = new ArrayList<>();

    private boolean enabled;

    protected Module(String name, String description, boolean defaultOn, boolean listed) {
        this.name = name;
        this.description = description;
        this.enabled = defaultOn;
        this.listed = listed;
    }

    protected Setting add(Setting s) {
        settings.add(s);
        return s;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Called at the START of every client tick (before input is handled) while enabled and in a world. */
    public void onPreTick(MinecraftClient mc) {}

    /** Called every client tick while enabled and in a world. */
    public void onTick(MinecraftClient mc) {}

    /** Called every frame while enabled and in a world. */
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {}
}
