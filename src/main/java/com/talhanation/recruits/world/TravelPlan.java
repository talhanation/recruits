package com.talhanation.recruits.world;

import com.talhanation.recruits.RecruitEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Where a travelling group wants to go. It lives on the leader
 * ({@link com.talhanation.recruits.entities.AbstractTravelEntity}) while he really walks and in the
 * {@link SimulatedTravel} while the group is out of the world, so both move it by the same rules.
 * <p>
 * A plan follows a player or one of the owner's companions, or walks a list of points, or both: then the points
 * are where the group goes when the player or companion is not there (any more).
 * <p>
 * A {@link #route} is a patrol: it never ends, waits at every point and then goes on to the next one, round in a
 * circle or there and back - the same rules as {@code AbstractLeaderEntity#updateWaypointIndex()}.
 */
public class TravelPlan {
    /** A target player who is gone (offline, dead, other dimension) is given up after this many ticks. */
    public static final long TARGET_GRACE = 1200L;
    private static final long TARGET_PRESENT = -1L;

    /** Follow this player while he is online in the dimension of the journey. */
    @Nullable
    public UUID targetPlayer;
    /**
     * Go to this companion, wherever he is right now - in the world, on a simulated journey or in an unloaded chunk.
     * His place comes from the companion register ({@link RecruitsCompanionManager}).
     */
    @Nullable
    public UUID targetCompanion;
    /** The target player or companion was given up; the plan goes on with its points. */
    public boolean targetLost;
    private long targetGoneSince = TARGET_PRESENT;
    /** Way points, walked in order. With a target player: where to go when he is not there. */
    public final List<BlockPos> points = new ArrayList<>();
    public int index;
    /** Blocks per second, measured while the group really walked. 0 = not measured yet. */
    public double speed;
    /** A journey by ship: the group is put back on water. */
    public boolean water;

    /** A patrol: the points are walked again and again, see {@link #advanceRoute()}. */
    public boolean route;
    /** Route: from the last point on to the first one again. Otherwise the route is walked there and back. */
    public boolean cycle;
    /** Route that is walked there and back: on the way back. */
    public boolean returning;
    /** Route: seconds to wait at each point, parallel to {@link #points}. */
    public final List<Integer> waits = new ArrayList<>();
    /** Route: standing at the current point until {@link #waitUntil} (game time), then on to the next one. */
    public boolean waiting;
    public long waitUntil;
    /** Route by ship: the way over water from way point to way point. Null: not found yet, the straight line it is. */
    @Nullable
    public SeaRoute sea;
    /** Where on the sea way the ship is: the leg (and its direction) and the next corner of it. Found again after loading. */
    private transient int seaLeg = Integer.MIN_VALUE;
    private transient int seaCorner;

    public static TravelPlan toPlayer(UUID player, @Nullable BlockPos fallback) {
        TravelPlan plan = new TravelPlan();
        plan.targetPlayer = player;
        if (fallback != null) plan.points.add(fallback.immutable());
        return plan;
    }

    public static TravelPlan toCompanion(UUID companion, @Nullable BlockPos fallback) {
        TravelPlan plan = new TravelPlan();
        plan.targetCompanion = companion;
        if (fallback != null) plan.points.add(fallback.immutable());
        return plan;
    }

    public static TravelPlan toPoint(BlockPos point) {
        TravelPlan plan = new TravelPlan();
        plan.points.add(point.immutable());
        return plan;
    }

    /**
     * Where to go right now.
     * @return null if there is nowhere to go: the target player is gone for a moment (see
     * {@link #isWaitingForTarget()}), or nothing is left of the plan
     */
    @Nullable
    public Vec3 resolve(MinecraftServer server, ResourceKey<Level> dimension, long gameTime) {
        if (targetPlayer != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(targetPlayer);
            if (player != null && player.isAlive() && player.level().dimension().equals(dimension)) {
                targetGoneSince = TARGET_PRESENT;
                return player.position();
            }
            if (targetGoneSince == TARGET_PRESENT) targetGoneSince = gameTime;
            if (gameTime - targetGoneSince < TARGET_GRACE) return null;
            targetPlayer = null;
            targetLost = true;
        }
        if (targetCompanion != null) {
            Vec3 companion = companionPosition(server, dimension);
            if (companion != null) return companion;
            // dead, dismissed, given away or in another dimension: there is nobody to go to
            targetCompanion = null;
            targetLost = true;
        }
        BlockPos point = currentPoint();
        return point == null ? null : Vec3.atCenterOf(point);
    }

    /**
     * By ship along a route: the next corner of the sea way to the current way point.
     *
     * @param destination the current way point, see {@link #resolve}
     * @return the destination itself once the sea way is sailed (or there is none), null if no water leads on
     */
    @Nullable
    public Vec3 seaStop(double x, double z, Vec3 destination) {
        if (sea == null || !route || !water) return destination;
        // the leg she is on: from the way point before the current one, on the way back from the one after it
        boolean back = !cycle && returning;
        int leg = cycle ? Math.floorMod(index - 1, Math.max(1, points.size())) : back ? index : index - 1;
        if (leg < 0 || leg >= sea.legs()) return destination;
        int[] way = sea.leg(leg);
        int corners = way.length / 2;
        int key = back ? -leg - 1 : leg;
        if (seaLeg != key) {
            seaLeg = key;
            seaCorner = nearestCorner(way, back, x, z);
        }
        for (; seaCorner < corners; seaCorner++) {
            int at = (back ? corners - 1 - seaCorner : seaCorner) * 2;
            double cx = way[at] + 0.5D, cz = way[at + 1] + 0.5D;
            if (Math.abs(cx - x) > 1.0D || Math.abs(cz - z) > 1.0D) return new Vec3(cx, destination.y, cz);
        }
        // a way that ends short of the way point: she lies where the water ends
        return sea.isPartial(leg) && !back ? null : destination;
    }

    /** @return the corner to make for from here: the end of the stretch of the way she is nearest to */
    private static int nearestCorner(int[] way, boolean back, double x, double z) {
        int corners = way.length / 2, best = 0;
        double bestSqr = Double.MAX_VALUE;
        for (int i = 0; i + 1 < corners; i++) {
            int a = (back ? corners - 1 - i : i) * 2, b = (back ? corners - 2 - i : i + 1) * 2;
            double dx = way[b] - way[a], dz = way[b + 1] - way[a + 1], lengthSqr = dx * dx + dz * dz;
            double t = lengthSqr < 1.0E-6D ? 0.0D : ((x - way[a]) * dx + (z - way[a + 1]) * dz) / lengthSqr;
            double clamped = Math.max(0.0D, Math.min(1.0D, t));
            double px = way[a] + dx * clamped - x, pz = way[a + 1] + dz * clamped - z;
            double sqr = px * px + pz * pz;
            if (sqr >= bestSqr) continue;
            bestSqr = sqr;
            best = t <= 0.0D ? i : i + 1; // before the stretch: to its start first
        }
        return best;
    }

    /** Where the target companion is: he himself if he is in the world, otherwise what the register knows. */
    @Nullable
    private Vec3 companionPosition(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(targetCompanion);
        if (entity != null && entity.isAlive()) return entity.position();

        RecruitsCompanionManager companions = RecruitEvents.recruitsCompanionManager;
        CompanionRecord record = companions == null ? null : companions.get(targetCompanion);
        if (record == null || !record.dimension.equals(dimension.location())) return null;
        return Vec3.atCenterOf(record.pos);
    }

    public boolean followsPlayer() {
        return targetPlayer != null;
    }

    /** The journey ends where somebody is, a player or a companion, not at a fixed point. */
    public boolean followsTarget() {
        return targetPlayer != null || targetCompanion != null;
    }

    /** The target player is gone, but not yet given up: the group waits where it is. */
    public boolean isWaitingForTarget() {
        return targetPlayer != null && targetGoneSince != TARGET_PRESENT;
    }

    @Nullable
    public BlockPos currentPoint() {
        return index >= 0 && index < points.size() ? points.get(index) : null;
    }

    public int currentWaitSeconds() {
        return index >= 0 && index < waits.size() ? Math.max(0, waits.get(index)) : 0;
    }

    /** Route: the next point after the wait. Mirrors {@code AbstractLeaderEntity#updateWaypointIndex()}. */
    public void advanceRoute() {
        if (points.isEmpty()) return;
        boolean last = index >= points.size() - 1;
        if (cycle) {
            index = last ? 0 : index + 1;
        } else if (returning) {
            if (index <= 0) returning = false;
            else index--;
        } else {
            if (last) returning = true;
            else index++;
        }
    }

    /** @return true if there was a further point to go on to */
    public boolean nextPoint() {
        if (index + 1 >= points.size()) return false;
        index++;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (targetPlayer != null) tag.putUUID("TargetPlayer", targetPlayer);
        if (targetCompanion != null) tag.putUUID("TargetCompanion", targetCompanion);
        tag.putBoolean("TargetLost", targetLost);
        tag.putLong("TargetGoneSince", targetGoneSince);
        long[] packed = new long[points.size()];
        for (int i = 0; i < packed.length; i++) packed[i] = points.get(i).asLong();
        tag.putLongArray("Points", packed);
        tag.putInt("Index", index);
        tag.putDouble("Speed", speed);
        tag.putBoolean("Water", water);
        if (route) {
            tag.putBoolean("Route", true);
            tag.putBoolean("Cycle", cycle);
            tag.putBoolean("Returning", returning);
            tag.putIntArray("Waits", waits);
            tag.putBoolean("Waiting", waiting);
            tag.putLong("WaitUntil", waitUntil);
            if (sea != null) tag.put("Sea", sea.save());
        }
        return tag;
    }

    public static TravelPlan load(CompoundTag tag) {
        TravelPlan plan = new TravelPlan();
        if (tag.hasUUID("TargetPlayer")) plan.targetPlayer = tag.getUUID("TargetPlayer");
        if (tag.hasUUID("TargetCompanion")) plan.targetCompanion = tag.getUUID("TargetCompanion");
        plan.targetLost = tag.getBoolean("TargetLost");
        plan.targetGoneSince = tag.contains("TargetGoneSince") ? tag.getLong("TargetGoneSince") : TARGET_PRESENT;
        for (long packed : tag.getLongArray("Points")) plan.points.add(BlockPos.of(packed));
        plan.index = tag.getInt("Index");
        plan.speed = tag.getDouble("Speed");
        plan.water = tag.getBoolean("Water");
        plan.route = tag.getBoolean("Route");
        plan.cycle = tag.getBoolean("Cycle");
        plan.returning = tag.getBoolean("Returning");
        for (int wait : tag.getIntArray("Waits")) plan.waits.add(wait);
        plan.waiting = tag.getBoolean("Waiting");
        plan.waitUntil = tag.getLong("WaitUntil");
        if (tag.contains("Sea", 10)) plan.sea = SeaRoute.load(tag.getCompound("Sea"));
        return plan;
    }
}
