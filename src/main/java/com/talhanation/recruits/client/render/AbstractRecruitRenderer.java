package com.talhanation.recruits.client.render;

import com.talhanation.recruits.client.models.ManModel;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.LivingEntity;

public abstract class AbstractRecruitRenderer<E extends AbstractRecruitEntity> extends RecruitsBipedRenderer<E, ManModel<E>>{
    public AbstractRecruitRenderer(EntityRendererManager mgr) {
        super(mgr, new ManModel(), new ManModel(0.5F, true), new ManModel(1.0F, true), 0.5F);
    }

    /*
    @Override
    protected boolean shouldShowName(E recruit) {
        LivingEntity owner = recruit.getOwner();
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientplayerentity = minecraft.player;
        assert clientplayerentity != null;
        if (owner != null && clientplayerentity.getUUID() == recruit.getOwnerUUID() && RecruitsModConfig.RenderNameTagforOwner.get()){
            return true;
        }
        else
            return false;
    }

     */

}
