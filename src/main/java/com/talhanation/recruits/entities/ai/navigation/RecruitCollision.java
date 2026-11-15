package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Body collision of one recruit. Server side only, nothing is saved.
 * <p>
 * Recruits on foot collide hard, the rules below. Mounted recruits only collide soft: their mounts never
 * block anything, but two of them standing in each other are gently pushed apart
 * ({@link #separateFromMounts}). Hard collision with the wide hitbox of a horse jammed them at every ledge.
 * <ul>
 *     <li>Recruits are solid for each other ({@link #collides}), but only sideways: a recruit standing in a
 *     narrow passage blocks the others, nobody can stand on another recruit. Not on ladders. Players and other
 *     mobs are not affected.</li>
 *     <li>Queues: a recruit pressed against another recruit without progress first steps aside to the right
 *     or left, if there is free ground ({@link #trySidestep()}).</li>
 *     <li>Deadlock breaker: if that does not help for {@link #BLOCKED_TICKS_UNTIL_PASS} ticks (no room on either
 *     side), it may pass through recruits for {@link #PASS_THROUGH_TICKS} ticks.</li>
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
    /** The free spot is looked for one body width plus this to the side (recruit 0.8, horse 1.6). */
    private static final double SIDESTEP_MARGIN = 0.2D;
    /** Sideways push as part of that distance; with ground friction this carries the body about that far. */
    private static final double SIDESTEP_PUSH = 0.5D;
    /** Mounts closer than this part of their width are pushed apart (formation spacing is wider). */
    private static final double MOUNT_OVERLAP = 0.85D;
    /** Push per check (every 2nd tick), vanilla mobs push with 0.05 per tick. */
    private static final double MOUNT_PUSH = 0.08D;
    /** One sidestep per blockage: if the recruit is stuck again right after it, the deadlock breaker takes over. */
    private static final int SIDESTEP_COOLDOWN = 100;

    private final AbstractRecruitEntity recruit;
    private int blockedTicks;
    private int passThroughTicks;
    @Nullable
    private Vec3 lastCheckPos;
    private long nextSidestepTick;
    private long jumpCheckTick = -1;
    private boolean jumpUseful;

    public RecruitCollision(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    /**
     * The entity that moves and collides for this recruit: the horse / camel it rides, otherwise the recruit
     * itself. Null if it has no body collision at all (in a boat, on a siege weapon, ...).
     */
    @Nullable
    public static Entity bodyOf(AbstractRecruitEntity recruit) {
        Entity vehicle = recruit.getVehicle();
        if (vehicle == null) return recruit;
        return vehicle instanceof AbstractHorse ? vehicle : null;
    }

    /** The recruit that collides hard with this entity as its body: only a recruit on foot. */
    @Nullable
    private static AbstractRecruitEntity recruitOf(Entity body) {
        return body instanceof AbstractRecruitEntity recruit && !recruit.isPassenger() ? recruit : null;
    }

    /**
     * Recruit on foot vs recruit on foot: solid, unless one is dying or currently allowed to pass through.
     * Everything else (mounts, riders, other mobs) never collides here.
     * Only blocks sideways: if the other body is so far below or above that one could step or land
     * on its head (vertical overlap not larger than the step height), they do not collide. They then
     * overlap for a moment and the normal mob pushing separates them - no recruit ever stands on another.
     */
    public static boolean collides(Entity self, Entity other) {
        AbstractRecruitEntity recruit = recruitOf(self);
        if (recruit == null) return false;
        AbstractRecruitEntity otherRecruit = recruitOf(other);

        return otherRecruit != null
                && recruit.getCollision().passThroughTicks <= 0
                && otherRecruit.getCollision().passThroughTicks <= 0
                && !otherRecruit.isDeadOrDying()
                && verticalOverlap(self.getBoundingBox(), other.getBoundingBox()) > Math.max(self.getStepHeight(), other.getStepHeight()) + 0.01D
                // nobody blocks a ladder: a recruit on it can not step aside, so a climbing recruit passes
                // through the others - on the ladder and while getting on or off at its top and bottom
                && !isClimbing(recruit)
                && !isClimbing(otherRecruit);
    }

    private static boolean isClimbing(AbstractRecruitEntity recruit) {
        if (recruit.isPassenger()) return false;
        return recruit.getNavigation() instanceof RecruitPathNavigation navigation ? navigation.isClimbing() : recruit.onClimbable();
    }

    private static double verticalOverlap(AABB a, AABB b) {
        return Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY);
    }

    public void tick() {
        if (this.passThroughTicks > 0) {
            this.passThroughTicks--;
            return;
        }
        if ((this.recruit.tickCount + this.recruit.getId()) % 2 != 0) return;
        if (this.recruit.isPassenger()) {
            Entity mount = bodyOf(this.recruit);
            if (mount != null) this.separateFromMounts(mount);
            return;
        }
        if ((this.recruit.tickCount + this.recruit.getId()) % CHECK_INTERVAL != 0) return;

        Entity body = this.recruit;
        Vec3 pos = this.recruit.position();
        Vec3 lastPos = this.lastCheckPos;
        this.lastCheckPos = pos;

        // cheap checks first, the entity query only runs when the recruit is really stuck
        boolean stuck = lastPos != null
                && body.horizontalCollision
                && !this.recruit.getNavigation().isDone()
                && pos.subtract(lastPos).horizontalDistanceSqr() < MIN_PROGRESS_SQR
                && isPressedAgainstRecruit(body);

        this.blockedTicks = stuck ? this.blockedTicks + CHECK_INTERVAL : 0;
        if (this.blockedTicks >= BLOCKED_TICKS_UNTIL_PASS) {
            this.blockedTicks = 0;
            this.passThroughTicks = PASS_THROUGH_TICKS;
        } else if (stuck) {
            this.trySidestep(body);
        }
    }

    /**
     * Soft collision of mounted recruits, like the pushing of vanilla mobs (which skips ridden animals):
     * if the mount stands clearly inside the mount of another recruit, it is pushed away a little. It never
     * blocks, so ledges and slopes are no problem. Mounts that only touch (formation) are left alone.
     */
    private void separateFromMounts(Entity mount) {
        for (AbstractHorse other : mount.level().getEntitiesOfClass(AbstractHorse.class, mount.getBoundingBox(),
                horse -> horse != mount && horse.getFirstPassenger() instanceof AbstractRecruitEntity)) {
            double dx = mount.getX() - other.getX();
            double dz = mount.getZ() - other.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > (mount.getBbWidth() + other.getBbWidth()) * 0.5D * MOUNT_OVERLAP) continue;

            if (distance < 0.01D) {
                // exactly inside each other: any direction, but a different one for each
                dx = Math.cos(mount.getId());
                dz = Math.sin(mount.getId());
                distance = 1.0D;
            }
            mount.push(dx / distance * MOUNT_PUSH, 0.0D, dz / distance * MOUNT_PUSH);
        }
    }

    /**
     * Stuck behind another recruit: look right and left, and if there is free ground step aside. The recruit
     * then walks on next to the queue; its navigation brings it back to the path behind the blocker.
     * Even ids look right first, odd ids left first, so a queue fans out to both sides.
     */
    private void trySidestep(Entity body) {
        long now = body.level().getGameTime();
        if (now < this.nextSidestepTick || !body.onGround()) return;

        Vec3 dir = this.movingDirection(body);
        double distance = body.getBbWidth() + SIDESTEP_MARGIN;
        double side = (this.recruit.getId() & 1) == 0 ? 1.0D : -1.0D;
        for (int i = 0; i < 2; i++, side = -side) {
            double sx = -dir.z * side * distance;
            double sz = dir.x * side * distance;
            if (!this.canStepTo(body, sx, sz)) continue;

            body.setDeltaMovement(body.getDeltaMovement().add(sx * SIDESTEP_PUSH, 0.0D, sz * SIDESTEP_PUSH));
            this.nextSidestepTick = now + SIDESTEP_COOLDOWN;
            return;
        }
    }

    /** No block and no solid entity (recruit, siege weapon, ...) at that offset, and ground below it - never off an edge. */
    private boolean canStepTo(Entity body, double dx, double dz) {
        Level level = body.level();
        AABB box = body.getBoundingBox().move(dx, 0.0D, dz);
        // lifted slightly at the bottom so slightly uneven ground (path block, farmland) does not count as a wall
        AABB upper = new AABB(box.minX, box.minY + 0.1D, box.minZ, box.maxX, box.maxY, box.maxZ);
        // the ground has to be below the middle third of the body
        double inset = body.getBbWidth() / 3.0D;
        return level.noCollision(body, upper)
                && this.hasBlockCollision(level, box.deflate(inset, 0.0D, inset).move(0.0D, -0.6D, 0.0D));
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
        Vec3 dir = this.movingDirection(this.recruit);
        AABB box = this.recruit.getBoundingBox();
        // lifted slightly so the floor we stand on does not count
        AABB ahead = box.move(dir.x * PROBE_DISTANCE, 0.05D, dir.z * PROBE_DISTANCE);
        Level level = this.recruit.level();

        return this.hasBlockCollision(level, ahead)                                   // a block, not an entity
                && !this.hasBlockCollision(level, ahead.move(0, JUMP_CLEARANCE, 0))  // low enough to land on
                && !this.hasBlockCollision(level, box.move(0, JUMP_CLEARANCE, 0));   // head room
    }

    private Vec3 movingDirection(Entity body) {
        MoveControl move = this.recruit.getMoveControl(); // the one of the mount when riding
        if (move.hasWanted()) {
            double dx = move.getWantedX() - body.getX();
            double dz = move.getWantedZ() - body.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-3D) return new Vec3(dx / length, 0, dz / length);
        }
        return Vec3.directionFromRotation(0, body.getYRot());
    }

    private boolean hasBlockCollision(Level level, AABB box) {
        return level.getBlockCollisions(this.recruit, box).iterator().hasNext();
    }

    private static boolean isPressedAgainstRecruit(Entity body) {
        AABB box = body.getBoundingBox().inflate(0.15D, 0.0D, 0.15D);
        return !body.level().getEntities(body, box, other -> collides(body, other)).isEmpty();
    }
}
