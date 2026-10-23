package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPathfinder;
import com.talhanation.recruits.pathfinding.NodeEvaluatorGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RecruitPathNavigation extends AsyncGroundPathNavigation {
    AbstractRecruitEntity recruit;
    /** While true, {@link RecruitClimbGoal} steers the recruit on a ladder; the path is still followed. */
    private boolean climbControlled;

    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new RecruitsPathNodeEvaluator();

        nodeEvaluator.setCanOpenDoors(true);
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanFloat(true);

        return nodeEvaluator;
    };

    public RecruitPathNavigation(AbstractRecruitEntity recruit, Level world) {
        super(recruit, world);
        this.recruit = recruit;
    }

    // called from the super constructors: fields of this class (recruit) are not set yet here
    @Override
    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        if (RecruitsServerConfig.UseAsyncPathfinding.get()) {
            return new AsyncPathfinder(this.nodeEvaluator, range, nodeEvaluatorGenerator, this.level);
        }
        return new PathFinder(this.nodeEvaluator, range);
    }

    public void setClimbControlled(boolean climbControlled) {
        this.climbControlled = climbControlled;
    }

    @Override
    protected void moveTowards(Vec3 target) {
        if (!this.climbControlled) super.moveTowards(target);
    }

    // also keep following (and creating) paths while hanging on a ladder
    @Override
    protected boolean canUpdatePath() {
        return super.canUpdatePath() || this.mob.onClimbable();
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        this.recruit.setMaxFallDistance(1);
        ((RecruitsPathNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return this.moveTo(this.createPath(new BlockPos((int) x, (int) y, (int) z), 0), speed);
    }
}
