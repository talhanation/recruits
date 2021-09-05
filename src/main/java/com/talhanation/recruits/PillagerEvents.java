package com.talhanation.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PillagerEvents {

    @SubscribeEvent
    public void attackRecruit(EntityJoinWorldEvent event) {
    Entity entity = event.getEntity();

    if (entity instanceof AbstractIllagerEntity){
        AbstractIllagerEntity illager = (AbstractIllagerEntity) entity;
        illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AbstractRecruitEntity.class, true));

    }

    if (entity instanceof MonsterEntity){
        MonsterEntity monster = (MonsterEntity) entity;
        monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractRecruitEntity.class, true));
    }

    }
}
