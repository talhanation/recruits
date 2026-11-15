package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AbstractTravelEntity;
import com.talhanation.recruits.world.SimulatedTravelSaveData.Walker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runs all simulated journeys. A travelling group ({@link AbstractTravelEntity} with his company, mounts and
 * passengers) really walks only where a player can see it. Everywhere else it is taken out of the world and
 * moves as a {@link SimulatedTravel}: a position that advances towards the destination, no chunks are loaded.
 * <ul>
 *     <li><b>Out of the world</b> ({@link #pack}): no player is within watching range of the group, or the
 *     group stands in a chunk whose entities do not tick (edge of the simulation distance).</li>
 *     <li><b>Back into the world</b> ({@link #unpack}): the position is in a chunk whose entities tick and a
 *     player is within watching range. The group appears at the edge of that range and walks on from there.</li>
 *     <li><b>Stuck</b> (half a minute without coming closer) with nobody close: the group skips a stretch out of the
 *     world. After a minute and a half it does so even in front of a player, e.g. a messenger in a walled yard.</li>
 *     <li><b>Time away</b> (hunger, morale, pay) is made up for when the group comes back.</li>
 * </ul>
 * The leader reports every second while he walks ({@link #track}). When his chunk is about to be unloaded
 * (player logged out or teleported away) the group is taken out of the world just before
 * ({@link #onChunkLevelChanged}). A leader who went silent is looked for: his chunk stopped ticking, or he was
 * saved into it when the server stopped.
 */
public class SimulatedTravelManager {
    private static final int INTERVAL = 20;
    /** A player closer than the tracking range of the group plus this counts as watching: the group comes back. */
    private static final int WATCH_MARGIN = 16;
    /** The group leaves the world only this much further out, so it does not flip at the edge. */
    private static final int WATCH_HYSTERESIS = 16;
    /** On its way a group never appears closer than this to a player. */
    private static final double MIN_APPEAR_DISTANCE = 48.0D;
    /** Next to its target player a group appears at least this far from him. */
    private static final int MIN_APPEAR_AT_TARGET = 16;
    /** Not a block closer to the destination for this long counts as stuck. */
    public static final int STUCK_SECONDS = 30;
    /** Stuck this long, the group leaves the world even right in front of a player - a journey has to go on. */
    public static final int STUCK_HARD_SECONDS = 90;
    private static final double STUCK_PACK_DISTANCE = 64.0D;
    private static final double STUCK_SKIP = 48.0D;
    private static final double SPAWN_SKIP = 16.0D;
    private static final int SHIP_SPACING = 14;
    private static final long UNPACK_DWELL = 100L;
    private static final long WALKER_SILENT = 60L;
    private static final long RECOVER_INTERVAL = 600L;
    private static final int RECOVER_ATTEMPTS = 20;
    private static final double MAX_STEP_SECONDS = 10.0D;
    private static final double MIN_SPEED = 0.5D;

    private SimulatedTravelSaveData data;
    /** Guards {@link #onChunkLevelChanged} against running inside itself. */
    private boolean handlingChunk;
    private final SeaRoutePlanner seaPlanner = new SeaRoutePlanner();

    @Nullable
    public static SimulatedTravelManager get() {
        return RecruitEvents.simulatedTravelManager;
    }

    public void load(ServerLevel overworld) {
        data = SimulatedTravelSaveData.get(overworld);
    }

    public boolean isOutOfWorld(UUID leader) {
        return data != null && data.travels.containsKey(leader);
    }

    /** The journey of a group that is out of the world. The record must not be changed. */
    @Nullable
    public SimulatedTravel getTravel(UUID leader) {
        return data == null ? null : data.travels.get(leader);
    }

    // ---- Sea ways ----

    /**
     * Finds the way over water along the route of a captain, in the background ({@link SeaRoutePlanner}). The
     * result goes to {@link AbstractTravelEntity#onSeaRoute} - or straight into his journey if he left the world
     * meanwhile.
     *
     * @param depth   blocks of water his ship needs
     * @param radius  blocks either side of her keel line the hull takes
     * @param surface the y just above the water she floats on
     */
    public void planSeaRoute(AbstractTravelEntity leader, List<BlockPos> points, boolean cycle, long signature,
                             int depth, double radius, int surface) {
        if (!(leader.level() instanceof ServerLevel level)) return;
        UUID id = leader.getUUID();
        seaPlanner.request(level, id, points, cycle, signature, depth, radius, surface, route -> {
            if (level.getEntity(id) instanceof AbstractTravelEntity present && present.isAlive()) {
                present.onSeaRoute(route);
                return;
            }
            SimulatedTravel travel = data == null ? null : data.travels.get(id);
            if (travel == null || !travel.plan.route || !travel.plan.water) return;
            travel.plan.sea = route;
            data.setDirty();
        });
    }

    // ---- Walking leaders ----

    /**
     * Sign of life of a really walking leader, once a second. Takes the group out of the world when nobody watches it.
     * @param stuckSeconds for how long the leader has not come closer to his destination
     */
    public void track(AbstractTravelEntity leader, int stuckSeconds) {
        if (data == null || !(leader.level() instanceof ServerLevel level)) return;
        long now = gameTime(level.getServer());

        Walker walker = data.walkers.get(leader.getUUID());
        if (walker == null) {
            walker = new Walker(level.dimension().location(), leader.blockPosition());
            data.walkers.put(leader.getUUID(), walker);
        }
        walker.dimension = level.dimension().location();
        walker.pos = leader.blockPosition();
        walker.lastSeen = now;
        walker.attempts = 0;
        data.setDirty();
        if (now < walker.noPackBefore) return;

        List<Entity> roots = roots(leader);
        boolean stuck = stuckSeconds >= STUCK_SECONDS;
        boolean pack;
        if (stuckSeconds >= STUCK_HARD_SECONDS) pack = true;
        else if (stuck) pack = !anyPlayerWithin(level, roots, STUCK_PACK_DISTANCE);
        else pack = !anyPlayerWithin(level, roots, watchRange(level, roots) + WATCH_MARGIN + WATCH_HYSTERESIS);

        if (pack) pack(level, leader, roots, stuck ? STUCK_SKIP : 0.0D);
    }

    /** Sign of life of a leader who is on a mission but not on the road, once a second. He stays in the world. */
    public void watch(AbstractTravelEntity leader) {
        if (data == null || !(leader.level() instanceof ServerLevel level)) return;
        Walker walker = data.walkers.get(leader.getUUID());
        if (walker == null) {
            walker = new Walker(level.dimension().location(), leader.blockPosition());
            data.walkers.put(leader.getUUID(), walker);
        }
        walker.dimension = level.dimension().location();
        walker.pos = leader.blockPosition();
        walker.lastSeen = gameTime(level.getServer());
        walker.attempts = 0;
        data.setDirty();
    }

    /**
     * A chunk changes its ticket level. When it drops out of the loaded world, its entities are saved and unloaded
     * within this tick - a leader on a mission in it would be frozen in the chunk. He is taken out of the world
     * right now instead and carries on from here. (The chunk unload event comes too late: he is saved by then.)
     * <p>
     * Not while the server stops: the save data is written before the chunks go, so he stays in his chunk and is
     * looked for after the next start.
     */
    public void onChunkLevelChanged(ServerLevel level, long chunk, int oldLevel, int newLevel) {
        if (data == null || handlingChunk || data.walkers.isEmpty()) return; // cheap: this fires for every chunk
        if (ChunkLevel.fullStatus(newLevel) != FullChunkStatus.INACCESSIBLE || ChunkLevel.fullStatus(oldLevel) == FullChunkStatus.INACCESSIBLE) return;
        if (!level.getServer().isRunning()) return;

        int chunkX = ChunkPos.getX(chunk);
        int chunkZ = ChunkPos.getZ(chunk);
        List<AbstractTravelEntity> leaving = null;
        for (Map.Entry<UUID, Walker> entry : data.walkers.entrySet()) {
            Walker walker = entry.getValue();
            // the saved position is up to a second old: he may have crossed into the next chunk since
            if (Math.abs((walker.pos.getX() >> 4) - chunkX) > 1 || Math.abs((walker.pos.getZ() >> 4) - chunkZ) > 1) continue;
            if (!walker.dimension.equals(level.dimension().location())) continue;
            if (level.getEntity(entry.getKey()) instanceof AbstractTravelEntity leader && leader.isAlive()
                    && leader.getRootVehicle().chunkPosition().toLong() == chunk) { // a rider is saved with his mount
                if (leaving == null) leaving = new ArrayList<>();
                leaving.add(leader);
            }
        }
        if (leaving == null) return;

        handlingChunk = true;
        try {
            for (AbstractTravelEntity leader : leaving) this.packStranded(level, leader);
        } catch (RuntimeException e) {
            // this runs in the middle of the chunk system: a fault here must not take the server down
            Main.LOGGER.error("Recruits: could not take a travelling group out of the world before its chunk unloads", e);
        } finally {
            handlingChunk = false;
        }
    }

    /** Nobody is left where this leader is. On the road he just goes on out of the world, otherwise he is asked first. */
    private void packStranded(ServerLevel level, AbstractTravelEntity leader) {
        if (!leader.isTravelling()) leader.onTravelStranded();
        if (leader.isTravelling()) pack(level, leader, roots(leader), 0.0D);
        // he decided to stay (e.g. a captain in his harbour): he is saved into his chunk and reports again when it loads
        else if (data.walkers.remove(leader.getUUID()) != null) data.setDirty();
    }

    /** The leader arrived, gave up or is gone for good. */
    public void untrack(UUID leader) {
        if (data != null && data.walkers.remove(leader) != null) data.setDirty();
    }

    // ---- Tick ----

    public void tick(MinecraftServer server) {
        if (data == null || server.getTickCount() % INTERVAL != 0) return;
        long now = gameTime(server);

        if (!data.walkers.isEmpty()) checkWalkers(server, now);
        if (data.travels.isEmpty()) return;

        for (SimulatedTravel travel : new ArrayList<>(data.travels.values())) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, travel.dimension));
            if (level == null) continue; // dimension is gone or not loaded, try again later

            advance(server, level, travel, now);
            if (shouldUnpack(level, travel)) {
                unpack(level, travel, now);
                continue;
            }
            // at the companion he was sent to, and nobody is here to see it: the errand is done out of the world
            if (travel.atDestination && travel.plan.targetCompanion != null) arriveOutOfWorld(level, travel);
            if (RecruitEvents.recruitsCompanionManager != null) {
                RecruitEvents.recruitsCompanionManager.updateTravelling(travel.leader, travel.dimension,
                        BlockPos.containing(travel.x, travel.y, travel.z), now);
            }
        }
        data.setDirty();
    }

    /** Leaders who did not report: frozen at the edge of the simulation distance, or unloaded with their chunk. */
    private void checkWalkers(MinecraftServer server, long now) {
        List<AbstractTravelEntity> frozen = null;
        Iterator<Map.Entry<UUID, Walker>> iterator = data.walkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Walker> entry = iterator.next();
            Walker walker = entry.getValue();
            if (now - walker.lastSeen < WALKER_SILENT) continue;

            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, walker.dimension));
            if (level == null) continue;

            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof AbstractTravelEntity leader && leader.isAlive() && leader.isOnMission()) {
                // In the world but not ticking: nobody can walk here, a journey goes on out of the world.
                // Who is not on the road waits (his player may come back) - unless he was just fetched from an
                // unloaded chunk, then nobody is here at all.
                boolean fetched = walker.attempts > 0;
                if ((leader.isTravelling() || fetched) && !level.isPositionEntityTicking(leader.blockPosition())) {
                    if (frozen == null) frozen = new ArrayList<>();
                    frozen.add(leader);
                }
                continue;
            }
            if (entity != null) { // loaded, but not on a mission (any more)
                iterator.remove();
                data.setDirty();
                continue;
            }

            // not in the world: his chunk was unloaded under him. Entities are only loaded while a player is online.
            if (now < walker.nextRecover || server.getPlayerList().getPlayerCount() == 0) continue;
            if (++walker.attempts > RECOVER_ATTEMPTS) {
                Main.LOGGER.warn("Recruits: travelling leader {} was not found again at {} and is given up", entry.getKey(), walker.pos);
                iterator.remove();
                data.setDirty();
                continue;
            }
            walker.nextRecover = now + RECOVER_INTERVAL;
            // loads his chunk and its neighbours for a few seconds without ticking them, just to get the entities;
            // the ticket runs out by itself. He is taken out of the world as soon as he shows up (frozen, above).
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(walker.pos), 1, walker.pos);
        }

        if (frozen == null) return;
        for (AbstractTravelEntity leader : frozen) {
            this.packStranded((ServerLevel) leader.level(), leader);
        }
    }

    // ---- Out of the world ----

    /**
     * Takes the leader and his company (with mounts and passengers) out of the world.
     * @return false if the group stays: no plan, a player rides along, or the leader could not be saved
     */
    private boolean pack(ServerLevel level, AbstractTravelEntity leader, List<Entity> roots, double skip) {
        TravelPlan plan = leader.getTravelPlan();
        if (plan == null) return false;
        for (Entity root : roots) {
            if (root.getSelfAndPassengers().anyMatch(entity -> entity instanceof Player)) return false;
        }

        List<CompoundTag> saved = new ArrayList<>();
        List<Entity> savedRoots = new ArrayList<>();
        for (Entity root : roots) {
            CompoundTag tag = new CompoundTag();
            if (root.saveAsPassenger(tag)) {
                saved.add(tag);
                savedRoots.add(root);
            } else if (root == roots.get(0)) {
                return false; // without the leader nobody travels
            }
        }

        double measured = plan.speed > 0.0D ? plan.speed : leader.getDefaultTravelSpeed();
        double speed = Math.max(MIN_SPEED, measured * RecruitsServerConfig.TravelSpeedFactor.get());
        long now = gameTime(level.getServer());
        SimulatedTravel travel = new SimulatedTravel(leader.getUUID(), leader.getOwnerUUID(), level.dimension().location(),
                leader.getX(), leader.getY(), leader.getZ(), leader.getYRot(), plan, speed, now, skip, watchRange(level, roots), saved);

        // the record first: should a removal fail, nobody is lost - whoever stays in the world is just not added again
        data.walkers.remove(leader.getUUID());
        data.travels.put(travel.leader, travel);
        data.setDirty();

        for (Entity root : savedRoots) {
            List<Entity> all = new ArrayList<>();
            root.getIndirectPassengers().forEach(all::add);
            all.add(root);
            // not killed and not discarded: nothing dies, drops or is counted off, the entities just leave this world
            for (Entity entity : all) entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        }
        if (RecruitEvents.recruitsCompanionManager != null) {
            RecruitEvents.recruitsCompanionManager.snapshot(leader); // how he looks now, for his screen from far away
            RecruitEvents.recruitsCompanionManager.updateTravelling(travel.leader, travel.dimension, leader.blockPosition(), now);
        }
        Main.LOGGER.debug("Recruits travel: {} left the world at {} with {} companions, {} blocks/s ({}), skip {}",
                leader.getName().getString(), leader.blockPosition(), saved.size() - 1, String.format("%.2f", speed),
                plan.speed > 0.0D ? "measured" : "default", skip);
        return true;
    }

    /** The leader's own vehicle first, then the vehicles of his company. A rider counts as his mount. */
    private static List<Entity> roots(AbstractTravelEntity leader) {
        List<Entity> roots = new ArrayList<>();
        roots.add(leader.getRootVehicle());
        for (Entity companion : leader.getTravelCompany()) {
            if (companion == null || !companion.isAlive() || companion.level() != leader.level()) continue;
            Entity root = companion.getRootVehicle();
            if (!roots.contains(root) && root.isAlive()) roots.add(root);
        }
        return roots;
    }

    // ---- On the way ----

    private static void advance(MinecraftServer server, ServerLevel level, SimulatedTravel travel, long now) {
        double seconds = Math.min(MAX_STEP_SECONDS, (now - travel.lastStep) / 20.0D);
        travel.lastStep = now;
        if (seconds <= 0.0D) return;

        TravelPlan plan = travel.plan;
        if (plan.route && plan.waiting) { // a patrol stands at its way point for the wait time, then goes on
            travel.atDestination = false;
            if (now < plan.waitUntil) return;
            plan.waiting = false;
            plan.advanceRoute();
        }

        Vec3 destination = travel.plan.resolve(server, level.dimension(), now);
        if (destination == null) { // waiting for the target player, or nothing left: stays where it is
            travel.atDestination = !travel.plan.isWaitingForTarget();
            return;
        }

        double left = travel.speed * seconds;
        travel.y = destination.y;
        boolean arrived = false;
        // by ship along a route: from corner to corner of the sea way, the way point at its end
        for (int corner = 0; corner < 16 && !arrived; corner++) {
            Vec3 stop = plan.seaStop(travel.x, travel.z, destination);
            if (stop == null) { // no water leads on: she lies here
                travel.atDestination = false;
                return;
            }
            double dx = stop.x - travel.x;
            double dz = stop.z - travel.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.01D) travel.yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

            if (distance > left) {
                travel.x += dx / distance * left;
                travel.z += dz / distance * left;
                travel.skip = Math.max(0.0D, travel.skip - left);
                travel.atDestination = false;
                return;
            }
            travel.x = stop.x;
            travel.z = stop.z;
            travel.skip = Math.max(0.0D, travel.skip - distance);
            left -= distance;
            arrived = stop == destination;
        }
        if (!arrived) {
            travel.atDestination = false;
            return;
        }

        travel.skip = 0.0D;
        if (plan.route) { // a patrol never ends
            plan.waiting = true;
            plan.waitUntil = now + plan.currentWaitSeconds() * 20L;
            travel.atDestination = false;
            return;
        }
        // way points are passed on the way; the last one and a target player are the end of the journey
        travel.atDestination = plan.followsTarget() || !plan.nextPoint();
    }

    // ---- Changed while out of the world ----

    /**
     * The leader of a group that is out of the world, as a copy in no world, standing where the journey got to.
     * Everything read from or done to it has to do without the world around it.
     */
    @Nullable
    private static Entity leaderCopy(ServerLevel level, SimulatedTravel travel) {
        if (travel.entities.isEmpty()) return null;
        Entity root = EntityType.loadEntityRecursive(travel.entities.get(0), level, entity -> entity);
        if (root == null) return null;
        for (Entity entity : root.getSelfAndPassengers().toList()) {
            entity.moveTo(travel.x, travel.y, travel.z, travel.yaw, 0.0F);
        }
        return root;
    }

    @Nullable
    private static AbstractTravelEntity leaderOf(Entity root, UUID leader) {
        for (Entity entity : root.getSelfAndPassengers().toList()) {
            if (entity instanceof AbstractTravelEntity traveller && entity.getUUID().equals(leader)) return traveller;
        }
        return null;
    }

    /** Keeps what was done to the copy of the leader (and his mount or ship). */
    private void keepLeaderCopy(SimulatedTravel travel, Entity root) {
        CompoundTag tag = new CompoundTag();
        if (root.saveAsPassenger(tag)) travel.entities.set(0, tag);
        data.setDirty();
    }

    /**
     * The journey to a companion reached him while the group is out of the world. The leader does what he came
     * for ({@link AbstractTravelEntity#onTravelArrivedOutOfWorld}) and travels on.
     */
    private void arriveOutOfWorld(ServerLevel level, SimulatedTravel travel) {
        TravelPlan next = null;
        try {
            Entity root = leaderCopy(level, travel);
            AbstractTravelEntity leader = root == null ? null : leaderOf(root, travel.leader);
            if (leader != null) {
                next = leader.onTravelArrivedOutOfWorld(travel.plan);
                keepLeaderCopy(travel, root);
            }
        } catch (RuntimeException e) {
            Main.LOGGER.error("Recruits: travelling leader {} could not do his errand out of the world", travel.leader, e);
        }

        if (next != null) {
            travel.plan = next;
        } else {
            // nothing new: on with the way points of the old plan, the companion is done with
            travel.plan.targetCompanion = null;
        }
        travel.atDestination = false;
        data.setDirty();
    }

    /**
     * Changes the leader of a group that is out of the world, e.g. with the orders of a Messenger. The change is
     * done on a copy of the leader ({@link AbstractTravelEntity#changeOutOfWorld}) and takes effect on the journey
     * right away: a new route is followed from the next step on.
     *
     * @return false if there is no such group out of the world, or its leader could not be read
     */
    public boolean orderOutOfWorld(ServerLevel anyLevel, UUID leaderId, Consumer<AbstractTravelEntity> change) {
        SimulatedTravel travel = data == null ? null : data.travels.get(leaderId);
        if (travel == null) return false;
        ServerLevel level = anyLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, travel.dimension));
        if (level == null) return false;

        Entity root = leaderCopy(level, travel);
        AbstractTravelEntity leader = root == null ? null : leaderOf(root, leaderId);
        if (leader == null) return false;

        TravelPlan next = leader.changeOutOfWorld(travel.plan, () -> change.accept(leader));
        keepLeaderCopy(travel, root);
        if (next == null) {
            // he does not travel any more: the group waits here until somebody comes
            next = TravelPlan.toPoint(BlockPos.containing(travel.x, travel.y, travel.z));
            next.water = travel.plan.water;
        }
        travel.plan = next;
        travel.atDestination = false;
        data.setDirty();
        return true;
    }

    private static boolean shouldUnpack(ServerLevel level, SimulatedTravel travel) {
        BlockPos pos = BlockPos.containing(travel.x, travel.y, travel.z);
        if (!level.isPositionEntityTicking(pos)) return false;
        if (!anyPlayerWithin(level, travel.x, travel.z, travel.watchRange + WATCH_MARGIN)) return false;
        if (travel.atDestination) return true;
        if (travel.skip > 0.0D) return false;
        return !anyPlayerWithin(level, travel.x, travel.z, MIN_APPEAR_DISTANCE);
    }

    // ---- Back into the world ----

    private void unpack(ServerLevel level, SimulatedTravel travel, long now) {
        BlockPos spawn = findSpawnPos(level, travel);
        if (spawn == null) { // water, a cliff, a wall: a bit further on
            travel.skip = SPAWN_SKIP;
            return;
        }

        long away = Math.max(0L, now - travel.packedAt);
        RandomSource random = level.getRandom();
        AbstractTravelEntity leader = null;

        for (int i = 0; i < travel.entities.size(); i++) {
            BlockPos pos = spawn;
            if (i > 0) {
                BlockPos near;
                if (travel.plan.water) {
                    // ships in line astern, a good ship's length apart, so no hull lies in another
                    double back = Math.toRadians(travel.yaw + 90.0F);
                    BlockPos astern = spawn.offset((int) Math.round(-Math.cos(back) * SHIP_SPACING * i), 0, (int) Math.round(-Math.sin(back) * SHIP_SPACING * i));
                    near = findFreeNear(level, astern, 0, 3, true, random);
                } else {
                    near = findFreeNear(level, spawn, 0, 2 + Math.min(6, i / 4), false, random);
                }
                if (near != null) pos = near;
            }
            double px = pos.getX() + 0.5D, py = pos.getY(), pz = pos.getZ() + 0.5D;
            Entity root = EntityType.loadEntityRecursive(travel.entities.get(i), level, entity -> {
                entity.moveTo(px, py, pz, travel.yaw, 0.0F);
                return entity;
            });
            // an entity that is already in the world (old copy after a crash) is not added a second time
            if (root == null || !level.tryAddFreshEntityWithPassengers(root)) {
                Main.LOGGER.warn("Recruits: could not put a travelling entity of {} back into the world at {}", travel.leader, pos);
                continue;
            }
            for (Entity entity : root.getSelfAndPassengers().toList()) {
                if (entity instanceof AbstractRecruitEntity recruit) recruit.catchUpAwayTime(away);
                if (entity instanceof AbstractTravelEntity traveller && entity.getUUID().equals(travel.leader)) leader = traveller;
            }
        }

        data.travels.remove(travel.leader);
        data.setDirty();
        if (leader == null) return; // the journey ends here, see the warning above

        Walker walker = new Walker(travel.dimension, leader.blockPosition());
        walker.lastSeen = now;
        walker.noPackBefore = now + UNPACK_DWELL;
        data.walkers.put(travel.leader, walker);
        leader.resumeTravel(travel.plan);
        Main.LOGGER.debug("Recruits travel: {} is back in the world at {} after {} s", leader.getName().getString(), spawn, away / 20L);
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, SimulatedTravel travel) {
        boolean water = travel.plan.water;
        BlockPos center = BlockPos.containing(travel.x, travel.y, travel.z);
        boolean atTargetPlayer = travel.atDestination && travel.plan.followsPlayer();

        if (travel.atDestination && !travel.plan.followsTarget()) {
            // arrived at a way point: its own height first, it may be a post under a roof
            BlockPos point = travel.plan.currentPoint();
            if (point != null && level.hasChunkAt(point)) {
                if (isFree(level, point.above(), water)) return point.above();
                if (isFree(level, point, water)) return point;
            }
        }

        int min = atTargetPlayer ? MIN_APPEAR_AT_TARGET : 0;
        BlockPos free = findFreeNear(level, center, min, min + (water ? 10 : 6), water, level.getRandom());
        if (free != null || !travel.atDestination) return free;
        // at the end of the journey the group has to appear somewhere
        BlockPos fallback = surface(level, center, water);
        return fallback != null ? fallback : center;
    }

    /** A free spot on the surface around the center, at least min and at most max blocks away (per axis). */
    @Nullable
    private static BlockPos findFreeNear(ServerLevel level, BlockPos center, int min, int max, boolean water, RandomSource random) {
        for (int i = 0; i < 16; i++) {
            int dx = 0, dz = 0;
            if (i > 0 || min > 0) {
                dx = random.nextInt(max * 2 + 1) - max;
                dz = random.nextInt(max * 2 + 1) - max;
                if (Math.max(Math.abs(dx), Math.abs(dz)) < min) continue;
            }
            BlockPos pos = surface(level, center.offset(dx, 0, dz), water);
            if (pos != null && isFree(level, pos, water)) return pos;
        }
        return null;
    }

    /**
     * The surface of the column, or null if its chunk is not loaded - a search never loads chunks.
     * Under a ceiling (Nether) the heightmap is the roof of the world, so the nearest free spot around the height
     * of the journey is taken instead.
     */
    @Nullable
    private static BlockPos surface(ServerLevel level, BlockPos column, boolean water) {
        if (!level.hasChunkAt(column)) return null;
        if (!level.dimensionType().hasCeiling()) {
            return level.getHeightmapPos(water ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        }
        for (int d = 0; d <= 24; d++) {
            if (isFree(level, column.above(d), water)) return column.above(d);
            if (isFree(level, column.below(d), water)) return column.below(d);
        }
        return null;
    }

    private static boolean isFree(Level level, BlockPos pos, boolean water) {
        boolean open = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        if (water) {
            // room for a hull: water under this spot and three blocks to every side
            return open && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).is(FluidTags.WATER)
                    && level.getFluidState(pos.offset(3, -1, 0)).is(FluidTags.WATER) && level.getFluidState(pos.offset(-3, -1, 0)).is(FluidTags.WATER)
                    && level.getFluidState(pos.offset(0, -1, 3)).is(FluidTags.WATER) && level.getFluidState(pos.offset(0, -1, -3)).is(FluidTags.WATER);
        }
        if (!level.getBlockState(pos.below()).isSolid()) return false;
        return open && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty();
    }

    // ---- Helpers ----

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** How far a player sees this group, in blocks: its tracking range, but never further than the view distance. */
    private static int watchRange(ServerLevel level, List<Entity> roots) {
        int range = 0;
        for (Entity root : roots) {
            for (Entity entity : root.getSelfAndPassengers().toList()) {
                range = Math.max(range, entity.getType().clientTrackingRange() * 16);
            }
        }
        return Math.min(range, level.getServer().getPlayerList().getViewDistance() * 16);
    }

    private static boolean anyPlayerWithin(ServerLevel level, List<Entity> roots, double range) {
        for (Entity root : roots) {
            if (anyPlayerWithin(level, root.getX(), root.getZ(), range)) return true;
        }
        return false;
    }

    private static boolean anyPlayerWithin(ServerLevel level, double x, double z, double range) {
        double rangeSqr = range * range;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz <= rangeSqr) return true;
        }
        return false;
    }
}
