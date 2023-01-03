package com.talhanation.recruits.client.render.human;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitHumanRenderer;
import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AssassinHumanRenderer extends AbstractRecruitHumanRenderer<AbstractOrderAbleEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/assassin.png"),
    };

    public AssassinHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractOrderAbleEntity p_110775_1_) {
        return TEXTURE[0];
    }
}