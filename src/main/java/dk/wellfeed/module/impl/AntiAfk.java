package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AntiAfk extends Module {
    private final NumberSetting seconds = addSetting(new NumberSetting("Seconds", 20, 5, 120, 5));

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
        if (timer < (int) (seconds.get() * 20)) return;
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
