package com.talhanation.recruits.util;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.RenderBowmanEntity;
import com.talhanation.recruits.client.render.RenderRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEventBusSub {

    @SubscribeEvent
    public static void clientsetup(FMLClientSetupEvent event){
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.RECRUIT.get(), RenderRecruitEntity::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.BOWMAN.get(), RenderBowmanEntity::new );
    }

}