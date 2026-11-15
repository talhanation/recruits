package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Finds the way over water along a whole patrol route, however far it reaches - without loading a single chunk.
 * <p>
 * <b>Water.</b> Along every stretch from way point to way point a strip of chunks is looked at: a chunk that is
 * loaded anyway is asked directly, of every other one only the height maps are read from its file, in the
 * background. A column is water if its surface lies at the level the ship floats on; the depth is the room down to
 * the sea floor. A chunk that was never generated is unknown and counts as land.
 * <p>
 * <b>Way.</b> A* in cells of 4 blocks over these columns. A cell carries the ship if a disc of her beam plus a block
 * to spare around its centre (three blocks at least) touches only deep enough water. No way in the strip: it is made wider, twice. Still no
 * way: the leg ends at the nearest water that can be reached and counts as partial.
 * <p>
 * What the height maps cannot show: a bridge reads as land, so does water under overhanging trees; water that is
 * not at the level of the ship (a river higher up) is land too. Ice and dams are seen once their chunk was saved.
 * <p>
 * One route at a time, the others wait: reading is spread out so the loading of chunks for players is not held
 * up. The search itself runs off the server thread. Everything else here belongs to the server thread.
 */
public final class SeaRoutePlanner {
    static final int CELL = 4;
    /** Chunks read in one go, then the server gets a turn. */
    private static final int BATCH = 96;
    /** Blocks to either side of the straight line that are looked at - wider only if no way is found. */
    private static final int[] WIDTHS = {96, 256, 512};
    /** More chunks than this are not read for one widening of a leg. */
    private static final int MAX_CHUNKS = 20000;
    /** Columns kept from leg to leg (they overlap at the way points) up to this many chunks. */
    private static final int KEEP_CHUNKS = 24000;
    /** A way point on land or in the shallows: the nearest cell that carries her within this many cells counts. */
    private static final int SNAP = 10;
    private static final int MAX_NODES = 250000;
    /** A corner is pulled straight over this many cells at most. */
    private static final int SMOOTH_LOOK = 64;
    /** Room to spare beyond a tight fit that makes open water, blocks. The way keeps to it where it can. */
    private static final double OPEN_EXTRA = 4.0D;
    /** The disc around a cell is never smaller than this: the discs of neighbouring cells have to overlap. */
    private static final double MIN_RADIUS = 3.0D;
    private static final double TIGHT_COST = 1.0D, HEURISTIC_WEIGHT = 1.2D, DIAGONAL = Math.sqrt(2.0D);
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final byte[] UNKNOWN = new byte[0];
    private static final byte BLOCKED = 1, TIGHT = 2, OPEN = 3;

    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    @Nullable
    private Job running;

    private static final class Job {
        final ServerLevel level;
        final UUID owner;
        final List<BlockPos> points;
        final boolean cycle;
        final long signature;
        final int depth, surface;
        final double radius;
        final Consumer<SeaRoute> done;
        /** per chunk 256 columns: depth of the water, 0 = none. An empty array: unknown. */
        final Long2ObjectOpenHashMap<byte[]> columns = new Long2ObjectOpenHashMap<>();
        final int[][] legs;
        final boolean[] partial;
        final long started = System.currentTimeMillis();
        int leg, stage, read;
        LongArrayList pending = new LongArrayList();
        int pendingAt;
        boolean cancelled;

        Job(ServerLevel level, UUID owner, List<BlockPos> points, boolean cycle, long signature, int depth, double radius,
            int surface, Consumer<SeaRoute> done) {
            this.level = level;
            this.owner = owner;
            this.points = points;
            this.cycle = cycle;
            this.signature = signature;
            this.depth = depth;
            this.radius = radius;
            this.surface = surface;
            this.done = done;
            int count = points.size() < 2 ? 0 : cycle ? points.size() : points.size() - 1;
            this.legs = new int[count][];
            this.partial = new boolean[count];
        }

        BlockPos from() {
            return points.get(leg);
        }

        BlockPos to() {
            return points.get((leg + 1) % points.size());
        }
    }

    private record Found(int[] way, boolean reached, boolean widen) {
    }

    /**
     * Finds the way along these way points. A request of the same owner that still waits is replaced.
     *
     * @param owner   who asks - one request each
     * @param depth   blocks of water the ship needs
     * @param radius  blocks either side of her keel line the hull takes, see {@code ShipProfile#tightRadius()}
     * @param surface the y just above the water she floats on
     * @param done    gets the result, on the server thread
     */
    public void request(ServerLevel level, UUID owner, List<BlockPos> points, boolean cycle, long signature, int depth,
                        double radius, int surface, Consumer<SeaRoute> done) {
        if (running != null && running.owner.equals(owner)) {
            if (running.signature == signature) return;
            running.cancelled = true;
        }
        for (Iterator<Job> it = queue.iterator(); it.hasNext(); ) {
            if (it.next().owner.equals(owner)) it.remove();
        }
        queue.add(new Job(level, owner, List.copyOf(points), cycle, signature, Math.max(1, Math.min(7, depth)),
                Math.max(MIN_RADIUS, radius + 1.0D), surface, done));
        next();
    }

    private void next() {
        if (running != null) return;
        running = queue.poll();
        if (running != null) startLeg(running);
    }

    private void finish(Job job) {
        if (running == job) running = null;
        if (!job.cancelled) {
            Main.LOGGER.debug("Recruits travel: sea route of {} legs found in {} ms, {} chunks read",
                    job.legs.length, System.currentTimeMillis() - job.started, job.read);
            try {
                job.done.accept(new SeaRoute(job.signature, job.legs, job.partial));
            } catch (RuntimeException e) {
                Main.LOGGER.error("Recruits travel: could not hand over a sea route", e);
            }
        }
        next();
    }

    // ---- Reading the water ----

    private void startLeg(Job job) {
        if (job.cancelled || job.leg >= job.legs.length) {
            finish(job);
            return;
        }
        if (job.columns.size() > KEEP_CHUNKS) job.columns.clear();
        job.stage = 0;
        if (!startStage(job)) { // a leg too long to look at: no way known
            job.legs[job.leg] = new int[0];
            job.partial[job.leg] = true;
            job.leg++;
            startLeg(job);
        }
    }

    /** @return false if this widening would read more chunks than allowed - nothing was started then */
    private boolean startStage(Job job) {
        LongArrayList chunks = strip(job, WIDTHS[job.stage]);
        if (chunks.size() > MAX_CHUNKS) return false;
        job.pending = chunks;
        job.pendingAt = 0;
        readBatch(job);
        return true;
    }

    /** The chunks within this many blocks of the straight line of the leg that are not known yet. */
    private static LongArrayList strip(Job job, int width) {
        BlockPos a = job.from(), b = job.to();
        double reach = width + 12.0D;
        int minX = (Math.min(a.getX(), b.getX()) - width) >> 4, maxX = (Math.max(a.getX(), b.getX()) + width) >> 4;
        int minZ = (Math.min(a.getZ(), b.getZ()) - width) >> 4, maxZ = (Math.max(a.getZ(), b.getZ()) + width) >> 4;
        LongArrayList chunks = new LongArrayList();
        // a strip of this size cannot be within the limit: do not even count it
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 64L * MAX_CHUNKS) {
            chunks.size(MAX_CHUNKS + 1);
            return chunks;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (distanceToSegment(x * 16 + 8, z * 16 + 8, a.getX(), a.getZ(), b.getX(), b.getZ()) > reach) continue;
                long key = ChunkPos.asLong(x, z);
                if (!job.columns.containsKey(key)) chunks.add(key);
            }
        }
        return chunks;
    }

    private static double distanceToSegment(double x, double z, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az, lengthSqr = dx * dx + dz * dz;
        double t = lengthSqr < 1.0E-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D, ((x - ax) * dx + (z - az) * dz) / lengthSqr));
        return Math.hypot(ax + dx * t - x, az + dz * t - z);
    }

    private void readBatch(Job job) {
        while (!job.cancelled && job.pendingAt < job.pending.size()) {
            int end = Math.min(job.pending.size(), job.pendingAt + BATCH);
            List<Long> keys = new ArrayList<>();
            List<CompletableFuture<byte[]>> reads = new ArrayList<>();
            int minY = job.level.getMinBuildHeight(), height = job.level.getHeight(), surface = job.surface;
            for (; job.pendingAt < end; job.pendingAt++) {
                long key = job.pending.getLong(job.pendingAt);
                int x = ChunkPos.getX(key), z = ChunkPos.getZ(key);
                LevelChunk loaded = job.level.getChunkSource().getChunkNow(x, z);
                if (loaded != null) {
                    job.columns.put(key, fromChunk(loaded, surface));
                    continue;
                }
                keys.add(key);
                reads.add(job.level.getChunkSource().chunkMap.read(new ChunkPos(x, z))
                        .thenApply(tag -> fromTag(tag.orElse(null), minY, height, surface))
                        .exceptionally(error -> UNKNOWN));
            }
            if (reads.isEmpty()) continue;
            job.read += reads.size();
            CompletableFuture.allOf(reads.toArray(new CompletableFuture[0])).whenCompleteAsync((nothing, error) -> {
                if (!job.level.getServer().isSameThread()) return; // the server stopped meanwhile
                for (int i = 0; i < keys.size(); i++) job.columns.put((long) keys.get(i), reads.get(i).getNow(UNKNOWN));
                readBatch(job);
            }, job.level.getServer());
            return;
        }
        if (job.cancelled) finish(job);
        else search(job);
    }

    private static byte[] fromChunk(LevelChunk chunk, int surface) {
        byte[] columns = new byte[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1 != surface) continue;
                int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) + 1;
                columns[z * 16 + x] = (byte) Mth.clamp(surface - floor, 0, 7);
            }
        }
        return columns;
    }

    /** The columns of a chunk from its file - only finished chunks, only the height maps are looked at. */
    private static byte[] fromTag(@Nullable CompoundTag tag, int minY, int height, int surface) {
        if (tag == null) return UNKNOWN;
        if (tag.contains("Level", Tag.TAG_COMPOUND)) { // saved before 1.18 and not loaded since
            tag = tag.getCompound("Level");
            minY = 0;
            height = 256;
        }
        if (!tag.getString("Status").endsWith("full")) return UNKNOWN;
        CompoundTag maps = tag.getCompound("Heightmaps");
        long[] top = maps.getLongArray("WORLD_SURFACE"), floor = maps.getLongArray("OCEAN_FLOOR");
        int bits = Mth.ceillog2(height + 1), perLong = 64 / bits;
        int needed = (256 + perLong - 1) / perLong;
        if (top.length != needed || floor.length != needed) return UNKNOWN;
        long mask = (1L << bits) - 1L;
        byte[] columns = new byte[256];
        for (int i = 0; i < 256; i++) {
            int shift = (i % perLong) * bits;
            if ((int) ((top[i / perLong] >> shift) & mask) + minY != surface) continue;
            int floorY = (int) ((floor[i / perLong] >> shift) & mask) + minY;
            columns[i] = (byte) Mth.clamp(surface - floorY, 0, 7);
        }
        return columns;
    }

    // ---- The way ----

    private void search(Job job) {
        BlockPos from = job.from(), to = job.to();
        Search search = new Search(job.columns, job.depth, job.radius);
        CompletableFuture.supplyAsync(() -> search.find(from.getX(), from.getZ(), to.getX(), to.getZ()), Util.backgroundExecutor())
                .whenCompleteAsync((found, error) -> {
                    if (!job.level.getServer().isSameThread()) return; // the server stopped meanwhile
                    if (error != null) Main.LOGGER.error("Recruits travel: sea route search failed", error);
                    if (job.cancelled) {
                        finish(job);
                        return;
                    }
                    boolean reached = found != null && found.reached();
                    if (!reached && found != null && found.widen() && job.stage + 1 < WIDTHS.length) {
                        job.stage++;
                        if (startStage(job)) return;
                    }
                    job.legs[job.leg] = found == null ? new int[0] : found.way();
                    job.partial[job.leg] = !reached;
                    job.leg++;
                    startLeg(job);
                }, job.level.getServer());
    }

    /** One search over the columns read so far. Used by one thread at a time. */
    private static final class Search {
        private final Long2ObjectOpenHashMap<byte[]> columns;
        private final int depth;
        private final double tight, open;
        private final Long2ByteOpenHashMap classes = new Long2ByteOpenHashMap();
        private long lastChunk = Long.MIN_VALUE;
        private byte[] lastColumns = UNKNOWN;

        Search(Long2ObjectOpenHashMap<byte[]> columns, int depth, double radius) {
            this.columns = columns;
            this.depth = depth;
            this.tight = radius;
            this.open = radius + OPEN_EXTRA;
        }

        Found find(int fromX, int fromZ, int toX, int toZ) {
            int[] start = snap(cellOf(fromX), cellOf(fromZ)), goal = snap(cellOf(toX), cellOf(toZ));
            if (start == null) return new Found(new int[0], false, false);
            boolean goalInWater = goal != null;
            int gx = goalInWater ? goal[0] : cellOf(toX), gz = goalInWater ? goal[1] : cellOf(toZ);

            Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
            PriorityQueue<Node> queue = new PriorityQueue<>();
            Node first = new Node(start[0], start[1], null, 0.0D, heuristic(start[0], start[1], gx, gz));
            nodes.put(key(first.x, first.z), first);
            queue.add(first);
            Node closest = first, end = null;
            int expanded = 0;
            while (!queue.isEmpty() && expanded < MAX_NODES) {
                Node node = queue.poll();
                if (node.closed) continue;
                node.closed = true;
                expanded++;
                if (node.h < closest.h) closest = node;
                if (node.x == gx && node.z == gz) {
                    end = node;
                    break;
                }
                for (int[] step : NEIGHBOURS) {
                    int nx = node.x + step[0], nz = node.z + step[1];
                    byte cls = classOf(nx, nz);
                    if (cls == BLOCKED) continue;
                    boolean diagonal = step[0] != 0 && step[1] != 0;
                    // no cutting a corner of land
                    if (diagonal && (classOf(node.x + step[0], node.z) == BLOCKED || classOf(node.x, node.z + step[1]) == BLOCKED)) continue;
                    double g = node.g + (diagonal ? DIAGONAL : 1.0D) * (cls == TIGHT ? 1.0D + TIGHT_COST : 1.0D);
                    long key = key(nx, nz);
                    Node known = nodes.get(key);
                    if (known != null && (known.closed || g >= known.g)) continue;
                    if (known != null) known.closed = true; // the stale entry is skipped later
                    Node better = new Node(nx, nz, node, g, heuristic(nx, nz, gx, gz));
                    nodes.put(key, better);
                    queue.add(better);
                }
            }
            boolean reached = end != null;
            // out of nodes is not a matter of the strip: more water would not help
            boolean widen = !reached && goalInWater && expanded < MAX_NODES;
            return new Found(smooth(reached ? end : closest), reached, widen);
        }

        /** Pulls the chain of cells tight: from each corner straight on to the furthest cell with only water between - first cell by cell over a limited stretch, then over the corners that are left. */
        private int[] smooth(Node end) {
            List<Node> chain = new ArrayList<>();
            for (Node node = end; node != null; node = node.parent) chain.add(node);
            Collections.reverse(chain);
            List<Node> corners = pull(chain, SMOOTH_LOOK);
            corners = pull(corners, corners.size()); // the few corners left: once more, as far as the water is clear
            int[] way = new int[corners.size() * 2];
            for (int i = 0; i < corners.size(); i++) {
                way[i * 2] = corners.get(i).x * CELL + CELL / 2;
                way[i * 2 + 1] = corners.get(i).z * CELL + CELL / 2;
            }
            return way;
        }

        private List<Node> pull(List<Node> chain, int look) {
            List<Node> corners = new ArrayList<>();
            corners.add(chain.get(0));
            int at = 0;
            while (at < chain.size() - 1) {
                int furthest = at + 1;
                for (int j = Math.min(chain.size() - 1, at + look); j > at + 1; j--) {
                    if (isClear(chain.get(at), chain.get(j))) {
                        furthest = j;
                        break;
                    }
                }
                corners.add(chain.get(furthest));
                at = furthest;
            }
            return corners;
        }

        private boolean isClear(Node a, Node b) {
            double ax = a.x + 0.5D, az = a.z + 0.5D, dx = b.x - a.x, dz = b.z - a.z;
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dz) * 2.0D));
            for (int i = 1; i < steps; i++) {
                double t = (double) i / steps;
                if (classOf((int) Math.floor(ax + dx * t), (int) Math.floor(az + dz * t)) == BLOCKED) return false;
            }
            return true;
        }

        /** @return the nearest cell that carries her, null if there is none close by */
        @Nullable
        private int[] snap(int cx, int cz) {
            for (int ring = 0; ring <= SNAP; ring++) {
                int[] best = null;
                int bestSqr = Integer.MAX_VALUE;
                for (int i = -ring; i <= ring; i++) {
                    for (int j = -ring; j <= ring; j++) {
                        if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
                        int sqr = i * i + j * j;
                        if (sqr >= bestSqr || classOf(cx + i, cz + j) == BLOCKED) continue;
                        best = new int[]{cx + i, cz + j};
                        bestSqr = sqr;
                    }
                }
                if (best != null) return best;
            }
            return null;
        }

        private byte classOf(int cx, int cz) {
            long key = key(cx, cz);
            byte known = classes.get(key);
            if (known != 0) return known;
            double x = cx * CELL + CELL / 2.0D, z = cz * CELL + CELL / 2.0D;
            byte cls = !discFits(x, z, tight) ? BLOCKED : discFits(x, z, open) ? OPEN : TIGHT;
            classes.put(key, cls);
            return cls;
        }

        private boolean discFits(double x, double z, double radius) {
            double radiusSqr = radius * radius;
            int minX = Mth.floor(x - radius), maxX = Mth.floor(x + radius);
            int minZ = Mth.floor(z - radius), maxZ = Mth.floor(z + radius);
            for (int bx = minX; bx <= maxX; bx++) {
                double nx = Math.max(bx, Math.min(x, bx + 1.0D)) - x;
                for (int bz = minZ; bz <= maxZ; bz++) {
                    double nz = Math.max(bz, Math.min(z, bz + 1.0D)) - z;
                    if (nx * nx + nz * nz < radiusSqr && depthAt(bx, bz) < depth) return false;
                }
            }
            return true;
        }

        private int depthAt(int x, int z) {
            long chunk = ChunkPos.asLong(x >> 4, z >> 4);
            if (chunk != lastChunk) {
                lastChunk = chunk;
                byte[] found = columns.get(chunk);
                lastColumns = found == null ? UNKNOWN : found;
            }
            return lastColumns.length == 0 ? 0 : lastColumns[(z & 15) * 16 + (x & 15)];
        }

        private static double heuristic(int x, int z, int gx, int gz) {
            int dx = Math.abs(gx - x), dz = Math.abs(gz - z);
            return (Math.max(dx, dz) + (DIAGONAL - 1.0D) * Math.min(dx, dz)) * HEURISTIC_WEIGHT;
        }

        private static int cellOf(int block) {
            return Math.floorDiv(block, CELL);
        }

        private static long key(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
    }

    private static final class Node implements Comparable<Node> {
        final int x, z;
        @Nullable
        final Node parent;
        final double g, h;
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
            return Double.compare(g + h, other.g + other.h);
        }
    }
}
