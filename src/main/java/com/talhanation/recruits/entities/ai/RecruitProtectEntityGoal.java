package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.List;
import java.util.Optional;

public class RecruitProtectEntityGoal extends Goal {

    private final AbstractRecruitEntity recruit;
    private LivingEntity protectingMob;
    private double range;
    private int timeToRecalcPath;

    public RecruitProtectEntityGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getShouldProtect();
    }

    public boolean canContinueToUse() {
        return canUse() && this.protectingMob != null;
    }

    public void start(){
        timeToRecalcPath = 0;
        this.getProtecting();
        //this.recruit.setIsFollowing(true);
        boolean isHorseBack = recruit.getVehicle() instanceof AbstractHorse;
        this.range = isHorseBack ? 20D : 10D;
    }

    public void clear(){
        recruit.shouldProtect(false,null);
        recruit.setFollowState(3);
        //this.recruit.setIsFollowing(false);
        this.protectingMob = null;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {

        if(this.protectingMob != null) {

            boolean isClose = protectingMob.distanceTo(this.recruit) <= range;
            if(!isClose){
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay(10);
                    recruit.getNavigation().moveTo(protectingMob, 1.15F);

                    if(this.recruit instanceof CaptainEntity captain && captain.getVehicle() instanceof Boat){
                        captain.setSailPos(this.protectingMob.getOnPos());
                    }
                }

                if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                    this.recruit.getJumpControl().jump();
                }
            }
        }

        if (this.protectingMob == null || !protectingMob.isAlive()) {
            if(this.protectingMob != null && !protectingMob.isAlive()){
                if (recruit.getOwner() != null)recruit.getOwner().sendSystemMessage(TEXT_PROTECT_DIED(recruit.getName().getString()));
            }
            clear();
        }

        if(this.recruit.tickCount % 20 == 0){
            this.getProtecting();
            this.checkMounts();
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

    private void checkMounts(){
        if(protectingMob != null){
            Entity protectingVehicle = this.protectingMob.getVehicle();
            Entity ownVehicle = this.recruit.getVehicle();
            if(protectingVehicle == null ){
                if(ownVehicle instanceof AbstractHorse) ownVehicle.stopRiding();
                else this.recruit.stopRiding();
            }
            else if(protectingVehicle instanceof Boat boat) {
                if(ownVehicle instanceof Boat && this.recruit instanceof CaptainEntity) {
                    return;
                }
                else if(ownVehicle instanceof AbstractHorse) {
                    ownVehicle.startRiding(boat);
                }
                else this.recruit.shouldMount(true, boat.getUUID());
            }
        }

    }
}