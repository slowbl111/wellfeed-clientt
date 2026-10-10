package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class Fly extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", 0.5, 0.1, 2.0, 0.1));

    public Fly() {
        super("Fly", "Flyv frit. Space = op, Shift = ned.", Category.MOVEMENT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        double yaw = Math.toRadians(p.getYaw());
        double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
        double leftX = Math.cos(yaw), leftZ = Math.sin(yaw);

        double mx = 0, mz = 0, my = 0;
        if (mc.options.forwardKey.isPressed()) { mx += forwardX; mz += forwardZ; }
        if (mc.options.backKey.isPressed())    { mx -= forwardX; mz -= forwardZ; }
        if (mc.options.leftKey.isPressed())    { mx += leftX;    mz += leftZ; }
        if (mc.options.rightKey.isPressed())   { mx -= leftX;    mz -= leftZ; }

        double s = speed.get();
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0) { mx = mx / len * s; mz = mz / len * s; }

        if (mc.options.jumpKey.isPressed())  my += s;
        if (mc.options.sneakKey.isPressed()) my -= s;

        p.setVelocity(mx, my, mz);
    }
}
