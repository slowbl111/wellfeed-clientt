package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;

public class AutoMine extends Module {
    private boolean pressing = false;

    public AutoMine() {
        super("Auto Mine", "Graver automatisk blokken du kigger på.", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        boolean lookingAtBlock = mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;

        if (lookingAtBlock && mc.currentScreen == null) {
            mc.options.attackKey.setPressed(true);
            pressing = true;
        } else if (pressing) {
            mc.options.attackKey.setPressed(false);
            pressing = false;
        }
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (pressing) {
            mc.options.attackKey.setPressed(false);
            pressing = false;
        }
    }
}
