package com.talhanation.recruits.client.render.human;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitHumanRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ShieldmanHumanRenderer extends AbstractRecruitHumanRenderer<AbstractRecruitEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_shieldman_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_shieldman_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_shieldman_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_shieldman_4.png"),
    };

    @Override
    public ResourceLocation getTextureLocation(AbstractRecruitEntity recruit) {
        return TEXTURE[recruit.getVariant()];
    }

    public ShieldmanHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

}

