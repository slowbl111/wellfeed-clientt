package com.wellfeed.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumSetting extends Setting {
    public final double min;
    public final double max;
    public final double step;
    public double value;

    public NumSetting(String name, double min, double max, double step, double def) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(def);
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        v = Math.round((v - min) / step) * step + min;
        value = Math.round(v * 1000.0) / 1000.0;
    }

    public int asInt() {
        return (int) Math.round(value);
    }

    public String display() {
        return step < 1 ? String.format(Locale.ROOT, "%.1f", value) : String.valueOf(asInt());
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        set(element.getAsDouble());
    }
}
