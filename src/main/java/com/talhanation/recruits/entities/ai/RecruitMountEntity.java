package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
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
        Main.LOGGER.debug(" Recruit should mount?: "+ recruit.getShouldMount());
        if (mount != null)Main.LOGGER.debug(" getMount(): "+ mount);
        if(this.recruit.getVehicle() == null && this.mount != null) {
            recruit.getNavigation().moveTo(mount, 1.15F);
            Main.LOGGER.debug("...Moving to Mount...");
            if(recruit.distanceTo(mount) < 2D){
                recruit.startRiding(mount);
            }
        }

        if (this.recruit.getVehicle() != null && recruit.getVehicle().equals(mount) || this.mount == null || !mount.canAddPassenger(recruit)) {
            recruit.setShouldMount(false);
            this.mount = null;
            recruit.setMountUUID(Optional.empty());
            Main.LOGGER.debug("boot wurde gefunden und mounted oder es wurde zerstört oder es kann nicht gemountet werden");
        }
    }

    private Entity findMount(){
        Main.LOGGER.debug("...Searching Mount...");
        List<Entity> list = recruit.level.getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(32D));
        for(Entity mount : list){
            if (recruit.getMountUUID() != null && mount.getUUID().equals(recruit.getMountUUID()) && mount.canAddPassenger(this.recruit)){
                this.mount = mount;
            }
        }
        return null;
    }
}