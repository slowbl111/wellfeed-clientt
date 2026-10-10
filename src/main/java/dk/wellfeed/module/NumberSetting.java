package dk.wellfeed.module;

import java.util.Locale;

public class NumberSetting {
    private final String name;
    private final double min, max, step;
    private double value;

    public NumberSetting(String name, double value, double min, double max, double step) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    public String getName() { return name; }
    public double get() { return value; }

    public void set(double v) {
        double snapped = Math.round(v / step) * step;
        this.value = Math.max(min, Math.min(max, snapped));
    }

    public void increase() { set(value + step); }
    public void decrease() { set(value - step); }

    public String display() {
        if (step >= 1.0) return String.valueOf((int) Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
