package com.wellfeed.module;

public enum Category {
    MOVEMENT("Movement"),
    RENDER("Render"),
    AUTOMATION("Automation"),
    MISC("Misc");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
