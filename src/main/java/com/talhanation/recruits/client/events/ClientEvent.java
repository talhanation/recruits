package com.talhanation.recruits.client.events;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.models.RecruitModel;
import com.talhanation.recruits.client.render.*;
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

    @SubscribeEvent
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event){
        EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitRenderer::new );
        EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanRenderer::new );
        EntityRenderers.register(ModEntityTypes.NOMAD.get(), BowmanRenderer::new );
        //EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossBowmanRenderer::new );
        EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitRenderer::new );
        EntityRenderers.register(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseRenderer::new );
        //EntityRenderers.register(ModEntityTypes.ASSASSIN.get(), AssassinRenderer::new );
        //EntityRenderers.register(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinRenderer::new );
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClientEvent.RECRUIT, RecruitModel::defineLayer);
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