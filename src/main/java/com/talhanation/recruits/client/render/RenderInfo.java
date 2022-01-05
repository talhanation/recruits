package com.talhanation.recruits.client.render;

import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

public class RenderInfo {
    protected final ResourceLocation textureNoPattern;

    protected final ResourceLocation texturePattern;

    protected final RenderMaterial materialNoPattern;

    protected final RenderMaterial materialPattern;

    public RenderInfo(ResourceLocation texNoPattern, ResourceLocation texPattern) {
        this.textureNoPattern = texNoPattern;
        this.texturePattern = texPattern;
        this.materialNoPattern = new RenderMaterial(AtlasTexture.LOCATION_BLOCKS, this.textureNoPattern);
        this.materialPattern = new RenderMaterial(AtlasTexture.LOCATION_BLOCKS, this.texturePattern);
    }

    public RenderMaterial getMaterialNoPattern() {
        return this.materialNoPattern;
    }

    public RenderMaterial getMaterialWithPattern() {
        return this.materialPattern;
    }

    public void stitchTextures(TextureStitchEvent.Pre ev) {
        ev.addSprite(this.textureNoPattern);
        ev.addSprite(this.texturePattern);
    }
}