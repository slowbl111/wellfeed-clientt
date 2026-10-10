package dk.wellfeed.gui;

import dk.wellfeed.WellfeedClient;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ClickGui extends Screen {
    private static final int ACCENT = 0xFF7B5CFF;
    private static final int BTN_W = 110;
    private static final int BTN_H = 20;
    private static final int GAP = 6;
    private static final int COLUMNS = 3;

    public ClickGui() {
        super(Text.literal(WellfeedClient.NAME));
    }

    @Override
    protected void init() {
        List<Module> modules = ModuleManager.getModules();
        int gridW = COLUMNS * BTN_W + (COLUMNS - 1) * GAP;
        int startX = (this.width - gridW) / 2;
        int startY = this.height / 2 - 40;

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int x = startX + (i % COLUMNS) * (BTN_W + GAP);
            int y = startY + (i / COLUMNS) * (BTN_H + GAP);

            ButtonWidget btn = ButtonWidget.builder(label(m), b -> {
                        m.toggle();
                        b.setMessage(label(m));
                    })
                    .dimensions(x, y, BTN_W, BTN_H)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(m.getDescription())))
                    .build();
            this.addDrawableChild(btn);
        }
    }

    private Text label(Module m) {
        return Text.literal(m.getName() + ": " + (m.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Egen mørk baggrund + panel (ingen vanilla blur)
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        int panelW = COLUMNS * BTN_W + (COLUMNS - 1) * GAP + 40;
        int panelX = (this.width - panelW) / 2;
        int panelY = this.height / 2 - 80;
        int panelH = 170;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE0101018);
        context.fill(panelX, panelY, panelX + panelW, panelY + 3, ACCENT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 65, ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Tryk ESC for at lukke"),
                this.width / 2, this.height / 2 + 70, 0xFF888888);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
