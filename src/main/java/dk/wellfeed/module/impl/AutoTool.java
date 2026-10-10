package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    public AutoTool() {
        super("Auto Tool", "Vælger det hurtigste værktøj i hotbar når du graver.", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        PlayerInventory inv = p.getInventory();

        int current = inv.getSelectedSlot();
        int best = current;
        float bestSpeed = inv.getStack(current).getMiningSpeedMultiplier(state);

        for (int i = 0; i < 9; i++) {
            float speed = inv.getStack(i).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        if (best != current) inv.setSelectedSlot(best);
    }
}
