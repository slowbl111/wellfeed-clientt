package dev.wellfeed.module;

/** A toggle or a stepped number, shown in the click GUI. */
public final class Setting {
    public enum Type { BOOL, NUM }

    public final String name;
    public final Type type;
    private boolean bool;
    private double num;
    private final double min, max, step;

    private Setting(String name, Type type, boolean bool, double num, double min, double max, double step) {
        this.name = name;
        this.type = type;
        this.bool = bool;
        this.num = num;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public static Setting bool(String name, boolean def) {
        return new Setting(name, Type.BOOL, def, 0, 0, 0, 0);
    }

    public static Setting num(String name, double def, double min, double max, double step) {
        return new Setting(name, Type.NUM, false, def, min, max, step);
    }

    public boolean getBool() { return bool; }
    public double getNum() { return num; }
    public double step() { return step; }
    public double min() { return min; }
    public double max() { return max; }

    /** 0..1 position of the current value, for sliders. */
    public double fraction() { return max > min ? (num - min) / (max - min) : 0; }

    /** Set from a 0..1 slider position, snapped to the step. */
    public void setFraction(double f) {
        double v = min + Math.max(0, Math.min(1, f)) * (max - min);
        v = Math.round(v / step) * step;
        num = Math.max(min, Math.min(max, v));
    }

    public void toggle() { bool = !bool; }

    /** dir = -1 or +1 */
    public void nudge(int dir) {
        double v = num + dir * step;
        v = Math.round(v / step) * step;
        num = Math.max(min, Math.min(max, v));
    }
}
