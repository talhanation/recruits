package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.util.ResourceLocation;


public class BowmanRenderer extends AbstractManRenderer<BowmanEntity>{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public BowmanRenderer(EntityRendererManager mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(BowmanEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
