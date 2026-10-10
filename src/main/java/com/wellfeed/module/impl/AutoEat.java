package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.NumSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Set;

public class AutoEat extends Module {
    private static final Set<Item> AVOID = Set.of(
            Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH,
            Items.CHORUS_FRUIT, Items.SUSPICIOUS_STEW, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.HONEY_BOTTLE);

    private final NumSetting hunger = add(new NumSetting("Eat At", 1, 19, 1, 14));
    private boolean eating;
    private int eatTicks;
    private int previous = -1;

    public AutoEat() {
        super("AutoEat", "Eats food from your hotbar when your hunger bar drops.", Category.AUTOMATION);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        PlayerInventory inv = p.getInventory();

        if (eating) {
            eatTicks++;
            if ((eatTicks > 5 && !p.isUsingItem()) || eatTicks > 80) {
                stop();
            } else {
                mc.options.useKey.setPressed(true);
            }
            return;
        }
        if (mc.currentScreen != null || p.isUsingItem()) {
            return;
        }
        if (p.getHungerManager().getFoodLevel() <= hunger.asInt() && p.canConsume(false)) {
            int slot = foodSlot(inv);
            if (slot >= 0) {
                previous = inv.getSelectedSlot();
                inv.setSelectedSlot(slot);
                eating = true;
                eatTicks = 0;
                mc.options.useKey.setPressed(true);
            }
        }
    }

    private int foodSlot(PlayerInventory inv) {
        int best = -1;
        int bestNutrition = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || AVOID.contains(s.getItem())) {
                continue;
            }
            FoodComponent food = s.get(DataComponentTypes.FOOD);
            if (food != null && food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                best = i;
            }
        }
        return best;
    }

    private void stop() {
        mc.options.useKey.setPressed(false);
        if (previous >= 0 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previous);
        }
        previous = -1;
        eating = false;
        eatTicks = 0;
    }

    @Override
    protected void onDisable() {
        if (eating) {
            stop();
        }
    }
}
