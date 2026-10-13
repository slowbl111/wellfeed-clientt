package dev.wellfeed.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    /** Shown in the on-screen list of enabled modules. */
    public final boolean listed;
    /** Remembered as on/off between game launches. */
    public final boolean persistEnabled;
    /** Active only while its key is held (Zoom) instead of toggling. */
    public final boolean holdToUse;
    public final int defaultKey;
    public final List<Setting> settings = new ArrayList<>();

    /** Filled in by WellfeedClient for modules that have a key. */
    public KeyBinding key;

    private boolean enabled;

    protected Module(String name, String description, Category category, boolean defaultOn, boolean listed,
                     boolean persistEnabled, boolean holdToUse, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultOn;
        this.listed = listed;
        this.persistEnabled = persistEnabled;
        this.holdToUse = holdToUse;
        this.defaultKey = defaultKey;
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

    /** Used when loading config: sets the flag without running onEnable/onDisable. */
    public void setEnabledSilently(boolean on) { enabled = on; }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Called every client tick while enabled and in a world. */
    public void onTick(MinecraftClient mc) {}

    /** Called every frame while enabled and in a world. */
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {}
}
