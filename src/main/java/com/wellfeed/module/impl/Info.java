package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;

/** Drawn by HudRenderer; this class only holds the on/off state and options. */
public class Info extends Module {
    public final BoolSetting coords = add(new BoolSetting("Coordinates", true));
    public final BoolSetting direction = add(new BoolSetting("Direction", true));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));

    public Info() {
        super("Info", "Shows coordinates, facing and FPS in the top-left corner.", Category.RENDER);
    }
}
