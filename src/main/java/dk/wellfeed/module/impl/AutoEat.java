package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;

public class AutoEat extends Module {
    private static final int EAT_BELOW = 14; // spis når sult er 14 eller lavere

    private boolean eating = false;
    private int previousSlot = -1;
    private int eatTicks = 0;

    public AutoEat() {
        super("Auto Eat", "Spiser mad fra hotbar når du er sulten.", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        PlayerInventory inv = p.getInventory();
        int food = p.getHungerManager().getFoodLevel();

        if (eating) {
            eatTicks++;
            boolean finished = (!p.isUsingItem() && eatTicks > 5) || food >= 20 || eatTicks > 100;
            if (finished) {
                stopEating(mc, inv);
            } else {
                mc.options.useKey.setPressed(true);
            }
            return;
        }

        if (mc.currentScreen != null || food > EAT_BELOW) return;

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).contains(DataComponentTypes.FOOD)) {
                previousSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                eating = true;
                eatTicks = 0;
                return;
            }
        }
    }

    private void stopEating(MinecraftClient mc, PlayerInventory inv) {
        mc.options.useKey.setPressed(false);
        if (previousSlot != -1) inv.setSelectedSlot(previousSlot);
        previousSlot = -1;
        eating = false;
        eatTicks = 0;
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (eating && mc.player != null) stopEating(mc, mc.player.getInventory());
        eating = false;
    }
}
