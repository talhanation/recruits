package com.talhanation.recruits.pathfinding;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable picture of all entity obstacles of one level, built by {@link ObstacleIndex} on the server
 * thread and only read by the async path workers. Every obstacle is stored in each block cell its hitbox
 * touches, so a lookup costs a few hash gets no matter how many obstacles exist.
 */
public final class ObstacleSnapshot {

    static final ObstacleSnapshot EMPTY = new ObstacleSnapshot(List.of());

    private static final double EPS = 1.0E-4D;
    /** Hitboxes covering more cells than this are skipped (protects against absurd modded hitboxes). */
    private static final int MAX_CELLS_PER_OBSTACLE = 4096;

    /**
     * @param standing true: recruit standing still, only a cost malus. false: solid hitbox, node is blocked.
     */
    record Obstacle(int id, boolean standing, @Nullable UUID group, AABB box) {}

    private final Long2ObjectMap<Obstacle[]> cells;
    private final boolean hasSolid;
    private final boolean hasStanding;

    ObstacleSnapshot(List<Obstacle> obstacles) {
        Long2ObjectOpenHashMap<List<Obstacle>> building = new Long2ObjectOpenHashMap<>();
        boolean solid = false;
        boolean standing = false;

        for (Obstacle obstacle : obstacles) {
            AABB box = obstacle.box();
            int x0 = Mth.floor(box.minX), x1 = Mth.floor(box.maxX - EPS);
            int y0 = Mth.floor(box.minY), y1 = Mth.floor(box.maxY - EPS);
            int z0 = Mth.floor(box.minZ), z1 = Mth.floor(box.maxZ - EPS);
            if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > MAX_CELLS_PER_OBSTACLE) continue;

            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++)
                    for (int z = z0; z <= z1; z++) {
                        long key = BlockPos.asLong(x, y, z);
                        List<Obstacle> inCell = building.get(key);
                        if (inCell == null) building.put(key, inCell = new ArrayList<>(2));
                        inCell.add(obstacle);
                    }

            if (obstacle.standing()) standing = true;
            else solid = true;
        }

        this.cells = new Long2ObjectOpenHashMap<>(building.size());
        for (Long2ObjectMap.Entry<List<Obstacle>> entry : building.long2ObjectEntrySet()) {
            this.cells.put(entry.getLongKey(), entry.getValue().toArray(new Obstacle[0]));
        }
        this.hasSolid = solid;
        this.hasStanding = standing;
    }

    /** View of this snapshot for one path of the given mob. Server thread only. */
    Query query(Mob mob) {
        return this.cells.isEmpty() ? Query.NONE : new Query(this, mob);
    }

    /**
     * Obstacle view for one mob and one path: knows the mob's size, its own id/group and which
     * obstacles to ignore. Set up on the server thread (constructor, {@link #ignoreObstaclesAt}),
     * afterwards only read by the path worker.
     */
    public static final class Query {
        public static final Query NONE = new Query(EMPTY, null);

        private final ObstacleSnapshot snapshot;
        private final IntArrayList ignoredIds = new IntArrayList(2);
        private final int selfId;
        @Nullable
        private final UUID selfGroup;
        private final double halfWidth;
        private final double height;

        private Query(ObstacleSnapshot snapshot, @Nullable Mob mob) {
            this.snapshot = snapshot;
            this.selfId = mob == null ? -1 : mob.getId();
            this.selfGroup = mob instanceof AbstractRecruitEntity recruit ? recruit.getGroup() : null;
            this.halfWidth = mob == null ? 0 : mob.getBbWidth() / 2.0D;
            this.height = mob == null ? 0 : mob.getBbHeight();

            // never avoid our own vehicle
            for (Entity vehicle = mob == null ? null : mob.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
                this.ignoredIds.add(vehicle.getId());
            }
        }

        public boolean hasSolidObstacles() {
            return this.snapshot.hasSolid;
        }

        /**
         * Do not avoid obstacles at this path target, e.g. the boat the recruit wants to board or the cart
         * it wants to upkeep from - otherwise the target itself would be unreachable. Server thread only.
         */
        public void ignoreObstaclesAt(int x, int y, int z) {
            if (this.snapshot.cells.isEmpty()) return;
            double cx = x + 0.5D, cy = y + 0.5D, cz = z + 0.5D;

            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        Obstacle[] inCell = this.snapshot.cells.get(BlockPos.asLong(x + dx, y + dy, z + dz));
                        if (inCell == null) continue;
                        for (Obstacle obstacle : inCell) {
                            AABB b = obstacle.box();
                            // target block center inside or within half a block of the hitbox
                            if (cx >= b.minX - 0.5D && cx <= b.maxX + 0.5D
                                    && cy >= b.minY - 0.5D && cy <= b.maxY + 0.5D
                                    && cz >= b.minZ - 0.5D && cz <= b.maxZ + 0.5D
                                    && !this.ignoredIds.contains(obstacle.id())) {
                                this.ignoredIds.add(obstacle.id());
                            }
                        }
                    }
        }

        /** The mob standing on this node would intersect a solid obstacle. */
        public boolean isBlocked(int x, int y, int z) {
            return this.snapshot.hasSolid && this.intersectsObstacle(x, y, z, false);
        }

        /** A standing recruit (not this mob, not its group) occupies this node. */
        public boolean isCrowded(int x, int y, int z) {
            return this.snapshot.hasStanding && this.intersectsObstacle(x, y, z, true);
        }

        private boolean intersectsObstacle(int x, int y, int z, boolean standing) {
            double minX = x + 0.5D - this.halfWidth, maxX = x + 0.5D + this.halfWidth;
            double minZ = z + 0.5D - this.halfWidth, maxZ = z + 0.5D + this.halfWidth;
            double minY = y + 0.01D, maxY = y + this.height;

            for (int cx = Mth.floor(minX); cx <= Mth.floor(maxX - EPS); cx++)
                for (int cy = Mth.floor(minY); cy <= Mth.floor(maxY - EPS); cy++)
                    for (int cz = Mth.floor(minZ); cz <= Mth.floor(maxZ - EPS); cz++) {
                        Obstacle[] inCell = this.snapshot.cells.get(BlockPos.asLong(cx, cy, cz));
                        if (inCell == null) continue;
                        for (Obstacle obstacle : inCell) {
                            if (obstacle.standing() != standing || obstacle.id() == this.selfId) continue;
                            if (standing && this.selfGroup != null && this.selfGroup.equals(obstacle.group())) continue;
                            if (this.ignoredIds.contains(obstacle.id())) continue;
                            if (obstacle.box().intersects(minX, minY, minZ, maxX, maxY, maxZ)) return true;
                        }
                    }
            return false;
        }
    }
}
