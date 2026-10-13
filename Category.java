package dev.wellfeed.module;

public enum Category {
    RENDER("Render"),
    MOVEMENT("Movement"),
    HUD("HUD"),
    MISC("Misc");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
