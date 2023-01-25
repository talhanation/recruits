package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RecruitVillagerRenderer extends AbstractRecruitVillagerRenderer {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_recruit_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_recruit_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_recruit_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_recruit_4.png"),
    };

    public RecruitVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity inventoryEntity) {
        return TEXTURE[((AbstractRecruitEntity) inventoryEntity).getVariant()];
    }
}