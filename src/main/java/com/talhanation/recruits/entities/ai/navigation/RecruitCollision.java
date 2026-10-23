package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Body collision of one recruit. Server side only, nothing is saved.
 * <ul>
 *     <li>Recruits are solid for each other ({@link #collides}), but only sideways: a recruit standing in a
 *     narrow passage blocks the others, nobody can stand on another recruit. Players and other mobs are not affected.</li>
 *     <li>Deadlock breaker: a recruit pressed against another recruit without progress for
 *     {@link #BLOCKED_TICKS_UNTIL_PASS} ticks may pass through recruits for {@link #PASS_THROUGH_TICKS} ticks.</li>
 *     <li>{@link #tryJumpOverTerrain()}: only jumps if that actually clears a block in front -
 *     never at entities, never at walls.</li>
 * </ul>
 */
public class RecruitCollision {

    private static final int CHECK_INTERVAL = 10;
    private static final int BLOCKED_TICKS_UNTIL_PASS = 60;
    private static final int PASS_THROUGH_TICKS = 40;
    /** Less horizontal movement than this (squared) per check interval counts as "stuck". */
    private static final double MIN_PROGRESS_SQR = 0.5D * 0.5D;
    /** How far ahead the jump check looks for a block. */
    private static final double PROBE_DISTANCE = 0.3D;
    /** A jump reaches ~1.25 blocks; to land on a full block the box must be free 1.0 above, plus margin. */
    private static final double JUMP_CLEARANCE = 1.05D;

    private final AbstractRecruitEntity recruit;
    private int blockedTicks;
    private int passThroughTicks;
    @Nullable
    private Vec3 lastCheckPos;
    private long jumpCheckTick = -1;
    private boolean jumpUseful;

    public RecruitCollision(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    /**
     * Recruit vs recruit: solid, unless one is mounted, dying or currently allowed to pass through.
     * Only blocks sideways: if the other recruit is so far below or above that one could step or land
     * on its head (vertical overlap not larger than the step height), they do not collide. They then
     * overlap for a moment and the normal mob pushing separates them - no recruit ever stands on another.
     */
    public static boolean collides(AbstractRecruitEntity self, AbstractRecruitEntity other) {
        return self.getCollision().passThroughTicks <= 0
                && other.getCollision().passThroughTicks <= 0
                && !self.isPassenger()
                && !other.isPassenger()
                && !other.isDeadOrDying()
                && verticalOverlap(self.getBoundingBox(), other.getBoundingBox()) > Math.max(self.getStepHeight(), other.getStepHeight()) + 0.01D;
    }

    private static double verticalOverlap(AABB a, AABB b) {
        return Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY);
    }

    public void tick() {
        if (this.passThroughTicks > 0) {
            this.passThroughTicks--;
            return;
        }
        if ((this.recruit.tickCount + this.recruit.getId()) % CHECK_INTERVAL != 0) return;

        Vec3 pos = this.recruit.position();
        Vec3 lastPos = this.lastCheckPos;
        this.lastCheckPos = pos;

        // cheap checks first, the entity query only runs when the recruit is really stuck
        boolean stuck = lastPos != null
                && this.recruit.horizontalCollision
                && !this.recruit.isPassenger()
                && !this.recruit.getNavigation().isDone()
                && pos.subtract(lastPos).horizontalDistanceSqr() < MIN_PROGRESS_SQR
                && this.isPressedAgainstRecruit();

        this.blockedTicks = stuck ? this.blockedTicks + CHECK_INTERVAL : 0;
        if (this.blockedTicks >= BLOCKED_TICKS_UNTIL_PASS) {
            this.blockedTicks = 0;
            this.passThroughTicks = PASS_THROUGH_TICKS;
        }
    }

    /**
     * Use this instead of "jump on every horizontal collision". Jumps only if a block (not an entity)
     * is in the way, that block is low enough to land on, and there is head room.
     * Result is cached per tick because several goals may ask in the same tick.
     */
    public void tryJumpOverTerrain() {
        if (!(this.recruit.horizontalCollision || this.recruit.minorHorizontalCollision) || !this.recruit.onGround()) return;

        long now = this.recruit.level().getGameTime();
        if (now != this.jumpCheckTick) {
            this.jumpCheckTick = now;
            this.jumpUseful = this.canJumpOverBlockAhead();
        }
        if (this.jumpUseful) this.recruit.getJumpControl().jump();
    }

    private boolean canJumpOverBlockAhead() {
        Vec3 dir = this.movingDirection();
        AABB box = this.recruit.getBoundingBox();
        // lifted slightly so the floor we stand on does not count
        AABB ahead = box.move(dir.x * PROBE_DISTANCE, 0.05D, dir.z * PROBE_DISTANCE);
        Level level = this.recruit.level();

        return this.hasBlockCollision(level, ahead)                                   // a block, not an entity
                && !this.hasBlockCollision(level, ahead.move(0, JUMP_CLEARANCE, 0))  // low enough to land on
                && !this.hasBlockCollision(level, box.move(0, JUMP_CLEARANCE, 0));   // head room
    }

    private Vec3 movingDirection() {
        MoveControl move = this.recruit.getMoveControl();
        if (move.hasWanted()) {
            double dx = move.getWantedX() - this.recruit.getX();
            double dz = move.getWantedZ() - this.recruit.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-3D) return new Vec3(dx / length, 0, dz / length);
        }
        return Vec3.directionFromRotation(0, this.recruit.getYRot());
    }

    private boolean hasBlockCollision(Level level, AABB box) {
        return level.getBlockCollisions(this.recruit, box).iterator().hasNext();
    }

    private boolean isPressedAgainstRecruit() {
        AABB box = this.recruit.getBoundingBox().inflate(0.15D, 0.0D, 0.15D);
        return !this.recruit.level().getEntitiesOfClass(AbstractRecruitEntity.class, box,
                other -> other != this.recruit && collides(this.recruit, other)).isEmpty();
    }
}
