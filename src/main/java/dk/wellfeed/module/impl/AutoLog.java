package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    private final NumberSetting limit = addSetting(new NumberSetting("Health", 6, 2, 18, 1)); // 2 health = 1 hjerte

    public AutoLog() {
        super("Auto Log", "Afbryder forbindelsen ved lav health (justerbar).", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getHealth() <= (float) limit.get()) {
            setEnabled(false);
            mc.getNetworkHandler().getConnection()
                    .disconnect(Text.literal("Wellfeed Client: Auto Log (lav health)"));
        }
    }
}
