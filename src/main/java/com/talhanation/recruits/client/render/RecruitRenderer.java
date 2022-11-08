package com.talhanation.recruits.client.render;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class RecruitRenderer extends AbstractRecruitRenderer{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/recruit.png"),
    };

    public ResourceLocation getTextureLocation(RecruitEntity rec) {
        return TEXTURE[0];
    }

    public RecruitRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

}
