package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Keeps a Totem of Undying in your offhand. Searches the hotbar first, then the
 * main inventory, and swaps it in with the offhand-swap click (button 40).
 */
public final class AutoTotemModule extends Module {
    public static final AutoTotemModule INSTANCE = new AutoTotemModule();

    private final Setting delay = add(Setting.num("Delay (ticks)", 1, 0, 10, 1));
    private int cooldown;

    private AutoTotemModule() {
        super("AutoTotem", "Keeps a totem in your offhand from hotbar or inventory.", false, true);
    }

    @Override
    protected void onEnable() {
        cooldown = 0;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        // Only act in the world or the player's own inventory screen.
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            cooldown = 0;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int invIndex = findTotem(p);
        if (invIndex < 0) return;

        // PlayerScreenHandler slots: 9-35 main inventory, 36-44 hotbar.
        int screenSlot = invIndex < 9 ? 36 + invIndex : invIndex;
        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, screenSlot, 40, SlotActionType.SWAP, p);
        cooldown = (int) delay.getNum();
    }

    /** Inventory index (0-8 hotbar, 9-35 main) of a totem, or -1. */
    private static int findTotem(ClientPlayerEntity p) {
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }
}
