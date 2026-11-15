package com.talhanation.recruits.entities;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.SeaRoute;
import com.talhanation.recruits.world.SimulatedTravelManager;
import com.talhanation.recruits.world.TravelPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A recruit who can go on a long journey that carries on while no player is near.
 * <p>
 * The subclass says where to go ({@link #startTravel(TravelPlan)}) and what happens at the end
 * ({@link #onTravelArrived(TravelPlan)}). This class walks there while a player can see it and reports to the
 * {@link SimulatedTravelManager} every second, which takes the group out of the world when nobody watches and
 * puts it back further along the way. The plan is saved with the entity, so a journey survives being out of
 * the world, unloaded chunks and restarts.
 * <p>
 * Who travels along ({@link #getTravelCompany()}) and how the leader moves ({@link #travelMoveTowards}) can be
 * overridden, e.g. for an army or a ship.
 */
public abstract class AbstractTravelEntity extends BowmanEntity {
    /** How far ahead the next stretch is set. Short enough for the pathfinder, long enough to keep walking. */
    private static final double STRETCH = 32.0D;
    /** Seconds of real walking before the measured speed counts. */
    private static final int MEASURE_SECONDS = 10;

    @Nullable
    private TravelPlan travelPlan;
    @Nullable
    private Vec3 lastTravelPos;
    private double travelMoved;
    private int travelMovedSeconds;
    /** Closest he came to the current destination, and for how many seconds he has not come closer. */
    private double travelBestDistance = Double.MAX_VALUE;
    private int travelNoProgressSeconds;

    public AbstractTravelEntity(EntityType<? extends AbstractTravelEntity> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        if (this.level().isClientSide() || Math.floorMod(this.tickCount + this.getId(), 20) != 0) return;

        if (this.travelPlan != null) {
            this.travelStep();
        } else if (this.isOnMission()) {
            // not on the road right now, but away from home: the manager keeps an eye on him, see onTravelStranded()
            SimulatedTravelManager manager = SimulatedTravelManager.get();
            if (manager != null) manager.watch(this);
        }
    }

    /** One second of a journey in the world: arrive, wait for the target player, or walk on and report. */
    private void travelStep() {
        SimulatedTravelManager manager = SimulatedTravelManager.get();
        TravelPlan plan = this.travelPlan;
        if (manager == null || plan == null || !this.isAlive() || !(this.level() instanceof ServerLevel level)) return;

        if (this.steersOwnTravel()) { // a patrol leader moves by his own rules, here he only reports
            this.measureTravelSpeed(plan);
            manager.track(this, 0);
            return;
        }

        Vec3 destination = plan.resolve(level.getServer(), level.dimension(), level.getServer().overworld().getGameTime());
        if (destination == null) {
            if (plan.isWaitingForTarget()) {
                this.travelHalt();
                manager.track(this, 0);
            } else {
                this.finishTravel();
            }
            return;
        }

        double dx = destination.x - this.getX();
        double dz = destination.z - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= this.getTravelArrivalRange(plan.followsTarget())) {
            // way points are passed on the way; the last one and a target player or companion are the end of the journey
            if (plan.followsTarget() || !plan.nextPoint()) this.finishTravel();
            else this.travelBestDistance = Double.MAX_VALUE;
            return;
        }

        // stuck is not coming closer, not standing still: at a wall he may walk up and down for ever
        if (distance < this.travelBestDistance - 1.0D) {
            this.travelBestDistance = distance;
            this.travelNoProgressSeconds = 0;
        } else {
            this.travelNoProgressSeconds++;
        }

        this.measureTravelSpeed(plan);
        this.travelMoveTowards(destination, distance);
        manager.track(this, this.travelNoProgressSeconds);
    }

    /** The speed of the group is what it really walks: mounts, patrol speed and slow companions are all in it. */
    private void measureTravelSpeed(TravelPlan plan) {
        Vec3 pos = this.position();
        if (this.lastTravelPos != null) {
            double dx = pos.x - this.lastTravelPos.x;
            double dz = pos.z - this.lastTravelPos.z;
            double moved = Math.sqrt(dx * dx + dz * dz);
            if (moved >= 0.5D && moved < 30.0D) { // less is standing, more is a teleport
                this.travelMoved += moved;
                this.travelMovedSeconds++;
                if (this.travelMovedSeconds >= MEASURE_SECONDS) plan.speed = this.travelMoved / this.travelMovedSeconds;
                if (this.travelMovedSeconds >= 120) { // newer stretches count more
                    this.travelMoved /= 2.0D;
                    this.travelMovedSeconds /= 2;
                }
            }
        }
        this.lastTravelPos = pos;
    }

    ///////////////////////////////////TRAVEL/////////////////////////////////////////

    /** Starts a journey, a running one is replaced. */
    public void startTravel(TravelPlan plan) {
        this.travelPlan = plan;
        this.resetTravelMeasure();
    }

    /** Ends the journey where the entity stands, without arriving. */
    public void stopTravel() {
        if (this.travelPlan == null) return;
        this.travelPlan = null;
        this.resetTravelMeasure();
        SimulatedTravelManager manager = SimulatedTravelManager.get();
        if (manager != null) manager.untrack(this.getUUID());
    }

    /** The way over water along the route was found, see {@code SimulatedTravelManager#planSeaRoute}. */
    public void onSeaRoute(SeaRoute route) {
    }

    /** Called by the {@link SimulatedTravelManager} when the group is back in the world: the journey goes on from here. */
    public void resumeTravel(TravelPlan plan) {
        this.travelPlan = plan;
        this.resetTravelMeasure();
        this.onTravelResumed();
    }

    private void finishTravel() {
        TravelPlan plan = this.travelPlan;
        this.travelPlan = null;
        this.resetTravelMeasure();
        SimulatedTravelManager manager = SimulatedTravelManager.get();
        if (manager != null) manager.untrack(this.getUUID());
        if (plan != null) this.onTravelArrived(plan);
    }

    private void resetTravelMeasure() {
        this.lastTravelPos = null;
        this.travelMoved = 0.0D;
        this.travelMovedSeconds = 0;
        this.travelBestDistance = Double.MAX_VALUE;
        this.travelNoProgressSeconds = 0;
    }

    public boolean isTravelling() {
        return this.travelPlan != null;
    }

    @Nullable
    public TravelPlan getTravelPlan() {
        return this.travelPlan;
    }

    /**
     * Walks the next stretch towards the destination: a point at most {@value #STRETCH} blocks ahead on the surface,
     * taken as hold position, so the normal hold-position goal does the walking.
     */
    protected void travelMoveTowards(Vec3 destination, double distance) {
        Vec3 target = destination;
        if (distance > STRETCH) {
            double x = this.getX() + (destination.x - this.getX()) / distance * STRETCH;
            double z = this.getZ() + (destination.z - this.getZ()) / distance * STRETCH;
            BlockPos column = BlockPos.containing(x, this.getY(), z);
            double y = this.level().hasChunkAt(column)
                    ? this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ())
                    : this.getY();
            target = new Vec3(x, y, z);
        }
        this.setHoldPos(target);
        if (this.getFollowState() != 3) this.setFollowState(3);
    }

    /** Stands still where he is, e.g. while the target player is gone for a moment. */
    protected void travelHalt() {
        this.setHoldPos(this.position());
        if (this.getFollowState() != 3) this.setFollowState(3);
    }

    /**
     * true: the subclass moves the leader itself while he is in the world and keeps the plan in step with where he
     * is going (a patrol with its formation, a ship). This class then only measures the speed and reports.
     */
    protected boolean steersOwnTravel() {
        return false;
    }

    /**
     * Away from home with something to finish, on the road or not (a messenger waiting at his target player, a
     * patrol leader resupplying). While true the {@link SimulatedTravelManager} watches him.
     */
    public boolean isOnMission() {
        return this.isTravelling();
    }

    /**
     * His chunk is about to be unloaded while he is on a mission but not on the road: nobody is left here, he would
     * be frozen in the chunk. Start a journey here ({@link #startTravel}) to carry on out of the world.
     */
    public void onTravelStranded() {
    }

    /** Who travels with the leader (escort, army, crew). Mounts and passengers come along by themselves. */
    public List<? extends Entity> getTravelCompany() {
        return List.of();
    }

    /** Blocks per second for a journey that goes out of the world before the real speed could be measured. */
    public double getDefaultTravelSpeed() {
        return this.getVehicle() != null ? RecruitsServerConfig.TravelSpeedMounted.get() : RecruitsServerConfig.TravelSpeedOnFoot.get();
    }

    /**
     * How close counts as arrived, in blocks. A target player or companion moves and may stand where the leader
     * cannot go (a ship); the subclass walks up to him afterwards if it has to.
     */
    protected double getTravelArrivalRange(boolean targetMoves) {
        return targetMoves ? 24.0D : 3.0D;
    }

    /**
     * The journey is over: the leader stands at its last way point or near its target player.
     * @param plan the finished plan; {@link TravelPlan#targetLost} tells if the target player was given up on the way
     */
    protected void onTravelArrived(TravelPlan plan) {
    }

    /** The group was put back into the world and walks on. */
    protected void onTravelResumed() {
    }

    /**
     * The journey to a companion ({@link TravelPlan#targetCompanion}) reached him while nobody watches: the group
     * stays out of the world. Called on a copy of the leader that is in no world - it must not look for entities
     * around it. What it changes on itself is kept.
     * @return the journey to go on with, null to go on with the rest of the old plan (its way points)
     */
    @Nullable
    public TravelPlan onTravelArrivedOutOfWorld(TravelPlan plan) {
        return null;
    }

    /**
     * Somebody changes this leader while his group is out of the world, e.g. a Messenger hands him new orders.
     * Called on a copy of the leader that is in no world, standing where the journey got to. What the change does
     * to him is kept, and the journey goes on with what he wants afterwards.
     * @param plan   the journey as it is right now
     * @param change what is done to him
     * @return the journey to go on with, null if he does not travel any more: the group then waits where it is
     */
    @Nullable
    public TravelPlan changeOutOfWorld(TravelPlan plan, Runnable change) {
        this.travelPlan = plan;
        change.run();
        return this.travelPlan;
    }

    ////////////////////////////////////DATA////////////////////////////////////

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.travelPlan != null) nbt.put("TravelPlan", this.travelPlan.save());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.travelPlan = nbt.contains("TravelPlan") ? TravelPlan.load(nbt.getCompound("TravelPlan")) : null;
    }
}
