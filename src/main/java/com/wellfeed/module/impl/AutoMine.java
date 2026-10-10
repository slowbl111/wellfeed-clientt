package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.BoolSetting;
import com.wellfeed.setting.ModeSetting;
import com.wellfeed.setting.MultiSetting;
import com.wellfeed.setting.NumSetting;
import com.wellfeed.util.ToolUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Mines blocks you can see within reach: turns to face them, swaps to the best tool
 * and breaks them one at a time. It never mines through walls and never walks for you.
 */
public class AutoMine extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Ores", "Vein", "Any Block"));
    private final NumSetting range = add(new NumSetting("Range", 1, 4.5, 0.5, 4.0));
    private final MultiSetting targets = add(new MultiSetting("Targets",
            new String[]{"Coal", "Copper", "Iron", "Gold", "Redstone", "Lapis", "Diamond", "Emerald", "Debris"},
            "Iron", "Diamond"));
    private final BoolSetting toolSwap = add(new BoolSetting("Tool Swap", true));
    private final BoolSetting avoidLava = add(new BoolSetting("Avoid Lava", true));

    private BlockPos current;
    private String veinId;

    public AutoMine() {
        super("AutoMine", "Mines visible blocks in reach: chosen ores, a whole vein, or anything.", Category.AUTOMATION);
    }

    @Override
    public String tag() {
        return mode.get();
    }

    @Override
    protected void onEnable() {
        current = null;
        veinId = null;
    }

    @Override
    protected void onDisable() {
        current = null;
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (mc.currentScreen != null || mc.interactionManager == null) {
            return;
        }

        if (current != null && !stillValid(current)) {
            current = null;
        }
        if (current == null) {
            current = find(true);
            if (current == null && mode.is("Vein") && veinId != null) {
                veinId = null;
                current = find(false);
            }
            if (current != null && mode.is("Vein")) {
                veinId = idOf(mc.world.getBlockState(current));
            }
        }
        if (current == null) {
            return;
        }

        BlockHitResult hit = sight(current);
        if (hit == null) {
            current = null;
            return;
        }
        if (!aim(Vec3d.ofCenter(current))) {
            return; // still turning towards it
        }
        if (toolSwap.value) {
            int slot = ToolUtil.bestSlot(mc.world.getBlockState(current));
            if (slot != p.getInventory().getSelectedSlot()) {
                p.getInventory().setSelectedSlot(slot);
            }
        }
        mc.interactionManager.updateBlockBreakingProgress(current, hit.getSide());
        p.swingHand(Hand.MAIN_HAND);
    }

    // ------------------------------------------------------------------ targeting

    private BlockPos find(boolean applyVeinFilter) {
        ClientPlayerEntity p = mc.player;
        Vec3d eye = p.getEyePos();
        double max = range.value * range.value;
        int r = (int) Math.ceil(range.value);
        BlockPos base = p.getBlockPos();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(base.add(-r, -r, -r), base.add(r, r + 1, r))) {
            double d = eye.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (d > max || d >= bestDist) {
                continue;
            }
            BlockState st = mc.world.getBlockState(pos);
            if (!matches(st, pos, applyVeinFilter)) {
                continue;
            }
            if (avoidLava.value && lavaNear(pos)) {
                continue;
            }
            if (sight(pos) == null) {
                continue;
            }
            best = pos.toImmutable();
            bestDist = d;
        }
        return best;
    }

    private boolean matches(BlockState st, BlockPos pos, boolean applyVeinFilter) {
        if (st.isAir() || !st.getFluidState().isEmpty()) {
            return false;
        }
        if (mode.is("Any Block")) {
            return st.getHardness(mc.world, pos) >= 0 && !st.hasBlockEntity() && !st.isOf(Blocks.BEDROCK);
        }
        String id = idOf(st);
        if (!isTarget(id)) {
            return false;
        }
        return !(applyVeinFilter && mode.is("Vein") && veinId != null && !id.equals(veinId));
    }

    private boolean isTarget(String id) {
        if (targets.has("Debris") && id.equals("ancient_debris")) {
            return true;
        }
        for (String name : targets.options) {
            if (!name.equals("Debris") && targets.has(name)
                    && id.contains(name.toLowerCase() + "_ore")) {
                return true;
            }
        }
        return false;
    }

    private static String idOf(BlockState st) {
        return Registries.BLOCK.getId(st.getBlock()).getPath();
    }

    private boolean lavaNear(BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (mc.world.getFluidState(pos.offset(d)).isIn(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private boolean stillValid(BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        double max = range.value * range.value;
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > max) {
            return false;
        }
        return sight(pos) != null;
    }

    /** The hit on {@code pos} if nothing blocks the line from your eyes to it, otherwise null. */
    private BlockHitResult sight(BlockPos pos) {
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), Vec3d.ofCenter(pos),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
            return hit;
        }
        return null;
    }

    /** Turns the camera towards a point (limited per tick). Returns true once it is roughly facing it. */
    private boolean aim(Vec3d target) {
        ClientPlayerEntity p = mc.player;
        Vec3d eye = p.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        float wantYaw = (float) (MathHelper.atan2(dz, dx) * 57.29577951308232) - 90f;
        float wantPitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232);

        float yawDiff = MathHelper.wrapDegrees(wantYaw - p.getYaw());
        float pitchDiff = wantPitch - p.getPitch();
        float step = 45f;
        p.setYaw(p.getYaw() + MathHelper.clamp(yawDiff, -step, step));
        p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(pitchDiff, -step, step), -90f, 90f));
        return Math.abs(yawDiff) < 8f && Math.abs(pitchDiff) < 8f;
    }
}
