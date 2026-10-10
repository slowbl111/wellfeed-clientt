package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "Maximum light everywhere (client-side night vision).", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 100000, 0, false, false, false));
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) {
            return;
        }
        StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        // Only remove the long effect we added, never a real potion.
        if (e != null && e.getDuration() > 50000) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }
}
