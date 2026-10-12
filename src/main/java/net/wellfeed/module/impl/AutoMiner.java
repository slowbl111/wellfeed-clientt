package net.wellfeed.module.impl;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.wellfeed.module.Module;
import net.wellfeed.module.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Mines out a cuboid defined by two corner positions, one layer at a time from the top down.
 * Flies toward the next block if it is out of reach.
 */
public class AutoMiner extends Module {
    private static final int MAX_BLOCKS = 2_000_000;

    public final NumberSetting reach = add(new NumberSetting("Reach", 4.0, 2.0, 4.5));
    public final NumberSetting flySpeed = add(new NumberSetting("Fly Speed", 0.6, 0.1, 1.5));

    public BlockPos pos1 = null;
    public BlockPos pos2 = null;

    private List<BlockPos> targets = new ArrayList<>();
    private int index;
    private int minedCount;

    public AutoMiner() {
        super("AutoMiner", "Set two corners and it mines the whole region out, top layer first.", Category.WORLD);
    }

    // ---- region helpers (used by GUI and commands) ----

    public boolean hasRegion() {
        return pos1 != null && pos2 != null;
    }

    public long regionVolume() {
        if (!hasRegion()) return 0;
        long dx = Math.abs(pos1.getX() - pos2.getX()) + 1L;
        long dy = Math.abs(pos1.getY() - pos2.getY()) + 1L;
        long dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1L;
        return dx * dy * dz;
    }

    public int remaining() {
        return Math.max(0, targets.size() - index);
    }

    public int mined() {
        return minedCount;
    }

    // ---- lifecycle ----

    @Override
    protected void onEnable(MinecraftClient mc) {
        if (!hasRegion()) {
            say(mc, "Set both corners first (GUI or /wf pos1, /wf pos2).");
            deferDisable();
            return;
        }
        if (regionVolume() > MAX_BLOCKS) {
            say(mc, "Region is too big (" + regionVolume() + " blocks, max " + MAX_BLOCKS + ").");
            deferDisable();
            return;
        }
        buildTargets();
        index = 0;
        minedCount = 0;
        if (mc.player != null) {
            mc.player.getAbilities().allowFlying = true;
        }
        say(mc, "Mining " + targets.size() + " blocks.");
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        if (mc.player != null) mc.player.setVelocity(Vec3d.ZERO);
    }

    private boolean pendingDisable;

    private void deferDisable() {
        pendingDisable = true;
    }

    private void buildTargets() {
        targets = new ArrayList<>();
        int minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());
        for (int y = maxY; y >= minY; y--) {
            boolean flip = false;
            for (int x = minX; x <= maxX; x++) {
                // serpentine rows so the miner never has to cross the whole layer
                if (!flip) {
                    for (int z = minZ; z <= maxZ; z++) targets.add(new BlockPos(x, y, z));
                } else {
                    for (int z = maxZ; z >= minZ; z--) targets.add(new BlockPos(x, y, z));
                }
                flip = !flip;
            }
        }
    }

    // ---- main loop ----

    @Override
    public void onTick(MinecraftClient mc) {
        if (pendingDisable) {
            pendingDisable = false;
            setEnabled(false);
            return;
        }
        ClientPlayerEntity p = mc.player;
        World world = mc.world;
        if (p == null || world == null || mc.interactionManager == null) return;

        // skip anything already air / fluid / unbreakable
        while (index < targets.size() && !isMineable(world, targets.get(index))) {
            if (world.getBlockState(targets.get(index)).isAir()) minedCount++;
            index++;
        }
        if (index >= targets.size()) {
            say(mc, "Done. Region cleared.");
            setEnabled(false);
            return;
        }

        BlockPos t = targets.get(index);
        Vec3d center = Vec3d.ofCenter(t);
        Vec3d eye = new Vec3d(p.getX(), p.getEyeY(), p.getZ());
        double dist = eye.distanceTo(center);

        lookAt(p, eye, center);

        if (dist > reach.get() - 0.3) {
            // out of reach: fly toward it
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            Vec3d dir = center.subtract(eye).normalize().multiply(flySpeed.get());
            p.setVelocity(dir);
            mc.interactionManager.cancelBlockBreaking();
            return;
        }

        // in reach: hover and mine
        p.setVelocity(Vec3d.ZERO);
        selectBestTool(p, world.getBlockState(t));
        Direction face = Direction.getFacing(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        mc.interactionManager.updateBlockBreakingProgress(t, face);
        p.swingHand(Hand.MAIN_HAND);
    }

    private static boolean isMineable(World world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        if (s.isAir()) return false;
        if (!s.getFluidState().isEmpty() && s.getBlock().getDefaultState().getFluidState().isStill()) return false;
        return s.getHardness(world, pos) >= 0; // bedrock etc. are -1
    }

    private static void lookAt(ClientPlayerEntity p, Vec3d eye, Vec3d target) {
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(dy, horiz) * 180.0 / Math.PI);
        p.setYaw(yaw);
        p.setPitch(MathHelper.clamp(pitch, -90f, 90f));
    }

    private static void selectBestTool(ClientPlayerEntity p, BlockState state) {
        int best = p.getInventory().getSelectedSlot();
        float bestSpeed = p.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().getStack(i);
            float sp = st.getMiningSpeedMultiplier(state);
            if (sp > bestSpeed) {
                bestSpeed = sp;
                best = i;
            }
        }
        if (best != p.getInventory().getSelectedSlot()) p.getInventory().setSelectedSlot(best);
    }

    private static void say(MinecraftClient mc, String msg) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Wellfeed] " + msg), false);
    }
}
