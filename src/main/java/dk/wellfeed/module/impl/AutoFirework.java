package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFirework extends Module {
    private int cooldown = 0;

    public AutoFirework() {
        super("Auto Firework", "Bruger raketter fra hotbar når du flyver langsomt med elytra.", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (cooldown > 0) { cooldown--; return; }
        if (p.isOnGround()) return;
        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;
        if (p.getVelocity().length() > 1.0) return;

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.FIREWORK_ROCKET)) { slot = i; break; }
        }
        if (slot == -1) return;

        int previous = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        inv.setSelectedSlot(previous);
        cooldown = 40;
    }
}
