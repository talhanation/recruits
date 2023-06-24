package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.HorsemanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class HorsemanVillagerRenderer extends AbstractRecruitVillagerRenderer{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_horseman_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_horseman_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_horseman_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_horseman_4.png"),
    };

    public HorsemanVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity horseman) {
        return TEXTURE[((HorsemanEntity) horseman).getVariant()];
    }
}