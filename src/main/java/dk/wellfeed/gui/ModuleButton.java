package dk.wellfeed.gui;

import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModuleButton extends ButtonWidget {
    private final Module module;

    public ModuleButton(int x, int y, int width, int height, Module module) {
        super(x, y, width, height, Text.literal(module.getName()),
                b -> module.toggle(), DEFAULT_NARRATION_SUPPLIER);
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean on = module.isEnabled();
        boolean hover = this.isHovered();

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        int bg = on ? 0xFF2B2346 : (hover ? 0xFF292938 : 0xFF1D1D2A);
        context.fill(x, y, x + w, y + h, bg);
        if (on) {
            context.fill(x, y, x + 2, y + h, Theme.accent(0));
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, module.getName(), x + 7, y + (h - 8) / 2,
                on ? Theme.TEXT : Theme.TEXT_DIM);

        // Lille til/fra-kontakt i højre side
        int pw = 18, ph = 8;
        int px = x + w - pw - 5;
        int py = y + (h - ph) / 2;
        context.fill(px, py, px + pw, py + ph, on ? Theme.accent(1.5) : 0xFF3A3A4C);
        int kx = on ? px + pw - ph + 1 : px + 1;
        context.fill(kx, py + 1, kx + ph - 2, py + ph - 1, 0xFFFFFFFF);
    }
}
