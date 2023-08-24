package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.List;

public class RecruitProtectEntityGoal extends Goal {

    private final AbstractRecruitEntity recruit;
    private LivingEntity protectingMob;
    private double range;

    public RecruitProtectEntityGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getShouldProtect() && !recruit.needsToGetFood();
    }

    public boolean canContinueToUse() {
        return canUse() && this.protectingMob != null;
    }

    public void start(){
        this.getProtecting();
        boolean isHorseBack = recruit.getVehicle() instanceof AbstractHorse;
        this.range = isHorseBack ? 15D : 8D;
    }

    public void stop(){
        recruit.shouldProtect(false,null);
        recruit.setFollowState(3);
        this.protectingMob = null;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {

        if(this.protectingMob != null) {
            boolean isClose = protectingMob.distanceTo(this.recruit) <= range;
            if(!isClose){
                recruit.getNavigation().moveTo(protectingMob, 1.15F);
            }
        }

        if (this.protectingMob == null || !protectingMob.isAlive()) {
            if(this.protectingMob != null && !protectingMob.isAlive()){
                if (recruit.getOwner() != null)recruit.getOwner().sendSystemMessage(TEXT_PROTECT_DIED(recruit.getName().getString()));
            }
            stop();
        }
    }

    public void getProtecting(){
        List<LivingEntity> list = recruit.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, recruit.getBoundingBox().inflate(32D));
        for(LivingEntity livings : list){
            if (recruit.getProtectUUID() != null && livings.getUUID().equals(recruit.getProtectUUID())){
                this.protectingMob = livings;
            }
        }
    }
    private MutableComponent TEXT_PROTECT_DIED(String name) {
        return Component.translatable("chat.recruits.text.protect_died", name);
    }
}