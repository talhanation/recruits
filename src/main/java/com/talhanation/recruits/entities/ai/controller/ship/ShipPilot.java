package com.talhanation.recruits.entities.ai.controller.ship;

/**
 * Turns "where to and how fast" into the keys a player would press: W/S/A/D
 * and the sail key. Nothing else - the ship's own drive does the rest, with
 * the same cooldowns and limits a player has.
 *
 * Plain Java on purpose, no Minecraft types: the controller feeds it a
 * {@link State} per tick and sends the {@link Keys} it gets back.
 *
 * Three things a ship needs that a horse does not:
 * <ul>
 *   <li>Turning is inertial. Without counter rudder a hull swings 110-150 degrees
 *       past its heading, so the rudder is laid against the turn early, see steer.</li>
 *   <li>She stops by coasting: sails furled (sail key, instant), she runs out over
 *       {@link ShipProfile#stoppingDistance}. The approach brakes exactly there.</li>
 *   <li>Sails only change one step per cooldown, so the sail state is chosen once
 *       and held, and the last metres are made under oars or creeping (sails 0 + W).</li>
 * </ul>
 */
public final class ShipPilot {

    /** One tick's keys, what a players' client would send. */
    public static final class Keys {
        public boolean forward, backward, left, right;
        /** press the sail key this tick - an edge, not held */
        public boolean sailKey;

        void clear() {
            this.forward = this.backward = this.left = this.right = this.sailKey = false;
        }
    }

    /** One look at the ship, taken every tick. Speed in blocks/tick, turn rate in degrees/tick (+ = starboard). */
    public record State(double x, double z, float yaw, float speed, float rotSpeed, int sailState, int sailCooldown) {}

    /** degrees: closer than this to the wanted heading counts as on course */
    private static final float HEADING_DEADBAND = 1.5F;
    /** degrees per tick: slower than this the bow counts as still */
    private static final float ROT_SETTLED = 0.1F;
    /** degrees: a goal further off the bow than this is turned to before making way */
    private static final float TURN_FIRST_ANGLE = 90.0F;
    /** degrees: under oars or creeping she only pulls once roughly pointed at the goal */
    private static final float PULL_ANGLE = 35.0F;
    /** ticks to wait after a sail key press before pressing again - the key toggles */
    private static final int SAIL_KEY_LOCK = 4;
    /** blocks: a goal that moved further than this starts a new approach */
    private static final double NEW_GOAL_DISTANCE = 2.0D;
    /** blocks/tick: a goal faster than this is followed by matching speed instead of stopping at it */
    private static final float MOVING_GOAL_SPEED = 0.02F;
    /** blocks/tick of extra speed per block she is behind a moving goal */
    private static final float CONVOY_GAIN = 0.01F;

    private final Keys keys = new Keys();
    private ShipProfile profile;

    // the drive right now, see ShipBridge#getCondition
    private float maxSpeedNow;
    private float sailSpeedFactor = 1.0F;
    private float windMultiplier = 1.0F;
    private float windStrength;
    private boolean headWind;

    private int sailKeyLock;
    private boolean arrived;
    private boolean braking;
    private double goalX = Double.NaN;
    private double goalZ = Double.NaN;

    public void setProfile(ShipProfile profile) {
        this.profile = profile;
        if (this.maxSpeedNow <= 0.0F) this.maxSpeedNow = profile.maxSpeed();
    }

    /** Refreshed about once a second from ShipBridge#getCondition. */
    public void setConditions(float maxSpeedNow, float sailSpeedFactor, float windMultiplier, float windStrength, boolean headWind) {
        this.maxSpeedNow = maxSpeedNow > 0.0F ? maxSpeedNow : (this.profile != null ? this.profile.maxSpeed() : 0.0F);
        this.sailSpeedFactor = sailSpeedFactor;
        this.windMultiplier = windMultiplier;
        this.windStrength = windStrength;
        this.headWind = headWind;
    }

    public void reset() {
        this.keys.clear();
        this.sailKeyLock = 0;
        this.arrived = false;
        this.braking = false;
        this.goalX = Double.NaN;
        this.goalZ = Double.NaN;
    }

    public boolean hasArrived() {
        return this.arrived;
    }

    /* ---------------- commands ---------------- */

    /** Sails in, no way, the swing taken out of the bow. */
    public Keys stop(State state) {
        this.begin();
        this.furl(state);
        this.killSwing(state);
        return this.keys;
    }

    /** Turns on the spot to the given yaw with the sails furled. */
    public Keys face(State state, float yaw) {
        this.begin();
        this.furl(state);
        this.steer(state, yaw);
        return this.keys;
    }

    public boolean isFacing(State state, float yaw) {
        return Math.abs(wrap(yaw - state.yaw())) < HEADING_DEADBAND * 2.0F && Math.abs(state.rotSpeed()) < ROT_SETTLED;
    }

    /**
     * Sails to the goal and keeps station there: stops within deadZone and
     * stays put until the goal moves or she drifts out of it.
     *
     * @param goalSpeed how fast the goal itself moves, blocks/tick - 0 for a
     *                  fixed position, the followed entity's speed otherwise
     * @param speedCap  share of the current max speed she may use, 0..1
     */
    public Keys station(State state, double goalX, double goalZ, float goalSpeed, float deadZone, float speedCap) {
        this.begin();

        double dx = goalX - state.x();
        double dz = goalZ - state.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean movingGoal = goalSpeed >= MOVING_GOAL_SPEED;

        // A goal that jumped is a new approach. One that creeps along (a
        // followed owner) is not - otherwise every step of it would set the
        // sails again and the canvas would flap.
        double newGoal = Math.max(NEW_GOAL_DISTANCE, deadZone);
        if (Double.isNaN(this.goalX) || sqr(goalX - this.goalX) + sqr(goalZ - this.goalZ) > newGoal * newGoal) {
            this.goalX = goalX;
            this.goalZ = goalZ;
            this.braking = false;
            if (distance > deadZone) this.arrived = false;
        }

        float exitRadius = deadZone + Math.max(2.0F, this.profile.length() * 0.5F);
        if (this.arrived && (movingGoal || distance > exitRadius)) {
            this.arrived = false;
            this.braking = false;
        }
        if (!movingGoal && !this.arrived && distance <= deadZone && Math.abs(state.speed()) < 0.05F) {
            this.arrived = true;
        }

        if (this.arrived) {
            this.furl(state);
            this.killSwing(state);
            return this.keys;
        }

        float bearing = yawTo(dx, dz);
        float error = Math.abs(wrap(bearing - state.yaw()));
        this.steer(state, bearing);

        float remaining = (float) distance - deadZone;

        if (movingGoal) {
            this.convoy(state, remaining, goalSpeed, speedCap, error);
            return this.keys;
        }

        if (remaining <= 0.0F) {
            // inside the zone and still carrying way: let her run out
            this.furl(state);
            return this.keys;
        }

        // From the moment she could not stop in time any more she only coasts:
        // furl once, then make the rest under oars or creeping. No sail goes up
        // again for this goal, so the canvas does not flap on the approach.
        float latency = Math.abs(state.speed());
        if (!this.braking && this.profile.stoppingDistance(state.speed()) + latency >= remaining) {
            this.braking = true;
        }
        // the goal got away again (followed entity): worth setting sail once more
        if (this.braking && remaining > this.profile.stoppingDistance(this.maxSpeedNow) * 2.0F + this.profile.length()) {
            this.braking = false;
        }

        if (error > TURN_FIRST_ANGLE) {
            // goal astern or abeam: no way on, turn first
            this.furl(state);
            return this.keys;
        }

        if (this.braking) {
            this.pull(state, Math.min(this.profile.coastSpeedFor(remaining), this.maxSpeedNow * speedCap), error);
            return this.keys;
        }

        float wanted = this.cruise(speedCap, error);
        if (this.shouldRow(wanted)) {
            this.pull(state, wanted, error);
        } else {
            this.setSails(state, this.sailStateFor(wanted));
        }
        return this.keys;
    }

    /**
     * Keeping up with a goal that moves, the way a player does it: the sails
     * only come in steps, so she runs on the state just faster than the goal
     * until she is close, then on the one just slower until she has dropped
     * back half a length - a few sail changes a minute, not one a second.
     * A ship with oars holds any speed up to the oar speed with W alone.
     */
    private void convoy(State state, float remaining, float goalSpeed, float speedCap, float error) {
        if (error > TURN_FIRST_ANGLE) {
            this.furl(state);
            return;
        }
        float cruise = this.cruise(speedCap, error);
        float wanted = Math.min(cruise, remaining > 0.0F ? goalSpeed + CONVOY_GAIN * remaining : goalSpeed * 0.8F);

        boolean oarsSuffice = this.profile.hasOars() && this.maxSpeedNow * this.profile.oarFactor() >= wanted;
        if (oarsSuffice || this.shouldRow(wanted)) {
            this.pull(state, wanted, error);
            return;
        }

        // far behind: full canvas to catch up
        float catchUp = this.profile.stoppingDistance(this.maxSpeedNow) + 2.0F * this.profile.length();
        if (remaining > catchUp) {
            this.setSails(state, this.sailStateFor(cruise));
            return;
        }

        int faster = this.sailStateFor(goalSpeed * 1.1F);
        int slower = faster - 1;
        int current = state.sailState();
        float gap = Math.max(4.0F, this.profile.length() * 0.5F);

        int target;
        if (remaining > gap) target = faster;
        else if (remaining <= 0.0F) target = slower;
        else target = Math.max(slower, Math.min(faster, current)); // in between: leave the canvas alone

        if (target == 0) this.pull(state, wanted, error);
        else this.setSails(state, target);
    }

    /** the speed worth making on this heading error: less while the bow still points away */
    private float cruise(float speedCap, float error) {
        return this.maxSpeedNow * speedCap * (float) Math.max(0.35D, Math.cos(Math.toRadians(error)));
    }

    /* ---------------- rudder ---------------- */

    /**
     * Steers to a yaw like a helmsman who knows his ship: the rudder goes
     * against the turn as soon as the bow would swing past the heading
     * otherwise. Keys: D = starboard (yaw grows), A = port.
     */
    private void steer(State state, float targetYaw) {
        float error = wrap(targetYaw - state.yaw());
        float swing = state.rotSpeed();

        if (Math.abs(error) < HEADING_DEADBAND && Math.abs(swing) < ROT_SETTLED) return;

        boolean swingingTowards = swing != 0.0F && Math.signum(swing) == Math.signum(error);
        if (swingingTowards && Math.abs(error) <= this.profile.brakeAngle(swing) + Math.abs(swing)) {
            // she would overshoot: counter rudder - unless a single tick of it would turn the swing round
            if (Math.abs(swing) > this.profile.rotBrake()) {
                if (swing > 0.0F) this.keys.left = true;
                else this.keys.right = true;
            }
            return;
        }

        if (error > 0.0F) this.keys.right = true;
        else this.keys.left = true;
    }

    /** Takes the swing out of the bow without wanting any particular heading. */
    private void killSwing(State state) {
        float swing = state.rotSpeed();
        if (Math.abs(swing) <= this.profile.rotBrake()) return;
        if (swing > 0.0F) this.keys.left = true;
        else this.keys.right = true;
    }

    /* ---------------- drive ---------------- */

    /** Sails 0; W pressed while slower than wanted - oars on a galley, creeping on everything else. */
    private void pull(State state, float wanted, float error) {
        if (state.sailState() > 0) {
            this.pressSailKey();
            return;
        }
        if (error <= PULL_ANGLE && state.speed() < wanted - 0.002F) this.keys.forward = true;
    }

    /** All canvas in at once - the sail key, as a player would. */
    private void furl(State state) {
        if (state.sailState() > 0) this.pressSailKey();
    }

    /** Works the sails towards a state: the sail key to set them, then W/S one step per cooldown. */
    private void setSails(State state, int target) {
        int current = state.sailState();
        if (current == 0) {
            this.pressSailKey();
        } else if (target > current) {
            this.keys.forward = true;
        } else if (target < current) {
            this.keys.backward = true;
        }
    }

    /** @return true if oars (or creeping) serve better than canvas for this speed */
    private boolean shouldRow(float wanted) {
        if (!this.profile.hasSails() || this.sailSpeedFactor <= 0.0F) return true;
        if (!this.profile.hasOars()) return false;
        float oarSpeed = this.maxSpeedNow * this.profile.oarFactor();
        // oars faster than full canvas (head wind, torn sails), or good enough while the canvas suffers
        return oarSpeed >= this.sailSpeed(4)
                || (oarSpeed >= wanted * 0.9F && (this.headWind || this.sailSpeedFactor < 1.0F));
    }

    /** @return the lowest sail state that makes the wanted speed - with a margin before giving up a set state */
    private int sailStateFor(float wanted) {
        for (int state = 1; state <= 4; state++) {
            if (this.sailSpeed(state) >= wanted * 0.97F) return state;
        }
        return 4;
    }

    /** The drive under a sail state - the formula of Ship#controlShip. */
    private float sailSpeed(int sailState) {
        float share = sailState / 4.0F;
        float wind = 1.0F + (this.windMultiplier - 1.0F) * this.windStrength * share;
        return this.maxSpeedNow * share * this.sailSpeedFactor * wind;
    }

    private void pressSailKey() {
        if (this.sailKeyLock > 0) return;
        this.keys.sailKey = true;
        this.sailKeyLock = SAIL_KEY_LOCK;
    }

    private void begin() {
        this.keys.clear();
        if (this.sailKeyLock > 0) this.sailKeyLock--;
    }

    /* ---------------- math ---------------- */

    /** @return the yaw that points along (dx, dz), Minecraft convention: 0 = +Z (south), 90 = -X (west) */
    static float yawTo(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** @return the angle wrapped to [-180, 180) */
    static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private static double sqr(double value) {
        return value * value;
    }
}
