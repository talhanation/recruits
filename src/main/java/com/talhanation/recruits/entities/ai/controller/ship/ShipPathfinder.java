package com.talhanation.recruits.entities.ai.controller.ship;

import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

/**
 * The way over the water: A* in 2x2 block cells, and every question about
 * water the captain has (is there room here, is it free ahead).
 *
 * <b>Columns.</b> One cache per level, shared by all captains and cleared
 * every minute: per block column the depth under the surface and the room
 * above it (bridges, overhangs - the masts). The room above is only measured
 * where the heightmap shows anything above the water at all.
 *
 * <b>Cells.</b> A cell fits her hull if a disc of half her beam around its
 * centre touches only deep enough water. How far around a cell everything
 * fits gives its class: TIGHT (the hull just fits), CLEAR (a block to spare),
 * OPEN (room to swing round), WIDE (OPEN and six blocks more). ESCAPE is water
 * under the keel close to where she lies, so a ship pressed against the shore
 * can get off it. A step costs more the worse the class, so the way keeps off
 * the coast where the water allows it.
 *
 * <b>Other ships</b> are land for one search: their hull plus a block, and a
 * moving one also the water it makes in the next three seconds, once more to
 * its starboard side - two ships meeting head on both keep to starboard.
 *
 * Synchronous and bounded: MAX_NODES expansions, a search that stops getting
 * closer gives up, goals beyond HORIZON are planned in stages, an unreachable
 * goal gives the way to the nearest cell that can be reached.
 */
public final class ShipPathfinder {
    public static final int MAX_DEPTH = 7, MAX_HEADROOM = 31;
    public static final double HORIZON = 128.0D;
    static final int CELL = 2;
    private static final int MAX_NODES = 3000, STAGNATION = 1500, ESCAPE_RADIUS = 4, WIDE_EXTRA = 3;
    private static final double HEURISTIC_WEIGHT = 1.3D, DIAGONAL = Math.sqrt(2.0D);
    /** a way that cannot reach the goal ends in CLEAR rather than TIGHT water if that is at most ~4 cells further off */
    private static final double PARTIAL_SLACK = 4.0D * HEURISTIC_WEIGHT;
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public static final byte BLOCKED = 0, ESCAPE = 1, TIGHT = 2, CLEAR = 3, OPEN = 4, WIDE = 5;
    /** extra cost of a step into a cell, by class */
    private static final double[] EXTRA_COST = {0.0D, 8.0D, 3.0D, 1.0D, 0.3D, 0.0D};

    // ---- other ships ----
    /** blocks around her that other ships are looked for */
    private static final double SCAN = 64.0D;
    /** ticks of way a moving ship is taken to make */
    private static final int PREDICT = 60;
    /** blocks: the longest hull there is - a ship this close to her goal is the one she follows */
    static final double LONGEST = 16.0D;

    /** What came out of a search. */
    public record Result(List<double[]> waypoints, boolean reachedGoal) {}

    /** Other ships as stretches of water: segment a-b with a radius, {ax, az, bx, bz, r}. */
    public record Obstacles(List<double[]> capsules, List<Entity> ships) {
        static final Obstacles NONE = new Obstacles(List.of(), List.of());

        boolean covers(double x, double z) {
            for (double[] c : this.capsules) {
                if (capsuleDistanceSqr(c, x, z) <= c[4] * c[4]) return true;
            }
            return false;
        }

        /** @return true if the stretch from-to, a hull of halfBeam wide, comes into another ship's water */
        public boolean crosses(double fromX, double fromZ, double toX, double toZ, double halfBeam) {
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(toX - fromX, toZ - fromZ)));
            for (double[] c : this.capsules) {
                double reach = c[4] + halfBeam;
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    if (capsuleDistanceSqr(c, fromX + (toX - fromX) * t, fromZ + (toZ - fromZ) * t) <= reach * reach) return true;
                }
            }
            return false;
        }
    }

    @FunctionalInterface
    private interface Columns {
        boolean fits(int x, int z);
    }

    private final Columns columns;
    private final double tight;
    private final int clearRing, openRing, wideRing;
    private final int originX, originZ;
    /** per finder: 1 = the hull fits centred in the cell, 2 = not */
    private final Long2ByteOpenHashMap fits = new Long2ByteOpenHashMap();
    /** per finder: class + 1 */
    private final Long2ByteOpenHashMap classes = new Long2ByteOpenHashMap();

    private ShipPathfinder(Columns columns, ShipProfile profile, double originX, double originZ) {
        this.columns = columns;
        this.tight = profile.tightRadius();
        this.clearRing = Math.max(1, (int) Math.ceil((profile.clearRadius() - this.tight) / CELL));
        this.openRing = Math.max(this.clearRing, (int) Math.ceil((profile.swingRadius() - this.tight) / CELL));
        this.wideRing = this.openRing + WIDE_EXTRA;
        this.originX = cellOf(originX);
        this.originZ = cellOf(originZ);
    }

    /** A finder for her from where she lies; other ships as obstacles, or none. */
    public static ShipPathfinder of(Entity ship, ShipProfile profile, Obstacles obstacles) {
        Columns water = waterFor(ship, profile);
        Columns columns = obstacles.capsules().isEmpty() ? water
                : (x, z) -> !obstacles.covers(x + 0.5D, z + 0.5D) && water.fits(x, z);
        return new ShipPathfinder(columns, profile, ship.getX(), ship.getZ());
    }

    /* ---------------- questions about water ---------------- */

    /** @return true if water deep enough for her, with room for the masts, reaches this far around the point */
    public static boolean isOpenWater(Entity ship, ShipProfile profile, double x, double z, double radius) {
        return discFits(waterFor(ship, profile), x, z, radius);
    }

    /** @return true if every column the hull passes over going straight this far is deep, open water */
    public static boolean isWaterAhead(Entity ship, ShipProfile profile, Vec3 heading, double look) {
        Columns water = waterFor(ship, profile);
        double radius = profile.beam / 2.0D + 0.5D, reach = look + profile.length * 0.5D;
        for (double d = 1.0D; d <= reach; d += 1.0D) {
            if (!discFits(water, ship.getX() + heading.x * d, ship.getZ() + heading.z * d, radius)) return false;
        }
        return true;
    }

    /** @return true if the straight way is wide water all along - no search needed */
    public boolean isWide(double fromX, double fromZ, double toX, double toZ) {
        return this.isClear(fromX, fromZ, toX, toZ, WIDE);
    }

    /* ---------------- other ships ---------------- */

    /** @return the ships around her as obstacles - her own, the one she follows and one she lies against left out */
    public static Obstacles ships(Entity ship, double goalX, double goalZ) {
        List<Boat> found = ship.level().getEntitiesOfClass(Boat.class, ship.getBoundingBox().inflate(SCAN, 8.0D, SCAN),
                other -> other != ship && other.isAlive() && SmallShipsBridge.isShip(other));
        if (found.isEmpty()) return Obstacles.NONE;
        List<double[]> capsules = new ArrayList<>();
        List<Entity> ships = new ArrayList<>();
        for (Boat other : found) {
            CompoundTag profile = SmallShipsBridge.getProfile(other);
            double length = Math.max(2.0D, profile.getFloat("Length")), beam = Math.max(1.0D, profile.getFloat("Beam"));
            if (Math.hypot(goalX - other.getX(), goalZ - other.getZ()) < length) continue; // her goal lies on it
            float speed = Math.max(0.0F, SmallShipsBridge.getSpeed(other));
            Vec3 fwd = CaptainShipController.forward(other.getYRot());
            double r = beam / 2.0D + 1.0D, ahead = length / 2.0D + speed * PREDICT;
            double bx = other.getX() + fwd.x * ahead, bz = other.getZ() + fwd.z * ahead;
            List<double[]> own = new ArrayList<>(2);
            own.add(new double[]{other.getX() - fwd.x * length / 2.0D, other.getZ() - fwd.z * length / 2.0D, bx, bz, r});
            if (speed > 0.05F) {
                Vec3 sb = CaptainShipController.forward(other.getYRot() + 90.0F).scale(beam);
                own.add(new double[]{other.getX() + sb.x, other.getZ() + sb.z, bx + sb.x, bz + sb.z, r});
            }
            boolean touching = false;
            for (double[] c : own) touching |= capsuleDistanceSqr(c, ship.getX(), ship.getZ()) <= c[4] * c[4];
            if (touching) continue; // she has to be able to plan away from it
            capsules.addAll(own);
            ships.add(other);
        }
        return capsules.isEmpty() ? Obstacles.NONE : new Obstacles(capsules, ships);
    }

    private static double capsuleDistanceSqr(double[] c, double x, double z) {
        double dx = c[2] - c[0], dz = c[3] - c[1], lengthSqr = dx * dx + dz * dz;
        double t = lengthSqr < 1.0E-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D, ((x - c[0]) * dx + (z - c[1]) * dz) / lengthSqr));
        double px = c[0] + dx * t - x, pz = c[1] + dz * t - z;
        return px * px + pz * pz;
    }

    /* ---------------- search ---------------- */

    /** Plans from the ship's position to the goal - or towards it, see the class comment. */
    public Result plan(double fromX, double fromZ, double goalX, double goalZ) {
        double dx = goalX - fromX, dz = goalZ - fromZ, distance = Math.hypot(dx, dz);
        boolean staged = distance > HORIZON;
        if (staged) {
            goalX = fromX + dx / distance * HORIZON;
            goalZ = fromZ + dz / distance * HORIZON;
        }
        int gx = cellOf(goalX), gz = cellOf(goalZ);
        Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        Node start = new Node(cellOf(fromX), cellOf(fromZ), null, 0.0D, 0.0D);
        start.h = heuristic(start.x, start.z, gx, gz);
        nodes.put(key(start.x, start.z), start);
        open.add(start);

        // where a way that cannot reach the goal ends: the cell nearest to it she can lie in,
        // with a block to spare if that is not much further off
        Node closest = start, bestTight = null, bestClear = null;
        int expanded = 0, sinceCloser = 0;
        boolean reached = false;
        while (!open.isEmpty() && expanded < MAX_NODES && sinceCloser < STAGNATION) {
            Node node = open.poll();
            if (node.closed) continue;
            node.closed = true;
            expanded++;
            if (node.h < closest.h) {
                closest = node;
                sinceCloser = 0;
            } else {
                sinceCloser++;
            }
            if (node.x == gx && node.z == gz) {
                bestTight = node;
                bestClear = null;
                reached = true;
                break;
            }
            byte here = this.classOf(node.x, node.z);
            if (node != start && here >= TIGHT && (bestTight == null || node.h < bestTight.h)) bestTight = node;
            if (node != start && here >= CLEAR && (bestClear == null || node.h < bestClear.h)) bestClear = node;

            for (int[] step : NEIGHBOURS) {
                int nx = node.x + step[0], nz = node.z + step[1];
                byte cls = this.classOf(nx, nz);
                if (cls == BLOCKED) continue;
                boolean diagonal = step[0] != 0 && step[1] != 0;
                // no squeezing diagonally between two blocked cells
                if (diagonal && this.classOf(node.x + step[0], node.z) == BLOCKED && this.classOf(node.x, node.z + step[1]) == BLOCKED) continue;
                double g = node.g + (diagonal ? DIAGONAL : 1.0D) * (1.0D + EXTRA_COST[cls]);
                long key = key(nx, nz);
                Node next = nodes.get(key);
                if (next == null || (!next.closed && g < next.g)) {
                    if (next != null) next.closed = true; // the stale entry is skipped later
                    Node better = new Node(nx, nz, node, g, heuristic(nx, nz, gx, gz));
                    nodes.put(key, better);
                    open.add(better);
                }
            }
        }
        Node best = bestTight;
        if (bestClear != null && (best == null || bestClear.h <= best.h + PARTIAL_SLACK)) best = bestClear;
        boolean reachedGoal = reached && !staged; // a staged goal is only a point on the way
        return new Result(best == null ? List.of() : this.smooth(best, fromX, fromZ), reachedGoal);
    }

    /** Pulls the cell chain tight: from each corner on to the furthest cell in sight - through water at least as wide as the chain it cuts off. */
    private List<double[]> smooth(Node end, double fromX, double fromZ) {
        List<Node> chain = new ArrayList<>();
        for (Node node = end; node != null; node = node.parent) chain.add(node);
        Collections.reverse(chain);
        List<double[]> corners = new ArrayList<>();
        byte[] narrowest = new byte[chain.size()];
        double cx = fromX, cz = fromZ;
        int at = 0;
        while (at < chain.size() - 1) {
            byte running = WIDE;
            for (int j = at + 1; j < chain.size(); j++) {
                running = (byte) Math.min(running, this.classOf(chain.get(j).x, chain.get(j).z));
                narrowest[j] = running;
            }
            int furthest = at + 1;
            for (int j = chain.size() - 1; j > at + 1; j--) {
                if (this.isClear(cx, cz, centre(chain.get(j).x), centre(chain.get(j).z), narrowest[j])) {
                    furthest = j;
                    break;
                }
            }
            cx = centre(chain.get(furthest).x);
            cz = centre(chain.get(furthest).z);
            corners.add(new double[]{cx, cz});
            at = furthest;
        }
        return corners;
    }

    /** @return true if every cell on the line is at least of this class and the hull fits at every point of it - the first cell left out */
    private boolean isClear(double fromX, double fromZ, double toX, double toZ, byte minClass) {
        int sx = cellOf(fromX), sz = cellOf(fromZ);
        double dx = toX - fromX, dz = toZ - fromZ;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dz)));
        for (int i = 1; i <= steps; i++) {
            double px = fromX + dx * i / steps, pz = fromZ + dz * i / steps;
            int x = cellOf(px), z = cellOf(pz);
            if (x == sx && z == sz) continue;
            byte cls = this.classOf(x, z);
            if (cls < minClass) return false;
            // only in TIGHT water is the point itself worth a look - CLEAR fits anywhere in the cell
            if (minClass >= TIGHT && cls < CLEAR && !discFits(this.columns, px, pz, this.tight)) return false;
        }
        return true;
    }

    /* ---------------- cells ---------------- */

    private byte classOf(int cx, int cz) {
        long key = key(cx, cz);
        byte known = this.classes.get(key);
        if (known != 0) return (byte) (known - 1);
        byte cls = this.measureClass(cx, cz);
        this.classes.put(key, (byte) (cls + 1));
        return cls;
    }

    /** The class from how many rings of cells around this one fit the hull too. */
    private byte measureClass(int cx, int cz) {
        if (!this.fitsTight(cx, cz)) {
            int ox = cx - this.originX, oz = cz - this.originZ;
            boolean nearOrigin = ox * ox + oz * oz <= ESCAPE_RADIUS * ESCAPE_RADIUS;
            return nearOrigin && discFits(this.columns, centre(cx), centre(cz), 0.5D) ? ESCAPE : BLOCKED;
        }
        int ring = 0;
        while (ring < this.wideRing && this.annulusFits(cx, cz, ring + 1)) ring++;
        if (ring < this.clearRing) return TIGHT;
        if (ring < this.openRing) return CLEAR;
        if (ring < this.wideRing) return OPEN;
        return WIDE;
    }

    /** @return true if every cell in the ring k cells around this one fits the hull - the cells inside are known to fit */
    private boolean annulusFits(int cx, int cz, int k) {
        int inner = (k - 1) * (k - 1) + (k - 1), outer = k * k + k;
        for (int i = -k; i <= k; i++) {
            for (int j = -k; j <= k; j++) {
                int d = i * i + j * j;
                if (d <= inner || d > outer) continue;
                if (!this.fitsTight(cx + i, cz + j)) return false;
            }
        }
        return true;
    }

    private boolean fitsTight(int cx, int cz) {
        long key = key(cx, cz);
        byte known = this.fits.get(key);
        if (known != 0) return known == 1;
        boolean free = discFits(this.columns, centre(cx), centre(cz), this.tight);
        this.fits.put(key, free ? (byte) 1 : (byte) 2);
        return free;
    }

    /** @return true if a disc of this radius around the point touches only columns that carry the hull */
    private static boolean discFits(Columns columns, double x, double z, double radius) {
        double radiusSqr = radius * radius;
        int minX = (int) Math.floor(x - radius), maxX = (int) Math.floor(x + radius);
        int minZ = (int) Math.floor(z - radius), maxZ = (int) Math.floor(z + radius);
        for (int bx = minX; bx <= maxX; bx++) {
            double nx = Math.max(bx, Math.min(x, bx + 1.0D)) - x;
            for (int bz = minZ; bz <= maxZ; bz++) {
                double nz = Math.max(bz, Math.min(z, bz + 1.0D)) - z;
                if (nx * nx + nz * nz < radiusSqr && !columns.fits(bx, bz)) return false;
            }
        }
        return true;
    }

    private static double heuristic(int x, int z, int gx, int gz) {
        int dx = Math.abs(gx - x), dz = Math.abs(gz - z);
        return (Math.max(dx, dz) + (DIAGONAL - 1.0D) * Math.min(dx, dz)) * HEURISTIC_WEIGHT; // octile
    }

    static int cellOf(double coordinate) {
        return (int) Math.floor(coordinate / CELL);
    }

    static double centre(int cell) {
        return cell * CELL + CELL / 2.0D;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static final class Node implements Comparable<Node> {
        final int x, z;
        @Nullable final Node parent;
        final double g;
        double h;
        boolean closed;

        Node(int x, int z, @Nullable Node parent, double g, double h) {
            this.x = x;
            this.z = z;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.g + this.h, other.g + other.h);
        }
    }

    /* ---------------- water columns ---------------- */

    /** @return the column check for her hull at her surface: depth under the keel, room for the masts */
    private static Columns waterFor(Entity ship, ShipProfile profile) {
        Level level = ship.level();
        WaterCache cache = WaterCache.of(level);
        int surfaceY = surfaceY(level, ship.getX(), ship.getY(), ship.getZ());
        int depth = profile.requiredDepth, headroom = Math.min(MAX_HEADROOM, (int) Math.ceil(profile.airDraft));
        return (x, z) -> {
            int column = cache.get(x, surfaceY, z);
            return column >= 0 && (column & 7) >= depth && (column >> 3) >= headroom;
        };
    }

    /** @return the y of the water surface block she floats on - near her y, or her own block y if none is found */
    public static int surfaceY(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), top = (int) Math.floor(y) + 1;
        for (int by = top; by >= top - 4; by--) {
            if (!level.getFluidState(pos.set(bx, by, bz)).is(FluidTags.WATER)) continue;
            if (!level.getFluidState(pos.set(bx, by + 1, bz)).is(FluidTags.WATER)) return by;
        }
        return (int) Math.floor(y);
    }

    /** depth | headroom << 3 per column, -1 for an unloaded chunk (never loaded for this). Server thread only. */
    private static final class WaterCache {
        private static final Map<Level, WaterCache> BY_LEVEL = new WeakHashMap<>();
        private static final long CLEAR_INTERVAL = 1200L;
        private final Level level;
        private final Long2IntOpenHashMap columns = new Long2IntOpenHashMap();
        private long clearedAt;

        private WaterCache(Level level) {
            this.level = level;
            this.columns.defaultReturnValue(-1);
            this.clearedAt = level.getGameTime();
        }

        static WaterCache of(Level level) {
            WaterCache cache = BY_LEVEL.computeIfAbsent(level, WaterCache::new);
            long now = level.getGameTime();
            if (now - cache.clearedAt > CLEAR_INTERVAL || now < cache.clearedAt) {
                cache.columns.clear();
                cache.clearedAt = now;
            }
            return cache;
        }

        int get(int x, int surfaceY, int z) {
            long key = BlockPos.asLong(x, surfaceY, z);
            int cached = this.columns.get(key);
            if (cached >= 0) return cached;
            if (!this.level.hasChunk(x >> 4, z >> 4)) return -1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, surfaceY, z);
            int depth = 0;
            while (depth < MAX_DEPTH && this.passable(pos, true)) {
                depth++;
                pos.move(0, -1, 0);
            }
            int headroom = MAX_HEADROOM;
            // the heightmap counts water too: anything higher than the surface stands above it
            if (depth > 0 && this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > surfaceY + 1) {
                headroom = 0;
                pos.set(x, surfaceY + 1, z);
                while (headroom < MAX_HEADROOM && this.passable(pos, false)) {
                    headroom++;
                    pos.move(0, 1, 0);
                }
            }
            int value = depth == 0 ? 0 : depth | headroom << 3;
            this.columns.put(key, value);
            return value;
        }

        /** water a hull passes through (kelp and seagrass yes, a waterlogged fence no) - or free air above it */
        private boolean passable(BlockPos pos, boolean water) {
            BlockState state = this.level.getBlockState(pos);
            return (!water || state.getFluidState().is(FluidTags.WATER)) && state.getCollisionShape(this.level, pos).isEmpty();
        }
    }
}
