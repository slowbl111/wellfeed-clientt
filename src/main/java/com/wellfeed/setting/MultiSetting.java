package com.wellfeed.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashSet;
import java.util.Set;

public class MultiSetting extends Setting {
    public final String[] options;
    private final Set<String> selected = new LinkedHashSet<>();

    public MultiSetting(String name, String[] options, String... defaults) {
        super(name);
        this.options = options;
        for (String d : defaults) {
            selected.add(d);
        }
    }

    public boolean has(String option) {
        return selected.contains(option);
    }

    public boolean any() {
        return !selected.isEmpty();
    }

    public void toggle(String option) {
        if (!selected.remove(option)) {
            selected.add(option);
        }
    }

    @Override
    public JsonElement save() {
        JsonArray arr = new JsonArray();
        for (String s : selected) {
            arr.add(new JsonPrimitive(s));
        }
        return arr;
    }

    @Override
    public void load(JsonElement element) {
        selected.clear();
        for (JsonElement e : element.getAsJsonArray()) {
            String s = e.getAsString();
            for (String o : options) {
                if (o.equals(s)) {
                    selected.add(s);
                }
            }
        }
    }
}
