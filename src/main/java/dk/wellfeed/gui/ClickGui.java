package dk.wellfeed.gui;

import dk.wellfeed.Config;
import dk.wellfeed.WellfeedClient;
import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;
import dk.wellfeed.module.ModuleManager;
import dk.wellfeed.module.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static final int PANEL_W = 160;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 19;
    private static final int PAD = 4;
    private static final int GAP = 8;

    private final List<ModuleButton> buttons = new ArrayList<>();
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
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
                Module m = list.get(r);
                int bw = PANEL_W - PAD * 2;
                int bh = ROW_H - 2;
                buttons.add(new ModuleButton(bx, by, bw, bh, m));
                // Almindelig knap med tom tekst håndterer klikket; ModuleButton tegner udseendet ovenpå.
                this.addDrawableChild(ButtonWidget.builder(Text.literal(""), b -> m.toggle())
                        .dimensions(bx, by, bw, bh)
                        .build());
            }
        }

        // Nulstil tast-tilstand så en tast der allerede holdes nede ikke tæller som nyt tryk
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            prevKeys[k] = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
        }
    }

    // Hold musen over et modul og tryk en tast: binder den. DEL/BKSP rydder. Pile justerer indstilling.
    private void pollKeys(int mouseX, int mouseY) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();

        ModuleButton hovered = null;
        for (ModuleButton b : buttons) {
            if (b.isOver(mouseX, mouseY)) { hovered = b; break; }
        }

        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            boolean down = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
            if (down && !prevKeys[k] && hovered != null) {
                handleKey(hovered.getModule(), k);
            }
            prevKeys[k] = down;
        }
    }

    private void handleKey(Module m, int key) {
        // Taster der bruges af menuen selv kan ikke bindes
        if (key == GLFW.GLFW_KEY_ESCAPE || key == WellfeedClient.MENU_KEY || key == WellfeedClient.PANIC_KEY) return;
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_TAB) return;

        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            m.setKey(-1);
            return;
        }

        NumberSetting s = m.getSetting();
        if (s != null && key == GLFW.GLFW_KEY_RIGHT) { s.increase(); Config.save(); return; }
        if (s != null && key == GLFW.GLFW_KEY_LEFT)  { s.decrease(); Config.save(); return; }

        m.setKey(key);
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

        // Tegn modul-rækkerne oven på de tomme knapper
        for (ModuleButton b : buttons) {
            b.draw(context, mouseX, mouseY);
        }

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

        // Hjælpetekst / beskrivelse af modulet musen peger på
        String hint = "Hold musen over et modul: tryk en tast for at binde  |  DEL rydder  |  pile justerer";
        for (ModuleButton b : buttons) {
            if (b.isOver(mouseX, mouseY)) {
                hint = b.getModule().getName() + ": " + b.getModule().getDescription();
                break;
            }
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hint),
                this.width / 2, top + maxH + 10, Theme.TEXT_DIM);

        String footer = "Panic: " + KeyNames.of(WellfeedClient.PANIC_KEY)
                + " slukker alle moduler  |  ESC lukker";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(footer),
                this.width / 2, top + maxH + 22, 0xFF666677);

        pollKeys(mouseX, mouseY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
