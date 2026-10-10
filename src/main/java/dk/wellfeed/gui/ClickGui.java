package dk.wellfeed.gui;

import dk.wellfeed.WellfeedClient;
import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static final int PANEL_W = 128;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 19;
    private static final int PAD = 4;
    private static final int GAP = 8;

    private final List<ModuleButton> buttons = new ArrayList<>();
    private int[] panelX;
    private int[] panelH;
    private int top;
    private int maxH;

    public ClickGui() {
        super(Text.literal(WellfeedClient.NAME));
    }

    @Override
    protected void init() {
        buttons.clear();
        Category[] cats = Category.values();
        int n = cats.length;

        int maxRows = 1;
        for (Category c : cats) {
            maxRows = Math.max(maxRows, ModuleManager.byCategory(c).size());
        }

        int totalW = n * PANEL_W + (n - 1) * GAP;
        int startX = (this.width - totalW) / 2;
        maxH = HEADER_H + PAD + maxRows * ROW_H + PAD;
        top = Math.max(24, (this.height - maxH) / 2);

        panelX = new int[n];
        panelH = new int[n];

        for (int c = 0; c < n; c++) {
            List<Module> list = ModuleManager.byCategory(cats[c]);
            panelX[c] = startX + c * (PANEL_W + GAP);
            panelH[c] = HEADER_H + PAD + list.size() * ROW_H + PAD;

            for (int r = 0; r < list.size(); r++) {
                int bx = panelX[c] + PAD;
                int by = top + HEADER_H + PAD + r * ROW_H;
                ModuleButton btn = new ModuleButton(bx, by, PANEL_W - PAD * 2, ROW_H - 2, list.get(r));
                buttons.add(btn);
                this.addDrawableChild(btn);
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0000000);

        for (int c = 0; c < panelX.length; c++) {
            int x = panelX[c];
            context.fill(x, top, x + PANEL_W, top + panelH[c], Theme.PANEL);
            context.fill(x, top, x + PANEL_W, top + HEADER_H, Theme.PANEL_HEADER);
            context.fill(x, top, x + PANEL_W, top + 2, Theme.accent(c * 0.7));
            context.fill(x, top + HEADER_H - 1, x + PANEL_W, top + HEADER_H, 0xFF2A2A3C);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Titel
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(WellfeedClient.NAME.toUpperCase()),
                this.width / 2, top - 16, Theme.accent(0));

        // Kategori-overskrifter med antal aktive moduler
        Category[] cats = Category.values();
        for (int c = 0; c < cats.length; c++) {
            List<Module> list = ModuleManager.byCategory(cats[c]);
            int active = 0;
            for (Module m : list) if (m.isEnabled()) active++;

            context.drawTextWithShadow(this.textRenderer, cats[c].getLabel(),
                    panelX[c] + PAD + 3, top + 8, Theme.TEXT);

            String count = active + "/" + list.size();
            int cw = this.textRenderer.getWidth(count);
            context.drawTextWithShadow(this.textRenderer, count,
                    panelX[c] + PANEL_W - cw - PAD - 3, top + 8, Theme.TEXT_DIM);
        }

        // Beskrivelse af modulet musen peger på
        String hint = "Hold musen over et modul for info  |  ESC lukker";
        for (ModuleButton b : buttons) {
            if (b.isHovered()) {
                hint = b.getModule().getName() + ": " + b.getModule().getDescription();
                break;
            }
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hint),
                this.width / 2, top + maxH + 10, Theme.TEXT_DIM);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
