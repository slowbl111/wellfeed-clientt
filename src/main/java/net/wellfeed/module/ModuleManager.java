package net.wellfeed.module;

import net.minecraft.client.MinecraftClient;
import net.wellfeed.module.impl.AutoMiner;
import net.wellfeed.module.impl.AutoSprint;
import net.wellfeed.module.impl.Fly;
import net.wellfeed.module.impl.Fullbright;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    public final List<Module> modules = new ArrayList<>();
    public final Fly fly = new Fly();
    public final AutoMiner autoMiner = new AutoMiner();
    public final Fullbright fullbright = new Fullbright();
    public final AutoSprint autoSprint = new AutoSprint();

    private ModuleManager() {
        modules.add(fly);
        modules.add(autoSprint);
        modules.add(autoMiner);
        modules.add(fullbright);
    }

    public List<Module> byCategory(Module.Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick(mc);
        }
    }
}
