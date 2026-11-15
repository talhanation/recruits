package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncPath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Moves a recruit on ladders / vines along its path (the pathfinder routes over climbable columns, see
 * RecruitsPathNodeEvaluator#addClimbNeighbors).
 * <p>
 * Vanilla can not do this: the MoveControl jumps inside a ladder block (it has a collision shape), and
 * jumping on a ladder always means "climb up". So while the recruit is in a climbable block, or about to
 * drop into one from above, the MoveControl is parked and the velocity is set directly.
 * <p>
 * The rules only look at where the recruit is and at which height its path leaves the ladder column -
 * not at which path node the navigation currently calls "next":
 * <ul>
 *     <li>the path leaves the column further up: climb up</li>
 *     <li>the path leaves the column further down: climb down</li>
 *     <li>the height fits: step off towards the exit</li>
 * </ul>
 * The recruit is kept 0.1 away from the ladder plate, otherwise it ends up standing on the plate.
 * Closed wooden trapdoors in the column are opened and closed again once passed.
 * The navigation keeps following the path. No goal flags, so it never blocks other goals.
 */
public class RecruitClimbGoal extends Goal {

    private static final double CLIMB_UP_SPEED = 0.2D;
    private static final double CLIMB_DOWN_SPEED = 0.15D;
    private static final double HORIZONTAL_SPEED = 0.15D;
    /** Distance kept from the ladder / trapdoor plate. Small enough that the navigation still counts the node as reached. */
    private static final double WALL_DISTANCE = 0.1D;
    /** Ticks without getting anywhere until the path is dropped. */
    private static final int STUCK_TICKS = 60;
    /** Moving less than this does not count as getting anywhere. */
    private static final double PROGRESS_DISTANCE = 0.5D;
    private static final int TRAPDOOR_CLOSE_TIMEOUT = 60;

    private final AbstractRecruitEntity recruit;
    @Nullable
    private Vec3 lastPos;
    private int ticksWithoutProgress;
    @Nullable
    private BlockPos openedTrapdoor;
    private boolean passedTrapdoor;
    private int trapdoorTicks;

    public RecruitClimbGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return this.shouldClimb();
    }

    @Override
    public boolean canContinueToUse() {
        return this.shouldClimb() || this.openedTrapdoor != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.lastPos = null;
        this.ticksWithoutProgress = 0;
    }

    @Override
    public void stop() {
        this.setClimbControlled(false);
        if (this.openedTrapdoor != null) {
            this.setTrapdoorOpen(this.openedTrapdoor, false);
            this.openedTrapdoor = null;
        }
    }

    @Override
    public void tick() {
        boolean climb = this.shouldClimb();
        this.setClimbControlled(climb);
        if (climb) this.climb();
        this.tickTrapdoor();
    }

    // ------------------------------------------------------------------ when

    private boolean shouldClimb() {
        if (this.recruit.isPassenger()) return false;
        Path path = this.recruit.getNavigation().getPath();
        if (path == null) return false;

        if (isCalculating(path)) return this.isOnLadder() && !this.recruit.onGround(); // hold on until the new path is there
        if (path.isDone()) return false;
        if (this.isOnLadder()) return true;

        // standing beside the top of a ladder and the path goes down into it
        Node to = path.getNextNode();
        if (to.y >= this.recruit.getY() - 0.5D || !this.canClimbAt(to.x, to.y, to.z)) return false;
        // ... really down: not the ladder the recruit just climbed out of (the navigation may be one node behind)
        int after = path.getNextNodeIndex() + 1;
        return after >= path.getNodeCount() || path.getNode(after).y <= to.y;
    }

    private static boolean isCalculating(Path path) {
        return path instanceof AsyncPath asyncPath && !asyncPath.isProcessed();
    }

    /** Feet in a climbable block, or in the air right above one (dropping in from above, climbing out at the top). */
    private boolean isOnLadder() {
        BlockPos feet = this.recruit.blockPosition();
        return this.canClimbAt(feet.getX(), feet.getY(), feet.getZ())
                || (!this.recruit.onGround() && this.canClimbAt(feet.getX(), feet.getY() - 1, feet.getZ()));
    }

    private boolean canClimbAt(int x, int y, int z) {
        return ClimbableBlocks.get().canClimbAt(this.recruit.level(), x, y, z);
    }

    // ------------------------------------------------------------------ how

    private void climb() {
        // park the vanilla MoveControl: no jumping, no forward walking
        this.recruit.getMoveControl().setWantedPosition(this.recruit.getX(), this.recruit.getY(), this.recruit.getZ(), 0.0D);

        Path path = this.recruit.getNavigation().getPath();
        BlockPos feet = this.recruit.blockPosition();
        // only inside a climbable block the height is controlled, above it gravity does the work
        boolean inClimbable = this.canClimbAt(feet.getX(), feet.getY(), feet.getZ());
        double vy = this.recruit.getDeltaMovement().y;
        Vec3 target;

        if (path == null || isCalculating(path) || path.isDone()) {
            // hold on
            target = this.anchor(feet);
            if (inClimbable) vy = 0.0D;
        } else if (!this.isOnLadder()) {
            // beside the top of the ladder: walk over the edge and drop into the column
            Node to = path.getNextNode();
            target = this.anchor(new BlockPos(to.x, to.y, to.z));
            this.openTrapdoorsAhead(path);
            this.checkProgress();
        } else {
            // the rest of the path inside this column ("last"), and the node where it leaves the column ("exit")
            int index = path.getNextNodeIndex();
            Node last = null;
            while (index < path.getNodeCount() && path.getNode(index).x == feet.getX() && path.getNode(index).z == feet.getZ()) {
                last = path.getNode(index++);
            }
            Node exit = index < path.getNodeCount() ? path.getNode(index) : null; // null only if the path ends in the column

            // the height at which the recruit leaves the column
            // (a node that is higher but not beside the column is not reached by climbing here)
            int goalY = feet.getY();
            if (last != null) goalY = exit != null && exit.y > last.y ? exit.y : last.y;
            else if (exit.y <= feet.getY() || (Math.abs(exit.x - feet.getX()) <= 1 && Math.abs(exit.z - feet.getZ()) <= 1)) goalY = exit.y;
            double dy = goalY - this.recruit.getY();

            Vec3 column = this.anchor(feet);
            Vec3 out = exit == null ? column : new Vec3(exit.x + 0.5D, exit.y, exit.z + 0.5D);

            if (goalY > feet.getY()) {
                // up, in the column until the feet are above the exit edge
                target = column;
                if (inClimbable) vy = CLIMB_UP_SPEED;
            } else if (goalY < feet.getY() || (inClimbable && dy < -0.05D)) {
                // down, never jump off early
                target = column;
                if (inClimbable) vy = Math.max(-CLIMB_DOWN_SPEED, dy);
            } else {
                // right height: step off (or over the edge at the top), without sliding down meanwhile
                target = out;
                if (inClimbable && !this.recruit.onGround()) vy = 0.0D;
            }
            this.openTrapdoorsAhead(path);
            this.checkProgress();
        }

        double dx = target.x - this.recruit.getX();
        double dz = target.z - this.recruit.getZ();
        double factor = Math.min(0.5D, HORIZONTAL_SPEED / Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4D));
        this.recruit.setDeltaMovement(dx * factor, vy, dz * factor);
    }

    /**
     * Center of the ladder column at this block, moved away from the plate if it is a ladder or a trapdoor.
     * Also works for the block right above the ladder.
     */
    private Vec3 anchor(BlockPos pos) {
        Level level = this.recruit.level();
        BlockState state = level.getBlockState(pos);
        if (!hasPlate(state)) state = level.getBlockState(pos.below());

        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (hasPlate(state)) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING); // points away from the wall
            x += facing.getStepX() * WALL_DISTANCE;
            z += facing.getStepZ() * WALL_DISTANCE;
        }
        return new Vec3(x, pos.getY(), z);
    }

    private static boolean hasPlate(BlockState state) {
        return state.getBlock() instanceof LadderBlock || state.getBlock() instanceof TrapDoorBlock;
    }

    /** Something is in the way: drop the path instead of hanging on the ladder forever. */
    private void checkProgress() {
        Vec3 pos = this.recruit.position();
        if (this.lastPos == null || pos.distanceToSqr(this.lastPos) > PROGRESS_DISTANCE * PROGRESS_DISTANCE) {
            this.lastPos = pos;
            this.ticksWithoutProgress = 0;
        } else if (++this.ticksWithoutProgress >= STUCK_TICKS) {
            this.lastPos = null;
            this.ticksWithoutProgress = 0;
            this.recruit.getNavigation().stop();
        }
    }

    private void setClimbControlled(boolean controlled) {
        if (this.recruit.getNavigation() instanceof RecruitPathNavigation navigation) {
            navigation.setClimbControlled(controlled);
        }
    }

    // ------------------------------------------------------------------ trapdoors

    private void openTrapdoorsAhead(Path path) {
        int end = Math.min(path.getNextNodeIndex() + 2, path.getNodeCount());
        for (int i = path.getNextNodeIndex(); i < end; i++) {
            Node node = path.getNode(i);
            BlockPos pos = new BlockPos(node.x, node.y, node.z);
            if (!ClimbableBlocks.isClosedWoodenTrapdoor(this.recruit.level().getBlockState(pos))) continue;

            if (this.openedTrapdoor != null && !this.openedTrapdoor.equals(pos)) {
                this.setTrapdoorOpen(this.openedTrapdoor, false);
            }
            this.setTrapdoorOpen(pos, true);
            this.openedTrapdoor = pos;
            this.passedTrapdoor = false;
            this.trapdoorTicks = TRAPDOOR_CLOSE_TIMEOUT;
        }
    }

    /** Closes the opened trapdoor once the recruit went through it and left it (or after a timeout). */
    private void tickTrapdoor() {
        if (this.openedTrapdoor == null) return;

        AABB hatch = new AABB(this.openedTrapdoor);
        AABB body = this.recruit.getBoundingBox();
        if (body.intersects(hatch)) this.passedTrapdoor = true;

        boolean left = this.passedTrapdoor && !body.intersects(hatch.inflate(0.5D));
        if (left || --this.trapdoorTicks <= 0) {
            this.setTrapdoorOpen(this.openedTrapdoor, false);
            this.openedTrapdoor = null;
        }
    }

    private void setTrapdoorOpen(BlockPos pos, boolean open) {
        Level level = this.recruit.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TrapDoorBlock) || state.getValue(TrapDoorBlock.OPEN) == open) return;
        // never close it on someone climbing through
        if (!open && !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return;

        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(this.recruit, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }
}
