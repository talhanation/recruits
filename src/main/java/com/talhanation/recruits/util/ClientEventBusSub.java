package com.talhanation.recruits.util;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEventBusSub {

    @SubscribeEvent
    public static void clientsetup(FMLClientSetupEvent event){
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.RECRUIT.get(), RecruitRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.BOWMAN.get(), BowmanRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.NOMAD.get(), BowmanRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.CROSSBOWMAN.get(), CrossBowmanRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseRenderer::new );

        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.ASSASSIN.get(), AssassinRenderer::new );
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinRenderer::new );
    }

}