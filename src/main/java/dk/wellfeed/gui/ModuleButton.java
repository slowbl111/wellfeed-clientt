package dk.wellfeed.gui;

import dk.wellfeed.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

// Ren hjælpeklasse: tegner en modul-række. Selve klikket håndteres af en
// almindelig knap i ClickGui, som ligger under og har tom tekst.
public class ModuleButton {
    private final Module module;
    private final int x, y, w, h;

    public ModuleButton(int x, int y, int w, int h, Module module) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public boolean isOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public void draw(DrawContext context, int mouseX, int mouseY) {
        boolean on = module.isEnabled();
        boolean hover = isOver(mouseX, mouseY);

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
