package dev.wellfeed.module;

/** A setting shown in the click GUI: on/off toggle, slider, or a cycling list of choices. */
public final class Setting {
    public enum Type { BOOL, NUM, MODE }

    public final String name;
    public final Type type;
    private boolean bool;
    private double num;
    private final double min, max, step;
    private final String[] modes;
    private int mode;

    private Setting(String name, Type type) {
        this(name, type, false, 0, 0, 0, 0, null, 0);
    }

    private Setting(String name, Type type, boolean bool, double num, double min, double max, double step,
                    String[] modes, int mode) {
        this.name = name;
        this.type = type;
        this.bool = bool;
        this.num = num;
        this.min = min;
        this.max = max;
        this.step = step;
        this.modes = modes;
        this.mode = mode;
    }

    public static Setting bool(String name, boolean def) {
        return new Setting(name, Type.BOOL, def, 0, 0, 0, 0, null, 0);
    }

    public static Setting num(String name, double def, double min, double max, double step) {
        return new Setting(name, Type.NUM, false, def, min, max, step, null, 0);
    }

    public static Setting mode(String name, int defIndex, String... modes) {
        return new Setting(name, Type.MODE, false, 0, 0, 0, 0, modes, defIndex);
    }

    // ---- bool
    public boolean getBool() { return bool; }
    public void setBool(boolean v) { bool = v; }
    public void toggle() { bool = !bool; }

    // ---- number
    public double getNum() { return num; }
    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }

    public void setNum(double v) {
        v = Math.round(v / step) * step;
        num = Math.max(min, Math.min(max, v));
    }

    /** 0..1 position of the current value, for drawing a slider. */
    public double fraction() {
        return max <= min ? 0 : (num - min) / (max - min);
    }

    public void setFraction(double f) {
        setNum(min + Math.max(0, Math.min(1, f)) * (max - min));
    }

    // ---- mode
    public int getMode() { return mode; }
    public String getModeName() { return modes[mode]; }
    public void setMode(int i) { mode = Math.floorMod(i, modes.length); }
    public void cycle(int dir) { setMode(mode + dir); }
}
