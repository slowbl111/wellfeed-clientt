package net.wellfeed.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.GameMode;
import net.wellfeed.module.Module;
import net.wellfeed.module.NumberSetting;

public class Fly extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 2.0, 0.5, 10.0));

    public Fly() {
        super("Fly", "Creative-style flight. Best in singleplayer or on servers that allow it.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        apply(mc.player, true);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        p.getAbilities().setFlySpeed(0.05f);
        boolean keep = mc.interactionManager != null
                && (mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE
                || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR);
        if (!keep) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        apply(mc.player, false);
    }

    private void apply(ClientPlayerEntity p, boolean first) {
        if (p == null) return;
        p.getAbilities().allowFlying = true;
        p.getAbilities().setFlySpeed((float) (0.05 * speed.get()));
        if (first && !p.isOnGround()) p.getAbilities().flying = true;
    }
}
