package dk.wellfeed.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    private final String name;
    private final String description;
    private boolean enabled;

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (value) onEnable(mc); else onDisable(mc);
    }

    protected void onEnable(MinecraftClient mc) {}
    protected void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
}
