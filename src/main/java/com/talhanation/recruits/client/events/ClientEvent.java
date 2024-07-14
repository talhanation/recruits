package com.talhanation.recruits.client.events;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.models.RecruitVillagerModel;
import com.talhanation.recruits.client.render.RecruitHumanRenderer;
import com.talhanation.recruits.client.render.RecruitVillagerRenderer;
import com.talhanation.recruits.client.render.layer.RecruitArmorLayer;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEvent {

    public static ModelLayerLocation RECRUIT = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit"), "recruit");
    public static ModelLayerLocation RECRUIT_OUTER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_outer_layer"), "recruit_outer_layer");
    public static ModelLayerLocation RECRUIT_INNER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_inner_layer"), "recruit_inner_layer");

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void entityRenderersEvent(EntityRenderersEvent.RegisterRenderers event){
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get()){
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitVillagerRenderer::new );

            //COMPANIONS
            EntityRenderers.register(ModEntityTypes.MESSENGER.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.PATROL_LEADER.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.CAPTAIN.get(), RecruitVillagerRenderer::new );

        }
        else{
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitHumanRenderer::new );

            //COMPANIONS
            EntityRenderers.register(ModEntityTypes.MESSENGER.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.PATROL_LEADER.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.CAPTAIN.get(), RecruitHumanRenderer::new );
        }
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClientEvent.RECRUIT, RecruitVillagerModel::createLayerDefinition);
        event.registerLayerDefinition(ClientEvent.RECRUIT_OUTER_ARMOR, RecruitArmorLayer::createOuterArmorLayer);
        event.registerLayerDefinition(ClientEvent.RECRUIT_INNER_ARMOR, RecruitArmorLayer::createInnerArmorLayer);

    }

    @Nullable
    public static Entity getEntityByLooking() {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit instanceof EntityHitResult entityHitResult){
            return entityHitResult.getEntity();
        }
        return null;
    }
}