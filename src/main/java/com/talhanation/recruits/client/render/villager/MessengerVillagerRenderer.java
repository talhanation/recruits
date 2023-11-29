package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MessengerVillagerRenderer extends AbstractRecruitVillagerRenderer{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_assassin.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_assassin.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_assassin.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_assassin.png"),
    };

    public MessengerVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity bowman) {
        return TEXTURE[((AbstractRecruitEntity) bowman).getVariant()];
    }
}