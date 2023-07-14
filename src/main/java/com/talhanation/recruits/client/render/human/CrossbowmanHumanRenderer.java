package com.talhanation.recruits.client.render.human;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitHumanRenderer;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CrossbowmanHumanRenderer extends AbstractRecruitHumanRenderer<CrossBowmanEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_crossbowman_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_crossbowman_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_crossbowman_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_crossbowman_4.png"),
    };

    public CrossbowmanHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(CrossBowmanEntity bowmanEntity) {
        return TEXTURE[bowmanEntity.getVariant()];
    }
}