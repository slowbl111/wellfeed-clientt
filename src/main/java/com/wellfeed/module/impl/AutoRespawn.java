package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.NumSetting;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    private final NumSetting delay = add(new NumSetting("Delay", 0, 40, 1, 10));
    private int waited;

    public AutoRespawn() {
        super("AutoRespawn", "Respawns automatically after you die.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen instanceof DeathScreen) {
            if (++waited >= delay.asInt()) {
                mc.player.requestRespawn();
                waited = 0;
            }
        } else {
            waited = 0;
        }
    }
}
