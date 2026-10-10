package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import net.minecraft.client.network.ClientPlayerEntity;

public class Sprint extends Module {
    private final BoolSetting hungerSafe = add(new BoolSetting("Hunger Safe", true));

    public Sprint() {
        super("Sprint", "Keeps you sprinting while you walk forward.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (!mc.options.forwardKey.isPressed() || p.isSneaking() || p.isUsingItem() || p.horizontalCollision) {
            return;
        }
        if (hungerSafe.value && p.getHungerManager().getFoodLevel() <= 6 && !p.getAbilities().allowFlying) {
            return;
        }
        p.setSprinting(true);
    }
}
