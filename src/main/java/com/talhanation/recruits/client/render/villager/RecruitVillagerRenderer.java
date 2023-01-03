package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RecruitVillagerRenderer extends AbstractRecruitVillagerRenderer {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager.png"),
    };

    public RecruitVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractRecruitEntity p_110775_1_) {
        return TEXTURE[0];
    }
}