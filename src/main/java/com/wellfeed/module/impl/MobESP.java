package com.wellfeed.module.impl;

import com.wellfeed.module.Category;
import com.wellfeed.module.Module;
import com.wellfeed.setting.MultiSetting;
import com.wellfeed.setting.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Outlines mobs and dropped items. Players are intentionally never highlighted. */
public class MobESP extends Module {
    private final MultiSetting targets = add(new MultiSetting("Targets",
            new String[]{"Hostiles", "Animals", "Items"}, "Hostiles", "Items"));
    private final NumSetting range = add(new NumSetting("Range", 8, 64, 4, 32));
    private final Set<Integer> marked = new HashSet<>();

    public MobESP() {
        super("MobESP", "Outlines hostile mobs, animals and dropped items. Never players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        double r2 = range.value * range.value;
        Set<Integer> seen = new HashSet<>();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e instanceof PlayerEntity) {
                continue;
            }
            boolean want = (targets.has("Hostiles") && e instanceof Monster)
                    || (targets.has("Animals") && e instanceof AnimalEntity)
                    || (targets.has("Items") && e instanceof ItemEntity);
            if (!want || e.squaredDistanceTo(mc.player) > r2) {
                continue;
            }
            if (!marked.contains(e.getId())) {
                if (e.isGlowing()) {
                    continue; // already highlighted by the game, leave it alone
                }
                e.setGlowing(true);
                marked.add(e.getId());
            }
            seen.add(e.getId());
        }
        for (Iterator<Integer> it = marked.iterator(); it.hasNext(); ) {
            int id = it.next();
            if (!seen.contains(id)) {
                Entity e = mc.world.getEntityById(id);
                if (e != null) {
                    e.setGlowing(false);
                }
                it.remove();
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.world != null) {
            for (int id : marked) {
                Entity e = mc.world.getEntityById(id);
                if (e != null) {
                    e.setGlowing(false);
                }
            }
        }
        marked.clear();
    }
}
