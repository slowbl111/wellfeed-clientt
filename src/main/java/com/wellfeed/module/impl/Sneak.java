package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;

public class Sneak extends Module {
    public Sneak() {
        super("Sneak", "Holds the sneak key for you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen == null) {
            mc.options.sneakKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.sneakKey.setPressed(false);
    }
}
