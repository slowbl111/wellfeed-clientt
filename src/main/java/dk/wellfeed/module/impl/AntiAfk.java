package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AntiAfk extends Module {
    private static final int INTERVAL_TICKS = 400; // ca. hvert 20. sekund

    private int timer = 0;
    private boolean releasePending = false;
    private boolean turnRight = true;

    public AntiAfk() {
        super("Anti AFK", "Hopper og drejer lidt med jævne mellemrum.", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        if (releasePending) {
            mc.options.jumpKey.setPressed(false);
            releasePending = false;
        }

        timer++;
        if (timer < INTERVAL_TICKS) return;
        timer = 0;

        if (mc.currentScreen != null) return;

        p.setYaw(p.getYaw() + (turnRight ? 4.0f : -4.0f));
        turnRight = !turnRight;

        if (p.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
            releasePending = true;
        }
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (releasePending) {
            mc.options.jumpKey.setPressed(false);
            releasePending = false;
        }
        timer = 0;
    }
}
