package net.wellfeed.module;

public class NumberSetting {
    public final String name;
    public final double min, max;
    private double value;

    public NumberSetting(String name, double value, double min, double max) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.value = value;
    }

    public double get() {
        return value;
    }

    public void set(double v) {
        this.value = Math.max(min, Math.min(max, v));
    }

    /** 0..1 position of the value, used by the GUI slider. */
    public double progress() {
        return (value - min) / (max - min);
    }
}
