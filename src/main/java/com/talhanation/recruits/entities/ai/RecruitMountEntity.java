package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;
import java.util.Optional;

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
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if(this.recruit.getVehicle() == null && this.mount != null) {
            recruit.getNavigation().moveTo(mount, 1.15F);
            if(recruit.distanceTo(mount) < 2D){
                recruit.startRiding(mount);
            }
        }

        if (this.recruit.getVehicle() != null && recruit.getVehicle().equals(mount)) {
            recruit.setShouldMount(false);
            this.mount = null;
            recruit.setMountUUID(Optional.empty());
        }
    }

    private void findMount(){
        List<Entity> list = recruit.level.getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(32D));
        for(Entity mount : list){
            if (recruit.getMountUUID() != null && mount.getUUID().equals(recruit.getMountUUID()) && RecruitsModConfig.MountWhiteList.get().contains(mount.getEncodeId())){
                this.mount = mount;
            }
        }
    }
}