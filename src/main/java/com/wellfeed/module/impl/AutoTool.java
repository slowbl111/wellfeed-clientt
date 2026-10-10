package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.util.ToolUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", true));
    private int previous = -1;

    public AutoTool() {
        super("AutoTool", "Switches to the fastest hotbar tool while you break a block.", Category.AUTOMATION);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) {
            return;
        }
        PlayerInventory inv = mc.player.getInventory();
        if (mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.world.getBlockState(hit.getBlockPos());
            int best = ToolUtil.bestSlot(state);
            if (best != inv.getSelectedSlot()) {
                if (previous < 0) {
                    previous = inv.getSelectedSlot();
                }
                inv.setSelectedSlot(best);
            }
        } else if (previous >= 0) {
            if (switchBack.value) {
                inv.setSelectedSlot(previous);
            }
            previous = -1;
        }
    }

    @Override
    protected void onDisable() {
        previous = -1;
    }
}
