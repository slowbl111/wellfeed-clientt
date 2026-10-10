package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.setting.MultiSetting;
import com.wellfeed.setting.NumSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Harvests ripe wheat, carrots and potatoes in reach and replants from your hotbar. */
public class AutoFarm extends Module {
    private final MultiSetting crops = add(new MultiSetting("Crops",
            new String[]{"Wheat", "Carrot", "Potato"}, "Wheat", "Carrot", "Potato"));
    private final NumSetting range = add(new NumSetting("Range", 1, 4.5, 0.5, 4.0));
    private final BoolSetting replant = add(new BoolSetting("Replant", true));

    private int cooldown;
    private int previousSlot = -1;

    public AutoFarm() {
        super("AutoFarm", "Harvests ripe crops around you and replants them.", Category.AUTOMATION);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (mc.currentScreen != null || mc.interactionManager == null) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Vec3d eye = p.getEyePos();
        double max = range.value * range.value;
        int r = (int) Math.ceil(range.value);
        BlockPos base = p.getBlockPos();

        // 1) Harvest the nearest ripe crop.
        BlockPos harvest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(base.add(-r, -r, -r), base.add(r, r + 1, r))) {
            BlockState st = mc.world.getBlockState(pos);
            if (st.getBlock() instanceof CropBlock crop && crop.isMature(st) && cropWanted(st.getBlock())) {
                double d = eye.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (d <= max && d < bestDist) {
                    bestDist = d;
                    harvest = pos.toImmutable();
                }
            }
        }
        if (harvest != null) {
            mc.interactionManager.attackBlock(harvest, Direction.UP);
            p.swingHand(Hand.MAIN_HAND);
            cooldown = 2;
            return;
        }

        // 2) Replant empty farmland.
        if (replant.value) {
            PlayerInventory inv = p.getInventory();
            int seedSlot = seedSlot(inv);
            if (seedSlot >= 0) {
                for (BlockPos pos : BlockPos.iterate(base.add(-r, -r - 1, -r), base.add(r, r, r))) {
                    if (!mc.world.getBlockState(pos).isOf(Blocks.FARMLAND)
                            || !mc.world.getBlockState(pos.up()).isAir()) {
                        continue;
                    }
                    Vec3d top = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (eye.squaredDistanceTo(top) > max) {
                        continue;
                    }
                    if (previousSlot < 0) {
                        previousSlot = inv.getSelectedSlot();
                    }
                    inv.setSelectedSlot(seedSlot);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND,
                            new BlockHitResult(top, Direction.UP, pos.toImmutable(), false));
                    p.swingHand(Hand.MAIN_HAND);
                    cooldown = 2;
                    return;
                }
            }
        }

        // Nothing to do: put the original item back.
        if (previousSlot >= 0) {
            p.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }

    private boolean cropWanted(Block b) {
        if (b == Blocks.WHEAT) {
            return crops.has("Wheat");
        }
        if (b == Blocks.CARROTS) {
            return crops.has("Carrot");
        }
        if (b == Blocks.POTATOES) {
            return crops.has("Potato");
        }
        return false;
    }

    private int seedSlot(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            Item it = inv.getStack(i).getItem();
            if ((crops.has("Wheat") && it == Items.WHEAT_SEEDS)
                    || (crops.has("Carrot") && it == Items.CARROT)
                    || (crops.has("Potato") && it == Items.POTATO)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDisable() {
        if (previousSlot >= 0 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }
}
