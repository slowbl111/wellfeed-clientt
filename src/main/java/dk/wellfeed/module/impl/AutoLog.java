package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    private static final float HEALTH_LIMIT = 6.0f; // 3 hjerter

    public AutoLog() {
        super("Auto Log", "Afbryder forbindelsen ved lav health (3 hjerter).", Category.MISC);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getHealth() <= HEALTH_LIMIT) {
            setEnabled(false);
            mc.getNetworkHandler().getConnection()
                    .disconnect(Text.literal("Wellfeed Client: Auto Log (lav health)"));
        }
    }
}
