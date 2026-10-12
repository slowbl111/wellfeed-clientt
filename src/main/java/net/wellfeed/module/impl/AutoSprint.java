package net.wellfeed.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.wellfeed.module.Module;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Sprints automatically while you walk forward.", Category.MOVEMENT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        boolean forward = mc.options.forwardKey.isPressed();
        if (forward && !p.isSneaking() && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
