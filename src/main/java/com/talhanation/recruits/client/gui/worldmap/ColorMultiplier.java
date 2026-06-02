package com.talhanation.recruits.client.gui.worldmap;

record ColorMultiplier(float red, float green, float blue) {
    static ColorMultiplier uniform(float value) {
        return new ColorMultiplier(value, value, value);
    }
}