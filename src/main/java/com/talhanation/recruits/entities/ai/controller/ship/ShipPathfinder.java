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
 * <b>Other ships</b> are land for one search: their hull plus half a block,
 * where it lies when she plans - a ship under way is the traffic watch's
 * business. A hull never drops out, not even the one she follows: she keeps
 * astern of it. The way keeps to the right, see keepRight.
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
    /** blocks kept free around another hull */
    private static final double MARGIN = 0.5D;
    /** blocks between two points of a lane; how far across the water is looked for the banks */
    private static final double LANE_STEP = 16.0D, LANE_LOOK = 24.0D;

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

    /**
     * @return the hulls of the ships around her as obstacles - half a block
     * round each, less where she lies closer, so she can still plan away from it
     */
    public static Obstacles ships(Entity ship) {
        List<Boat> found = ship.level().getEntitiesOfClass(Boat.class, ship.getBoundingBox().inflate(SCAN, 8.0D, SCAN),
                other -> other != ship && other.isAlive() && SmallShipsBridge.isShip(other));
        if (found.isEmpty()) return Obstacles.NONE;
        List<double[]> capsules = new ArrayList<>(found.size());
        List<Entity> ships = new ArrayList<>(found.size());
        for (Boat other : found) {
            CompoundTag profile = SmallShipsBridge.getProfile(other);
            double length = Math.max(2.0D, profile.getFloat("Length")), beam = Math.max(1.0D, profile.getFloat("Beam"));
            Vec3 fwd = CaptainShipController.forward(other.getYRot());
            double[] hull = {other.getX() - fwd.x * length / 2.0D, other.getZ() - fwd.z * length / 2.0D,
                    other.getX() + fwd.x * length / 2.0D, other.getZ() + fwd.z * length / 2.0D, beam / 2.0D + MARGIN};
            double inside = Math.sqrt(capsuleDistanceSqr(hull, ship.getX(), ship.getZ()));
            if (inside < hull[4]) hull[4] = Math.max(0.0D, inside - 1.0D);
            capsules.add(hull);
            ships.add(other);
        }
        return new Obstacles(capsules, ships);
    }

    /**
     * Where two hulls going straight on first come closer than half a block -
     * each a segment along its heading, the beams round it.
     *
     * @return {ticks until then, her own speed towards the other then, the other's speed away from her then},
     * null if not within horizon ticks or if they touch already
     */
    @Nullable
    public static double[] contact(double x, double z, float yaw, float speed, double length, double beam,
                                   double ox, double oz, float oyaw, float ospeed, double olength, double obeam, int horizon) {
        Vec3 u = CaptainShipController.forward(yaw), w = CaptainShipController.forward(oyaw);
        // half the margin the way keeps: a way planned past it does not count as an approach
        double clearance = beam / 2.0D + obeam / 2.0D + MARGIN / 2.0D;
        for (int t = 0; t <= horizon; t += 2) {
            double px = x + u.x * speed * t, pz = z + u.z * speed * t;
            double qx = ox + w.x * ospeed * t, qz = oz + w.z * ospeed * t;
            double gap = segmentDistance(px, pz, u.x, u.z, length / 2.0D, qx, qz, w.x, w.z, olength / 2.0D);
            if (t == 0 && gap < clearance - MARGIN / 2.0D) return null; // already touching: that is not an approach any more
            if (gap < clearance) return approach(px, pz, u, speed, qx, qz, w, ospeed, Math.max(1, t));
        }
        return null;
    }

    private static double[] approach(double px, double pz, Vec3 u, double speed, double qx, double qz, Vec3 w, double ospeed, int ticks) {
        double nx = qx - px, nz = qz - pz, n = Math.max(1.0E-6D, Math.hypot(nx, nz));
        nx /= n;
        nz /= n;
        return new double[]{ticks, (u.x * nx + u.z * nz) * speed, (w.x * nx + w.z * nz) * ospeed};
    }

    /** @return the shortest distance between two centred segments a +- da * ha and b +- db * hb (d unit vectors) */
    static double segmentDistance(double ax, double az, double dax, double daz, double ha,
                                  double bx, double bz, double dbx, double dbz, double hb) {
        // the closest pair of points on two segments, the textbook way with clamping
        double rx = ax - bx, rz = az - bz;
        double b = dax * dbx + daz * dbz, c = dax * rx + daz * rz, f = dbx * rx + dbz * rz;
        double denom = 1.0D - b * b, s, t;
        s = denom > 1.0E-9D ? clamp((b * f - c) / denom, -ha, ha) : 0.0D;
        t = clamp(b * s + f, -hb, hb);
        s = clamp(b * t - c, -ha, ha);
        double px = ax + dax * s - (bx + dbx * t), pz = az + daz * s - (bz + dbz * t);
        return Math.hypot(px, pz);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
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

    /* ---------------- keep to the right ---------------- */

    /**
     * The way moved onto the starboard lane: every LANE_STEP blocks and at
     * every corner the water is measured across the way, left and right as far
     * as her hull can go, and the point laid to the middle of it plus a
     * quarter of its width to starboard - at most lane blocks. Where the water
     * is narrower than two lanes the point stays where it is. Two ships on the
     * same way in opposite directions so pass each other port to port without
     * having to give way. Every moved stretch has to be clear for the hull,
     * else that point stays; if the way cannot be kept that way, it stays as it was.
     *
     * @param corners the way, its last point the end
     * @param lane    blocks to starboard of the middle at most
     * @param moveEnd false: the end stays where it is (a stop, a goal to lie at)
     */
    public List<double[]> keepRight(double fromX, double fromZ, List<double[]> corners, double lane, boolean moveEnd) {
        if (corners.isEmpty() || lane <= 0.0D) return corners;
        List<double[]> plain = new ArrayList<>(), moved = new ArrayList<>();
        double px = fromX, pz = fromZ;
        for (int i = 0; i < corners.size(); i++) {
            double[] corner = corners.get(i);
            double dx = corner[0] - px, dz = corner[1] - pz, length = Math.hypot(dx, dz);
            if (length < 1.0E-6D) continue;
            double ux = dx / length, uz = dz / length;
            for (double t = LANE_STEP; t <= length - LANE_STEP / 2.0D; t += LANE_STEP) {
                double sx = px + ux * t, sz = pz + uz * t;
                plain.add(new double[]{sx, sz});
                moved.add(this.onLane(sx, sz, ux, uz, lane));
            }
            boolean last = i == corners.size() - 1;
            double vx = ux, vz = uz;
            if (!last) { // across the mean of the leg in and the leg out
                double[] next = corners.get(i + 1);
                double ox = next[0] - corner[0], oz = next[1] - corner[1], out = Math.hypot(ox, oz);
                if (out > 1.0E-6D && Math.hypot(ux + ox / out, uz + oz / out) > 0.2D) {
                    vx = ux + ox / out;
                    vz = uz + oz / out;
                    double v = Math.hypot(vx, vz);
                    vx /= v;
                    vz /= v;
                }
            }
            plain.add(corner);
            moved.add(last && !moveEnd ? corner : this.onLane(corner[0], corner[1], vx, vz, lane));
            px = corner[0];
            pz = corner[1];
        }
        // every stretch clear for the hull - a point that is not stays where it was
        List<double[]> way = new ArrayList<>(moved.size());
        double lx = fromX, lz = fromZ;
        for (int i = 0; i < moved.size(); i++) {
            double[] point = moved.get(i);
            if (point != plain.get(i) && !this.isClear(lx, lz, point[0], point[1], TIGHT)) point = plain.get(i);
            if (!this.isClear(lx, lz, point[0], point[1], TIGHT)) return corners; // the old way, then
            way.add(point);
            lx = point[0];
            lz = point[1];
        }
        return way;
    }

    /** @return the point on the starboard lane across heading (ux, uz) - see keepRight */
    private double[] onLane(double x, double z, double ux, double uz, double lane) {
        double rx = -uz, rz = ux; // starboard of the heading
        double right = this.free(x, z, rx, rz), left = this.free(x, z, -rx, -rz), width = left + right;
        if (width < 2.0D * lane) return new double[]{x, z}; // room for one only
        double to = Math.min(right - 1.0D, (right - left) / 2.0D + Math.min(lane, width / 4.0D));
        if (to <= 0.0D && to > -0.5D) return new double[]{x, z};
        return new double[]{x + rx * to, z + rz * to};
    }

    /** @return blocks her hull can move from the point that way, up to LANE_LOOK */
    private double free(double x, double z, double dx, double dz) {
        for (double d = 1.0D; d <= LANE_LOOK; d += 1.0D) {
            if (!discFits(this.columns, x + dx * d, z + dz * d, this.tight)) return d - 1.0D;
        }
        return LANE_LOOK;
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
