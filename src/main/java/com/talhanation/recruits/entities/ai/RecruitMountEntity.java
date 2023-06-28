package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.List;

public class RecruitMountEntity extends Goal {

    private final AbstractRecruitEntity recruit;
    private Entity mount;

    public RecruitMountEntity(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getShouldMount();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start(){
        this.findMount();
        this.recruit.setMountTimer(150);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if(this.recruit.getVehicle() == null && this.mount != null) {
            //if(mount.canAddPassenger()) {
            if(recruit.getMountTimer() > 0){
                recruit.getNavigation().moveTo(mount, 1.15F);
                if (recruit.distanceTo(mount) < 2D) {
                    recruit.startRiding(mount);
                }
            }
            else {
                this.clearMount();
            }
        }


        if (this.recruit.getVehicle() != null && recruit.getVehicle().equals(mount)) {
            this.clearMount();
        }
    }

    private void clearMount() {
        recruit.clearMount();
        this.mount = null;
    }

    private void findMount(){
        List<Entity> list = recruit.level.getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(32D));
        for(Entity mount : list){
            if (recruit.getMountUUID() != null && mount.getUUID().equals(recruit.getMountUUID()) && (RecruitsModConfig.MountWhiteList.get().contains(mount.getEncodeId()) || mount instanceof AbstractHorse)){
                this.mount = mount;
            }
        }
    }
}