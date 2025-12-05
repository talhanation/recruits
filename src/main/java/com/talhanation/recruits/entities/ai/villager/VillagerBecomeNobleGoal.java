package com.talhanation.recruits.entities.ai.villager;

import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.List;

public class VillagerBecomeNobleGoal extends Goal {

    public Villager villager;

    private int timer;
    public VillagerBecomeNobleGoal(Villager villager) {
        this.villager = villager;
    }

    @Override
    public boolean canUse() {
        return !this.villager.isBaby() && !villager.isSleeping() && this.villager.getVillagerData().getProfession().equals(VillagerProfession.NONE);
    }

    @Override
    public boolean canContinueToUse() {
        return timer > 0;
    }

    @Override
    public void start() {
        super.start();
        timer = 1200 + villager.getRandom().nextInt(600);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.villager.getCommandSenderWorld().isClientSide()) return;
        if(timer > 0) timer--;
    }

    @Override
    public void stop() {
        super.stop();
        if(this.villager.getCommandSenderWorld().isClientSide()) return;
        List<LivingEntity> list = this.villager.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, this.villager.getBoundingBox().inflate(64))
                .stream()
                .toList();

        boolean noblePresent = list.stream().anyMatch(living -> living instanceof VillagerNobleEntity);
        if (noblePresent) {
            return;
        }

        int villagers = (int) list.stream().filter(e -> e instanceof Villager).count();
        if(villagers >= 7){
            VillagerEvents.createNobleVillager(villager);
        }
    }
}