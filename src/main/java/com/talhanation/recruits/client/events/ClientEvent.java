package com.talhanation.recruits.client.events;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.models.RecruitVillagerModel;
import com.talhanation.recruits.client.render.human.*;
import com.talhanation.recruits.client.render.layer.RecruitArmorLayer;
import com.talhanation.recruits.client.render.villager.*;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
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
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event){
        if(RecruitsModConfig.RecruitsLookLikeVillagers.get()){
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), NomadVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), HorsemanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossbowmanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), ShieldmanVillagerRenderer::new );

            //COMPANIONS
            EntityRenderers.register(ModEntityTypes.MESSENGER.get(), MessengerVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.PATROL_LEADER.get(), ShieldmanVillagerRenderer::new );//TODO: add own renderer

        }
        else{
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), NomadHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), HorsemanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossbowmanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), ShieldmanHumanRenderer::new );

            //COMPANIONS
            EntityRenderers.register(ModEntityTypes.MESSENGER.get(), MessengerHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.PATROL_LEADER.get(), ShieldmanHumanRenderer::new );//TODO: add own renderer
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