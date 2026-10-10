package com.wellfeed.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public final class ToolUtil {
    private ToolUtil() {
    }

    /** Hotbar slot (0-8) that mines the given block fastest; the current slot when nothing is better. */
    public static int bestSlot(BlockState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerInventory inv = mc.player.getInventory();
        int best = inv.getSelectedSlot();
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float speed = inv.getStack(i).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed + 0.01f) {
                bestSpeed = speed;
                best = i;
            }
        }
        return best;
    }
}
