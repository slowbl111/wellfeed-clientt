package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.setting.NumSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFish extends Module {
    private final BoolSetting recast = add(new BoolSetting("Auto Recast", true));
    private final NumSetting recastDelay = add(new NumSetting("Recast Delay", 5, 40, 1, 15));
    private boolean waitingToRecast;
    private int timer;

    public AutoFish() {
        super("AutoFish", "Reels in when a fish bites and casts again. Hold a fishing rod.", Category.AUTOMATION);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (mc.currentScreen != null || !p.getMainHandStack().isOf(Items.FISHING_ROD)) {
            return;
        }
        FishingBobberEntity hook = p.fishHook;

        if (waitingToRecast) {
            if (--timer <= 0) {
                waitingToRecast = false;
                if (recast.value && hook == null) {
                    use(p);
                }
            }
            return;
        }

        // A bite pulls the bobber sharply downward.
        if (hook != null && hook.age > 40 && hook.isTouchingWater() && hook.getVelocity().y < -0.12) {
            use(p);
            waitingToRecast = true;
            timer = recastDelay.asInt();
        }
    }

    private void use(ClientPlayerEntity p) {
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);
    }

    @Override
    protected void onDisable() {
        waitingToRecast = false;
    }
}
