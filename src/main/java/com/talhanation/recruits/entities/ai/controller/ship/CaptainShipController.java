package com.talhanation.recruits.entities.ai.controller.ship;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.util.NPCArmy;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The captain of a Small Ships ship. He handles her the way a player has to:
 * W/A/S/D and the sail key through {@link SmallShipsBridge}, never a yaw, a
 * speed or a sail state written directly.
 *
 * <b>One state machine</b> ({@link Mode}), decided every DECIDE_INTERVAL ticks
 * in {@link #decide}:
 * <pre>
 * own ship going down               -> report, hands off
 * HARBOUR running                   -> stays until the routine is done
 * reason to retreat, can make way   -> HOMEWARD (home harbour) or FLEE - at once, with a report
 * target and allowed to fight       -> FIGHT at a {@link Station}
 * otherwise                         -> ORDERS (HOMEWARD stays HOMEWARD)
 * </pre>
 * The guns fire at whatever comes into their arc in every mode but HARBOUR.
 *
 * <b>Every tick</b> ({@link #tick}): at the helm the contacts and the guns
 * ({@link #think}), then the helm ({@link #steer}): goal of the mode ->
 * way (straight or {@link ShipPathfinder}) -> lookout -> keys. The only thing
 * that takes the helm out of this is backing up: when she is stuck, when the
 * bow jams in a turn, or when a turn on the spot needs a block before the bow.
 */
public final class CaptainShipController {

    /** What he does. See the class comment for when. */
    private enum Mode { ORDERS, FIGHT, HOMEWARD, FLEE, HARBOUR }

    /** Where he lies in a FIGHT: a quarter point around the target, the hold position, or with the one he follows. */
    private enum Station { QUARTER, HOLD, FOLLOW }

    /** The harbour routine: way off, ashore for the upkeep, aboard again, repair and balls into the hold. */
    private enum Step { MOOR, ASHORE, BOARD, REPAIR }

    private enum Reason { HULL, HEALTH, FIRED_UPON, NO_MEANS }

    private enum Trouble { STUCK, JAMMED }

    public enum Kind { SHIP, SIEGE_WEAPON, CAVALRY, INFANTRY }

    /**
     * One contact as a player on deck would see it. Of a ship only what shows:
     * guns, hull above or below half, canvas whole (0), torn (1) or gone (2).
     * Minor: a monster, a swimmer, someone in a rowing boat - fired at only
     * without bowmen aboard, never reported, never a reason to retreat.
     */
    public record Contact(Entity entity, Kind kind, List<Entity> people, double x, double y, double z,
                          float yaw, float speed, float rotSpeed, int guns, boolean hullBelowHalf, int sails,
                          boolean ramBow, float length, float airDraft, boolean sinking, boolean minor) {
        boolean isShip() {
            return this.kind == Kind.SHIP;
        }

        double distanceTo(double x, double z) {
            return Math.hypot(this.x - x, this.z - z);
        }
    }

    /** What the guns aim at, fresh every tick. ship: the target ship (its line of fire is free), null otherwise. */
    private record Mark(double x, double y, double z, float yaw, float speed, float rotSpeed, @Nullable Entity ship) {
        /** @return {x, y, z} where it will be after that many ticks - a turning ship along its curve */
        double[] lead(float ticks) {
            return CaptainShipController.lead(this.x, this.y, this.z, this.yaw, this.speed, this.rotSpeed, ticks);
        }
    }

    /** Where the mode wants her: keep station there (or sail through), lie on heading (NaN = any), rudder still while calm. */
    private record Goal(double x, double z, float heading, boolean stop, float speed, float deadZone, boolean calm) {}

    /** One look at the ship. Speed in blocks/tick, turn rate in degrees/tick (+ = starboard). */
    private record State(double x, double z, float yaw, float speed, float rotSpeed, int sailState) {}

    /** One tick's keys, what a player's client would send. sail: press the sail key this tick. */
    private static final class Keys {
        boolean forward, backward, left, right, sail;
    }

    /* ============================== constants =============================== */

    // ---- decisions ----
    private static final int DECIDE_INTERVAL = 20, MIN_MODE_TICKS = 100;
    /** ticks he makes for the last known position of a target out of sight */
    private static final int OUT_OF_SIGHT = 100;
    /** ticks without any contact before the fight is over */
    private static final int CALM = 200;
    private static final double LAND_TROOPS_RANGE = 60.0D;
    private static final int LEASH_IGNORE = 1200;
    private static final double IN_HARBOUR = 32.0D, RANGE_SHARE = 0.8D, CHANGE_SHARE = 0.5D;
    /** degrees off the target's bow of the quarter points */
    private static final float[] QUARTERS = {-45.0F, 45.0F, -135.0F, 135.0F};

    // ---- contacts and reports ----
    private static final int SEARCH_INTERVAL = 40, UNDER_FIRE = 100, CONTACT_MEMORY = 1200, UNIT_REPORT_GAP = 600;
    private static final double SEARCH_RADIUS = 100.0D;

    // ---- harbour ----
    private static final int MOOR_TIMEOUT = 200, ASHORE_TIMEOUT = 1200, BOARD_TIMEOUT = 2400, DOCKYARD_TIMEOUT = 2400, CREW_TIMEOUT = 400;

    // ---- helm and way ----
    private static final int FACE_TIMEOUT = 200, CONDITION_INTERVAL = 20, PROFILE_INTERVAL = 200, TRAIL_INTERVAL = 40;
    private static final int MIN_REPLAN = 20, REFRESH_PLAN = 200, SHIP_LOOK = 10, WAIT_RETRY = 40, GIVE_WAY = 100, PLANS_PER_TICK = 2;
    private static final double GOAL_JUMP = 16.0D, SHARP_TURN = 45.0D;
    private static final float STOPPED_TURN = 45.0F, SWING_SPEED = 0.1F, TURN_DONE = 30.0F;

    // ---- lookout and backing up ----
    private static final int LOOKOUT_INTERVAL = 5, STUCK_CHECK = 40, BACK_OFF_REPEAT = 300, REST_TICKS = 100;
    /** blocks/tick: below 16 km/h a grounding or a touch costs no hull */
    private static final float SAFE_SPEED = 0.2F, JAMMED_TURN = 3.0F;
    private static final double STUCK_PROGRESS = 0.5D, MIN_MOVE = 1.5D, SAFETY = 1.0D, BOW_ROOM = 1.0D;

    // ---- traffic: other ships ----
    /** blocks around her other ships are watched for an approach */
    private static final double TRAFFIC_RANGE = 48.0D;
    /** ticks between two looks; how long a slow-down holds after the danger is gone */
    private static final int TRAFFIC_LOOK = 10, TRAFFIC_HOLD = 40;
    /** blocks: a hull this close before the bow or behind the stern counts as touching */
    private static final double TOUCH = 0.75D;
    /** ticks she looks ahead for a ship coming too close; ticks a dodge holds at least, at most (until the other is past) */
    private static final int DODGE_LOOK = 60, DODGE_TICKS = 50, DODGE_MAX = 200;
    /** degrees she puts her course over to dodge */
    private static final float DODGE_ANGLE = 30.0F;
    /** degrees: a patrol waypoint where the route bends less than this is sailed through */
    private static final float PASS_ANGLE = 90.0F;
    /** ticks she waits for ships that close her way before she backs off and plans anew, whoever they are */
    private static final int BLOCKED_WAIT = 200;

    // ---- trail home ----
    private static final double TRAIL_STEP = 24.0D, TRAIL_LOOP = 16.0D, TRAIL_JUMP = 64.0D;
    private static final int TRAIL_MAX = 128;

    // ---- canvas, see helm ----
    private static final float HEADING_DEADBAND = 1.5F, ROT_SETTLED = 0.1F, TURN_FIRST = 90.0F, TURN_FIRST_HYSTERESIS = 15.0F, PULL_ANGLE = 35.0F;
    private static final int SAIL_KEY_LOCK = 4, SAIL_REST = 100, SAIL_INTENT = 40, SAIL_REVERSE_HOLD = 60;

    // ---- guns ----
    private static final int GUN_CHECK = 4, CROWDED_DECK = 4;
    private static final float AIM_STEP = 0.5F, STILL = 0.03F;

    private static long budgetTick = Long.MIN_VALUE;
    private static int budgetUsed;

    /* ================================ fields ================================ */

    private final CaptainEntity captain;
    @Nullable private Entity ship;
    @Nullable private ShipProfile profile;
    private int profileAge;
    private boolean wasAtHelm;

    // ---- mode ----
    private Mode mode = Mode.ORDERS;
    private int modeSince;
    private Station station = Station.QUARTER;
    @Nullable private Reason retreat;
    private boolean fleeRoute, homeThenHarbour;
    private int savedFollowState = -1;
    /** the player gave an order during a retreat: no retreat of his own until this fight is over */
    private boolean playerOverride;
    /** set while the controller itself changes the follow state - that is no player order */
    private boolean internalOrder;
    @Nullable private Vec3 savedHoldPos;

    // ---- harbour ----
    private Step step = Step.MOOR;
    private int stepSince, crewWaitSince = -1;
    @Nullable private UUID harbourShip;
    private boolean handHull, dockyard, harbourForRetreat, harbourDone;

    // ---- fight ----
    @Nullable private Contact target;
    @Nullable private Mark mark;
    private int targetSeen, targetHullOf = -1, quarter = -1, quarterTarget = -1, fightSideOf = Integer.MIN_VALUE;
    private final float[] targetHull = new float[2];
    private boolean fighting, starboardSide, shieldsUp, bowmenAboard;
    @Nullable private Vec3 fightStart, holdPoint;
    private final Map<Integer, Integer> ignored = new HashMap<>();
    private int lastContact = Integer.MIN_VALUE / 2, lastTarget = Integer.MIN_VALUE / 2, lastHit = Integer.MIN_VALUE / 2;
    private List<Contact> contacts = List.of(), warContacts = List.of();

    // ---- reports ----
    private final Map<UUID, Integer> reported = new HashMap<>();
    @Nullable private UUID sinkingShip;
    private int lastUnitReport = Integer.MIN_VALUE / 2;

    // ---- guns ----
    private final int[] fired = {Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2};
    private final float[] laidPitch = {Float.NaN, Float.NaN}, laidRotation = {Float.NaN, Float.NaN};
    private ShipProfile.Ammo ammo = ShipProfile.Ammo.BALL;
    private boolean ammoSet;

    // ---- course ----
    private float faceYaw, goalSpeed, wantedYaw = Float.NaN;
    private int faceTicks, homeIndex = -1, turnDir;
    private boolean bowChecked;
    @Nullable private BlockPos lastGoal;
    private final List<BlockPos> trail = new ArrayList<>();
    @Nullable private BlockPos trailLastSeen;

    // ---- way: path == null and !noWay is straight ----
    @Nullable private List<double[]> path;
    private int pathIndex, plannedAt = -MIN_REPLAN, blockedSince;
    private boolean pathReachesGoal, noWay, replan, blockedByPlan, shipAhead;
    private double pathFromX, pathFromZ, noWayX, noWayZ, plannedX = Double.NaN, plannedZ = Double.NaN;
    @Nullable private Entity blocker;
    /** the last goal turned out to be out of reach for her hull, ships or not: she lies as close as she gets */
    private boolean goalUnreachable;
    private double unreachableX, unreachableZ;
    /** waypoints the owner was told about, see reportUnreachable */
    private final java.util.Set<Long> reportedUnreachable = new java.util.HashSet<>();

    // ---- lookout and backing up ----
    private float lookoutCap = Float.MAX_VALUE, stuckYaw;
    private boolean lookStop, lookReverse, pressing, rudderHeld;
    @Nullable private Trouble trouble;
    private double stuckX = Double.NaN, stuckZ;
    private int rudderTicks, pressTicks, backTicks, backedAt = Integer.MIN_VALUE / 2, backStreak, restUntil = Integer.MIN_VALUE / 2;
    private double backFromX, backFromZ, backAlongX, backAlongZ, backDistance;
    /** -1 backing off astern, +1 moving off ahead (a ship touches her stern) */
    private int backSign = -1;

    // ---- traffic ----
    /** blocks/tick she may make for the ships about, MAX_VALUE: no limit */
    private float trafficCap = Float.MAX_VALUE;
    @Nullable private Entity trafficShip;
    private int trafficSeen = Integer.MIN_VALUE / 2;
    /** the ship she dodges by holding evadeYaw, or null */
    @Nullable private Entity evadeFrom;
    private float evadeYaw, pathYaw = Float.NaN;
    private int evadeSince;
    /** she is sailing for somewhere this tick, not keeping station - only then does she give way or count as jammed */
    private boolean wantsWay;

    // ---- helm ----
    private final Keys keys = new Keys();
    private int sailKeyLock, lastSail = -1, sailAge = SAIL_REST, lastSailDir, sailWish;
    private boolean wishing, turningFirst, arrived, braking;
    private double helmGoalX = Double.NaN, helmGoalZ = Double.NaN;

    public CaptainShipController(CaptainEntity captain) {
        this.captain = captain;
    }

    /* ================================ public ================================= */

    /** @return true while this captain stands at the helm of a ship he can sail */
    public boolean isActive() {
        return this.ship != null && this.profile != null && SmallShipsBridge.isHelmsman(this.ship, this.captain);
    }

    @Nullable
    public ShipProfile getProfile() {
        return this.profile;
    }

    /** Turns the ship on the spot to this yaw (face command), then keeps station again. */
    public void face(float yaw) {
        this.onPlayerOrder();
        this.faceYaw = yaw;
        this.faceTicks = FACE_TIMEOUT;
        this.turnDir = 0;
        this.bowChecked = false;
    }

    /** Sails home along the trail and keeps station in the harbour. @return false without a home harbour */
    public boolean returnHome() {
        return this.goHome(false);
    }

    /**
     * A player order (move, follow, hold, protect, face): it counts, also in a
     * retreat. It ends the way home, the harbour routine and a retreat; he
     * retreats on his own again only after this fight is over.
     */
    public void onPlayerOrder() {
        this.cancelHomeward();
        this.harbourDone = false; // the leader leaves its upkeep for the order - see CommandEvents#checkPatrolLeaderState
        if (this.retreat != null) this.callOffRetreat();
        if (this.mode == Mode.FLEE || this.mode == Mode.HARBOUR) this.enter(Mode.ORDERS);
    }

    /** Any change of the follow state ends the way home - and an upkeep run to the harbour. */
    public void cancelHomeward() {
        if (this.internalOrder || this.mode != Mode.HOMEWARD) return;
        boolean upkeep = this.homeThenHarbour && !this.harbourForRetreat;
        this.homeThenHarbour = false;
        this.enter(Mode.ORDERS);
        if (upkeep) this.harbourDone = true;
    }

    public boolean isHomeward() {
        return this.mode == Mode.HOMEWARD;
    }

    /** @return true while she waits for this ship to clear her way */
    public boolean isWaitingFor(@Nullable Entity ship) {
        return ship != null && this.blocker == ship;
    }

    /** @return true while she keeps station where she was sent - for "wander freely": time for the next point */
    public boolean hasArrived() {
        return this.arrived;
    }

    public boolean isRetreating() {
        return this.retreat != null;
    }

    /** @return blocks he engages from: the effective distance of a ball shot */
    public double engageRange() {
        ShipProfile.Gun ball = ShipProfile.gun(ShipProfile.Ammo.BALL, this.profile != null && this.profile.fineGrain());
        return ball != null ? ball.effectiveRange() : 45.0D;
    }

    /** @return true if he has something to fight with: guns and balls above the reserve */
    public boolean canFight() {
        return this.profile != null && this.hasGunsAndAmmo();
    }

    /** @return true while the harbour routine runs, the way to the harbour for it included */
    public boolean isHarbourRunning() {
        return this.mode == Mode.HARBOUR || (this.mode == Mode.HOMEWARD && this.homeThenHarbour);
    }

    /** @return true once when an upkeep routine has finished - whoever started it takes it from there */
    public boolean takeHarbourDone() {
        boolean done = this.harbourDone;
        this.harbourDone = false;
        return done;
    }

    /** Upkeep on a ship: to the home harbour if it is far, ashore to the upkeep chest, repair, balls into the hold. */
    public void startHarbour() {
        this.startRoutine(false);
    }

    /**
     * A spot to sail to when wandering freely: open water with room to turn,
     * 24 to 96 blocks from where she lies, at most 128 from where the wandering began.
     */
    @Nullable
    public BlockPos wanderPoint(BlockPos origin, RandomSource random) {
        if (this.ship == null || this.profile == null) return null;
        int surfaceY = ShipPathfinder.surfaceY(this.ship.level(), this.ship.getX(), this.ship.getY(), this.ship.getZ());
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D, distance = 24.0D + random.nextDouble() * 72.0D;
            double x = this.ship.getX() + Math.cos(angle) * distance, z = this.ship.getZ() + Math.sin(angle) * distance;
            if (Math.hypot(x - origin.getX(), z - origin.getZ()) > 128.0D) continue;
            if (ShipPathfinder.isOpenWater(this.ship, this.profile, x, z, this.profile.swingRadius() + 2.0D)) return BlockPos.containing(x, surfaceY, z);
        }
        return null;
    }

    /** Lets go of the helm - the keys are released while she is still his. Call before the captain dismounts. */
    public void release() {
        if (this.ship != null && SmallShipsBridge.isHelmsman(this.ship, this.captain)) {
            SmallShipsBridge.pressKeys(this.ship, false, false, false, false);
        }
        this.detach();
    }

    /** A new home harbour, or none: the trail home starts over from it. */
    public void resetTrail(@Nullable BlockPos harbour) {
        this.trail.clear();
        this.trailLastSeen = null;
        if (harbour != null) this.trail.add(harbour.immutable());
    }

    public void save(CompoundTag tag) {
        tag.putLongArray("HomePath", this.trail.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putBoolean("Homeward", this.mode == Mode.HOMEWARD && !this.homeThenHarbour);
    }

    /** Restores the trail - only if it belongs to this harbour - and a way home that was under way. */
    public void load(CompoundTag tag, BlockPos harbour) {
        this.resetTrail(harbour);
        long[] packed = tag.getLongArray("HomePath");
        if (packed.length > 0 && BlockPos.of(packed[0]).equals(harbour)) {
            for (int i = 1; i < packed.length && i < TRAIL_MAX; i++) this.trail.add(BlockPos.of(packed[i]));
        }
        if (tag.getBoolean("Homeward")) this.returnHome();
    }

    /** Server side, every tick. */
    public void tick() {
        if (SmallShipsBridge.isAvailable()) {
            if (this.isActive()) {
                this.captain.enemyArmy = null; // the leader's own search is off at the helm
                this.think();
                this.wasAtHelm = true;
            } else if (this.wasAtHelm) {
                this.leaveHelm();
                this.wasAtHelm = false;
            }
            this.steer();
        }
        if (this.mode == Mode.HARBOUR) this.harbourTick();
    }

    /* ============================ decisions ================================== */

    private void think() {
        int now = this.captain.tickCount, phase = Math.floorMod(this.captain.getId(), SEARCH_INTERVAL);
        State own = stateOf(this.ship);
        if (this.captain.getLastHurtByMob() != null && now - this.captain.getLastHurtByMobTimestamp() < UNDER_FIRE) {
            this.lastHit = Math.max(this.lastHit, this.captain.getLastHurtByMobTimestamp());
        }
        if (now % SEARCH_INTERVAL == phase) {
            this.searchContacts();
            if (this.mode != Mode.HARBOUR) this.reportContacts();
        } else if (now % 20 == phase % 20) {
            this.refreshContacts();
        }
        if (!this.warContacts.isEmpty()) this.lastContact = now;

        if (this.profile.isSinking()) {
            this.reportSinking();
            this.mark = null;
            return;
        }
        if (now % DECIDE_INTERVAL == Math.floorMod(this.captain.getId(), DECIDE_INTERVAL)) this.decide(own);
        this.act(own);
    }

    private void decide(State own) {
        int now = this.captain.tickCount;
        this.bowmenAboard = this.hasBowmenAboard();
        if (this.mode == Mode.HARBOUR) {
            this.target = null;
            return;
        }
        if (this.retreat != null) {
            this.followRetreat();
            if (this.retreat != null) {
                this.target = this.nearestArmed(own);
                this.commandCrew();
                return;
            }
        }
        if (this.playerOverride && now - this.lastContact > CALM && now - this.lastHit >= UNDER_FIRE) this.playerOverride = false;
        Reason reason = this.playerOverride ? null : this.retreatReason(now);
        if (reason != null && this.profile.canMakeWay()) {
            this.startRetreat(reason);
            this.target = this.nearestArmed(own);
            this.commandCrew();
            return;
        }
        // the leader wanted to retreat, but there is no way: back to the route
        if (this.captain.getLeaderState() == AbstractLeaderEntity.State.RETREATING) this.resumePatrol();

        this.target = this.chooseTarget(own);
        if (this.target != null) this.lastTarget = now;
        Station wanted = this.target != null || this.strategicFire() ? this.wantedStation() : null;
        boolean free = this.mode != Mode.FIGHT || now - this.modeSince >= MIN_MODE_TICKS;
        if (wanted == null) {
            if (this.fighting && now - this.lastTarget > CALM) this.endFight();
            if (this.mode == Mode.FIGHT && free) this.enter(Mode.ORDERS);
        } else if (this.mode != Mode.HOMEWARD && free && (this.mode != Mode.FIGHT || this.station != wanted)) {
            this.station = wanted;
            this.enter(Mode.FIGHT);
        }
        if (this.mode == Mode.FIGHT) this.engage();
        this.commandCrew();
    }

    /** @return where to fight, null: no fight - hold fire, nothing to fire, or keep patrolling (the guns still take what passes) */
    @Nullable
    private Station wantedStation() {
        if (!this.captain.getShouldRanged() || !this.hasGunsAndAmmo()) return null;
        if (this.strategicFire()) return Station.HOLD;
        if (this.target == null) return null;
        if (this.onPatrol()) {
            AbstractLeaderEntity.EnemyAction action = AbstractLeaderEntity.EnemyAction.fromIndex(this.captain.getEnemyAction());
            if (action == AbstractLeaderEntity.EnemyAction.KEEP_PATROLLING) return null;
            if (action == AbstractLeaderEntity.EnemyAction.HOLD) return Station.HOLD;
        } else {
            int follow = this.captain.getFollowState();
            if (follow == 2 || follow == 3 || follow == 4) return Station.HOLD;
            if (follow == 1 || follow == 5) return Station.FOLLOW;
        }
        // no way on, or a monster or swimmer not worth sailing for: broadside on from where she is
        if (!this.profile.canMakeWay() || this.target.minor()) return Station.HOLD;
        return Station.QUARTER;
    }

    private void enter(Mode next) {
        if (next == this.mode) return;
        this.mode = next;
        this.modeSince = this.captain.tickCount;
        this.holdPoint = null;
        this.quarter = -1;
        this.debug("mode " + next + (next == Mode.FIGHT ? " " + this.station : ""));
    }

    /** Each one alone is enough - only with an enemy about, passive and under fire excepted. */
    @Nullable
    private Reason retreatReason(int now) {
        boolean underFire = now - this.lastHit < UNDER_FIRE;
        if (this.captain.getState() == 3) return underFire ? Reason.FIRED_UPON : null; // passive
        if (this.captain.getLeaderState() == AbstractLeaderEntity.State.RETREATING) return Reason.HEALTH;
        if (this.warContacts.isEmpty()) return null;
        if (this.profile.hull() * 100.0F < RecruitsServerConfig.CaptainRetreatHullPercent.get()) return Reason.HULL;
        if (this.captain.getHealth() < this.captain.getMaxHealth() * 0.25F) return Reason.HEALTH;
        for (Contact contact : this.warContacts) {
            boolean armed = (contact.isShip() && (contact.guns() > 0 || contact.ramBow())) || contact.kind() == Kind.SIEGE_WEAPON;
            if (armed && !contact.sinking() && !this.hasGunsAndAmmo()) return Reason.NO_MEANS;
        }
        return null;
    }

    /** The priority filters, the score sorts within: guns that reach her +2, a ram bow close by +2, battered +1, minus distance / 50, the current target +0.3. */
    @Nullable
    private Contact chooseTarget(State own) {
        int now = this.captain.tickCount;
        this.ignored.values().removeIf(until -> until < now);
        Entity protect = this.protectedOne();
        ShipProfile.Gun ball = ShipProfile.gun(ShipProfile.Ammo.BALL, this.profile.fineGrain());
        double reach = ball != null ? ball.maxRange() : 78.0D;
        List<Contact> all = new ArrayList<>();
        for (Contact contact : this.contacts) {
            if (contact.sinking() || this.ignored.containsKey(contact.entity().getId())) continue;
            if (contact.minor() && this.bowmenAboard) continue; // the bowmen deal with those
            if (protect != null && contact.distanceTo(protect.getX(), protect.getZ()) > reach) continue;
            all.add(contact);
        }
        if (all.isEmpty()) {
            // out of sight: to where it was seen last, for a while
            Contact last = this.target;
            return last != null && last.entity().isAlive() && now - this.targetSeen <= OUT_OF_SIGHT ? last : null;
        }
        IHasTargetPriority.TargetPriority priority = IHasTargetPriority.TargetPriority.fromIndex(this.captain.getTargetPriority());
        List<Contact> chosen = all.stream().filter(contact -> switch (priority) {
            case SHIPS -> contact.isShip();
            case SIEGE_WEAPONS -> contact.kind() == Kind.SIEGE_WEAPON;
            case INFANTRY -> contact.kind() == Kind.INFANTRY;
            case CAVALRY -> contact.kind() == Kind.CAVALRY;
            default -> true;
        }).toList();
        if (chosen.isEmpty()) chosen = all;

        ShipProfile.Gun enemyBall = ShipProfile.gun(ShipProfile.Ammo.BALL, false); // his powder does not show
        double enemyReach = enemyBall != null ? enemyBall.maxRange() : 78.0D;
        Contact best = null;
        double bestScore = -Double.MAX_VALUE;
        for (Contact contact : chosen) {
            double distance = contact.distanceTo(own.x(), own.z()), score;
            if (priority == IHasTargetPriority.TargetPriority.CLOSEST) {
                score = -distance;
            } else {
                score = -distance / 50.0D;
                if (contact.guns() > 0 && distance <= enemyReach) score += 2.0D;
                if (contact.ramBow() && distance < 3.0D * contact.length()) score += 2.0D;
                if (contact.hullBelowHalf() || contact.sails() > 0) score += 1.0D;
                if (this.target != null && this.target.entity() == contact.entity()) score += 0.3D;
            }
            if (score > bestScore) {
                bestScore = score;
                best = contact;
            }
        }
        return best;
    }

    @Nullable
    private Contact nearestArmed(State own) {
        Contact best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Contact contact : this.contacts) {
            if (contact.sinking() || (contact.minor() && this.bowmenAboard)) continue;
            double distance = contact.distanceTo(own.x(), own.z()) - (contact.guns() > 0 ? 1000.0D : 0.0D);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = contact;
            }
        }
        return best;
    }

    /** @return the one he follows or protects - he fights only near it - or null */
    @Nullable
    private Entity protectedOne() {
        if (this.onPatrol()) return null;
        int follow = this.captain.getFollowState();
        if (follow == 1) return this.captain.getOwner();
        if (follow == 5) return this.captain.getProtectingMob();
        return null;
    }

    private void engage() {
        if (!this.fighting) {
            this.fighting = true;
            this.fightStart = this.ship.position();
            AbstractLeaderEntity.State state = this.captain.getLeaderState();
            if (state == AbstractLeaderEntity.State.PATROLLING || state == AbstractLeaderEntity.State.WAITING) {
                this.captain.setPatrolState(AbstractLeaderEntity.State.ATTACKING);
            }
        }
        // the leash: chased too far from where it began - let it go
        if (this.station == Station.QUARTER && this.target != null && this.fightStart != null
                && this.ship.position().distanceTo(this.fightStart) > RecruitsServerConfig.CaptainChaseLeash.get()) {
            this.ignored.put(this.target.entity().getId(), this.captain.tickCount + LEASH_IGNORE);
            this.debug("leash reached - letting " + this.target.entity().getName().getString() + " go");
            this.target = null;
            this.fightStart = null;
        }
    }

    /** No enemy for a while: shields down, back to the route. */
    private void endFight() {
        this.fighting = false;
        this.fightStart = null;
        this.target = null;
        this.shields(false);
        if (this.captain.getLeaderState() == AbstractLeaderEntity.State.ATTACKING) this.resumePatrol();
        this.debug("fight over");
    }

    /** Ranged crew aboard shoot at the target's people; shields up under fire. */
    private void commandCrew() {
        NPCArmy army = this.captain.army;
        if (army == null) return;
        this.shields(this.captain.tickCount - this.lastHit < UNDER_FIRE && (this.fighting || this.retreat != null));
        Contact target = this.target;
        if (target == null || target.people().isEmpty()) return;
        int next = 0;
        for (AbstractRecruitEntity recruit : army.getAllRecruitUnits()) {
            if (recruit.getVehicle() != this.ship || !NPCArmy.isRanged(recruit)) continue;
            LivingEntity current = recruit.getTarget();
            if (current != null && current.isAlive()) continue;
            Entity person = target.people().get(next++ % target.people().size());
            if (person instanceof LivingEntity living && recruit.canAttack(living)) recruit.setTarget(living);
        }
    }

    private boolean hasBowmenAboard() {
        if (this.captain.army == null) return false;
        for (AbstractRecruitEntity recruit : this.captain.army.getAllRecruitUnits()) {
            if (recruit.isAlive() && recruit.getVehicle() == this.ship && NPCArmy.isRanged(recruit)) return true;
        }
        return false;
    }

    private void shields(boolean up) {
        if (up == this.shieldsUp || this.captain.army == null) return;
        this.shieldsUp = up;
        for (AbstractRecruitEntity recruit : this.captain.army.getAllRecruitUnits()) {
            if (up && (recruit.getVehicle() != this.captain.getVehicle() || NPCArmy.isRanged(recruit))) continue;
            recruit.setShouldBlock(up);
        }
    }

    /** Every tick at the helm: the target fresh, the guns, mending at sea. */
    private void act(State own) {
        int now = this.captain.tickCount;
        boolean inSight = false;
        if (this.target != null) {
            Contact fresh = this.contactOf(this.target.entity());
            if (fresh != null) {
                this.target = fresh;
                this.targetSeen = now;
                inSight = true;
            } else if (now - this.targetSeen > OUT_OF_SIGHT || !this.target.entity().isAlive()) {
                this.target = null;
            }
        }
        this.mark = this.target != null ? this.markOf(this.target) : this.strategicMark();

        // the guns: in a fight, and at whatever passes their arc otherwise - only at what he can see
        if (this.mark != null && this.mode != Mode.HARBOUR) {
            boolean mayFire = this.captain.getShouldRanged() && this.captain.getState() != 3 && (this.target == null || inSight);
            this.guns(own, mayFire, this.retreat != null ? 0 : this.reserve());
        }
        // quiet: mend what can be mended at sea, one handful a second
        if (this.target == null && this.retreat == null && this.mode != Mode.HARBOUR && now - this.lastContact > CALM
                && now % 20 == Math.floorMod(this.captain.getId(), 20) && (this.profile.canPatchHull() || this.profile.canPatchSails())) {
            if (!SmallShipsBridge.repairHullByHand(this.ship, this.captain.getInventory())) {
                SmallShipsBridge.patchSails(this.ship, this.captain.getInventory());
            }
        }
    }

    private boolean strategicFire() {
        return this.captain.getShouldStrategicFire() && this.captain.StrategicFirePos() != null;
    }

    private boolean onPatrol() {
        AbstractLeaderEntity.State state = this.captain.getLeaderState();
        return state == AbstractLeaderEntity.State.PATROLLING || state == AbstractLeaderEntity.State.WAITING
                || state == AbstractLeaderEntity.State.ATTACKING;
    }

    private boolean hasGunsAndAmmo() {
        return this.profile.guns() > 0 && this.profile.balls() > this.reserve();
    }

    /** @return balls kept in the hold for the way home: CaptainAmmoReserveVolleys broadsides */
    private int reserve() {
        return RecruitsServerConfig.CaptainAmmoReserveVolleys.get() * this.profile.guns();
    }

    /* ============================== retreat ================================== */

    private void startRetreat(Reason reason) {
        this.retreat = reason;
        this.fighting = false;
        this.fightStart = null;
        this.savedFollowState = this.captain.getFollowState();
        this.savedHoldPos = this.captain.getHoldPos();
        boolean leaderRetreats = this.captain.getLeaderState() == AbstractLeaderEntity.State.RETREATING;
        String where;
        if (this.captain.getHarbourPos() != null) {
            this.harbourForRetreat = true;
            this.goHome(true);
            if (this.onPatrol() || leaderRetreats) this.captain.setPatrolState(AbstractLeaderEntity.State.RETREATING);
            where = "harbour";
        } else if (this.onPatrol() || (leaderRetreats && !this.captain.WAYPOINTS.isEmpty())) {
            // back along the route to its first waypoint - the leader sails it, its upkeep takes over there
            this.fleeRoute = true;
            this.captain.retreating = true;
            this.captain.setPatrolState(AbstractLeaderEntity.State.PATROLLING);
            this.enter(Mode.FLEE);
            where = "route";
        } else {
            this.fleeRoute = false;
            this.enter(Mode.FLEE);
            where = "away";
        }
        Component why = reason == Reason.HULL
                ? Component.translatable("chat.recruits.text.captain_reason_hull", Math.round(this.profile.hull() * 100.0F))
                : Component.translatable("chat.recruits.text.captain_reason_" + reason.name().toLowerCase(Locale.ROOT));
        this.tell(Component.translatable("chat.recruits.text.captain_retreat", this.captain.getName().getString(), why,
                Component.translatable("chat.recruits.text.captain_destination_" + where)));
        this.debug("retreat: " + reason + " -> " + where);
    }

    private void followRetreat() {
        int now = this.captain.tickCount;
        if (this.mode == Mode.HOMEWARD || this.mode == Mode.HARBOUR) return; // under way - the routine ends it
        if (this.mode == Mode.FLEE) {
            boolean over = this.fleeRoute ? !this.captain.retreating : now - this.lastContact > CALM && now - this.lastHit >= UNDER_FIRE;
            if (over) this.finishRetreat();
            return;
        }
        // a new order ended the way home
        this.callOffRetreat();
        if (this.captain.getLeaderState() == AbstractLeaderEntity.State.RETREATING) this.resumePatrol();
    }

    /** The player's order counts: the retreat is over, and he retreats on his own again only after this fight. */
    private void callOffRetreat() {
        this.retreat = null;
        this.harbourForRetreat = false;
        this.fleeRoute = false;
        this.captain.retreating = false;
        this.playerOverride = true;
        this.savedFollowState = -1;
        this.savedHoldPos = null;
        this.debug("retreat called off by an order");
    }

    private void finishRetreat() {
        boolean viaHarbour = this.harbourForRetreat;
        this.retreat = null;
        this.harbourForRetreat = false;
        this.shields(false);
        this.enter(Mode.ORDERS);
        if (viaHarbour) {
            if (this.captain.getLeaderState() == AbstractLeaderEntity.State.RETREATING) {
                this.resumePatrol();
            } else if (this.savedFollowState >= 0) {
                // back to what he was told before - the harbour made him hold there
                if (this.savedHoldPos != null) this.captain.setHoldPos(this.savedHoldPos);
                this.internalOrder = true;
                this.captain.setFollowState(this.savedFollowState);
                this.internalOrder = false;
            }
        }
        this.savedFollowState = -1;
        this.savedHoldPos = null;
        this.debug("retreat over");
    }

    /** @return a point 64 blocks away from the enemies - or from whoever hit him last - null without any */
    @Nullable
    private Vec3 awayFromEnemy(State own) {
        double x = 0.0D, z = 0.0D;
        int count = 0;
        for (Contact contact : this.warContacts) {
            x += contact.x();
            z += contact.z();
            count++;
        }
        LivingEntity attacker = this.captain.getLastHurtByMob();
        if (count == 0 && attacker != null && attacker.isAlive()) {
            x = attacker.getX();
            z = attacker.getZ();
            count = 1;
        }
        if (count == 0) return null;
        double dx = own.x() - x / count, dz = own.z() - z / count, distance = Math.max(1.0E-3D, Math.hypot(dx, dz));
        return new Vec3(own.x() + dx / distance * 64.0D, 0.0D, own.z() + dz / distance * 64.0D);
    }

    /** Back on the route, at the nearest waypoint. */
    private void resumePatrol() {
        List<BlockPos> waypoints = this.captain.WAYPOINTS;
        this.captain.retreating = false;
        if (waypoints.isEmpty()) {
            this.captain.setPatrolState(AbstractLeaderEntity.State.IDLE);
            return;
        }
        int nearest = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < waypoints.size(); i++) {
            double distance = this.captain.distanceToSqr(waypoints.get(i).getX(), this.captain.getY(), waypoints.get(i).getZ());
            if (distance < best) {
                best = distance;
                nearest = i;
            }
        }
        this.captain.setWaypointIndex(nearest);
        this.captain.currentWaypoint = waypoints.get(nearest);
        this.captain.setPatrolState(AbstractLeaderEntity.State.PATROLLING);
    }

    /* =============================== harbour ================================= */

    private void startRoutine(boolean forRetreat) {
        Entity ship = this.captain.getVehicle();
        this.harbourForRetreat = forRetreat;
        if (ship == null || !SmallShipsBridge.isShip(ship)) {
            this.finishRoutine();
            return;
        }
        BlockPos harbour = this.captain.getHarbourPos();
        if (harbour != null && harbour.distToCenterSqr(ship.position()) > IN_HARBOUR * IN_HARBOUR && this.goHome(true)) return; // there first
        this.harbourShip = ship.getUUID();
        this.handHull = true;
        this.debug("harbour routine started");
        this.enter(Mode.HARBOUR);
        this.step(Step.MOOR);
    }

    private void finishRoutine() {
        this.debug("harbour routine done");
        if (this.harbourForRetreat) {
            this.finishRetreat();
        } else {
            this.enter(Mode.ORDERS);
            this.harbourDone = true;
        }
    }

    /** Every tick while running - also while he is ashore for it. Every step gives up after a while. */
    private void harbourTick() {
        int age = this.captain.tickCount - this.stepSince;
        Entity ship = this.harbourShip();
        if (ship == null) {
            this.finishRoutine();
            return;
        }
        switch (this.step) {
            case MOOR -> {
                if (Math.abs(SmallShipsBridge.getSpeed(ship)) < 0.03F || age > MOOR_TIMEOUT) {
                    boolean upkeep = this.captain.getUpkeepPos() != null || this.captain.getUpkeepUUID() != null;
                    this.step(upkeep ? Step.ASHORE : Step.REPAIR);
                }
            }
            case ASHORE -> {
                // his upkeep goal ends with forcedUpkeep cleared
                if ((age > 40 && !this.captain.forcedUpkeep) || age > ASHORE_TIMEOUT) this.step(Step.BOARD);
            }
            case BOARD -> {
                if (this.captain.getVehicle() == ship) {
                    this.step(Step.REPAIR);
                } else if (age % 200 == 0) {
                    this.captain.shouldMount(true, ship.getUUID());
                    if (this.captain.army != null) RecruitCommanderUtil.setRecruitsMount(this.captain.army.getAllRecruitUnits(), ship.getUUID());
                } else if (age > BOARD_TIMEOUT) {
                    this.debug("could not get back aboard");
                    this.finishRoutine();
                }
            }
            case REPAIR -> {
                if (this.crewWaitSince < 0) {
                    boolean busy = this.dockyard
                            ? age <= DOCKYARD_TIMEOUT && (age % 20 != 0 || SmallShipsBridge.getCondition(ship).getBoolean("DockyardWork"))
                            : age % 10 != 0 || this.repairByHand(ship);
                    if (busy) return;
                    this.captain.refillCannonBalls();
                    this.crewWaitSince = this.captain.tickCount;
                } else if (this.crewAboard(ship) || this.captain.tickCount - this.crewWaitSince > CREW_TIMEOUT) {
                    this.finishRoutine();
                }
            }
        }
    }

    /** A step begins - with what it does once at its start. */
    private void step(Step next) {
        this.step = next;
        this.stepSince = this.captain.tickCount;
        this.debug("harbour " + next);
        if (next == Step.ASHORE) {
            List<AbstractRecruitEntity> crew = this.captain.army != null ? this.captain.army.getAllRecruitUnits() : List.of();
            this.captain.stopRiding();
            this.captain.setUpkeepTimer(0);
            this.captain.forcedUpkeep = true;
            RecruitCommanderUtil.setRecruitsDismount(crew);
            if (this.captain.getUpkeepPos() != null) RecruitCommanderUtil.setRecruitsUpkeep(crew, this.captain.getUpkeepPos(), this.captain.getUpkeepUUID());
        } else if (next == Step.REPAIR) {
            this.crewWaitSince = -1;
            Entity ship = this.harbourShip();
            this.dockyard = ship != null && this.startDockyard(ship);
        }
    }

    /** @return true if a dockyard within reach took the job - hull and canvas, else hull, else canvas - paid from his pockets */
    private boolean startDockyard(Entity ship) {
        Level level = this.captain.getCommandSenderWorld();
        BlockPos dockyard = findDockyard(level, ship);
        if (dockyard == null) return false;
        boolean started = SmallShipsBridge.startDockyardRepair(level, dockyard, ship, this.captain.getInventory(), true, true)
                || SmallShipsBridge.startDockyardRepair(level, dockyard, ship, this.captain.getInventory(), true, false)
                || SmallShipsBridge.startDockyardRepair(level, dockyard, ship, this.captain.getInventory(), false, true);
        this.debug(started ? "dockyard repair started" : "dockyard there, but not paid for");
        return started;
    }

    /** @return true while there is still something to mend by hand and the materials for it */
    private boolean repairByHand(Entity ship) {
        if (this.handHull) {
            if (SmallShipsBridge.repairHullByHand(ship, this.captain.getInventory())) return true;
            this.handHull = false;
        }
        return SmallShipsBridge.patchSails(ship, this.captain.getInventory());
    }

    @Nullable
    private static BlockPos findDockyard(Level level, Entity ship) {
        if (!(level instanceof ServerLevel)) return null;
        CompoundTag rules = ShipProfile.rules();
        int range = (rules != null ? rules.getInt("DockyardRange") : 16) + (int) Math.ceil(ship.getBbWidth());
        ChunkPos from = new ChunkPos(BlockPos.containing(ship.getX() - range, 0, ship.getZ() - range));
        ChunkPos to = new ChunkPos(BlockPos.containing(ship.getX() + range, 0, ship.getZ() + range));
        BlockPos best = null;
        double bestDistance = (double) range * range;
        for (int cx = from.x; cx <= to.x; cx++) {
            for (int cz = from.z; cz <= to.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockPos pos : level.getChunk(cx, cz).getBlockEntities().keySet()) {
                    double distance = pos.distToCenterSqr(ship.position());
                    if (distance >= bestDistance || !SmallShipsBridge.isDockyard(level, pos)) continue;
                    best = pos;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private boolean crewAboard(Entity ship) {
        if (this.captain.army == null) return true;
        for (AbstractRecruitEntity recruit : this.captain.army.getAllRecruitUnits()) {
            if (recruit.isAlive() && recruit.getVehicle() != ship) return false;
        }
        return true;
    }

    @Nullable
    private Entity harbourShip() {
        Entity vehicle = this.captain.getVehicle();
        if (vehicle != null && vehicle.getUUID().equals(this.harbourShip)) return vehicle;
        if (this.harbourShip == null || !(this.captain.getCommandSenderWorld() instanceof ServerLevel level)) return null;
        return level.getEntity(this.harbourShip);
    }

    /* ============================ way home ==================================== */

    /** HOMEWARD along the trail; thenHarbour: the harbour routine follows there. @return false without a harbour */
    private boolean goHome(boolean thenHarbour) {
        if (this.captain.getHarbourPos() == null) return false;
        this.homeThenHarbour = thenHarbour;
        this.homeIndex = -1;
        this.faceTicks = 0;
        this.enter(Mode.HOMEWARD);
        this.debug("homeward, trail has " + this.trail.size() + " points");
        return true;
    }

    /** One step of the way home: from the trail point nearest to her through the points backwards, then station in the harbour. */
    @Nullable
    private Goal homeGoal(State state) {
        BlockPos harbour = this.captain.getHarbourPos();
        if (harbour == null || this.trail.isEmpty()) {
            this.enter(Mode.ORDERS);
            return null;
        }
        if (this.homeIndex < 0 || this.homeIndex >= this.trail.size()) {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < this.trail.size(); i++) {
                double d = distanceSqr(this.trail.get(i), state);
                if (d < best) {
                    best = d;
                    this.homeIndex = i;
                }
            }
        }
        double reached = Math.max(1.5F * this.profile.length, 8.0F);
        while (this.homeIndex > 0 && distanceSqr(this.trail.get(this.homeIndex), state) <= reached * reached) this.homeIndex--;
        if (this.homeIndex > 0) {
            BlockPos next = this.trail.get(this.homeIndex);
            return new Goal(next.getX() + 0.5D, next.getZ() + 0.5D, Float.NaN, false, 0.0F, this.stationDeadZone(), false);
        }
        if (this.arrived && distanceSqr(harbour, state) <= IN_HARBOUR * IN_HARBOUR) {
            this.arriveHome(harbour);
            return null;
        }
        return new Goal(harbour.getX() + 0.5D, harbour.getZ() + 0.5D, Float.NaN, true, 0.0F, this.stationDeadZone(), false);
    }

    /** In the harbour: he holds there like after a hold order - or starts the routine he came for. */
    private void arriveHome(BlockPos harbour) {
        boolean routine = this.homeThenHarbour;
        this.homeThenHarbour = false;
        this.enter(Mode.ORDERS);
        this.captain.setHoldPos(Vec3.atBottomCenterOf(harbour));
        this.internalOrder = true;
        this.captain.setFollowState(2);
        this.internalOrder = false;
        this.tell(Component.translatable("chat.recruits.text.captain_harbour_arrived", this.captain.getName().getString()));
        if (routine) this.startRoutine(this.harbourForRetreat);
    }

    /** Records where she is now on the trail home - every TRAIL_INTERVAL ticks at the helm, not on the way home. */
    private void recordTrail(Entity vehicle) {
        BlockPos harbour = this.captain.getHarbourPos();
        if (harbour == null || this.trail.isEmpty()) return;
        BlockPos pos = vehicle.blockPosition();
        if (this.trailLastSeen != null && horizontal(this.trailLastSeen, pos) > TRAIL_JUMP) {
            this.resetTrail(harbour); // a teleport or a ship boarded elsewhere: start over
            this.debug("trail restarted - the ship jumped");
        }
        this.trailLastSeen = pos;
        if (horizontal(harbour, pos) <= IN_HARBOUR) {
            this.truncateTrail(1);
            return;
        }
        // back near a point sailed before: everything after it was a detour
        for (int i = 1; i < this.trail.size(); i++) {
            if (horizontal(this.trail.get(i), pos) < TRAIL_LOOP) {
                this.truncateTrail(i + 1);
                return;
            }
        }
        if (horizontal(this.trail.get(this.trail.size() - 1), pos) >= TRAIL_STEP) {
            this.trail.add(pos);
            if (this.trail.size() > TRAIL_MAX) {
                // every second point between the harbour and the newest one goes
                for (int i = this.trail.size() - 2; i > 0; i -= 2) this.trail.remove(i);
            }
        }
    }

    private void truncateTrail(int size) {
        while (this.trail.size() > size) this.trail.remove(this.trail.size() - 1);
    }

    /* ============================ contacts ==================================== */

    private void searchContacts() {
        List<LivingEntity> seen = this.captain.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class,
                this.captain.getBoundingBox().inflate(SEARCH_RADIUS),
                target -> target.isAlive() && target.getVehicle() != this.ship && !isDeepUnderWater(target)
                        && this.captain.shouldAttack(target) && this.captain.hasLineOfSight(target));
        // the people on one ship or weapon make one contact
        Map<Entity, List<Entity>> groups = new LinkedHashMap<>();
        for (LivingEntity living : seen) {
            Entity vehicle = living.getVehicle();
            Entity key = vehicle != null && (SmallShipsBridge.isShip(vehicle) || SiegeWeapon.isSiegeWeapon(vehicle)) ? vehicle : living;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(living);
        }
        this.buildContacts(groups);
    }

    /** Between two searches: the known contacts with fresh positions, the dead and sunk left out. */
    private void refreshContacts() {
        if (this.contacts.isEmpty()) return;
        Map<Entity, List<Entity>> groups = new LinkedHashMap<>();
        for (Contact contact : this.contacts) {
            if (!contact.entity().isAlive()) continue;
            List<Entity> people = new ArrayList<>();
            for (Entity person : contact.people()) {
                if (person.isAlive() && (contact.entity() == person || person.getVehicle() == contact.entity())) people.add(person);
            }
            if (!people.isEmpty()) groups.put(contact.entity(), people);
        }
        this.buildContacts(groups);
    }

    private void buildContacts(Map<Entity, List<Entity>> groups) {
        List<Contact> built = new ArrayList<>(groups.size());
        for (Map.Entry<Entity, List<Entity>> group : groups.entrySet()) built.add(describe(group.getKey(), group.getValue()));
        this.contacts = built;
        this.warContacts = built.stream().filter(contact -> !contact.minor()).toList();
    }

    /** @return the contact of this entity - the ship, siege weapon or living - or null */
    @Nullable
    private Contact contactOf(Entity entity) {
        for (Contact contact : this.contacts) {
            if (contact.entity() == entity || contact.people().contains(entity)) return contact;
        }
        return null;
    }

    /** Turns what Small Ships tells exactly into what a lookout would see. */
    private static Contact describe(Entity entity, List<Entity> people) {
        if (SmallShipsBridge.isShip(entity)) {
            CompoundTag profile = SmallShipsBridge.getProfile(entity), condition = SmallShipsBridge.getCondition(entity);
            return new Contact(entity, Kind.SHIP, people, entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYRot(), SmallShipsBridge.getSpeed(entity), SmallShipsBridge.getRotSpeed(entity),
                    profile.getInt("CannonsPort") + profile.getInt("CannonsStarboard"),
                    condition.getFloat("Damage") > Math.max(1.0F, condition.getFloat("MaxHealth")) / 2.0F,
                    Math.min(2, condition.getInt("SailCondition")), profile.getFloat("RamSelfDamage") < 1.0F,
                    profile.getFloat("Length"), profile.getFloat("AirDraft"),
                    condition.getBoolean("Sinking") || condition.getBoolean("Sunken"), false);
        }
        // anything else: the way it moved last tick
        double dx = entity.getX() - entity.xo, dz = entity.getZ() - entity.zo;
        float speed = (float) Math.hypot(dx, dz);
        float yaw = speed > 0.01F ? yawTo(dx, dz) : entity.getYRot();
        Entity vehicle = people.get(0).getVehicle();
        Kind kind = SiegeWeapon.isSiegeWeapon(entity) ? Kind.SIEGE_WEAPON : vehicle instanceof AbstractHorse ? Kind.CAVALRY : Kind.INFANTRY;
        boolean minor = kind != Kind.SIEGE_WEAPON && !(people.get(0) instanceof LivingEntity living && isWarTarget(living));
        return new Contact(entity, kind, people, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                yaw, speed, 0.0F, 0, false, 0, false, entity.getBbWidth(), entity.getBbHeight(), false, minor);
    }

    /** Crews of ships and siege weapons, players and recruits ashore or mounted. Monsters, swimmers and rowing boats are minor. */
    private static boolean isWarTarget(LivingEntity target) {
        Entity vehicle = target.getVehicle();
        if (vehicle != null && (SmallShipsBridge.isShip(vehicle) || SiegeWeapon.isSiegeWeapon(vehicle))) return true;
        if (vehicle != null && !(vehicle instanceof AbstractHorse)) return false;
        if (target.isInWater()) return false;
        return target instanceof Player || target instanceof AbstractRecruitEntity;
    }

    /** @return true two blocks or more under water - no gun aims there */
    private static boolean isDeepUnderWater(LivingEntity target) {
        if (!target.isUnderWater()) return false;
        BlockPos above = BlockPos.containing(target.getX(), target.getEyeY() + 2.0D, target.getZ());
        return target.level().getFluidState(above).is(FluidTags.WATER);
    }

    /* ============================== reports =================================== */

    /** New contacts to the owner - always. Once per contact, again after a minute away; troops alone at most every half minute. */
    private void reportContacts() {
        int now = this.captain.tickCount;
        List<Contact> fresh = new ArrayList<>();
        for (Contact contact : this.warContacts) {
            Integer seen = this.reported.put(contact.entity().getUUID(), now);
            if (seen == null || now - seen > CONTACT_MEMORY) fresh.add(contact);
        }
        this.reported.values().removeIf(seen -> now - seen > CONTACT_MEMORY);
        if (fresh.isEmpty()) return;
        List<Component> ships = new ArrayList<>();
        for (Contact contact : fresh) if (contact.isShip()) ships.add(contact.entity().getName());
        int units = fresh.size() - ships.size();
        if (ships.isEmpty()) {
            if (now - this.lastUnitReport < UNIT_REPORT_GAP) return;
            this.lastUnitReport = now;
        }
        List<Component> parts = new ArrayList<>();
        if (ships.size() == 1) parts.add(Component.translatable("chat.recruits.text.captain_contact_ship", ships.get(0)));
        else if (ships.size() > 1) parts.add(Component.translatable("chat.recruits.text.captain_contact_ships", ships.size(), join(ships)));
        if (units == 1) parts.add(Component.translatable("chat.recruits.text.captain_contact_unit"));
        else if (units > 1) parts.add(Component.translatable("chat.recruits.text.captain_contact_units", units));
        Entity first = fresh.get(0).entity();
        this.tell(Component.translatable("chat.recruits.text.captain_contact", this.captain.getName().getString(), join(parts),
                first.getBlockX(), first.getBlockY(), first.getBlockZ()));
    }

    /** His own ship is going down - once per ship. */
    private void reportSinking() {
        if (this.ship.getUUID().equals(this.sinkingShip)) return;
        this.sinkingShip = this.ship.getUUID();
        this.tell(Component.translatable("chat.recruits.text.captain_sinking", this.captain.getName().getString(),
                this.ship.getBlockX(), this.ship.getBlockY(), this.ship.getBlockZ()));
    }

    private static MutableComponent join(List<Component> parts) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(parts.get(i));
        }
        return joined;
    }

    private void tell(Component message) {
        Player owner = this.captain.getOwner();
        if (owner != null) owner.sendSystemMessage(message);
    }

    /* =============================== guns ===================================== */

    /**
     * Lays both broadsides on the mark and fires a side when the shot can hit.
     * Lead: the ball hits where the target is after fuze plus flight, and
     * leaves from where the muzzle is at the end of the fuze - her own way and
     * turn included. Small Ships reads the aim when the gun goes off, so a
     * fuzing side is laid anew every tick. Fire when the point is in the arc,
     * half the side is loaded, the target within the effective distance (a
     * still one within the greatest range), not going down, no other ship in
     * the line of fire, and more balls in the hold than the reserve.
     */
    private void guns(State own, boolean mayFire, int reserve) {
        int now = this.captain.tickCount;
        boolean fuzing = this.isFuzing(now);
        if (!fuzing && now % GUN_CHECK != Math.floorMod(this.captain.getId(), GUN_CHECK)) return;
        if (!fuzing) this.chooseAmmo(own);
        ShipProfile.Gun gun = ShipProfile.gun(this.ammo, this.profile.fineGrain());
        CompoundTag rules = ShipProfile.rules();
        if (gun == null || rules == null) return;

        ListTag cannons = SmallShipsBridge.getCannons(this.ship);
        for (int side = 0; side <= 1; side++) {
            boolean starboard = side == 1;
            double mx = 0.0D, my = 0.0D, mz = 0.0D;
            int count = 0, ready = 0;
            for (Tag tag : cannons) {
                CompoundTag cannon = (CompoundTag) tag;
                if (cannon.getBoolean("Starboard") != starboard || cannon.getBoolean("Manned")) continue;
                mx += cannon.getDouble("X");
                my += cannon.getDouble("Y");
                mz += cannon.getDouble("Z");
                count++;
                if (cannon.getBoolean("Ready")) ready++;
            }
            if (count == 0) continue;
            mx /= count;
            my /= count;
            mz /= count;
            int elapsed = now - this.fired[side];
            boolean sideFuzing = elapsed <= fuze(rules, false);
            float fuze = sideFuzing ? remainingFuze(rules, elapsed) : (fuze(rules, true) + fuze(rules, false)) / 2.0F;
            double[] aim = this.solve(gun, rules, own, mx, my, mz, fuze, starboard);
            if (aim == null) continue;
            // lay the side unless the aim is unchanged - never laid yet (NaN) counts as changed
            if (!(Math.abs(aim[0] - this.laidPitch[side]) < AIM_STEP && Math.abs(aim[1] - this.laidRotation[side]) < AIM_STEP)) {
                this.laidPitch[side] = (float) aim[0];
                this.laidRotation[side] = (float) aim[1];
                SmallShipsBridge.setBroadsideAim(this.ship, starboard, (float) aim[0], (float) aim[1]);
            }
            if (sideFuzing || !mayFire) continue;
            double limit = Math.abs(this.mark.speed()) < STILL ? gun.maxRange() : gun.effectiveRange();
            if (aim[3] <= 0.0D || ready * 2 < count || aim[2] > limit) continue;
            if ((this.target != null && this.target.sinking()) || this.profile.balls() <= reserve) continue;
            if (this.lineBlocked(mx, my, mz)) continue;
            if (SmallShipsBridge.fireBroadside(this.ship, this.captain, starboard) > 0) {
                this.fired[side] = now;
                return;
            }
        }
    }

    /** @return {elevation, rotation (clamped), horizontal distance, 1 if inside the arc} - null out of reach */
    @Nullable
    private double[] solve(ShipProfile.Gun gun, CompoundTag rules, State own, double mx, double my, double mz, float fuze, boolean starboard) {
        // the muzzle at the end of the fuze: turned with the hull and carried along by her way
        float turn = own.rotSpeed() * fuze, shotYaw = own.yaw() + turn;
        Vec3 fwd = forward(own.yaw()), right = forward(own.yaw() + 90.0F);
        double ox = mx - own.x(), oz = mz - own.z();
        double along = ox * fwd.x + oz * fwd.z, side = ox * right.x + oz * right.z;
        Vec3 shotFwd = forward(shotYaw), shotRight = forward(shotYaw + 90.0F), way = forward(own.yaw() + turn / 2.0F).scale(own.speed() * fuze);
        double sx = own.x() + way.x + shotFwd.x * along + shotRight.x * side;
        double sz = own.z() + way.z + shotFwd.z * along + shotRight.z * side;

        double[] point = this.mark.lead(fuze);
        ShipProfile.Gun.Solution solution = null;
        for (int i = 0; i < 3; i++) {
            solution = gun.solve(Math.hypot(point[0] - sx, point[2] - sz), point[1] - my);
            if (solution == null) return null;
            if (i < 2) point = this.mark.lead(fuze + solution.ticks());
        }
        float bearing = yawTo(point[0] - sx, point[2] - sz);
        float rotation = starboard ? wrap(bearing - (shotYaw + 90.0F)) : wrap(shotYaw - 90.0F - bearing);
        float limit = rules.getFloat("AimRotationMax");
        boolean inArc = Math.abs(rotation) <= limit && solution.elevation() >= rules.getFloat("AimMin") && solution.elevation() <= rules.getFloat("AimMax");
        return new double[]{solution.elevation(), Mth.clamp(rotation, -limit, limit), Math.hypot(point[0] - sx, point[2] - sz), inArc ? 1.0D : 0.0D};
    }

    /** Chain for a faster ship with canvas left, grape for troops or a full deck in reach, ball otherwise - only what is in the hold. */
    private void chooseAmmo(State own) {
        ShipProfile.Ammo wanted = ShipProfile.Ammo.BALL;
        Contact target = this.target;
        if (target != null) {
            ShipProfile.Gun grape = ShipProfile.gun(ShipProfile.Ammo.GRAPE, this.profile.fineGrain());
            boolean grapeRange = grape != null && Math.hypot(this.mark.x() - own.x(), this.mark.z() - own.z()) <= grape.effectiveRange();
            if (target.isShip() && target.sails() < 2 && Math.abs(target.speed()) > Math.abs(own.speed()) + STILL) wanted = ShipProfile.Ammo.CHAINED;
            else if (grapeRange && (!target.isShip() || target.people().size() >= CROWDED_DECK)) wanted = ShipProfile.Ammo.GRAPE;
        }
        if (this.profile.ammo(wanted) <= 0) {
            for (ShipProfile.Ammo type : ShipProfile.Ammo.values()) {
                if (this.profile.ammo(type) > 0) {
                    wanted = type;
                    break;
                }
            }
        }
        if (wanted != this.ammo || !this.ammoSet) {
            this.ammo = wanted;
            this.ammoSet = SmallShipsBridge.setAmmoPreference(this.captain, wanted.name());
        }
    }

    /** @return true if another ship lies in the line of fire */
    private boolean lineBlocked(double mx, double my, double mz) {
        Vec3 from = new Vec3(mx, my, mz), to = new Vec3(this.mark.x(), this.mark.y(), this.mark.z());
        for (Boat other : this.ship.level().getEntitiesOfClass(Boat.class, new AABB(from, to).inflate(8.0D),
                other -> other != this.ship && other != this.mark.ship() && SmallShipsBridge.isShip(other))) {
            for (AABB box : SmallShipsBridge.getHullBoxes(other)) {
                if (box.inflate(1.0D).clip(from, to).isPresent()) return true;
            }
        }
        return false;
    }

    /** @return true while a side she fired is still fuzing - keep the rudder still */
    private boolean isFuzing(int now) {
        CompoundTag rules = ShipProfile.rules();
        int max = rules != null ? fuze(rules, false) : 19;
        return now - this.fired[0] <= max || now - this.fired[1] <= max;
    }

    private static int fuze(CompoundTag rules, boolean min) {
        return rules.getInt(min ? "FuzeMin" : "FuzeMax");
    }

    /** @return expected ticks of fuze left when it has burnt for that long */
    private static float remainingFuze(CompoundTag rules, int elapsed) {
        return Math.max(0.0F, (Math.max(fuze(rules, true), elapsed + 1) + fuze(rules, false)) / 2.0F - elapsed);
    }

    /** Where the guns aim: hull, rigging or deck of a ship by the ammo, the middle of anything else. */
    private Mark markOf(Contact contact) {
        Entity entity = contact.entity();
        if (!contact.isShip()) return new Mark(entity.getX(), contact.y(), entity.getZ(), contact.yaw(), contact.speed(), 0.0F, null);
        if (this.targetHullOf != entity.getId()) {
            this.targetHullOf = entity.getId();
            float bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;
            for (AABB box : SmallShipsBridge.getHullBoxes(entity)) {
                bottom = Math.min(bottom, (float) (box.minY - entity.getY()));
                top = Math.max(top, (float) (box.maxY - entity.getY()));
            }
            this.targetHull[0] = bottom > top ? 0.0F : bottom;
            this.targetHull[1] = bottom > top ? 2.0F : top;
        }
        float bottom = this.targetHull[0], top = this.targetHull[1];
        double y = switch (this.ammo) {
            case CHAINED -> (top + Math.max(top, contact.airDraft())) / 2.0D;
            case GRAPE -> top + 0.8D;
            default -> bottom + (top - bottom) * 0.6D;
        };
        return new Mark(entity.getX(), entity.getY() + y, entity.getZ(), entity.getYRot(),
                SmallShipsBridge.getSpeed(entity), SmallShipsBridge.getRotSpeed(entity), entity);
    }

    @Nullable
    private Mark strategicMark() {
        if (!this.strategicFire()) return null;
        BlockPos pos = this.captain.StrategicFirePos();
        return new Mark(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F, 0.0F, null);
    }

    /* ============================== stations ================================== */

    /** @return where the mode wants her this tick, null: hold still */
    @Nullable
    private Goal goalFor(State own) {
        boolean calm = this.isFuzing(this.captain.tickCount);
        return switch (this.mode) {
            case HARBOUR -> null;
            case HOMEWARD -> this.homeGoal(own);
            case FLEE -> {
                if (this.fleeRoute) yield this.ordersGoal(Float.NaN, false);
                Vec3 away = this.awayFromEnemy(own);
                yield away == null ? null : new Goal(away.x, away.z, Float.NaN, false, 0.0F, this.stationDeadZone(), false);
            }
            case FIGHT -> {
                if (this.mark == null) yield this.ordersGoal(Float.NaN, false);
                yield switch (this.station) {
                    case QUARTER -> {
                        if (this.target == null) yield this.ordersGoal(Float.NaN, false);
                        double[] point = this.quarterPoint(own);
                        yield new Goal(point[0], point[1], (float) point[2], true, 0.0F, this.stationDeadZone(), calm);
                    }
                    case HOLD -> {
                        if (this.holdPoint == null) this.holdPoint = this.stationPoint(own);
                        float heading = broadsideHeading(this.holdPoint.x, this.holdPoint.z, this.mark.x(), this.mark.z(), this.fightSide(own));
                        yield new Goal(this.holdPoint.x, this.holdPoint.z, heading, true, 0.0F, this.stationDeadZone(), calm);
                    }
                    // she follows as ever, and lies broadside on while he lies still
                    case FOLLOW -> this.ordersGoal(broadsideHeading(own.x(), own.z(), this.mark.x(), this.mark.z(), this.fightSide(own)), calm);
                };
            }
            default -> this.ordersGoal(Float.NaN, false);
        };
    }

    /** @return the sail position the commands set (move, hold, follow, protect, patrol, wander), or null */
    @Nullable
    private Goal ordersGoal(float heading, boolean calm) {
        Goal astern = this.followGoal(heading, calm);
        if (astern != null) return astern;
        BlockPos goal = this.captain.getSailPos();
        if (goal == null) return null;
        // a patrol waypoint the route bends little at: sailed through, not stopped at
        boolean stop = !this.throughWaypoint();
        return new Goal(goal.getX() + 0.5D, goal.getZ() + 0.5D, heading, stop, this.goalSpeed, this.deadZone(), calm);
    }

    /**
     * Following someone aboard a ship of his own: not him, but a point astern
     * of that ship - her bow a gap behind its stern - kept at its speed, read
     * straight off it. Its hull stays an obstacle like any other.
     *
     * @return null if she does not follow anyone aboard another ship
     */
    @Nullable
    private Goal followGoal(float heading, boolean calm) {
        if (this.onPatrol()) return null;
        int follow = this.captain.getFollowState();
        Entity leader = follow == 1 ? this.captain.getOwner() : follow == 5 ? this.captain.getProtectingMob() : null;
        Entity ship = leader != null ? leader.getVehicle() : null;
        if (ship == null || ship == this.ship || !SmallShipsBridge.isShip(ship)) return null;
        float zone = Math.max(2.0F, this.profile.length * 0.25F), gap = Math.max(4.0F, this.profile.length * 0.5F);
        double behind = Math.max(2.0F, SmallShipsBridge.getProfile(ship).getFloat("Length")) / 2.0D + gap + zone + this.profile.length / 2.0D;
        Vec3 fwd = forward(ship.getYRot());
        this.goalSpeed = Math.max(0.0F, SmallShipsBridge.getSpeed(ship));
        return new Goal(ship.getX() - fwd.x * behind, ship.getZ() - fwd.z * behind, heading, true, this.goalSpeed, zone, calm);
    }

    /**
     * @return true if the patrol's current waypoint is one to sail through:
     * patrolling, not the first waypoint (the upkeep is there), no wait set
     * for it, a next one in the same direction (no turn at the end of an
     * open route, no retreat) and the route bending at most PASS_ANGLE there
     */
    private boolean throughWaypoint() {
        if (this.captain.getLeaderState() != AbstractLeaderEntity.State.PATROLLING || this.captain.retreating) return false;
        List<BlockPos> waypoints = this.captain.WAYPOINTS;
        int index = this.captain.getWaypointIndex();
        if (waypoints.size() < 2 || index <= 0 || index >= waypoints.size()) return false;
        List<Integer> waits = this.captain.WAYPOINT_WAIT_SECONDS;
        int wait = index < waits.size() ? waits.get(index) : this.captain.getWaitTimeInMin() * 60;
        if (wait > 0) return false;
        int next = index + 1 < waypoints.size() ? index + 1 : this.captain.getCycle() ? 0 : -1;
        if (next < 0) return false;
        BlockPos from = waypoints.get(index - 1), at = waypoints.get(index), to = waypoints.get(next);
        float in = yawTo(at.getX() - from.getX(), at.getZ() - from.getZ()), out = yawTo(to.getX() - at.getX(), to.getZ() - at.getZ());
        return Math.abs(wrap(out - in)) <= PASS_ANGLE;
    }

    /**
     * How close counts as at the current patrol waypoint, squared - see
     * AbstractLeaderEntity#getDistanceToReachWaypoint: one and a half lengths
     * (at least 12 blocks); any distance once she has crossed the line across
     * a waypoint she sails through, or once another ship lies on it and she
     * is within two lengths more.
     */
    public double waypointReach() {
        double reach = this.profile != null ? Math.max(12.0D, 1.5D * this.profile.length) : 12.0D;
        BlockPos at = this.captain.currentWaypoint;
        if (at == null || this.ship == null || this.profile == null) return reach * reach;
        // out of reach for her hull: as close as she gets is there
        if (this.goalUnreachable && this.noWay && Math.abs(this.unreachableX - at.getX() - 0.5D) < 1.0D && Math.abs(this.unreachableZ - at.getZ() - 0.5D) < 1.0D) {
            this.reportUnreachable(at);
            return Double.MAX_VALUE;
        }
        double dx = this.ship.getX() - at.getX() - 0.5D, dz = this.ship.getZ() - at.getZ() - 0.5D, distance = Math.hypot(dx, dz);
        if (distance > reach + 2.0D * this.profile.length) return reach * reach;
        if (this.throughWaypoint()) {
            BlockPos from = this.captain.WAYPOINTS.get(this.captain.getWaypointIndex() - 1);
            if (dx * (at.getX() - from.getX()) + dz * (at.getZ() - from.getZ()) > 0.0D) return Double.MAX_VALUE; // past it
        }
        return isTaken(this.ship, at) ? Double.MAX_VALUE : reach * reach;
    }

    /** Tells the owner once per waypoint that her hull cannot get there and she goes on from the nearest spot. */
    private void reportUnreachable(BlockPos waypoint) {
        if (this.reportedUnreachable.size() > 64) this.reportedUnreachable.clear();
        if (!this.reportedUnreachable.add(waypoint.asLong())) return;
        this.tell(Component.translatable("chat.recruits.text.captain_waypoint_unreachable", this.captain.getName().getString(),
                this.captain.getWaypointIndex() + 1));
    }

    /** @return true if another ship's hull covers the spot, half a block to spare */
    private static boolean isTaken(Entity own, BlockPos spot) {
        double x = spot.getX() + 0.5D, z = spot.getZ() + 0.5D;
        for (Boat other : own.level().getEntitiesOfClass(Boat.class, new AABB(spot).inflate(16.0D, 8.0D, 16.0D),
                other -> other != own && other.isAlive() && SmallShipsBridge.isShip(other))) {
            CompoundTag its = SmallShipsBridge.getProfile(other);
            double half = Math.max(1.0F, its.getFloat("Length")) / 2.0D, r = Math.max(1.0F, its.getFloat("Beam")) / 2.0D + 0.5D;
            Vec3 fwd = forward(other.getYRot());
            double along = Mth.clamp((x - other.getX()) * fwd.x + (z - other.getZ()) * fwd.z, -half, half);
            if (Math.hypot(other.getX() + fwd.x * along - x, other.getZ() + fwd.z * along - z) <= r) return true;
        }
        return false;
    }

    /**
     * Against a ship with guns: one of four quarter points at R, 45 degrees off
     * its bow or stern - outside both its broadsides. They turn with the
     * target; she keeps the one she has unless another is clearly nearer.
     * Against anything else: abeam of it at R. Always the same side of her own ship.
     *
     * @return {x, z, heading}
     */
    private double[] quarterPoint(State own) {
        Mark mark = this.mark;
        double range = this.range(), x = own.x(), z = own.z();
        if (this.target.isShip() && this.target.guns() > 0) {
            if (this.target.entity().getId() != this.quarterTarget) {
                this.quarter = -1;
                this.quarterTarget = this.target.entity().getId();
            }
            double current = Double.MAX_VALUE;
            if (this.quarter >= 0) {
                Vec3 p = quarterAt(mark, this.quarter, range);
                if (this.isOpenWater(p.x, p.z)) current = Math.hypot(p.x - own.x(), p.z - own.z());
            }
            int best = current < Double.MAX_VALUE ? this.quarter : -1;
            double bestDistance = current < Double.MAX_VALUE ? current - range * CHANGE_SHARE : Double.MAX_VALUE;
            for (int i = 0; i < QUARTERS.length; i++) {
                if (i == this.quarter) continue;
                Vec3 p = quarterAt(mark, i, range);
                double distance = Math.hypot(p.x - own.x(), p.z - own.z());
                if (distance < bestDistance && this.isOpenWater(p.x, p.z)) {
                    best = i;
                    bestDistance = distance;
                }
            }
            this.quarter = best;
            if (best >= 0) { // else nowhere deep enough around it: fire from where she is
                Vec3 p = quarterAt(mark, best, range);
                x = p.x;
                z = p.z;
            }
        } else {
            double dx = own.x() - mark.x(), dz = own.z() - mark.z(), distance = Math.max(1.0E-3D, Math.hypot(dx, dz));
            double ax = mark.x() + dx / distance * range, az = mark.z() + dz / distance * range;
            if (this.isOpenWater(ax, az)) {
                x = ax;
                z = az;
            }
        }
        return new double[]{x, z, broadsideHeading(x, z, mark.x(), mark.z(), this.fightSide(own))};
    }

    private static Vec3 quarterAt(Mark mark, int quarter, double range) {
        Vec3 dir = forward(mark.yaw() + QUARTERS[quarter]);
        return new Vec3(mark.x() + dir.x * range, 0.0D, mark.z() + dir.z * range);
    }

    /** @return the side of her own ship that faces the target - chosen once per target (the one that needs less turning) and kept */
    private boolean fightSide(State own) {
        int of = this.target != null ? this.target.entity().getId() : -2;
        if (of != this.fightSideOf) {
            this.fightSideOf = of;
            float sb = broadsideHeading(own.x(), own.z(), this.mark.x(), this.mark.z(), true);
            float port = broadsideHeading(own.x(), own.z(), this.mark.x(), this.mark.z(), false);
            this.starboardSide = Math.abs(wrap(sb - own.yaw())) <= Math.abs(wrap(port - own.yaw()));
        }
        return this.starboardSide;
    }

    /** @return where to hold: the hold position of a hold order, else where she is */
    private Vec3 stationPoint(State own) {
        int follow = this.captain.getFollowState();
        BlockPos sail = this.captain.getSailPos();
        if (!this.onPatrol() && (follow == 2 || follow == 3 || follow == 4) && sail != null) return new Vec3(sail.getX() + 0.5D, 0.0D, sail.getZ() + 0.5D);
        return new Vec3(own.x(), 0.0D, own.z());
    }

    /** @return R: 0.8 x the effective distance of the ammo; troops ashore from beyond their reach */
    private double range() {
        ShipProfile.Gun gun = ShipProfile.gun(this.ammo, this.profile.fineGrain());
        double range = (gun != null ? gun.effectiveRange() : 45.0D) * RANGE_SHARE;
        if (this.target != null && !this.target.isShip()) {
            ShipProfile.Gun ball = ShipProfile.gun(ShipProfile.Ammo.BALL, this.profile.fineGrain());
            range = Math.min(Math.max(range, LAND_TROOPS_RANGE), ball != null ? ball.maxRange() * 0.9D : 70.0D);
        }
        return range;
    }

    /** @return the heading that brings the target abeam on that side of her own ship */
    private static float broadsideHeading(double x, double z, double targetX, double targetZ, boolean starboard) {
        float bearing = yawTo(targetX - x, targetZ - z);
        return wrap(starboard ? bearing - 90.0F : bearing + 90.0F);
    }

    private boolean isOpenWater(double x, double z) {
        return ShipPathfinder.isOpenWater(this.ship, this.profile, x, z, this.profile.beam / 2.0D + 1.0D);
    }

    /* =============================== the helm ================================= */

    private void steer() {
        Entity vehicle = this.captain.getVehicle();
        if (vehicle == null || !SmallShipsBridge.isShip(vehicle)) {
            this.detach();
            return;
        }
        if (vehicle != this.ship) this.attach(vehicle);
        int now = this.captain.tickCount, phase = Math.floorMod(this.captain.getId(), CONDITION_INTERVAL);
        if (this.profile == null) {
            if (++this.profileAge >= 40) this.attach(vehicle); // the ship could not tell its profile yet
            return;
        }
        if (!SmallShipsBridge.isHelmsman(vehicle, this.captain)) return; // aboard, but somebody else has the helm
        if (++this.profileAge >= PROFILE_INTERVAL) {
            this.profileAge = 0;
            ShipProfile fresh = ShipProfile.of(vehicle); // upgrades change it
            if (fresh != null) this.profile = fresh;
        } else if (now % CONDITION_INTERVAL == phase) {
            if (this.profile.update(vehicle)) this.lastHit = now;
            this.updateGoalSpeed();
        }
        if (!SmallShipsBridge.canMakeWay(vehicle)) { // moored, in the dockyard or going down: hands off
            SmallShipsBridge.pressKeys(vehicle, false, false, false, false);
            return;
        }
        if (this.mode != Mode.HOMEWARD && now % TRAIL_INTERVAL == Math.floorMod(this.captain.getId(), TRAIL_INTERVAL)) this.recordTrail(vehicle);

        State state = stateOf(vehicle);
        this.lookout(vehicle, state);
        this.watchTraffic(vehicle, state);
        this.handleTrouble(vehicle, state);
        Keys keys;
        this.wantsWay = false;
        if (this.backTicks > 0) {
            keys = this.backUpTick(vehicle, state);
        } else if (this.faceTicks > 0) {
            this.faceTicks--;
            this.wantedYaw = this.faceYaw;
            keys = this.turnOnSpot(vehicle, state, this.faceYaw);
            if (Math.abs(wrap(this.faceYaw - state.yaw())) < HEADING_DEADBAND * 2.0F && Math.abs(state.rotSpeed()) < ROT_SETTLED) this.faceTicks = 0;
        } else if (now < this.restUntil) {
            keys = this.helmStop(state);
        } else if (this.evadeFrom != null) {
            keys = this.helmEvade(state);
        } else {
            Goal goal = this.goalFor(state);
            keys = goal == null ? this.halt(state) : this.sail(vehicle, state, goal);
        }
        if (this.backTicks <= 0) this.keepClear(state, keys);
        this.pressing = keys.forward || state.sailState() > 0;
        this.rudderHeld = keys.left || keys.right;
        // updateControls only writes the flags that changed, so this is cheap every tick
        SmallShipsBridge.pressKeys(vehicle, keys.forward, keys.backward, keys.left, keys.right);
        if (keys.sail) SmallShipsBridge.pressSailKey(vehicle);
    }

    private void attach(Entity vehicle) {
        this.detach();
        this.ship = vehicle;
        this.profile = ShipProfile.of(vehicle);
    }

    private void detach() {
        this.ship = null;
        this.profile = null;
        this.profileAge = 0;
        this.faceTicks = 0;
        this.backTicks = 0;
        this.turnDir = 0;
        this.lastGoal = null;
        this.goalSpeed = 0.0F;
        this.pressing = this.rudderHeld = this.shipAhead = this.blockedByPlan = false;
        this.wantedYaw = Float.NaN;
        this.blocker = null;
        this.lookoutCap = Float.MAX_VALUE;
        this.lookStop = this.lookReverse = false;
        this.trafficCap = Float.MAX_VALUE;
        this.trafficShip = null;
        this.evadeFrom = null;
        this.pathYaw = Float.NaN;
        this.resetStuck();
        this.clearWay();
        this.sailKeyLock = 0;
        this.lastSail = -1;
        this.sailAge = SAIL_REST;
        this.lastSailDir = this.sailWish = 0;
        this.wishing = this.turningFirst = this.arrived = this.braking = false;
        this.helmGoalX = this.helmGoalZ = Double.NaN;
    }

    /** Not at the helm any more: the fight is off, a retreat and the harbour routine go on. */
    private void leaveHelm() {
        this.fighting = false;
        this.fightStart = null;
        this.target = null;
        this.mark = null;
        if (this.mode == Mode.FIGHT) this.enter(Mode.ORDERS);
        this.contacts = this.warContacts = List.of();
        this.fired[0] = this.fired[1] = Integer.MIN_VALUE / 2;
        this.laidPitch[0] = this.laidPitch[1] = this.laidRotation[0] = this.laidRotation[1] = Float.NaN;
        this.ammoSet = false;
    }

    /** How fast the goal moves (a followed entity), from where it was a second ago. A fixed spot and its jumps are no speed. */
    private void updateGoalSpeed() {
        BlockPos goal = this.captain.getSailPos();
        int follow = this.captain.getFollowState();
        if ((follow != 1 && follow != 5) || goal == null || this.lastGoal == null) {
            this.goalSpeed = 0.0F;
        } else {
            float speed = (float) Math.sqrt(goal.distSqr(this.lastGoal)) / CONDITION_INTERVAL;
            if (speed > this.profile.maxSpeed * 2.0F) speed = 0.0F; // a teleport or a new target
            this.goalSpeed = this.goalSpeed * 0.5F + speed * 0.5F;
        }
        this.lastGoal = goal;
    }

    /** Toward the goal - and there, turned to its heading if it has one. */
    private Keys sail(Entity vehicle, State state, Goal goal) {
        Keys keys;
        double slack = goal.deadZone() + this.profile.length * 0.5F;
        if (goal.stop() && !Float.isNaN(goal.heading()) && this.arrived
                && sqr(goal.x() - state.x()) + sqr(goal.z() - state.z()) <= slack * slack) {
            this.wantedYaw = goal.heading();
            keys = this.turnOnSpot(vehicle, state, goal.heading());
        } else {
            keys = this.navigate(vehicle, state, goal);
        }
        if (goal.calm()) keys.left = keys.right = false; // the guns are fuzing
        return keys;
    }

    private Keys halt(State state) {
        this.clearWay();
        this.wantedYaw = Float.NaN;
        return this.helmStop(state);
    }

    /* ================================ the way ================================= */

    /** Sails to the goal: straight where the water allows it, along planned corners where not; stops for what the lookout sees. */
    private Keys navigate(Entity vehicle, State state, Goal goal) {
        this.updateWay(vehicle, state, goal);
        float cap = this.lookoutCap == Float.MAX_VALUE ? 1.0F : Mth.clamp(this.lookoutCap / Math.max(0.01F, this.profile.maxSpeedNow()), 0.05F, 1.0F);
        double aimX = goal.x(), aimZ = goal.z();
        boolean finalLeg = true, waitAtEnd = false;
        if (this.noWay) {
            aimX = this.noWayX;
            aimZ = this.noWayZ;
            waitAtEnd = true;
        } else if (this.path != null) {
            this.advance(state);
            finalLeg = false;
            if (this.pathIndex >= this.path.size() - 1 && this.pathReachesGoal) {
                finalLeg = true;
                if (!goal.stop()) { // sailing through: on along her lane
                    double[] end = this.path.get(this.path.size() - 1);
                    aimX = end[0];
                    aimZ = end[1];
                }
            } else if (this.pathIndex >= this.path.size()) {
                double[] end = this.path.get(this.path.size() - 1); // end of a partial way: wait there for the next plan
                aimX = end[0];
                aimZ = end[1];
                waitAtEnd = true;
            } else {
                aimX = this.path.get(this.pathIndex)[0];
                aimZ = this.path.get(this.pathIndex)[1];
            }
        }
        // other ships: planned round; one right before the bow she turns away from: slow, touching does no harm then
        boolean moving = Math.hypot(aimX - state.x(), aimZ - state.z()) > goal.deadZone() && !this.arrived;
        this.wantsWay = moving;
        this.watchShips(vehicle, state, aimX, aimZ, goal, moving);
        if (this.shipAhead) cap = Math.min(cap, Mth.clamp(SAFE_SPEED / Math.max(0.01F, this.profile.maxSpeedNow()), 0.05F, 1.0F));
        if (this.trafficCap != Float.MAX_VALUE) cap = Math.min(cap, Mth.clamp(this.trafficCap / Math.max(0.01F, this.profile.maxSpeedNow()), 0.05F, 1.0F));
        if (this.blocker != null) return this.waitForShip(vehicle, state);

        float aimYaw = yawTo(aimX - state.x(), aimZ - state.z()), turn = Math.abs(wrap(aimYaw - state.yaw()));
        this.wantedYaw = aimYaw;
        this.pathYaw = aimYaw;
        if (Math.hypot(aimX - state.x(), aimZ - state.z()) > goal.deadZone() && !this.arrived && Math.abs(state.speed()) < SWING_SPEED
                && turn > (this.lookStop ? STOPPED_TURN : TURN_FIRST)) {
            return this.turnOnSpot(vehicle, state, aimYaw);
        }
        if (this.lookStop) {
            // no way on: turn towards where she has to go; something right at the bow: a little astern, if there is water behind
            Keys keys = this.helmFace(state, aimYaw);
            if (this.lookReverse && state.sailState() == 0 && room(vehicle, state.yaw(), -1, 1.5D) >= 1.0D) {
                keys.forward = false;
                keys.backward = true;
            }
            return keys;
        }
        if (waitAtEnd) return this.helmStation(state, aimX, aimZ, 0.0F, goal.deadZone(), cap);
        if (finalLeg) return goal.stop() ? this.helmStation(state, aimX, aimZ, goal.speed(), goal.deadZone(), cap) : this.helmThrough(state, aimX, aimZ, cap);
        return this.helmThrough(state, aimX, aimZ, cap * this.cornerFactor(state));
    }

    /** Plans the way anew when it is due - rate limited, the goal jumping excepted. */
    private void updateWay(Entity vehicle, State state, Goal goal) {
        double moved = Double.isNaN(this.plannedX) ? Double.MAX_VALUE : Math.hypot(goal.x() - this.plannedX, goal.z() - this.plannedZ);
        boolean goalMoved = moved > Math.max(4.0D, Math.hypot(goal.x() - state.x(), goal.z() - state.z()) * 0.15D);
        if (goal.stop() && !goalMoved && this.arrived) return; // keeping station at an unchanged goal
        int now = this.captain.tickCount;
        boolean wayDone = this.path != null && this.pathIndex >= this.path.size() && !this.pathReachesGoal;
        boolean stale = now - this.plannedAt >= REFRESH_PLAN && (this.noWay || (this.path != null && !this.pathReachesGoal));
        boolean retry = this.blocker != null && now - this.plannedAt >= WAIT_RETRY;
        if (!goalMoved && !this.replan && !wayDone && !this.isOffWay(state) && !stale && !retry) return;
        if (moved <= GOAL_JUMP && now - this.plannedAt < MIN_REPLAN) return;
        if (!takePlanBudget(vehicle.level().getGameTime())) return;
        boolean wasBlocked = this.blockedByPlan;
        this.plan(vehicle, state, goal);
        if (wasBlocked && !this.blockedByPlan) this.blocker = null; // a way again: no more waiting
    }

    private void plan(Entity vehicle, State state, Goal goal) {
        double targetX = goal.x(), targetZ = goal.z();
        // keep to the right: not in a fight, not after a ship she follows, not for a few lengths; a stop stays where it is
        boolean lanes = this.mode != Mode.FIGHT && goal.speed() < 0.02F
                && Math.hypot(targetX - state.x(), targetZ - state.z()) >= 2.0D * this.profile.length;
        double lane = this.profile.beam + 1.0D;
        this.plannedAt = this.captain.tickCount;
        this.plannedX = targetX;
        this.plannedZ = targetZ;
        this.replan = false;
        this.blockedByPlan = false;
        this.goalUnreachable = false;
        this.path = null;
        this.pathIndex = 0;
        this.noWay = false;
        this.pathFromX = state.x();
        this.pathFromZ = state.z();
        ShipPathfinder.Obstacles ships = ShipPathfinder.ships(vehicle);
        {
            ShipPathfinder finder = ShipPathfinder.of(vehicle, this.profile, ships);
            if (finder.isWide(state.x(), state.z(), targetX, targetZ)) { // straight: well off any coast - on the right lane of it
                if (lanes) {
                    List<double[]> lane1 = finder.keepRight(state.x(), state.z(), List.of(new double[]{targetX, targetZ}), lane, !goal.stop());
                    if (lane1.size() > 1 || lane1.get(0)[0] != targetX || lane1.get(0)[1] != targetZ) {
                        this.path = lane1;
                        this.pathReachesGoal = true;
                    }
                }
                return;
            }

            ShipPathfinder.Result result = finder.plan(state.x(), state.z(), targetX, targetZ);
            double progress = 0.0D;
            if (!result.waypoints().isEmpty()) {
                double[] end = result.waypoints().get(result.waypoints().size() - 1);
                progress = Math.hypot(targetX - state.x(), targetZ - state.z()) - Math.hypot(targetX - end[0], targetZ - end[1]);
            }
            if (!result.waypoints().isEmpty() && (result.reachedGoal() || progress > 4.0D)) {
                this.path = lanes ? finder.keepRight(state.x(), state.z(), result.waypoints(), lane, !goal.stop() && result.reachedGoal())
                        : result.waypoints();
                this.pathReachesGoal = result.reachedGoal();
                this.debug("way: " + this.path.size() + " corners" + (ships.ships().isEmpty() ? "" : ", round " + ships.ships().size() + " ships")
                        + (result.reachedGoal() ? "" : ", partial"));
                return;
            }
        }
        // no way from here: keep station. The ships only block it if there is a way without them -
        // else the goal itself is out of reach for her hull (too shallow, too narrow) and she lies as close as she gets
        this.noWay = true;
        this.noWayX = state.x();
        this.noWayZ = state.z();
        if (!ships.ships().isEmpty() && this.reachableWithoutShips(vehicle, state, targetX, targetZ)) {
            this.blockedByPlan = true;
            this.block(nearest(ships.ships(), state));
            this.debug("way blocked by ships - waiting");
        } else {
            this.goalUnreachable = true;
            this.unreachableX = targetX;
            this.unreachableZ = targetZ;
            this.debug("goal " + Math.round(targetX) + " " + Math.round(targetZ) + " out of reach for her hull - holding as close as she gets");
        }
    }

    /** @return true if, with no ship about, she would reach the goal - or at least get on towards it from here */
    private boolean reachableWithoutShips(Entity vehicle, State state, double targetX, double targetZ) {
        ShipPathfinder free = ShipPathfinder.of(vehicle, this.profile, ShipPathfinder.Obstacles.NONE);
        if (free.isWide(state.x(), state.z(), targetX, targetZ)) return true;
        ShipPathfinder.Result result = free.plan(state.x(), state.z(), targetX, targetZ);
        if (result.reachedGoal()) return true;
        if (result.waypoints().isEmpty()) return false;
        double[] end = result.waypoints().get(result.waypoints().size() - 1);
        return Math.hypot(targetX - state.x(), targetZ - state.z()) - Math.hypot(targetX - end[0], targetZ - end[1]) > 4.0D;
    }

    /**
     * Every SHIP_LOOK ticks: another ship on the next stretch - plan anew round
     * it. A real hull right before the bow: if the way she wants to go leads
     * into it too, she waits for it; if she turns away from it, she only slows down.
     */
    private void watchShips(Entity vehicle, State state, double aimX, double aimZ, Goal goal, boolean moving) {
        if (this.captain.tickCount % SHIP_LOOK != Math.floorMod(this.captain.getId(), SHIP_LOOK)) return;
        this.shipAhead = false;
        if (!moving) {
            if (!this.blockedByPlan) this.block(null);
            return;
        }
        if (this.blocker == null && !this.replan) {
            ShipPathfinder.Obstacles ships = ShipPathfinder.ships(vehicle);
            double halfBeam = Math.max(0.0D, this.profile.beam / 2.0D - 1.0D);
            boolean crossing = !ships.ships().isEmpty() && (ships.crosses(state.x(), state.z(), aimX, aimZ, halfBeam)
                    || (this.path != null && this.pathIndex + 1 < this.path.size()
                    && ships.crosses(this.path.get(this.pathIndex)[0], this.path.get(this.pathIndex)[1],
                    this.path.get(this.pathIndex + 1)[0], this.path.get(this.pathIndex + 1)[1], halfBeam)));
            if (crossing) this.replan = true;
        }
        if (this.blockedByPlan) return;
        // the real hulls, as Small Ships would collide them - one that keeps ahead of her excepted
        double look = this.profile.stoppingDistance(Math.max(0.0F, state.speed())) + 1.5D;
        Entity onTheWay = null;
        if (shipAlong(vehicle, state, forward(state.yaw()), look) != null) {
            double distance = Math.max(1.0E-3D, Math.hypot(aimX - state.x(), aimZ - state.z()));
            onTheWay = shipAlong(vehicle, state, new Vec3((aimX - state.x()) / distance, 0.0D, (aimZ - state.z()) / distance), look);
            this.shipAhead = onTheWay == null;
            if (onTheWay != null) this.replan = true;
        }
        this.block(onTheWay);
    }

    /** @return the ship her hull would run into going this way - not one that makes at least her way in that direction: it keeps ahead */
    @Nullable
    private static Entity shipAlong(Entity vehicle, State state, Vec3 direction, double look) {
        for (double share : new double[]{0.5D, 1.0D}) {
            Entity hit = SmallShipsBridge.findShipAhead(vehicle, direction.scale(look * share));
            if (hit == null) continue;
            Vec3 its = forward(hit.getYRot());
            double away = SmallShipsBridge.getSpeed(hit) * (its.x * direction.x + its.z * direction.z);
            if (away < Math.max(0.0F, state.speed()) - 0.02D) return hit;
        }
        return null;
    }

    /** She holds still until the blocker is gone. Two captains waiting for each other: the one with the higher id backs off now and then. */
    private Keys waitForShip(Entity vehicle, State state) {
        int now = this.captain.tickCount;
        if (!this.blocker.isAlive()) {
            this.block(null);
            this.blockedByPlan = false;
            this.replan = true;
        } else if (now - this.blockedSince > GIVE_WAY) {
            // a hull right before her that does not go away: she backs off and plans round it - two
            // captains in each other's way: the one with the higher id does, the other keeps waiting
            Entity helmsman = SmallShipsBridge.getHelmsman(this.blocker);
            boolean mutual = helmsman instanceof CaptainEntity other && other.shipController.isWaitingFor(vehicle);
            // ships that close her way and go nowhere: after BLOCKED_WAIT she does not wait any longer either
            boolean gives = mutual ? this.captain.getId() > helmsman.getId() : !this.blockedByPlan || now - this.blockedSince > BLOCKED_WAIT;
            if (gives) {
                this.blockedSince = now;
                if (this.startBackUp(vehicle, state, -1)) {
                    this.replan = true;
                    this.debug("giving way to " + this.blocker.getName().getString());
                }
            }
        }
        this.wantedYaw = Float.NaN;
        Keys keys = this.helmStop(state);
        if (state.sailState() == 0 && state.speed() > 0.03F) keys.backward = true; // stop, not coast
        return keys;
    }

    private void block(@Nullable Entity ship) {
        if (ship != null && ship != this.blocker) this.blockedSince = this.captain.tickCount;
        this.blocker = ship;
    }

    @Nullable
    private static Entity nearest(List<Entity> ships, State state) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity ship : ships) {
            double distance = Math.hypot(ship.getX() - state.x(), ship.getZ() - state.z());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = ship;
            }
        }
        return best;
    }

    /** @return true if one more way may be planned this tick - all captains share PLANS_PER_TICK */
    private static boolean takePlanBudget(long gameTime) {
        if (gameTime != budgetTick) {
            budgetTick = gameTime;
            budgetUsed = 0;
        }
        return budgetUsed++ < PLANS_PER_TICK;
    }

    /** Moves on to the next corner once this one is reached or passed - not too early, that cuts the corner towards the coast. */
    private void advance(State state) {
        double reach = Math.max(this.profile.length * 0.5D, 4.0D);
        while (this.pathIndex < this.path.size()) {
            double[] corner = this.path.get(this.pathIndex);
            double fromX = this.pathIndex == 0 ? this.pathFromX : this.path.get(this.pathIndex - 1)[0];
            double fromZ = this.pathIndex == 0 ? this.pathFromZ : this.path.get(this.pathIndex - 1)[1];
            double dx = state.x() - corner[0], dz = state.z() - corner[1];
            boolean passed = (corner[0] - fromX) * dx + (corner[1] - fromZ) * dz > 0.0D;
            if (dx * dx + dz * dz > reach * reach && !passed) return;
            this.pathIndex++;
        }
    }

    /** @return true if she drifted well off the leg she is sailing */
    private boolean isOffWay(State state) {
        if (this.path == null || this.pathIndex >= this.path.size()) return false;
        double[] to = this.path.get(this.pathIndex);
        double fromX = this.pathIndex == 0 ? this.pathFromX : this.path.get(this.pathIndex - 1)[0];
        double fromZ = this.pathIndex == 0 ? this.pathFromZ : this.path.get(this.pathIndex - 1)[1];
        double lx = to[0] - fromX, lz = to[1] - fromZ, lengthSqr = lx * lx + lz * lz;
        double t = lengthSqr < 1.0E-6D ? 0.0D : Mth.clamp(((state.x() - fromX) * lx + (state.z() - fromZ) * lz) / lengthSqr, 0.0D, 1.0D);
        double allowed = Math.ceil(this.profile.beam / 2.0F) + 6.0D;
        return sqr(state.x() - fromX - lx * t) + sqr(state.z() - fromZ - lz * t) > allowed * allowed;
    }

    /** @return 0.5 approaching a sharp corner within braking distance, else 1 */
    private float cornerFactor(State state) {
        if (this.path == null || this.pathIndex + 1 >= this.path.size()) return 1.0F;
        double[] corner = this.path.get(this.pathIndex), after = this.path.get(this.pathIndex + 1);
        double inX = corner[0] - state.x(), inZ = corner[1] - state.z(), outX = after[0] - corner[0], outZ = after[1] - corner[1];
        double lengths = Math.hypot(inX, inZ) * Math.hypot(outX, outZ);
        if (lengths < 1.0E-6D) return 1.0F;
        double turn = Math.toDegrees(Math.acos(Mth.clamp((inX * outX + inZ * outZ) / lengths, -1.0D, 1.0D)));
        double near = this.profile.stoppingDistance(Math.max(0.0F, state.speed())) + this.profile.length * 2.0D;
        return turn > SHARP_TURN && Math.hypot(inX, inZ) < near ? 0.5F : 1.0F;
    }

    /** Forgets the way and its goal - the next goal is planned at once. */
    private void clearWay() {
        this.path = null;
        this.pathIndex = 0;
        this.noWay = this.pathReachesGoal = this.replan = false;
        this.plannedX = this.plannedZ = Double.NaN;
    }

    /* ======================= lookout and backing up =========================== */

    /**
     * Every LOOKOUT_INTERVAL ticks the water ahead, the way Small Ships itself
     * would move her: land or shallows - slow to a speed at which grounding
     * costs no hull, plan anew, stop if close. Every STUCK_CHECK ticks: no
     * progress although she pressed ahead (STUCK), or the bow does not come
     * round although the rudder is laid (JAMMED). Cheap: the column cache
     * first, the hull swept against the blocks only where land is.
     */
    private void lookout(Entity ship, State state) {
        if (this.rudderHeld) this.rudderTicks++;
        if (this.pressing && !this.lookStop) this.pressTicks++; // waiting where she was told to stop is not being stuck
        int now = this.captain.tickCount, phase = Math.floorMod(this.captain.getId(), STUCK_CHECK);
        if (now % STUCK_CHECK == phase) this.checkStuck(state);
        if (now % LOOKOUT_INTERVAL != phase % LOOKOUT_INTERVAL) return;

        this.lookoutCap = Float.MAX_VALUE;
        this.lookStop = this.lookReverse = false;
        if (state.speed() <= 0.03F) return; // not under way, or astern: nothing ahead to watch
        Vec3 heading = forward(state.yaw());
        double look = this.profile.stoppingDistance(state.speed()) + this.profile.length * 0.5D + 1.0D;
        if (ShipPathfinder.isWaterAhead(ship, this.profile, heading, look)) return;
        Vec3 wanted = heading.scale(look), allowed = SmallShipsBridge.sweep(ship, wanted);
        if (wanted.subtract(allowed).horizontalDistance() <= 0.25D) return;

        // already turning towards open water - a river bend: let her come round slowly
        double free = allowed.horizontalDistance();
        boolean turningAway = !Float.isNaN(this.wantedYaw) && Math.abs(wrap(this.wantedYaw - state.yaw())) > 10.0F
                && ShipPathfinder.isWaterAhead(ship, this.profile, forward(this.wantedYaw), look);
        this.lookoutCap = Math.min(SAFE_SPEED, this.profile.coastSpeedFor((float) Math.max(0.0D, free - 1.0D)));
        if (!turningAway) this.replan = true;
        if (free < (turningAway ? 1.0D : Math.max(2.0D, this.profile.length * 0.25D))) {
            this.lookStop = true;
            this.lookReverse = free < 1.0D;
        }
    }

    /**
     * Every TRAFFIC_LOOK ticks, while she makes way: the ships about with her
     * way and theirs, DODGE_LOOK ticks ahead. One that would come within half
     * a block of her hull: she dodges - DODGE_ANGLE to starboard for
     * DODGE_TICKS (overtaking, or land to starboard: to port), at half her
     * way - and goes back onto her way. Every ship does the same, nobody
     * waits: with the lane on the right they pass port to port. Should the
     * touch already lie within her stopping distance, she also brakes (see
     * keepClear). Ships lying still are the way's business, not this one's.
     * A hull touching her bow (or her stern while she goes astern) while she
     * presses into it: she moves off it and plans anew.
     */
    private void watchTraffic(Entity vehicle, State state) {
        int now = this.captain.tickCount;
        if (now % TRAFFIC_LOOK != Math.floorMod(this.captain.getId() + TRAFFIC_LOOK / 2, TRAFFIC_LOOK)) return;
        // a dodge ends once the other is past: back on the course of her way she would not touch it any more
        if (this.evadeFrom != null && now - this.evadeSince >= DODGE_TICKS) {
            float course = Float.isNaN(this.pathYaw) ? state.yaw() : this.pathYaw;
            double[] still = this.evadeFrom.isAlive() ? this.contactWith(state, course, Math.max(0.1F, state.speed()), this.evadeFrom, DODGE_LOOK) : null;
            if (still == null || still[1] <= 0.01D || now - this.evadeSince >= DODGE_MAX) {
                this.debug("dodged " + this.evadeFrom.getName().getString() + " - back on the way");
                this.evadeFrom = null;
                this.replan = true;
            }
        }
        float cap = Float.MAX_VALUE;
        Entity closest = null;
        double[] first = null;
        if (state.speed() > 0.03F) {
            for (Boat other : vehicle.level().getEntitiesOfClass(Boat.class, vehicle.getBoundingBox().inflate(TRAFFIC_RANGE, 8.0D, TRAFFIC_RANGE),
                    other -> other != vehicle && other.isAlive() && SmallShipsBridge.isShip(other))) {
                double[] contact = this.contactWith(state, state.yaw(), state.speed(), other, DODGE_LOOK);
                if (contact == null || contact[1] <= 0.01D) continue; // none, or it comes at her while she makes no way towards it
                // a brake only where she could not stop in time any more: her share of the water between them
                float away = (float) Math.max(0.0D, contact[2]);
                float room = (float) Math.max(0.0D, (contact[1] - away) * contact[0] - 1.0D - state.speed() * (TRAFFIC_LOOK + 2));
                cap = Math.min(cap, away + this.profile.coastSpeedFor(room));
                if (first == null || contact[0] < first[0]) {
                    first = contact;
                    closest = other;
                }
            }
        }
        if (closest != null && cap < state.speed()) {
            if (this.trafficShip != closest) this.debug("about to touch " + closest.getName().getString() + " - braking");
            this.trafficCap = cap;
            this.trafficShip = closest;
            this.trafficSeen = now;
        } else if (this.trafficShip != null && now - this.trafficSeen >= TRAFFIC_HOLD) {
            this.trafficCap = Float.MAX_VALUE;
            this.trafficShip = null;
        }
        // a ship lying still is the way's business - only one under way is dodged
        if (closest != null && Math.abs(SmallShipsBridge.getSpeed(closest)) > 0.05F
                && this.evadeFrom == null && this.wantsWay && this.backTicks <= 0 && this.faceTicks <= 0) {
            this.dodge(vehicle, state, closest);
        }

        // touching and pressing into it: off it, then a way round
        if (this.backTicks > 0 || now < this.restUntil) return;
        Vec3 fwd = forward(state.yaw());
        Entity ahead = SmallShipsBridge.findShipAhead(vehicle, fwd.scale(TOUCH));
        if (ahead != null && (this.pressing || state.speed() > 0.02F)) {
            this.debug("touching " + ahead.getName().getString() + " at the bow - backing off");
            this.evadeFrom = null;
            if (this.startBackUp(vehicle, state, -1)) this.replan = true;
        } else if (ahead == null && state.speed() < -0.02F) {
            Entity astern = SmallShipsBridge.findShipAhead(vehicle, fwd.scale(-TOUCH));
            if (astern != null) {
                this.debug("touching " + astern.getName().getString() + " at the stern - moving off ahead");
                if (this.startBackUp(vehicle, state, 1)) this.replan = true;
            }
        }
    }

    @Nullable
    private double[] contactWith(State state, float yaw, float speed, Entity other, int horizon) {
        CompoundTag its = SmallShipsBridge.getProfile(other);
        return ShipPathfinder.contact(state.x(), state.z(), yaw, speed, this.profile.length, this.profile.beam,
                other.getX(), other.getZ(), other.getYRot(), SmallShipsBridge.getSpeed(other),
                Math.max(2.0F, its.getFloat("Length")), Math.max(1.0F, its.getFloat("Beam")), horizon);
    }

    /**
     * DODGE_ANGLE off the course of her way: to starboard - to port when she
     * overtakes it (comes up from abaft its beam, faster), so the slower one
     * keeps its lane - and the other side where there is no water or another
     * ship in the way. Neither side: no dodge, the brake alone.
     */
    private void dodge(Entity vehicle, State state, Entity other) {
        float course = Float.isNaN(this.pathYaw) ? state.yaw() : this.pathYaw;
        boolean overtaking = Math.abs(wrap(yawTo(state.x() - other.getX(), state.z() - other.getZ()) - other.getYRot())) > 112.5F
                && state.speed() > SmallShipsBridge.getSpeed(other);
        double look = this.profile.length + 4.0D;
        for (int side : overtaking ? new int[]{-1, 1} : new int[]{1, -1}) {
            float yaw = wrap(course + side * DODGE_ANGLE);
            if (!ShipPathfinder.isWaterAhead(vehicle, this.profile, forward(yaw), look)) continue;
            boolean free = true;
            for (Boat ship : vehicle.level().getEntitiesOfClass(Boat.class, vehicle.getBoundingBox().inflate(TRAFFIC_RANGE, 8.0D, TRAFFIC_RANGE),
                    o -> o != vehicle && o.isAlive() && SmallShipsBridge.isShip(o))) {
                double[] contact = this.contactWith(state, yaw, state.speed(), ship, DODGE_LOOK);
                if (contact != null && contact[1] > 0.01D) {
                    free = false;
                    break;
                }
            }
            if (!free) continue;
            this.evadeFrom = other;
            this.evadeYaw = yaw;
            this.evadeSince = this.captain.tickCount;
            this.debug((overtaking ? "overtaking " : "dodging ") + other.getName().getString() + " - " + (side > 0 ? "starboard" : "port"));
            return;
        }
    }

    /** Holds the dodging course at half her way at most - no faster than she goes. */
    private Keys helmEvade(State state) {
        this.beginKeys(state);
        this.wantedYaw = this.evadeYaw;
        float error = wrap(this.evadeYaw - state.yaw());
        this.steerBy(state, error);
        this.drive(state, Math.max(0.05F, Math.min(state.speed(), this.profile.maxSpeedNow() * 0.5F)), Math.abs(error));
        return this.keys;
    }

    /**
     * Traffic ahead: no W and no canvas beyond what she may make - set canvas
     * comes in at once, furled canvas stays furled while even the first sail
     * state would be too much, and S while she still makes more way than she may.
     */
    private void keepClear(State state, Keys keys) {
        if (this.trafficCap == Float.MAX_VALUE) return;
        if (state.speed() >= this.trafficCap) keys.forward = false;
        if (state.sailState() > 0) {
            if (state.speed() > this.trafficCap) this.pressSailKey();
        } else {
            if (this.trafficCap < this.profile.sailSpeed(1)) keys.sail = false;
            if (state.speed() > this.trafficCap + 0.02F) keys.backward = true;
        }
    }

    private void checkStuck(State state) {
        boolean measured = !Double.isNaN(this.stuckX);
        boolean stuck = measured && this.pressing && this.pressTicks >= STUCK_CHECK * 3 / 4
                && Math.hypot(state.x() - this.stuckX, state.z() - this.stuckZ) < STUCK_PROGRESS;
        boolean jammed = measured && this.rudderTicks >= STUCK_CHECK / 2 && Math.abs(wrap(state.yaw() - this.stuckYaw)) < JAMMED_TURN
                && Math.abs(state.speed()) < 0.05F;
        this.stuckX = state.x();
        this.stuckZ = state.z();
        this.stuckYaw = state.yaw();
        this.rudderTicks = this.pressTicks = 0;
        this.trouble = jammed ? Trouble.JAMMED : stuck ? Trouble.STUCK : null;
    }

    private void resetStuck() {
        this.stuckX = Double.NaN;
        this.rudderTicks = this.pressTicks = 0;
        this.trouble = null;
    }

    /** Stuck: back off and plan anew. Jammed in a turn: the other way round, and if that jams too, back off first. */
    private void handleTrouble(Entity vehicle, State state) {
        Trouble trouble = this.trouble;
        this.trouble = null;
        if (trouble == null || this.backTicks > 0 || this.captain.tickCount < this.restUntil) return;
        this.replan = true;
        if (trouble == Trouble.STUCK) {
            this.debug("stuck - backing off");
            this.startBackUp(vehicle, state);
        } else if (!Float.isNaN(this.wantedYaw)) {
            int shortWay = wrap(this.wantedYaw - state.yaw()) >= 0.0F ? 1 : -1;
            if (this.turnDir == 0) {
                this.turnDir = -shortWay;
                this.debug("turn jammed - the other way round");
            } else if (!this.startBackUp(vehicle, state)) {
                this.turnDir = -this.turnDir;
            }
        }
    }

    /** Turns on the spot to the yaw. A big turn needs a block before the bow first - else she backs off. */
    private Keys turnOnSpot(Entity vehicle, State state, float yaw) {
        float error = Math.abs(wrap(yaw - state.yaw()));
        if (error < TURN_DONE) {
            this.turnDir = 0;
            this.bowChecked = false;
        } else if (!this.bowChecked && error > STOPPED_TURN && Math.abs(state.speed()) < SWING_SPEED) {
            this.bowChecked = true;
            if (room(vehicle, state.yaw(), 1, BOW_ROOM + 0.25D) < BOW_ROOM && this.startBackUp(vehicle, state)) return this.backUpTick(vehicle, state);
        }
        return this.turnDir == 0 ? this.helmFace(state, yaw) : this.helmTurn(state, yaw, this.turnDir);
    }

    /**
     * Backs her off straight astern - as far as there is water behind the
     * stern, most of her length, further when it happens again soon. Three
     * times within BACK_OFF_REPEAT: she gives up for REST_TICKS and plans anew.
     *
     * @return false if there is no room astern, or she gives up
     */
    private boolean startBackUp(Entity vehicle, State state) {
        return this.startBackUp(vehicle, state, -1);
    }

    /** @param sign -1 astern, +1 ahead - off a ship that touches her stern */
    private boolean startBackUp(Entity vehicle, State state, int sign) {
        int now = this.captain.tickCount;
        int streak = now - this.backedAt < BACK_OFF_REPEAT ? this.backStreak + 1 : 0;
        if (streak >= 3) {
            this.backStreak = 0;
            this.restUntil = now + REST_TICKS;
            this.replan = true;
            this.debug("cannot get clear here - holding still, planning anew");
            return false;
        }
        double wanted = Math.max(4.0D, this.profile.length * 0.75D) * (1.0D + 0.5D * streak);
        double astern = room(vehicle, state.yaw(), sign, wanted + SAFETY) - SAFETY;
        if (astern < MIN_MOVE) return false;
        Vec3 along = forward(state.yaw());
        this.backSign = sign;
        this.backStreak = streak;
        this.backedAt = now;
        this.backFromX = state.x();
        this.backFromZ = state.z();
        this.backAlongX = along.x;
        this.backAlongZ = along.z;
        this.backDistance = Math.min(astern, wanted);
        this.backTicks = (int) (this.backDistance / Math.max(0.01F, this.profile.manoeuvreSpeed) * 1.5D) + 40;
        this.resetStuck();
        this.debug((sign < 0 ? "backing off " : "moving off ahead ") + Math.round(this.backDistance) + " blocks");
        return true;
    }

    private Keys backUpTick(Entity vehicle, State state) {
        double made = this.backSign * ((state.x() - this.backFromX) * this.backAlongX + (state.z() - this.backFromZ) * this.backAlongZ);
        boolean done = made >= this.backDistance || --this.backTicks <= 0;
        // the stern (or bow) close to something: this is as far as she gets
        if (!done && this.captain.tickCount % LOOKOUT_INTERVAL == 0) done = room(vehicle, state.yaw(), this.backSign, SAFETY + 0.5D) < SAFETY;
        Keys keys = this.helmStop(state);
        if (done) {
            this.backTicks = 0;
            this.replan = true;
            return keys;
        }
        if (this.backSign < 0) keys.backward = state.sailState() == 0;
        else keys.forward = state.sailState() == 0;
        return keys;
    }

    /** @return blocks the hull could move straight ahead (sign +1) or astern (-1), up to distance */
    private static double room(Entity ship, float yaw, int sign, double distance) {
        return SmallShipsBridge.sweep(ship, forward(yaw).scale(sign * distance)).horizontalDistance();
    }

    /* ======================= keys, as a helmsman would ======================== */
    /*
     * Three things a ship needs that a horse does not: turning is inertial -
     * without counter rudder a hull swings 110-150 degrees past its heading;
     * she stops by coasting - the approach brakes exactly at her stopping
     * distance; and sails change one step per cooldown - so the canvas is
     * worked like a calm helmsman does it: in at once to stop or turn, set
     * again only after a few seconds (she creeps or rows meanwhile), never up
     * and down in quick turns.
     */

    private Keys helmStop(State state) {
        this.beginKeys(state);
        this.furl(state);
        this.killSwing(state);
        return this.keys;
    }

    private Keys helmFace(State state, float yaw) {
        this.beginKeys(state);
        this.furl(state);
        this.steerBy(state, wrap(yaw - state.yaw()));
        return this.keys;
    }

    /** Turns on the spot the given way round, even the long way: +1 starboard (D), -1 port (A). */
    private Keys helmTurn(State state, float yaw, int direction) {
        this.beginKeys(state);
        this.furl(state);
        float error = wrap(yaw - state.yaw());
        if (direction > 0 && error < 0.0F) error += 360.0F;
        else if (direction < 0 && error > 0.0F) error -= 360.0F;
        this.steerBy(state, error);
        return this.keys;
    }

    /**
     * Sails to the goal and keeps station there: stops within deadZone and
     * stays until the goal moves or she drifts out. A goal that moves
     * (goalSpeed, a followed entity) is kept up with instead: its speed plus a
     * little for every block she is behind.
     */
    private Keys helmStation(State state, double goalX, double goalZ, float goalSpeed, float deadZone, float speedCap) {
        this.beginKeys(state);
        double distance = Math.hypot(goalX - state.x(), goalZ - state.z());
        boolean moving = goalSpeed >= 0.02F;
        // a goal that jumped is a new approach - one that creeps along is not, or every step would set the sails again
        double newGoal = Math.max(2.0D, deadZone);
        if (Double.isNaN(this.helmGoalX) || sqr(goalX - this.helmGoalX) + sqr(goalZ - this.helmGoalZ) > newGoal * newGoal) {
            this.helmGoalX = goalX;
            this.helmGoalZ = goalZ;
            this.braking = false;
            if (distance > deadZone) this.arrived = false;
        }
        if (this.arrived && (moving || distance > deadZone + Math.max(2.0F, this.profile.length * 0.5F))) {
            this.arrived = this.braking = false;
        }
        if (!moving && !this.arrived && distance <= deadZone && Math.abs(state.speed()) < 0.05F) this.arrived = true;
        if (this.arrived) {
            this.furl(state);
            this.killSwing(state);
            return this.keys;
        }
        float bearing = yawTo(goalX - state.x(), goalZ - state.z()), error = Math.abs(wrap(bearing - state.yaw()));
        this.steerBy(state, wrap(bearing - state.yaw()));
        float remaining = (float) distance - deadZone;
        if (this.turnFirst(state, error)) {
            this.furl(state);
            return this.keys;
        }
        if (moving) {
            // closing in on it no faster than she can still stop behind it: else canvas in, and S
            float limit = goalSpeed + this.profile.coastSpeedFor(Math.max(0.0F, remaining));
            if (state.speed() > limit + 0.03F) {
                this.furl(state);
                if (state.sailState() == 0 && state.speed() > goalSpeed + 0.03F) this.keys.backward = true;
                return this.keys;
            }
            float wanted = remaining > 0.0F ? goalSpeed + 0.01F * remaining : goalSpeed * 0.8F;
            this.drive(state, Math.min(this.cruise(speedCap, error), Math.min(wanted, limit)), error);
            return this.keys;
        }
        if (remaining <= 0.0F) { // inside the zone and still carrying way: let her run out
            this.furl(state);
            return this.keys;
        }
        // from the moment she could not stop in time she only coasts: furled once, the rest under oars or creeping
        if (!this.braking && this.profile.stoppingDistance(state.speed()) + Math.abs(state.speed()) >= remaining) this.braking = true;
        if (this.braking && remaining > this.profile.stoppingDistance(this.profile.maxSpeedNow()) * 2.0F + this.profile.length) this.braking = false;
        if (this.braking) this.pull(state, Math.min(this.profile.coastSpeedFor(remaining), this.profile.maxSpeedNow() * speedCap), error);
        else this.drive(state, this.cruise(speedCap, error), error);
        return this.keys;
    }

    /** Sails at a point without stopping there - a corner on the way. */
    private Keys helmThrough(State state, double goalX, double goalZ, float speedCap) {
        this.beginKeys(state);
        this.arrived = this.braking = false;
        this.helmGoalX = this.helmGoalZ = Double.NaN;
        float bearing = yawTo(goalX - state.x(), goalZ - state.z()), error = Math.abs(wrap(bearing - state.yaw()));
        this.steerBy(state, wrap(bearing - state.yaw()));
        if (this.turnFirst(state, error)) this.furl(state);
        else this.drive(state, this.cruise(speedCap, error), error);
        return this.keys;
    }

    /** @return true while she should turn before making way: the goal abeam or astern - with hysteresis, no back and forth */
    private boolean turnFirst(State state, float error) {
        if (this.turningFirst) {
            if (error < TURN_FIRST - TURN_FIRST_HYSTERESIS) this.turningFirst = false;
        } else if (error > (state.sailState() > 0 ? TURN_FIRST + TURN_FIRST_HYSTERESIS : TURN_FIRST)) {
            this.turningFirst = true;
        }
        return this.turningFirst;
    }

    /** @return the speed worth making on this heading error: less while the bow still points away */
    private float cruise(float speedCap, float error) {
        return this.profile.maxSpeedNow() * speedCap * (float) Math.max(0.35D, Math.cos(Math.toRadians(error)));
    }

    /**
     * The rudder of a helmsman who knows his ship: against the turn as soon as
     * the bow would swing past the heading otherwise. D = starboard, A = port.
     *
     * @param error degrees still to turn, + = to starboard - may be more than 180
     */
    private void steerBy(State state, float error) {
        float swing = state.rotSpeed();
        if (Math.abs(error) < HEADING_DEADBAND && Math.abs(swing) < ROT_SETTLED) return;
        if (swing != 0.0F && Math.signum(swing) == Math.signum(error) && Math.abs(error) <= this.profile.brakeAngle(swing) + Math.abs(swing)) {
            // she would overshoot: counter rudder - unless a single tick of it would turn the swing round
            if (Math.abs(swing) > this.profile.rotBrake) {
                if (swing > 0.0F) this.keys.left = true;
                else this.keys.right = true;
            }
            return;
        }
        if (error > 0.0F) this.keys.right = true;
        else this.keys.left = true;
    }

    private void killSwing(State state) {
        float swing = state.rotSpeed();
        if (Math.abs(swing) <= this.profile.rotBrake) return;
        if (swing > 0.0F) this.keys.left = true;
        else this.keys.right = true;
    }

    /** Sails 0; W while slower than wanted - oars on a galley, creeping on everything else. */
    private void pull(State state, float wanted, float error) {
        if (state.sailState() > 0) this.pressSailKey();
        else if (error <= PULL_ANGLE && state.speed() < wanted - 0.002F) this.keys.forward = true;
    }

    private void furl(State state) {
        if (state.sailState() > 0) this.pressSailKey();
    }

    /**
     * Makes the wanted speed under oars or canvas. Canvas calmly: furled sails
     * go up only after SAIL_REST and once she has wanted them for SAIL_INTENT
     * (she creeps or rows meanwhile), a set state is worked the other way than
     * last time only after SAIL_REVERSE_HOLD - W/S one step per cooldown.
     */
    private void drive(State state, float wanted, float error) {
        if (this.profile.rowFor(wanted)) {
            this.pull(state, wanted, error);
            return;
        }
        int current = state.sailState(), target = this.profile.sailStateFor(wanted, current);
        if (current == 0) {
            this.wishing = true;
            if (++this.sailWish >= SAIL_INTENT && (this.lastSailDir >= 0 || this.sailAge >= SAIL_REST)) this.pressSailKey();
            else this.pull(state, wanted, error);
            return;
        }
        if (target == current) return;
        int direction = target > current ? 1 : -1;
        if (direction != this.lastSailDir && this.sailAge < SAIL_REVERSE_HOLD) return;
        if (direction > 0) this.keys.forward = true;
        else this.keys.backward = true;
    }

    private void pressSailKey() {
        if (this.sailKeyLock > 0) return;
        this.keys.sail = true;
        this.sailKeyLock = SAIL_KEY_LOCK;
    }

    /** Every helm command starts here, once per tick: fresh keys, and what the canvas did since last tick. */
    private void beginKeys(State state) {
        this.keys.forward = this.keys.backward = this.keys.left = this.keys.right = this.keys.sail = false;
        if (this.sailKeyLock > 0) this.sailKeyLock--;
        int sails = state.sailState();
        if (this.lastSail >= 0 && sails != this.lastSail) {
            this.lastSailDir = sails > this.lastSail ? 1 : -1;
            this.sailAge = 0;
        } else if (this.sailAge < 100000) {
            this.sailAge++;
        }
        this.lastSail = sails;
        if (!this.wishing) this.sailWish = 0; // the wish to set canvas only counts while it is unbroken
        this.wishing = false;
    }

    /* ================================ small =================================== */

    /** How close counts as there for a fixed spot, in blocks. */
    private float stationDeadZone() {
        return Math.max(0.75F * this.profile.length, 4.0F);
    }

    /** How close counts as there for the commands - wider while following. */
    private float deadZone() {
        int follow = this.captain.getFollowState();
        return follow == 1 || follow == 5 ? Math.max(1.5F * this.profile.length, 8.0F) : this.stationDeadZone();
    }

    /** @return the unit vector along a yaw, Minecraft convention: 0 = +Z (south), 90 = -X (west) */
    /**
     * Where something moving along its heading will be after that many ticks - a turning ship along its
     * curve. The captain's lead, also used by the siege engineer at a ground cannon.
     *
     * @param speed    blocks per tick along the heading (SmallShipsBridge#getSpeed for a ship)
     * @param rotSpeed degrees per tick (SmallShipsBridge#getRotSpeed for a ship, 0 for anything else)
     * @return {x, y, z}
     */
    public static double[] lead(double x, double y, double z, float yaw, float speed, float rotSpeed, float ticks) {
        Vec3 heading = forward(yaw + rotSpeed * ticks / 2.0F);
        return new double[]{x + heading.x * speed * ticks, y, z + heading.z * speed * ticks};
    }

    static Vec3 forward(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    /** @return the yaw that points along (dx, dz) */
    public static float yawTo(double dx, double dz) {
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

    private static double horizontal(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    private static double distanceSqr(BlockPos point, State state) {
        return sqr(point.getX() + 0.5D - state.x()) + sqr(point.getZ() + 0.5D - state.z());
    }

    private static State stateOf(Entity ship) {
        return new State(ship.getX(), ship.getZ(), ship.getYRot(), SmallShipsBridge.getSpeed(ship),
                SmallShipsBridge.getRotSpeed(ship), SmallShipsBridge.getSailState(ship));
    }

    /** Debug log line about this captain's decisions - visible with the debug log level. */
    private void debug(String message) {
        Main.LOGGER.debug("[Captain {}] {}", this.captain.getName().getString(), message);
    }
}
