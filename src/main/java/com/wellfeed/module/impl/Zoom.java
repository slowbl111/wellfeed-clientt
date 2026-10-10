package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.NumSetting;

public class Zoom extends Module {
    private final NumSetting fov = add(new NumSetting("FOV", 30, 70, 1, 30));
    private Integer previous;

    public Zoom() {
        super("Zoom", "Narrows your field of view while enabled. Bind it to a key to hold-and-toggle.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        previous = mc.options.getFov().getValue();
        mc.options.getFov().setValue(fov.asInt());
    }

    @Override
    public void onTick() {
        if (mc.options.getFov().getValue() != fov.asInt()) {
            mc.options.getFov().setValue(fov.asInt());
        }
    }

    @Override
    protected void onDisable() {
        if (previous != null) {
            mc.options.getFov().setValue(previous);
            previous = null;
        }
    }
}
