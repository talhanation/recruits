package com.talhanation.recruits.client.render.human;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitHumanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class AssassinHumanRenderer extends AbstractRecruitHumanRenderer {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/human/human_assassin.png"),
    };

    public AssassinHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return TEXTURE[0];
    }
}