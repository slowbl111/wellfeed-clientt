package com.wellfeed.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    public boolean value;

    public BoolSetting(String name, boolean def) {
        super(name);
        this.value = def;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        value = element.getAsBoolean();
    }
}
