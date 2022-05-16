package com.talhanation.recruits.client.render;

import net.minecraft.client.resources.model.Material;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

public class RenderInfo {
    protected final ResourceLocation textureNoPattern;

    protected final ResourceLocation texturePattern;

    protected final Material materialNoPattern;

    protected final Material materialPattern;

    public RenderInfo(ResourceLocation texNoPattern, ResourceLocation texPattern) {
        this.textureNoPattern = texNoPattern;
        this.texturePattern = texPattern;
        this.materialNoPattern = new Material(TextureAtlas.LOCATION_BLOCKS, this.textureNoPattern);
        this.materialPattern = new Material(TextureAtlas.LOCATION_BLOCKS, this.texturePattern);
    }

    public Material getMaterialNoPattern() {
        return this.materialNoPattern;
    }

    public Material getMaterialWithPattern() {
        return this.materialPattern;
    }

    public void stitchTextures(TextureStitchEvent.Pre ev) {
        ev.addSprite(this.textureNoPattern);
        ev.addSprite(this.texturePattern);
    }
}