package com.talhanation.recruits.util;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.render.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEventBusSub {

    @SubscribeEvent
    public static void clientsetup(FMLClientSetupEvent event){
        EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitRenderer::new );
        //EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanRenderer::new );
        //EntityRenderers.register(ModEntityTypes.NOMAD.get(), BowmanRenderer::new );
        //EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossBowmanRenderer::new );
        EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitRenderer::new );
        //EntityRenderers.register(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseRenderer::new );
        //EntityRenderers.register(ModEntityTypes.ASSASSIN.get(), AssassinRenderer::new );
        //EntityRenderers.register(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinRenderer::new );
    }

}