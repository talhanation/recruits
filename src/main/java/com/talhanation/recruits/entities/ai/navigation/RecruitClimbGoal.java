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
 * RecruitsPathNodeEvaluator#addClimbNeighbors). Climbing is fully controlled here instead of relying on
 * vanilla physics, which does not work for mobs on ladders:
 * <ul>
 *     <li>the vanilla MoveControl jumps inside a ladder block (it has a collision shape) and then keeps
 *     walking forward - up again, or off a free standing ladder,</li>
 *     <li>touching the wall on a ladder means climbing up, and a 0.6 wide mob in the column center is only
 *     ~0.01 away from the ladder plate - the slightest drift and it stands on the plate below or climbs.</li>
 * </ul>
 * So while active: MoveControl is parked, the recruit is held 0.15 away from the ladder plate, and its
 * velocity is set directly - up 0.2, down 0.15 per tick (vanilla ladder speeds). It enters a ladder from
 * above slowly, at the top it climbs until its feet are above the exit edge before stepping over, and at the
 * bottom it climbs down to the ground before stepping off.
 * Closed wooden trapdoors in the column are opened and closed again once passed.
 * The navigation keeps following the path. No goal flags, so it never blocks other goals.
 */
public class RecruitClimbGoal extends Goal {

    private static final double CLIMB_UP_SPEED = 0.2D;
    private static final double CLIMB_DOWN_SPEED = 0.15D;
    private static final double HORIZONTAL_SPEED = 0.15D;
    /** Distance kept from the ladder / trapdoor plate (no standing on it, no wall contact). */
    private static final double WALL_DISTANCE = 0.15D;
    /** Ticks without movement until the climb is given up and the path dropped. */
    private static final int STUCK_TICKS = 60;
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
        return this.shouldSteer();
    }

    @Override
    public boolean canContinueToUse() {
        return (this.shouldSteer() && this.ticksWithoutProgress < STUCK_TICKS) || this.openedTrapdoor != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.lastPos = this.recruit.position();
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
        boolean steer = this.shouldSteer();
        this.setClimbControlled(steer);
        if (steer) this.steer();
        this.tickTrapdoor();
    }

    // ------------------------------------------------------------------ when

    private boolean shouldSteer() {
        if (this.recruit.isPassenger()) return false;
        Path path = this.recruit.getNavigation().getPath();
        if (path == null) return false;

        boolean hanging = this.recruit.onClimbable() && !this.recruit.onGround();
        if (isCalculating(path)) return hanging; // hold on while a new path is being calculated
        if (path.isDone()) return false;

        Node to = path.getNextNode();
        return isClimbStep(path)
                || hanging
                // stepping down into a ladder column from above
                || (to.y < this.recruit.getY() - 0.5D && ClimbableBlocks.get().canClimbAt(this.recruit.level(), to.x, to.y, to.z));
    }

    private static boolean isCalculating(Path path) {
        return path instanceof AsyncPath asyncPath && !asyncPath.isProcessed();
    }

    /** Same column, other height. */
    private static boolean isClimbStep(Path path) {
        int next = path.getNextNodeIndex();
        if (next <= 0 || next >= path.getNodeCount()) return false;
        Node to = path.getNode(next);
        Node from = path.getNode(next - 1);
        return to.x == from.x && to.z == from.z && to.y != from.y;
    }

    // ------------------------------------------------------------------ how

    private void steer() {
        // park the vanilla MoveControl: no jumping, no forward walking
        this.recruit.getMoveControl().setWantedPosition(this.recruit.getX(), this.recruit.getY(), this.recruit.getZ(), 0.0D);

        Path path = this.recruit.getNavigation().getPath();
        BlockPos feet = this.recruit.blockPosition();
        boolean onLadder = this.recruit.onClimbable();
        double vy = this.recruit.getDeltaMovement().y;
        Vec3 target;

        if (path == null || isCalculating(path) || path.isDone()) {
            // hold position on the ladder
            target = this.anchor(feet.getX(), feet.getY(), feet.getZ());
            if (onLadder) vy = 0.0D;
        } else {
            Node to = path.getNextNode();
            double dy = to.y - this.recruit.getY();

            if (to.x == feet.getX() && to.z == feet.getZ()) {
                // in the column: straight up or down
                target = this.anchor(to.x, feet.getY(), to.z);
                if (onLadder) vy = dy > 0.05D ? CLIMB_UP_SPEED : dy < -0.05D ? -CLIMB_DOWN_SPEED : 0.0D;
            } else if (dy > 0.0D) {
                // leaving at the top: keep climbing, lean towards the exit for the last half block
                // (touching its edge only helps climbing) and step over once the feet are above it
                target = dy < 0.5D ? this.anchor(to.x, to.y, to.z) : this.anchor(feet.getX(), feet.getY(), feet.getZ());
                if (onLadder) vy = CLIMB_UP_SPEED;
            } else if (onLadder && !this.recruit.onGround() && dy < -0.05D) {
                // next node is beside and lower: climb down to its height first, never jump off early
                target = this.anchor(feet.getX(), feet.getY(), feet.getZ());
                vy = -CLIMB_DOWN_SPEED;
            } else {
                // stepping over into the next column: onto the ladder from above, or off it at the bottom
                target = this.anchor(to.x, to.y, to.z);
            }
            this.openTrapdoorsAhead(path);
        }

        double dx = target.x - this.recruit.getX();
        double dz = target.z - this.recruit.getZ();
        double factor = Math.min(0.5D, HORIZONTAL_SPEED / Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4D));
        this.recruit.setDeltaMovement(dx * factor, vy, dz * factor);

        this.checkProgress();
    }

    /** Block center, moved away from the plate if it is a ladder or a trapdoor. */
    private Vec3 anchor(int x, int y, int z) {
        double ax = x + 0.5D;
        double az = z + 0.5D;
        BlockState state = this.recruit.level().getBlockState(new BlockPos(x, y, z));
        if (state.getBlock() instanceof LadderBlock || state.getBlock() instanceof TrapDoorBlock) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING); // points away from the wall
            ax += facing.getStepX() * WALL_DISTANCE;
            az += facing.getStepZ() * WALL_DISTANCE;
        }
        return new Vec3(ax, y, az);
    }

    private void checkProgress() {
        Vec3 pos = this.recruit.position();
        if (this.lastPos == null || pos.distanceToSqr(this.lastPos) > 0.1D * 0.1D) {
            this.lastPos = pos;
            this.ticksWithoutProgress = 0;
        } else if (++this.ticksWithoutProgress >= STUCK_TICKS) {
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
