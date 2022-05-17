package com.talhanation.recruits;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class AttributeEvent {
    protected final Random random = new Random();

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.ASSASSIN.get(), AssassinEntity.setAttributes());
        event.put(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinLeaderEntity.setAttributes());
        event.put(ModEntityTypes.BOWMAN.get(), BowmanEntity.setAttributes());
        event.put(ModEntityTypes.CROSSBOWMAN.get(), CrossBowmanEntity.setAttributes());
        event.put(ModEntityTypes.NOMAD.get(), NomadEntity.setAttributes());
        event.put(ModEntityTypes.RECRUIT.get(), RecruitEntity.setAttributes());
        event.put(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseEntity.setAttributes());
        event.put(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitShieldmanEntity.setAttributes());
    }
}
