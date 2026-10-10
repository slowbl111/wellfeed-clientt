package dk.wellfeed.module;

public enum Category {
    MOVEMENT("Movement"),
    MISC("Misc"),
    RENDER("Render");

    private final String label;

    Category(String label) { this.label = label; }

    public String getLabel() { return label; }
}
