package dk.wellfeed.gui;

import dk.wellfeed.WellfeedClient;
import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ClickGui extends Screen {
    private static final int ACCENT = 0xFF7B5CFF;
    private static final int BTN_W = 110;
    private static final int BTN_H = 20;
    private static final int GAP = 6;

    private int panelX, panelY, panelW, panelH, startX;

    public ClickGui() {
        super(Text.literal(WellfeedClient.NAME));
    }

    @Override
    protected void init() {
        Category[] cats = Category.values();
        int cols = cats.length;

        int maxRows = 1;
        for (Category c : cats) {
            maxRows = Math.max(maxRows, ModuleManager.byCategory(c).size());
        }

        int gridW = cols * BTN_W + (cols - 1) * GAP;
        panelW = gridW + 40;
        panelH = 70 + maxRows * (BTN_H + GAP) + 24;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        startX = panelX + 20;
        int startY = panelY + 56;

        for (int c = 0; c < cols; c++) {
            List<Module> list = ModuleManager.byCategory(cats[c]);
            int x = startX + c * (BTN_W + GAP);
            for (int r = 0; r < list.size(); r++) {
                Module m = list.get(r);
                int y = startY + r * (BTN_H + GAP);

                ButtonWidget btn = ButtonWidget.builder(label(m), b -> {
                            m.toggle();
                            b.setMessage(label(m));
                        })
                        .dimensions(x, y, BTN_W, BTN_H)
                        .tooltip(Tooltip.of(Text.literal(m.getDescription())))
                        .build();
                this.addDrawableChild(btn);
            }
        }
    }

    private Text label(Module m) {
        return Text.literal(m.getName() + ": " + (m.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE0101018);
        context.fill(panelX, panelY, panelX + panelW, panelY + 3, ACCENT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, panelY + 14, ACCENT);

        Category[] cats = Category.values();
        for (int c = 0; c < cats.length; c++) {
            int cx = startX + c * (BTN_W + GAP) + BTN_W / 2;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(cats[c].getLabel()), cx, panelY + 38, 0xFFFFFFFF);
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Tryk ESC for at lukke"),
                this.width / 2, panelY + panelH - 14, 0xFF888888);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
