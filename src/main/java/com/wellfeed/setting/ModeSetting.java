package com.wellfeed.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting {
    public final String[] options;
    public int index;

    public ModeSetting(String name, String... options) {
        super(name);
        this.options = options;
    }

    public String get() {
        return options[index];
    }

    public boolean is(String option) {
        return options[index].equals(option);
    }

    public void next() {
        index = (index + 1) % options.length;
    }

    public void prev() {
        index = (index - 1 + options.length) % options.length;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement element) {
        String s = element.getAsString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(s)) {
                index = i;
                return;
            }
        }
    }
}
