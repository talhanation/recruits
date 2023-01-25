package com.talhanation.recruits.client.render.villager;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractOrderAbleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.swing.text.html.parser.Entity;

public class AssassinVillagerRenderer extends AbstractRecruitVillagerRenderer{

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/villager_assassin.png"),
    };

    public AssassinVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity inventoryEntity) {
        return TEXTURE[0];
    }
}