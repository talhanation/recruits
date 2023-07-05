package com.talhanation.recruits.client.render.villager;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ShieldmanVillagerRenderer extends AbstractRecruitVillagerRenderer {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_shieldman_1.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_shieldman_2.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_shieldman_3.png"),
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_shieldman_4.png"),

    };

    public ShieldmanVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity inventoryEntity) {
        return TEXTURE[((AbstractRecruitEntity) inventoryEntity).getVariant()];
    }
}