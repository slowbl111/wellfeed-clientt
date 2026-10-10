package dk.wellfeed.module.impl;

import dk.wellfeed.module.Category;
import dk.wellfeed.module.Module;

// Selve tegningen sker i gui/HudOverlay. Dette modul er bare til/fra-knappen.
public class Hud extends Module {
    public Hud() {
        super("HUD", "Viser aktive moduler på skærmen.", Category.RENDER);
    }
}
