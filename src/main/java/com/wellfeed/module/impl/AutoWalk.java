package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Holds the forward key for you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen == null) {
            mc.options.forwardKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.forwardKey.setPressed(false);
    }
}
