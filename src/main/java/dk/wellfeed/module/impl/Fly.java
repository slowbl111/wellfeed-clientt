package dk.wellfeed.module.impl;

import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class Fly extends Module {
    private static final double SPEED = 0.5; // blokke pr. tick

    public Fly() {
        super("Fly", "Flyv frit. Space = op, Shift = ned.");
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

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0) { mx = mx / len * SPEED; mz = mz / len * SPEED; }

        if (mc.options.jumpKey.isPressed())  my += SPEED;
        if (mc.options.sneakKey.isPressed()) my -= SPEED;

        p.setVelocity(mx, my, mz);
    }
}
