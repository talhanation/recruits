package com.talhanation.recruits;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeEvent {
    protected final Random random = new Random();

    @SubscribeEvent
    public static void entityAttributeEvent(final EntityAttributeCreationEvent event) {
        //event.put(ModEntityTypes.ASSASSIN.get(), AssassinEntity.setAttributes().build());
        //event.put(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinLeaderEntity.setAttributes().build());
        event.put(ModEntityTypes.BOWMAN.get(), BowmanEntity.setAttributes().build());
        event.put(ModEntityTypes.CROSSBOWMAN.get(), CrossBowmanEntity.setAttributes().build());
        event.put(ModEntityTypes.NOMAD.get(), NomadEntity.setAttributes().build());
        event.put(ModEntityTypes.RECRUIT.get(), RecruitEntity.setAttributes().build());
        event.put(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitShieldmanEntity.setAttributes().build());
        event.put(ModEntityTypes.HORSEMAN.get(), HorsemanEntity.setAttributes().build());
        event.put(ModEntityTypes.MESSENGER.get(), MessengerEntity.setAttributes().build());
        event.put(ModEntityTypes.PATROL_LEADER.get(), PatrolLeaderEntity.setAttributes().build());
        event.put(ModEntityTypes.CAPTAIN.get(), CaptainEntity.setAttributes().build());
        event.put(ModEntityTypes.SCOUT.get(), ScoutEntity.setAttributes().build());
    }
}
