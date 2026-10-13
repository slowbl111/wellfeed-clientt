package dev.wellfeed.modules;

import dev.wellfeed.module.Category;
import dev.wellfeed.module.Module;
import dev.wellfeed.module.Setting;
import dev.wellfeed.util.Render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Shows worn armor (helmet to boots) with a durability bar and percent. */
public final class ArmorHudModule extends Module {
    public static final ArmorHudModule INSTANCE = new ArmorHudModule();

    private final Setting percent = add(Setting.bool("Durability %", true));
    private final Setting offhand = add(Setting.bool("Show offhand", false));

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorHudModule() {
        super("ArmorHUD", "Worn armor with durability on screen.",
                Category.HUD, true, false, true, false, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onHud(DrawContext ctx, float tickDelta, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        boolean pct = percent.getBool();
        int cellH = pct ? 34 : 22;
        int count = SLOTS.length + (offhand.getBool() ? 1 : 0);
        int x = ctx.getScaledWindowWidth() - 8 - count * 22;
        int y = ctx.getScaledWindowHeight() - 58;

        for (int i = 0; i < count; i++) {
            ItemStack st = i < SLOTS.length ? p.getEquippedStack(SLOTS[i]) : p.getOffHandStack();
            if (!st.isEmpty()) {
                Render.roundRect(ctx, x, y, 20, cellH, 3, Render.PANEL);
                ctx.drawItem(st, x + 2, y + 2);
                ctx.drawStackOverlay(mc.textRenderer, st, x + 2, y + 2);

                if (pct && st.isDamageable()) {
                    int max = st.getMaxDamage();
                    float frac = max <= 0 ? 1f : (max - st.getDamage()) / (float) max;
                    Render.textCentered(ctx, mc.textRenderer, Math.round(frac * 100) + "%",
                            x + 10, y + 23, Render.redToGreen(frac), true);
                }
            }
            x += 22;
        }
    }
}
