package com.talhanation.recruits;


import com.talhanation.recruits.entities.ai.HorseAIRecruitRide;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RecruitEvents {


    @SubscribeEvent
    public void abstractHorseAi(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorseEntity){
            AbstractHorseEntity horse = (AbstractHorseEntity) entity;
            horse.goalSelector.addGoal(0, new HorseAIRecruitRide(horse, 1.5D));
        }
    }
}
