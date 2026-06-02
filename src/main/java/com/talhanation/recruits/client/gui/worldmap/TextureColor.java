package com.talhanation.recruits.client.gui.worldmap;

record TextureColor(int rgb, int tintIndex) {
    static final TextureColor EMPTY = new TextureColor(0, -1);
}
