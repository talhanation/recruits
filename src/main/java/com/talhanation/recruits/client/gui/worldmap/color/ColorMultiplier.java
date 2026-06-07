package com.talhanation.recruits.client.gui.worldmap.color;

public record ColorMultiplier(float red, float green, float blue) {
    public static ColorMultiplier uniform(float value) {
        return new ColorMultiplier(value, value, value);
    }
}
