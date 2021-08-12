package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.util.ResourceLocation;


public class RenderBowmanEntity<T extends CreatureEntity, M extends BipedModel<T>> extends BipedRenderer<T, M> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public RenderBowmanEntity(EntityRendererManager manager) {
        this(manager, (M)new PlayerModel<T>(0.0F, false), (M)new PlayerModel<T>(0.5F, false), (M)new PlayerModel<T>(1.0F, false));
    }


    public RenderBowmanEntity(EntityRendererManager manager, M model, M modelArmorHalf, M modelArmorFull) {
        super(manager, (M) model, 0.5F);
        addLayer(new BipedArmorLayer(this, modelArmorHalf, modelArmorFull));

    }


    public ResourceLocation getTextureLocation(T model) {
        return TEXTURE[0];
    }


}
