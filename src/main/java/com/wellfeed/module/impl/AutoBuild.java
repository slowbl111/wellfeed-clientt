package com.wellfeed.module.impl;

import com.wellfeed.gui.Toasts;
import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.setting.ModeSetting;
import com.wellfeed.setting.NumSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Places a simple blueprint in front of you, block by block, from your hotbar.
 * Only places against existing blocks and only within normal reach.
 */
public class AutoBuild extends Module {
    private static final double REACH = 4.5;

    private final ModeSetting blueprint = add(new ModeSetting("Blueprint", "Wall", "Platform", "Pyramid", "Bridge"));
    private final NumSetting width = add(new NumSetting("Width", 1, 15, 1, 5));
    private final NumSetting height = add(new NumSetting("Height", 1, 12, 1, 4));
    private final NumSetting delay = add(new NumSetting("Place Delay", 0, 10, 1, 2));
    private final ModeSetting material = add(new ModeSetting("Material", "Held", "First Block"));
    private final BoolSetting stopWhenDone = add(new BoolSetting("Stop When Done", true));

    private List<BlockPos> plan = new ArrayList<>();
    private int cooldown;
    private int idleTicks;

    public AutoBuild() {
        super("AutoBuild", "Builds a wall, platform, pyramid or bridge in front of you from hotbar blocks.", Category.AUTOMATION);
    }

    @Override
    public String tag() {
        return blueprint.get();
    }

    @Override
    protected void onEnable() {
        plan = makePlan();
        cooldown = 0;
        idleTicks = 0;
        Toasts.push("AutoBuild: " + plan.size() + " blocks planned");
    }

    @Override
    protected void onDisable() {
        plan = new ArrayList<>();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int remaining = 0;
        BlockPos next = null;
        for (BlockPos pos : plan) {
            if (mc.world.getBlockState(pos).isReplaceable()) {
                remaining++;
                if (next == null && tryPlace(pos)) {
                    next = pos;
                }
            }
        }

        if (remaining == 0) {
            Toasts.push("AutoBuild: done");
            if (stopWhenDone.value) {
                enabled = false;
            }
            return;
        }
        if (next != null) {
            cooldown = delay.asInt();
            idleTicks = 0;
        } else if (++idleTicks % 60 == 1) {
            Toasts.push("AutoBuild: step closer or add a block to build from (" + remaining + " left)");
        }
    }

    // ------------------------------------------------------------------ plan

    private List<BlockPos> makePlan() {
        ClientPlayerEntity p = mc.player;
        BlockPos base = p.getBlockPos();
        Direction f = p.getHorizontalFacing();
        Direction r = f.rotateYClockwise();
        int w = (int) width.value;
        int h = (int) height.value;
        List<BlockPos> out = new ArrayList<>();

        switch (blueprint.get()) {
            case "Wall" -> {
                BlockPos origin = base.offset(f, 2);
                for (int y = 0; y < h; y++) {
                    for (int i = 0; i < w; i++) {
                        out.add(origin.offset(r, i - w / 2).up(y));
                    }
                }
            }
            case "Platform" -> {
                BlockPos origin = base.offset(f, 1).down();
                for (int a = 0; a < w; a++) {
                    for (int b = 0; b < w; b++) {
                        out.add(origin.offset(r, a - w / 2).offset(f, b));
                    }
                }
            }
            case "Pyramid" -> {
                int size = w | 1; // keep it odd so there is a centre block
                BlockPos centre = base.offset(f, 2 + size / 2);
                for (int layer = 0; size - 2 * layer > 0; layer++) {
                    int half = (size - 2 * layer) / 2;
                    for (int a = -half; a <= half; a++) {
                        for (int b = -half; b <= half; b++) {
                            out.add(centre.offset(r, a).offset(f, b).up(layer));
                        }
                    }
                }
            }
            default -> { // Bridge
                for (int i = 1; i <= w; i++) {
                    out.add(base.offset(f, i).down());
                }
            }
        }
        // Build bottom-up, nearest first.
        Vec3d from = new Vec3d(p.getX(), p.getY(), p.getZ());
        out.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingDouble(pos -> from.squaredDistanceTo(Vec3d.ofCenter(pos))));
        return out;
    }

    // ------------------------------------------------------------------ placing

    private boolean tryPlace(BlockPos pos) {
        ClientPlayerEntity p = mc.player;
        if (p.getBoundingBox().intersects(new Box(pos))) {
            return false; // never place a block inside yourself
        }
        int slot = blockSlot();
        if (slot < 0) {
            return false;
        }
        Vec3d eye = p.getEyePos();
        for (Direction d : Direction.values()) {
            BlockPos support = pos.offset(d);
            BlockState st = mc.world.getBlockState(support);
            if (st.isAir() || st.isReplaceable() || st.hasBlockEntity()) {
                continue;
            }
            Direction face = d.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(support).add(
                    face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            if (eye.squaredDistanceTo(hitPos) > REACH * REACH) {
                continue;
            }
            p.getInventory().setSelectedSlot(slot);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, support, false));
            p.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private int blockSlot() {
        PlayerInventory inv = mc.player.getInventory();
        int selected = inv.getSelectedSlot();
        if (material.is("Held") && usable(inv.getStack(selected))) {
            return selected;
        }
        for (int i = 0; i < 9; i++) {
            if (usable(inv.getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean usable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi
                && !stack.isEmpty()
                && !bi.getBlock().getDefaultState().hasBlockEntity();
    }
}
