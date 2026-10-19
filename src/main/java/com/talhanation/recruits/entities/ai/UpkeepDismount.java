package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mounted recruits on their way to the upkeep: they ride up close, get off and walk the last bit to the
 * container. The mount is remembered ({@link AbstractRecruitEntity#remountAfterUpkeep}) and they get back
 * on once the upkeep is done. Shared by the block and the entity upkeep goal.
 *
 * Dismounting near the container (instead of right at the start) keeps the mount inside the search
 * radius of {@link RecruitMountEntityGoal}, so finding it again afterwards is reliable.
 */
public class UpkeepDismount {

    /** Get off when this close to the container. */
    private static final double DISMOUNT_DISTANCE = 12.0D;
    /** Also get off when the mount has not come closer for this long (fence, door, narrow path). */
    private static final int STUCK_TICKS = 60;
    /** "Closer" means at least this much. */
    private static final double MIN_PROGRESS = 0.5D;

    private final AbstractRecruitEntity recruit;
    private double bestDistance = Double.MAX_VALUE;
    private int stuckTicks;

    public UpkeepDismount(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    public void reset() {
        this.bestDistance = Double.MAX_VALUE;
        this.stuckTicks = 0;
    }

    /**
     * Call every tick on the way to the upkeep.
     * @return true when the recruit just got off - the caller should recalculate his path right away,
     * the old one belonged to the mount's navigation.
     */
    public boolean tick(Vec3 target) {
        if (!recruit.isPassenger() || !recruit.canDismountForUpkeep(recruit.getVehicle())) return false;

        double distance = Math.sqrt(recruit.position().distanceToSqr(target));
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }

        if (distance <= DISMOUNT_DISTANCE || stuckTicks >= STUCK_TICKS) {
            recruit.dismountForUpkeep();
            reset();
            return !recruit.isPassenger();
        }
        return false;
    }
}
