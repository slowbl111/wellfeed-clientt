package dev.wellfeed.modules;

import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Shows worn armor (helmet to boots) with durability bar and percent. */
public final class ArmorHudModule extends Module {
    public static final ArmorHudModule INSTANCE = new ArmorHudModule();

    private final Setting percent = add(Setting.bool("Durability %", true));

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorHudModule() {
        super("ArmorHUD", "Worn armor with durability on screen.", true, false);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        int x = ctx.getScaledWindowWidth() - 8 - SLOTS.length * 22;
        int y = ctx.getScaledWindowHeight() - 46;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack st = p.getEquippedStack(slot);
            if (!st.isEmpty()) {
                Render.roundRect(ctx, x, y, 20, percent.getBool() ? 32 : 22, Render.PANEL);
                ctx.drawItem(st, x + 2, y + 2);
                ctx.drawStackOverlay(mc.textRenderer, st, x + 2, y + 2);

                if (percent.getBool() && st.isDamageable()) {
                    int max = st.getMaxDamage();
                    float frac = max <= 0 ? 1f : (max - st.getDamage()) / (float) max;
                    int color = Render.lerpColor(0xFFE06C6C, 0xFFF5E6C8, frac);
                    Render.textCentered(ctx, mc.textRenderer, Math.round(frac * 100) + "%",
                            x + 10, y + 22, color, true);
                }
            }
            x += 22;
        }
    }
}
