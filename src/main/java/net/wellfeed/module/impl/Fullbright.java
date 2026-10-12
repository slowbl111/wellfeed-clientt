package net.wellfeed.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.wellfeed.module.Module;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "See in the dark with a client-side night vision effect.", Category.RENDER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || cur.getDuration() < 400) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
        }
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
