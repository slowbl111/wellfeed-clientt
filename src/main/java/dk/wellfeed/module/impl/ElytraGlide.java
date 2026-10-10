package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

public class ElytraGlide extends Module {
    private boolean tried = false;
    private boolean releasePending = false;

    public ElytraGlide() {
        super("Elytra Glide", "Åbner elytra automatisk når du falder.", Category.MOVEMENT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        if (releasePending) {
            mc.options.jumpKey.setPressed(false);
            releasePending = false;
        }

        if (p.isOnGround()) {
            tried = false;
            return;
        }
        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;

        // Et enkelt hop i luften åbner elytra, ligesom i vanilla.
        if (!tried && p.getVelocity().y < -0.15) {
            mc.options.jumpKey.setPressed(true);
            releasePending = true;
            tried = true;
        }
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (releasePending) {
            mc.options.jumpKey.setPressed(false);
            releasePending = false;
        }
        tried = false;
    }
}
