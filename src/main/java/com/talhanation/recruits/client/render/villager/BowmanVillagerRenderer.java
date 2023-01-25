package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BowmanVillagerRenderer extends AbstractRecruitVillagerRenderer{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_bowman_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_bowman_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_bowman_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_bowman_4.png"),
    };

    public BowmanVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity bowman) {
        return TEXTURE[((BowmanEntity) bowman).getVariant()];
    }
}