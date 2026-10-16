package com.talhanation.recruits.entities.ai.controller.ship;

import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import com.talhanation.recruits.entities.CaptainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * The captain at the helm of a Small Ships ship.
 *
 * Decides WHAT to do each tick and leaves the HOW to {@link ShipPilot}, which
 * answers with the keys a player would press. Those go to the ship through
 * {@link SmallShipsBridge} and nowhere else - no yaw, speed or sail state is
 * ever written directly.
 *
 * The goal is the captain's sail position, which the existing commands already
 * set: move, hold, follow, protect, patrol waypoints and wander. Following and
 * protecting keep pace with the moving goal; everything else sails there and
 * keeps station.
 *
 * Only active with a Small Ships that offers the bridge. Without it the old
 * SmallShipsController keeps working as before.
 */
public class ShipController {
    /** ticks a face command may take before it gives up */
    private static final int FACE_TIMEOUT = 200;
    /** ticks between two reads of the ship's condition (damage, wind, max speed) */
    private static final int CONDITION_INTERVAL = 20;
    /** ticks between two reads of the ship's profile - upgrades change it */
    private static final int PROFILE_INTERVAL = 200;

    private final CaptainEntity captain;
    private final ShipPilot pilot = new ShipPilot();

    @Nullable private Entity ship;
    @Nullable private ShipProfile profile;
    private int profileAge;

    private float faceYaw;
    private int faceTicks;

    /** where the goal was at the last condition read, to see how fast a followed entity moves */
    @Nullable private BlockPos lastGoal;
    private float goalSpeed;

    public ShipController(CaptainEntity captain) {
        this.captain = captain;
    }

    /** @return true while this captain stands at the helm of a ship he can sail */
    public boolean isActive() {
        return this.ship != null && this.profile != null && SmallShipsBridge.isHelmsman(this.ship, this.captain);
    }

    @Nullable
    public Entity getShip() {
        return this.ship;
    }

    @Nullable
    public ShipProfile getProfile() {
        return this.profile;
    }

    /** Turns the ship on the spot to this yaw (face command), then keeps station again. */
    public void face(float yaw) {
        this.faceYaw = yaw;
        this.faceTicks = FACE_TIMEOUT;
    }

    /** Server side, every tick. */
    public void tick() {
        Entity vehicle = this.captain.getVehicle();
        if (vehicle == null || !SmallShipsBridge.isShip(vehicle)) {
            this.detach();
            return;
        }
        if (vehicle != this.ship) this.attach(vehicle);
        if (this.profile == null) {
            // the ship could not tell its profile yet - ask again in two seconds
            if (++this.profileAge >= 40) this.attach(vehicle);
            return;
        }

        // aboard, but somebody else has the helm
        if (!SmallShipsBridge.isHelmsman(vehicle, this.captain)) return;

        // moored, in the dockyard or going down: hands off the helm
        if (!SmallShipsBridge.canMakeWay(vehicle)) {
            SmallShipsBridge.pressKeys(vehicle, false, false, false, false);
            return;
        }

        if (this.captain.tickCount % CONDITION_INTERVAL == Math.floorMod(this.captain.getId(), CONDITION_INTERVAL)) {
            this.readCondition(vehicle);
            this.updateGoalSpeed();
        }
        if (++this.profileAge >= PROFILE_INTERVAL) {
            this.profileAge = 0;
            ShipProfile fresh = ShipProfile.of(vehicle);
            if (fresh != null) {
                this.profile = fresh;
                this.pilot.setProfile(fresh);
            }
        }

        ShipPilot.State state = new ShipPilot.State(vehicle.getX(), vehicle.getZ(), vehicle.getYRot(),
                SmallShipsBridge.getSpeed(vehicle), SmallShipsBridge.getRotSpeed(vehicle),
                SmallShipsBridge.getSailState(vehicle), SmallShipsBridge.getSailCooldown(vehicle));

        ShipPilot.Keys keys;
        BlockPos goal = this.captain.getSailPos();
        if (this.faceTicks > 0) {
            this.faceTicks--;
            keys = this.pilot.face(state, this.faceYaw);
            if (this.pilot.isFacing(state, this.faceYaw)) this.faceTicks = 0;
        } else if (goal != null) {
            keys = this.pilot.station(state, goal.getX() + 0.5D, goal.getZ() + 0.5D, this.goalSpeed, this.deadZone(), 1.0F);
        } else {
            keys = this.pilot.stop(state);
        }

        // updateControls only writes the flags that changed, so this is cheap every tick
        SmallShipsBridge.pressKeys(vehicle, keys.forward, keys.backward, keys.left, keys.right);
        if (keys.sailKey) SmallShipsBridge.pressSailKey(vehicle);
    }

    /** Lets go of the helm - the keys are released while she is still his. Call before the captain dismounts. */
    public void release() {
        if (this.ship != null && SmallShipsBridge.isHelmsman(this.ship, this.captain)) {
            SmallShipsBridge.pressKeys(this.ship, false, false, false, false);
        }
        this.detach();
    }

    private void attach(Entity vehicle) {
        this.detach();
        this.ship = vehicle;
        this.profile = ShipProfile.of(vehicle);
        if (this.profile == null) return;
        this.pilot.setProfile(this.profile);
        this.readCondition(vehicle);
    }

    private void detach() {
        this.ship = null;
        this.profile = null;
        this.profileAge = 0;
        this.faceTicks = 0;
        this.lastGoal = null;
        this.goalSpeed = 0.0F;
        this.pilot.reset();
    }

    private void readCondition(Entity vehicle) {
        CompoundTag condition = SmallShipsBridge.getCondition(vehicle);
        if (condition.isEmpty()) return;
        this.pilot.setConditions(
                condition.getFloat("MaxSpeedNow"),
                condition.getFloat("SailSpeedFactor"),
                condition.getFloat("WindMultiplier"),
                condition.getFloat("WindStrength"),
                condition.getInt("WindZone") == 0);
    }

    /**
     * How fast the goal moves, from where it was a second ago. Only while
     * following or protecting - a move or hold order is a fixed spot, and its
     * jump to a new spot is an order, not a speed.
     */
    private void updateGoalSpeed() {
        BlockPos goal = this.captain.getSailPos();
        int followState = this.captain.getFollowState();
        boolean following = followState == 1 || followState == 5;

        if (!following || goal == null || this.lastGoal == null || this.profile == null) {
            this.goalSpeed = 0.0F;
            this.lastGoal = goal;
            return;
        }
        double dx = goal.getX() - this.lastGoal.getX();
        double dz = goal.getZ() - this.lastGoal.getZ();
        float speed = (float) Math.sqrt(dx * dx + dz * dz) / CONDITION_INTERVAL;
        // faster than any ship could follow: a teleport or a new target, not a speed
        if (speed > this.profile.maxSpeed() * 2.0F) speed = 0.0F;

        this.goalSpeed = this.goalSpeed * 0.5F + speed * 0.5F;
        this.lastGoal = goal;
    }

    /** How close counts as there, in blocks - by ship length, wider while following. */
    private float deadZone() {
        float length = this.profile != null ? this.profile.length() : 8.0F;
        int followState = this.captain.getFollowState();
        boolean following = followState == 1 || followState == 5;
        return following ? Math.max(1.5F * length, 8.0F) : Math.max(0.75F * length, 4.0F);
    }
}
