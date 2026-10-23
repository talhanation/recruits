package com.talhanation.recruits.pathfinding;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.ObstacleSnapshot.Obstacle;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per level: which entities the recruit pathfinder has to respect.
 * <ul>
 *     <li><b>solid</b>: collidable hitboxes ({@link Entity#canBeCollidedWith()}), e.g. siege weapons,
 *     ground cannons, boats, ships. Nodes inside them are blocked.</li>
 *     <li><b>standing</b>: recruits that stand still. Only a cost malus, so walking around is preferred
 *     but a path through a crowd is still found. Moving recruits are ignored.</li>
 * </ul>
 * Entities are tracked via join/leave events, the level is never scanned. Once per second the obstacles
 * are collected and a new snapshot is built only if their state changed.
 * <p>
 * Register with {@code MinecraftForge.EVENT_BUS.register(ObstacleIndex.class)} (static handlers).
 */
public final class ObstacleIndex {

    private static final int REFRESH_INTERVAL = 20;
    private static final double STANDING_SPEED_SQR = 0.05D * 0.05D;
    private static final Map<ServerLevel, ObstacleIndex> INDICES = new ConcurrentHashMap<>();

    /** Recruits and entities with a collidable hitbox. Server thread only. */
    private final Set<Entity> tracked = new ReferenceOpenHashSet<>();
    private volatile ObstacleSnapshot snapshot = ObstacleSnapshot.EMPTY;
    private long lastStateHash;

    /** Obstacle view for the next path of this mob. Server thread only. */
    public static ObstacleSnapshot.Query query(Mob mob) {
        ObstacleIndex index = mob.level() instanceof ServerLevel level ? INDICES.get(level) : null;
        return index == null ? ObstacleSnapshot.Query.NONE : index.snapshot.query(mob);
    }

    @Nullable
    private static Obstacle toObstacle(Entity entity) {
        if (!entity.isAlive()) return null;

        if (entity instanceof AbstractRecruitEntity recruit) {
            boolean standing = !recruit.isPassenger()
                    && recruit.getDeltaMovement().horizontalDistanceSqr() < STANDING_SPEED_SQR
                    && recruit.getNavigation().isDone();
            return standing ? new Obstacle(recruit.getId(), true, recruit.getGroup(), recruit.getBoundingBox()) : null;
        }

        return entity.canBeCollidedWith() && !entity.isSpectator()
                ? new Obstacle(entity.getId(), false, null, entity.getBoundingBox())
                : null;
    }

    private void refresh() {
        List<Obstacle> obstacles = new ArrayList<>();
        long hash = 17L;

        for (Iterator<Entity> it = this.tracked.iterator(); it.hasNext(); ) {
            Entity entity = it.next();
            if (entity.isRemoved()) {
                it.remove();
                continue;
            }
            Obstacle obstacle = toObstacle(entity);
            if (obstacle == null) continue;

            obstacles.add(obstacle);
            // quarter block resolution, so idle jitter does not cause rebuilds
            hash = hash * 31L + obstacle.id();
            hash = hash * 31L + Mth.floor(obstacle.box().minX * 4.0D);
            hash = hash * 31L + Mth.floor(obstacle.box().minY * 4.0D);
            hash = hash * 31L + Mth.floor(obstacle.box().minZ * 4.0D);
            hash = hash * 31L + Objects.hashCode(obstacle.group());
        }

        if (hash == this.lastStateHash) return;
        this.lastStateHash = hash;
        this.snapshot = obstacles.isEmpty() ? ObstacleSnapshot.EMPTY : new ObstacleSnapshot(obstacles);
    }

    // ------------------------------------------------------------------ events

    // LOWEST and not receiving canceled events: only track entities that really end up in the level
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level && (entity instanceof AbstractRecruitEntity || entity.canBeCollidedWith())) {
            INDICES.computeIfAbsent(level, l -> new ObstacleIndex()).tracked.add(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        ObstacleIndex index = event.getLevel() instanceof ServerLevel level ? INDICES.get(level) : null;
        if (index != null) index.tracked.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % REFRESH_INTERVAL != 0) return;

        ObstacleIndex index = INDICES.get(level);
        if (index != null) index.refresh();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) INDICES.remove(level);
    }
}
