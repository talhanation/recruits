package com.talhanation.recruits.client.gui.worldmap.color;

public record TextureColor(int rgb, int tintIndex) {
    public static final TextureColor EMPTY = new TextureColor(0, -1);
}
