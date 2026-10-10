package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;

public class AutoWalk extends Module {
    private boolean pressing = false;

    public AutoWalk() {
        super("Auto Walk", "Går fremad uden at du holder W nede.", Category.MOVEMENT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            mc.options.forwardKey.setPressed(true);
            pressing = true;
        } else if (pressing) {
            mc.options.forwardKey.setPressed(false);
            pressing = false;
        }
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (pressing) {
            mc.options.forwardKey.setPressed(false);
            pressing = false;
        }
    }
}
