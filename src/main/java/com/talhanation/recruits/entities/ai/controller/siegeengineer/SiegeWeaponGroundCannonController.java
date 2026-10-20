package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.smallships.GroundCannon;
import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.controller.ship.CaptainShipController;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPath;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Siege engineer at a Small Ships ground cannon. Like a player he only has the keys: A/D turn the gun
 * (1 degree per tick), the barrel keys lay it (0.75 degrees per tick), W drives it. The shot itself, cooldown
 * and fuze are Small Ships' (see {@link GroundCannon#fire()}).
 *
 * Aim: the pitch comes from the ball's real flight (Small Ships drag and gravity, step by step). The lead is
 * the captain's ({@link CaptainShipController#lead}): the ball hits where the target is after fuze plus flight -
 * a ship along its curve from its speed and turn rate, its crew turning with it, anything else along the way it
 * moved last tick. A ship is aimed at its hull, like the captain's round shot. Max barrel elevation is 30
 * degrees, which limits the reach to about 78 blocks with a plain ball (more with fine grain powder or downhill).
 */
public class SiegeWeaponGroundCannonController implements ISiegeController {
    public final boolean DEBUG = false;
    public static final int REACH = 50;
    public static final int RECALCULATION_TIME = 200;
    /** squared, horizontal - closer than 8 blocks a cannon is the wrong weapon */
    public static final float MIN_ENGAGE_DISTANCE = 64;
    /** squared, horizontal - about the reach of a plain ball at full elevation */
    public static final float MAX_ENGAGE_DISTANCE = 75 * 75;
    /** key deadbands in degrees, a little below one key step so the gun does not hunt */
    private static final float YAW_DEADBAND = 0.6F;
    private static final float PITCH_DEADBAND = 0.4F;
    /** fire when the gun lies this close to the solution */
    private static final float YAW_FIRE_TOLERANCE = 1.5F;
    private static final float PITCH_FIRE_TOLERANCE = 1.0F;
    private static final int MAX_FLIGHT_TICKS = 200;
    private static final double RAD_TO_DEG = 180.0D / Math.PI;

    public final SiegeEngineerEntity siegeEngineer;
    @Nullable public GroundCannon cannon;
    private final Level world;
    private final AsyncGroundPathNavigation pathNavigation;
    private Node currentNode;
    public Vec3 movementPos;
    public Path path;
    private int recalcPath;

    private boolean forward;
    private boolean left;
    private boolean right;
    private boolean barrelUp;
    private boolean barrelDown;

    // face rotation command
    private float faceYaw;
    private int faceTicks;

    // aim
    public Vec3 targetPos;
    private float desiredYaw;
    private float desiredPitch;
    private boolean hasSolution;
    private boolean noAmmoMessage;
    /** what the engineer picked (SiegeEngineerEntity#checkForPotentialEnemies): a ship, a siege weapon or a living */
    @Nullable private Entity targetEntity;
    /** tick of the last shot, for the fuze still to burn */
    private int firedAt = Integer.MIN_VALUE / 2;
    /** hull of the target ship relative to its position: {bottom, top}, for the ship of targetHullOf */
    private final float[] targetHull = new float[2];
    private int targetHullOf = -1;

    /**
     * What the gun aims at this tick and how it moves. x/z: the ship's centre (or the target itself), offset: where
     * on the ship the aim point is - it turns with her.
     */
    private record Mark(double x, double y, double z, float yaw, float speed, float rotSpeed, double offsetX, double offsetZ) {
        Vec3 lead(float ticks) {
            double[] centre = CaptainShipController.lead(this.x, this.y, this.z, this.yaw, this.speed, this.rotSpeed, ticks);
            double turn = Math.toRadians(this.rotSpeed * ticks);
            double cos = Math.cos(turn), sin = Math.sin(turn);
            return new Vec3(centre[0] + this.offsetX * cos - this.offsetZ * sin, centre[1], centre[2] + this.offsetX * sin + this.offsetZ * cos);
        }
    }

    public SiegeWeaponGroundCannonController(SiegeEngineerEntity siegeEngineer, Level world) {
        this.pathNavigation = new RecruitPathNavigation(siegeEngineer, world);
        this.world = world;
        this.siegeEngineer = siegeEngineer;
        this.pathNavigation.setCanFloat(false);
    }

    @Override
    public void tryMount(Entity entity) {
        if (GroundCannon.isGroundCannon(entity)) {
            this.cannon = new GroundCannon(entity, this.siegeEngineer);
            GroundCannon.Rules.load();
        }
    }

    @Override
    public void tryDismount() {
        this.reset();
        this.cannon = null;
    }

    @Override
    public void tick() {
        if (this.world.isClientSide() || cannon == null || siegeEngineer.getVehicle() != cannon.getEntity()) return;
        if (!cannon.isGunnerDriver()) return;

        tickControls();

        // only written to the gun's synched data when a key actually changes
        cannon.pressKeys(forward, false, left, right);
        cannon.pressBarrelKeys(barrelUp, barrelDown);
    }

    private void tickControls() {
        forward = false;
        left = false;
        right = false;
        barrelUp = false;
        barrelDown = false;

        if (faceTicks > 0) {
            faceTicks--;
            steerYaw(faceYaw);
            if (Math.abs(Mth.degreesDifference(gunYaw(), faceYaw)) < 3.0F) faceTicks = 0;
            return;
        }

        if (updateAttacking()) return;
        updateMovement();
    }

    // ========================= ATTACKING =========================

    @Override
    public boolean updateAttacking() {
        if (cannon == null || !this.siegeEngineer.getShouldRanged()) return false;

        calculateTargetPos();

        // a burning fuze: hold the lay, the shot leaves with whatever the barrel points at
        if (targetPos == null) return cannon.isFuzing();

        Mark mark = getMark();
        Vec3 aim = solve(mark);
        if (!hasSolution) return cannon.isFuzing();

        siegeEngineer.getLookControl().setLookAt(aim.x, aim.y, aim.z);
        steerYaw(desiredYaw);
        steerPitch(desiredPitch);

        boolean aligned = Math.abs(Mth.degreesDifference(gunYaw(), desiredYaw)) <= YAW_FIRE_TOLERANCE
                && Math.abs(gunPitch() - desiredPitch) <= PITCH_FIRE_TOLERANCE;

        if (aligned && cannon.isReady()) {
            if (cannon.fire()) {
                firedAt = siegeEngineer.tickCount;
                noAmmoMessage = false;
                if (DEBUG && siegeEngineer.getOwner() != null)
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FIRE!"));
            }
            else if (!cannon.hasAmmo() && !noAmmoMessage) {
                noAmmoMessage = true;
                if (siegeEngineer.getOwner() != null) {
                    siegeEngineer.getOwner().sendSystemMessage(Component.translatable("chat.recruits.text.noCannonAmmo", siegeEngineer.getName().getString()));
                }
            }
        }
        return true;
    }

    private void calculateTargetPos() {
        if (siegeEngineer.tickCount % 20 != 0) return;

        if (siegeEngineer.getShouldStrategicFire() && siegeEngineer.getStrategicFirePos() != null) {
            this.setTargetPos(siegeEngineer.getStrategicFirePos().getCenter());
        }
        else {
            this.siegeEngineer.checkForPotentialEnemies();
        }

        if (targetPos != null) {
            double dx = targetPos.x - cannon.getEntity().getX();
            double dz = targetPos.z - cannon.getEntity().getZ();
            double distance = dx * dx + dz * dz;
            if (distance < MIN_ENGAGE_DISTANCE || distance > MAX_ENGAGE_DISTANCE) targetPos = null;
        }
    }

    /** The entity the engineer picked, refreshed with every search (see SiegeEngineerEntity#checkForPotentialEnemies). */
    public void setTargetEntity(@Nullable Entity entity) {
        this.targetEntity = entity;
    }

    /** The captain's marks: a ship at its hull, its crew where they stand on it, a living at its middle, strategic fire as given. */
    private Mark getMark() {
        Entity entity = siegeEngineer.getShouldStrategicFire() ? null : targetEntity;
        if (entity == null || !entity.isAlive()) {
            LivingEntity target = siegeEngineer.getShouldStrategicFire() ? null : siegeEngineer.getTarget();
            entity = target != null && target.isAlive() ? target : null;
        }
        if (entity == null) return new Mark(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F, 0.0F, 0.0D, 0.0D);

        Entity ship = SmallShipsBridge.isShip(entity) ? entity : SmallShipsBridge.isShip(entity.getVehicle()) ? entity.getVehicle() : null;
        if (ship != null) {
            float yaw = ship.getYRot(), speed = SmallShipsBridge.getSpeed(ship), rotSpeed = SmallShipsBridge.getRotSpeed(ship);
            if (ship == entity) {
                float[] hull = hullOf(ship);
                return new Mark(ship.getX(), ship.getY() + hull[0] + (hull[1] - hull[0]) * 0.6D, ship.getZ(), yaw, speed, rotSpeed, 0.0D, 0.0D);
            }
            return new Mark(ship.getX(), entity.getY() + entity.getBbHeight() * 0.5D, ship.getZ(), yaw, speed, rotSpeed,
                    entity.getX() - ship.getX(), entity.getZ() - ship.getZ());
        }

        // anything else: the way it moved last tick
        double dx = entity.getX() - entity.xo, dz = entity.getZ() - entity.zo;
        float speed = (float) Math.hypot(dx, dz);
        float yaw = speed > 0.01F ? CaptainShipController.yawTo(dx, dz) : entity.getYRot();
        return new Mark(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), yaw, speed, 0.0F, 0.0D, 0.0D);
    }

    /** @return {bottom, top} of the ship's hull above its position, masts left out */
    private float[] hullOf(Entity ship) {
        if (targetHullOf != ship.getId()) {
            targetHullOf = ship.getId();
            float bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;
            for (AABB box : SmallShipsBridge.getHullBoxes(ship)) {
                bottom = Math.min(bottom, (float) (box.minY - ship.getY()));
                top = Math.max(top, (float) (box.maxY - ship.getY()));
            }
            targetHull[0] = bottom > top ? 0.0F : bottom;
            targetHull[1] = bottom > top ? 2.0F : top;
        }
        return targetHull;
    }

    /** @return ticks of fuze still to burn: the rest of a burning one, the mean of a new one (as the captain's remainingFuze) */
    private float fuzeTicks() {
        float min = GroundCannon.Rules.fuzeMin, max = GroundCannon.Rules.fuzeMax;
        int elapsed = siegeEngineer.tickCount - firedAt;
        if (elapsed >= 0 && elapsed <= max && cannon.isFuzing()) return Math.max(0.0F, (Math.max(min, elapsed + 1) + max) / 2.0F - elapsed);
        return (min + max) / 2.0F;
    }

    /**
     * Sets desiredYaw / desiredPitch: the ball hits where the mark is after fuze plus flight (three passes,
     * like the captain's solve).
     *
     * @return the point aimed at
     */
    private Vec3 solve(Mark mark) {
        Vec3 muzzle = cannon.getMuzzle();
        Vec3 pivot = cannon.getEntity().position();
        double muzzleForward = Math.sqrt(Mth.square(muzzle.x - pivot.x) + Mth.square(muzzle.z - pivot.z));
        double speed = cannon.getNextShotSpeed();
        float fuze = fuzeTicks();

        Vec3 aim = mark.lead(fuze);
        hasSolution = false;
        for (int pass = 0; pass < 3; pass++) {
            double dx = aim.x - pivot.x;
            double dz = aim.z - pivot.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz) - muzzleForward;
            double height = aim.y - muzzle.y;

            float[] solution = solvePitch(horizontal, height, speed, GroundCannon.Rules.drag, GroundCannon.Rules.gravity);
            if (solution == null) {
                hasSolution = false;
                return aim;
            }

            desiredYaw = (float) (Mth.atan2(dz, dx) * RAD_TO_DEG) - 90.0F;
            desiredPitch = solution[0];
            hasSolution = true;

            if (mark.speed() == 0.0F && mark.rotSpeed() == 0.0F) return aim;
            if (pass < 2) aim = mark.lead(fuze + solution[1]);
        }
        return aim;
    }

    /**
     * The ball's flight as Small Ships moves it: position += velocity, then velocity * drag and y - gravity.
     *
     * @return {pitch in entity x rot convention (negative = up), flight ticks} or null if out of reach
     */
    @Nullable
    public static float[] solvePitch(double horizontal, double height, double speed, double drag, double gravity) {
        if (horizontal <= 0) return null;
        float lowest = -GroundCannon.PITCH_MAX;  // elevation in degrees, positive = up
        float highest = -GroundCannon.PITCH_MIN;

        double[] high = heightAt(highest, horizontal, speed, drag, gravity);
        if (high == null || high[0] < height) return null;          // not even at full elevation
        double[] low = heightAt(lowest, horizontal, speed, drag, gravity);
        if (low != null && low[0] >= height) {
            // even the lowest lay flies over it: close and far below the gun, it cannot depress that much
            return low[0] - height < 1.5D ? new float[]{-lowest, (float) low[1]} : null;
        }

        float a = lowest, b = highest;
        double[] best = high;
        for (int i = 0; i < 16; i++) {
            float mid = (a + b) * 0.5F;
            double[] at = heightAt(mid, horizontal, speed, drag, gravity);
            if (at == null || at[0] < height) a = mid;
            else { b = mid; best = at; }
        }
        return new float[]{-b, (float) best[1]};
    }

    /** @return {height at the horizontal distance, ticks} or null if the ball never gets that far */
    @Nullable
    private static double[] heightAt(float elevation, double horizontal, double speed, double drag, double gravity) {
        double rad = Math.toRadians(elevation);
        double vx = speed * Math.cos(rad);
        double vy = speed * Math.sin(rad);
        double x = 0, y = 0;
        for (int tick = 1; tick <= MAX_FLIGHT_TICKS; tick++) {
            double nx = x + vx;
            double ny = y + vy;
            if (nx >= horizontal) {
                double f = (horizontal - x) / (nx - x);
                return new double[]{y + (ny - y) * f, tick - 1 + f};
            }
            x = nx;
            y = ny;
            vx *= drag;
            vy = vy * drag - gravity;
            if (vx < 1.0E-3) return null;
        }
        return null;
    }

    // ========================= KEYS =========================

    private float gunYaw() {
        return cannon.getEntity().getYRot();
    }

    private float gunPitch() {
        return cannon.getEntity().getXRot();
    }

    /** D (right) turns the gun to a higher yaw, A (left) to a lower one. */
    private void steerYaw(float yaw) {
        float diff = Mth.degreesDifference(gunYaw(), yaw);
        right = diff > YAW_DEADBAND;
        left = diff < -YAW_DEADBAND;
    }

    private void steerPitch(float pitch) {
        float diff = pitch - gunPitch();
        barrelUp = diff < -PITCH_DEADBAND;
        barrelDown = diff > PITCH_DEADBAND;
    }

    @Override
    public void startFaceRotation(float yaw) {
        this.faceYaw = Mth.wrapDegrees(yaw);
        this.faceTicks = 200;
    }

    // ========================= MOVEMENT =========================

    private void updateMovement() {
        calculateMovementPos();
        if (movementPos == null) {
            path = null;
            currentNode = null;
            return;
        }

        double distance = siegeEngineer.distanceToSqr(movementPos.x(), siegeEngineer.getY(), movementPos.z());
        double reach = siegeEngineer.getFollowState() == 1 || siegeEngineer.getFollowState() == 5 ? REACH * 2 : REACH;
        if (distance < reach) {
            path = null;
            currentNode = null;
            return;
        }

        if (--recalcPath <= 0) {
            recalcPath = RECALCULATION_TIME;
            this.path = pathNavigation.createPath(movementPos.x, movementPos.y, movementPos.z, 0);
            this.currentNode = null;
        }
        if (path != null && currentNode == null && (!(path instanceof AsyncPath ap) || ap.isProcessed()) && !path.isDone()) {
            this.currentNode = path.getNextNode();
        }
        if (path != null && currentNode != null) {
            Entity gun = cannon.getEntity();
            if (gun.distanceToSqr(currentNode.x + 0.5D, gun.getY(), currentNode.z + 0.5D) < 4.0D) {
                path.advance();
                currentNode = path.isDone() ? null : path.getNextNode();
            }
        }

        Vec3 goal = currentNode != null ? new Vec3(currentNode.x + 0.5D, 0, currentNode.z + 0.5D) : movementPos;
        Entity gun = cannon.getEntity();
        float yaw = (float) (Mth.atan2(goal.z - gun.getZ(), goal.x - gun.getX()) * RAD_TO_DEG) - 90.0F;
        steerYaw(yaw);
        // level the barrel while driving, drive only when roughly pointing the right way
        steerPitch(0.0F);
        forward = Math.abs(Mth.degreesDifference(gunYaw(), yaw)) < 20.0F;
    }

    private void calculateMovementPos() {
        movementPos = null;
        switch (siegeEngineer.getFollowState()) {
            case 0 -> {
                if (siegeEngineer.getShouldMovePos() && siegeEngineer.getMovePos() != null) {
                    movementPos = siegeEngineer.getMovePos().getCenter();
                }
            }
            case 1 -> {
                LivingEntity owner = siegeEngineer.getOwner();
                if (owner != null) movementPos = owner.position();
            }
            case 2, 3, 4 -> movementPos = siegeEngineer.getHoldPos();
            case 5 -> {
                LivingEntity protect = siegeEngineer.getProtectingMob();
                if (protect != null) movementPos = protect.position();
            }
        }
    }

    @Override
    public void calculatePath() {
        this.recalcPath = 0;
    }

    // ========================= REST =========================

    @Override
    public void reset() {
        forward = left = right = barrelUp = barrelDown = false;
        faceTicks = 0;
        if (cannon != null) cannon.releaseKeys();
    }

    @Override
    public Entity getSiegeEntity() {
        return cannon == null ? null : cannon.getEntity();
    }

    @Override
    public Vec3 getTargetPos() {
        return targetPos;
    }

    @Override
    public void setTargetPos(Vec3 vec3) {
        this.targetPos = vec3;
    }

    /** Small Ships' ground cannon has health but no repair - a player cannot mend it either. */
    @Override
    public boolean needsRepair() {
        return false;
    }

    @Override
    public boolean canRepair() {
        return false;
    }

    @Override
    public void tryRepair() {
    }
}
