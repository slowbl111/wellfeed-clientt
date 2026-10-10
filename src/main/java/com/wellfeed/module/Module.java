package com.wellfeed.module;

import com.wellfeed.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    /** Desired state (what the GUI and config change). */
    public boolean enabled;
    /** GLFW key code, or -1 when unbound. */
    public int key = -1;

    /** Managed by ModuleManager: whether onEnable has run. */
    public boolean active;
    /** Managed by ModuleManager: edge detection for hotkeys. */
    public boolean keyDown;

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        enabled = !enabled;
    }

    /** Short text shown next to the module name in the HUD list. */
    public String tag() {
        return "";
    }

    protected void onEnable() {
    }

    /** Must tolerate a null player/world: it also runs when leaving a world. */
    protected void onDisable() {
    }

    /** Called every client tick while active and in a world. */
    public void onTick() {
    }
}
