package com.talhanation.recruits.governor;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.GovernorEvent;
import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.NobleQuest;
import com.talhanation.recruits.world.NobleQuestProgress;
import com.talhanation.recruits.world.NobleReputation;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.recruits.world.RecruitsVillage;
import com.talhanation.recruits.world.RecruitsVillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Server-side brain of the Governor. Runs every {@code GovernorTickInterval} ticks
 * (phase from the entity id, so many governors do not run in the same tick):
 * rule check, chest check, garrison distribution, patrols, siege-engineer priority,
 * upkeep/loadout, and plans one task at a time (withdraw, hire, collect taxes, village quests, deposit).
 *
 * The Governor himself stands at the treasury chest. He leaves it only at the villagers' meeting time
 * (taxes at the bell, quests at a noble) and to hire new recruits.
 */
public class GovernorController {

    private static final int TASK_TIMEOUT = 1200;
    private static final int LOADOUT_COOLDOWN = 600;
    private static final int PATROL_TIMEOUT = 2400;
    /** Longest upkeep break of a guard (2 minutes), afterwards he is back on duty even if not healed. */
    private static final int BREAK_TIMEOUT = 2400;
    /** A break ends once the guard is fed to this and healed (or can not heal any more). */
    private static final float BREAK_DONE_HUNGER = 70F;
    /** A guard that came back from a break still hungry (empty upkeep chest) waits this long for the next one. */
    private static final int BREAK_RETRY_DELAY = 6000;
    /** Vanilla villager meeting (MEET 9000-11000) plus the time the nobles hand out the day's quests (until 12000). */
    private static final long MEETING_START = 9000L;
    private static final long MEETING_END = 12000L;

    private static final int XP_PER_DAY = 10;
    private static final int XP_TAX = 5;
    private static final int XP_HIRE = 5;
    private static final int XP_QUEST = 15;

    private enum TaskType {WITHDRAW, HIRE, COLLECT_TAX, DEPOSIT, QUEST}

    private static class Task {
        final TaskType type;
        @Nullable
        final BlockPos pos;
        @Nullable
        final UUID entity;
        @Nullable
        final GovernorUnitType unitType;
        final int cost;
        final boolean fromNoble;
        @Nullable
        Task next;
        int age;
        /** QUEST: the village, the offer to accept (null = turn in the active quest). */
        @Nullable
        UUID village;
        @Nullable
        UUID offer;

        Task(TaskType type, @Nullable BlockPos pos, @Nullable UUID entity, @Nullable GovernorUnitType unitType, int cost, boolean fromNoble) {
            this.type = type;
            this.pos = pos;
            this.entity = entity;
            this.unitType = unitType;
            this.cost = cost;
            this.fromNoble = fromNoble;
        }
    }

    private final GovernorEntity governor;
    private final Map<UUID, Long> lastLoadoutCall = new HashMap<>();
    private final List<AbstractRecruitEntity> lastFree = new ArrayList<>();
    /** Guards on an upkeep break: recruit -> game time the break started. Their post is taken over by the reserve. */
    private final Map<UUID, Long> breaks = new HashMap<>();
    /** Guards that came back hungry (no food in the upkeep chest): no new break before this game time. */
    private final Map<UUID, Long> breakCooldown = new HashMap<>();
    @Nullable
    private Task task;
    private int intervalCounter;
    private boolean wasRuling;
    private boolean travelling;
    @Nullable
    private BlockPos postTreasury;
    @Nullable
    private Vec3 postStand;

    public GovernorController(GovernorEntity governor) {
        this.governor = governor;
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        if (!(governor.getCommandSenderWorld() instanceof ServerLevel level)) return;

        if (task != null && ++task.age > TASK_TIMEOUT) {
            abortTask();
        }

        int interval = RecruitsServerConfig.GovernorTickInterval.get();
        if (Math.floorMod(governor.tickCount + governor.getId(), interval) != 0) return;
        intervalCounter++;

        runInterval(level, interval);
    }

    private void runInterval(ServerLevel level, int interval) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return;

        UUID claimId = governor.getGovernedClaim();
        if (claimId == null) {
            governor.setRuling(false);
            return;
        }
        if (level.dimension() != Level.OVERWORLD) return;

        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        ClaimGovernorData data = claim == null ? manager.getData(claimId) : manager.getOrCreate(claimId);
        GovernorManager.RuleStatus status = GovernorManager.checkRule(data, claim, governor.getUUID(), governor.getOwnerUUID());

        if (status != GovernorManager.RuleStatus.RULING) {
            stopRuling(level, data, claim, status);
            return;
        }

        if (data.getGovernorId() == null) {
            data.setGovernor(governor.getUUID(), governor.getOwnerUUID(), governor.getName().getString());
        }
        data.setGovernorLevel(governor.getXpLevel());
        governor.setRuling(true);
        wasRuling = true;
        if (governor.getListen()) governor.setListen(false);

        if (task != null && task.entity != null && level.getEntity(task.entity) == null) abortTask();

        // appointed from somewhere else: walk to the claim first, then start governing
        if (travelToClaim(level, claim)) return;

        long day = level.getDayTime() / 24000L;
        if (governor.getLastXpDay() != day) {
            governor.setLastXpDay(day);
            governor.addGovernorXp(XP_PER_DAY);
        }

        boolean slowTick = intervalCounter % Math.max(1, 200 / interval) == 0;
        if (slowTick || intervalCounter == 1) checkContainers(level, data);
        checkSiege(level, claim, data);

        UUID owner = governor.getOwnerUUID();
        List<AbstractRecruitEntity> garrison = GovernorManager.findGarrison(level, claim, owner);
        Map<UUID, AbstractRecruitEntity> byId = new HashMap<>();
        for (AbstractRecruitEntity recruit : garrison) byId.put(recruit.getUUID(), recruit);

        cleanupAssignments(level, data, byId);
        updateBreaks(level, claim, data, byId);
        updatePatrols(data, byId, interval);
        distribute(level, data, garrison, byId);

        int patrolInterval = RecruitsServerConfig.GovernorPatrolInterval.get();
        if (intervalCounter % Math.max(1, patrolInterval / interval) == 0) startPatrol(level, data);

        setSiegeEngineerPriority(level, claim, garrison);
        if (slowTick || intervalCounter == 1) manageUpkeepAndLoadout(level, claim, data, garrison);
        keepGovernorAtPost(level, data);

        if (task == null) planTask(level, claim, data, garrison);
    }

    private void stopRuling(ServerLevel level, @Nullable ClaimGovernorData data, @Nullable RecruitsClaim claim, GovernorManager.RuleStatus status) {
        boolean registered = data != null && governor.getUUID().equals(data.getGovernorId());
        if (registered) {
            data.clearGovernor();
        }
        // siege and claim removal are reported by GovernorEvents, so only report what this governor noticed itself
        if (registered) {
            String claimName = claim != null ? claim.getName() : "?";
            GovernorManager.notifyPlayer(level, governor.getOwnerUUID(), Component.translatable("chat.recruits.governor.stopped_ruling",
                    governor.getName().getString(), claimName,
                    Component.translatable("gui.recruits.governor.status." + status.getKey())));
        }
        abortTask();
        wasRuling = false;
        travelling = false;
        postTreasury = null;
        postStand = null;
        governor.setRuling(false);
        governor.setGovernedClaim(null);
        governor.setHomePos(null);
        governor.setListen(true);
    }

    // ------------------------------------------------------------------ chests and notices

    private void checkContainers(ServerLevel level, ClaimGovernorData data) {
        BlockPos treasury = data.getTreasuryPos();
        if (treasury == null) {
            data.treasuryMissing = true;
            data.lastKnownTreasury = -1;
        } else if (level.isLoaded(treasury)) {
            Container container = GovernorGarrison.getContainer(level, treasury);
            if (container == null) {
                data.treasuryMissing = true;
                data.lastKnownTreasury = -1;
                if (!data.treasuryMissingNotified) {
                    data.treasuryMissingNotified = true;
                    notifyOwner(level, Component.translatable("chat.recruits.governor.treasury_missing",
                            governor.getName().getString(), treasury.getX(), treasury.getY(), treasury.getZ()));
                }
            } else {
                data.treasuryMissing = false;
                data.treasuryMissingNotified = false;
                data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
            }
        }

        BlockPos missingUpkeep = null;
        boolean anyUpkeep = false;
        for (GovernorPost upkeep : data.getPosts(GovernorPost.Type.UPKEEP)) {
            anyUpkeep = true;
            if (level.isLoaded(upkeep.getPos()) && !GovernorGarrison.isContainer(level, upkeep.getPos())) {
                missingUpkeep = upkeep.getPos();
                break;
            }
        }
        data.upkeepMissing = !anyUpkeep || missingUpkeep != null;
        if (missingUpkeep != null) {
            if (!data.upkeepMissingNotified) {
                data.upkeepMissingNotified = true;
                notifyOwner(level, Component.translatable("chat.recruits.governor.upkeep_missing",
                        governor.getName().getString(), missingUpkeep.getX(), missingUpkeep.getY(), missingUpkeep.getZ()));
            }
        } else {
            data.upkeepMissingNotified = false;
        }
    }

    private void checkSiege(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data) {
        if (claim.isUnderSiege) {
            if (!data.siegeNotified) {
                data.siegeNotified = true;
                notifyOwner(level, Component.translatable("chat.recruits.governor.under_siege", governor.getName().getString(), claim.getName()));
            }
        } else {
            data.siegeNotified = false;
        }
    }

    private void notifyOwner(ServerLevel level, Component message) {
        GovernorManager.notifyPlayer(level, governor.getOwnerUUID(), message);
    }

    // ------------------------------------------------------------------ distribution

    private void cleanupAssignments(ServerLevel level, ClaimGovernorData data, Map<UUID, AbstractRecruitEntity> byId) {
        Set<UUID> seen = new HashSet<>();
        for (GovernorPost post : data.getPosts()) {
            UUID assigned = post.getAssigned();
            if (assigned == null) continue;
            if (!post.getType().isGuardPost()) {
                post.setAssigned(null);
                continue;
            }
            AbstractRecruitEntity recruit = byId.get(assigned);
            boolean missing = recruit == null && level.isLoaded(post.getPos());
            boolean wrongType = recruit != null && !fits(recruit, post.getType());
            if (missing || wrongType || !seen.add(assigned)) {
                post.setAssigned(null);
            }
        }

        Iterator<ClaimGovernorData.Patrol> iterator = data.patrols.iterator();
        while (iterator.hasNext()) {
            ClaimGovernorData.Patrol patrol = iterator.next();
            patrol.members.removeIf(id -> !byId.containsKey(id) || seen.contains(id));
            patrol.removeMissingPoints(data);
            if (patrol.members.isEmpty() || patrol.getCurrentPoint() == null) iterator.remove();
        }
    }

    private static boolean fits(AbstractRecruitEntity recruit, GovernorPost.Type postType) {
        GovernorUnitType type = GovernorUnitType.of(recruit);
        return type != null && type.fitsPost(postType);
    }

    /** Patrols walk from point to point; at each point they stay a moment, after the last one they return. */
    private void updatePatrols(ClaimGovernorData data, Map<UUID, AbstractRecruitEntity> byId, int interval) {
        int dwell = RecruitsServerConfig.GovernorPatrolDwellTicks.get();
        Iterator<ClaimGovernorData.Patrol> iterator = data.patrols.iterator();
        while (iterator.hasNext()) {
            ClaimGovernorData.Patrol patrol = iterator.next();
            BlockPos current = patrol.getCurrentPoint();
            if (current == null) {
                iterator.remove();
                continue;
            }
            Vec3 point = Vec3.atBottomCenterOf(current);

            boolean next = false;
            if (!patrol.arrived) {
                patrol.legTicks += interval;
                for (UUID id : patrol.members) {
                    AbstractRecruitEntity member = byId.get(id);
                    if (member != null && member.position().distanceToSqr(point) < 16D) {
                        patrol.arrived = true;
                        patrol.dwellLeft = dwell;
                        break;
                    }
                }
                // nobody gets there: skip this point
                if (!patrol.arrived && patrol.legTicks > PATROL_TIMEOUT) next = true;
            } else {
                patrol.dwellLeft -= interval;
                if (patrol.dwellLeft <= 0) next = true;
            }

            if (next && !patrol.advance()) {
                // route done: back to the reserve
                iterator.remove();
            }
        }
    }

    // ------------------------------------------------------------------ upkeep breaks of the guards

    /**
     * Guards can not leave their post on their own, so the Governor sends them on an upkeep break: a hungry guard
     * without food (or a wounded one that can not heal because he is too hungry) leaves his post, walks to the
     * nearest upkeep chest, eats and waits there until he is fed and healed. Meanwhile the nearest free recruit of
     * the reserve takes over the post. At most a third of the guards are on break at the same time, during a
     * siege only starving ones.
     */
    private void updateBreaks(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, Map<UUID, AbstractRecruitEntity> byId) {
        long now = level.getGameTime();

        // 1. breaks that are over
        Iterator<Map.Entry<UUID, Long>> iterator = breaks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            AbstractRecruitEntity recruit = byId.get(entry.getKey());
            if (recruit == null) {
                iterator.remove();
                continue;
            }
            boolean timeout = now - entry.getValue() > BREAK_TIMEOUT;
            boolean fed = !recruit.needsToGetFood() && !recruit.upkeepInProgress && recruit.getHunger() >= BREAK_DONE_HUNGER;
            // healing needs hunger >= 70, so a fed recruit heals; one that is still hungry stops waiting at the timeout
            boolean healed = recruit.getHealth() >= recruit.getMaxHealth() * 0.9F;
            if (timeout || (fed && healed)) {
                recruit.forcedUpkeep = false;
                if (!fed) breakCooldown.put(entry.getKey(), now + BREAK_RETRY_DELAY);
                iterator.remove();
            }
        }
        breakCooldown.values().removeIf(until -> until <= now);

        // 2. send hungry or wounded guards on a break
        List<GovernorPost> upkeeps = data.getPosts(GovernorPost.Type.UPKEEP);
        if (upkeeps.isEmpty()) return;
        int guardPosts = 0;
        for (GovernorPost post : data.getPosts()) if (post.getType().isGuardPost()) guardPosts++;
        int maxBreaks = Math.max(1, guardPosts / 3);

        for (GovernorPost post : data.getPosts()) {
            if (breaks.size() >= maxBreaks) break;
            UUID id = post.getAssigned();
            if (id == null || breaks.containsKey(id) || breakCooldown.containsKey(id)) continue;
            AbstractRecruitEntity recruit = byId.get(id);
            if (recruit == null || !needsBreak(recruit)) continue;
            boolean starving = recruit.getHunger() < 10F;
            if (claim.isUnderSiege && !starving) continue;

            GovernorPost upkeep = data.getNearest(GovernorPost.Type.UPKEEP, post.getStandPos());
            if (upkeep == null) continue;

            post.setAssigned(null);
            breaks.put(id, now);
            if (!upkeep.getPos().equals(recruit.getUpkeepPos())) {
                recruit.setUpkeepPos(upkeep.getPos());
                recruit.clearUpkeepEntity();
            }
            recruit.setUpkeepTimer(0);
            recruit.forcedUpkeep = true;
            // after eating he waits at the chest, not on his old post
            holdNear(recruit, Vec3.atBottomCenterOf(upkeep.getPos()));
        }
    }

    /** Hungry, or wounded and too hungry to heal - and nothing to eat in his pockets. */
    private static boolean needsBreak(AbstractRecruitEntity recruit) {
        if (recruit.getTarget() != null || recruit.hasFoodInInv()) return false;
        boolean hungry = recruit.getHunger() < 50F;
        boolean cannotHeal = recruit.getHealth() < recruit.getMaxHealth() && recruit.getHunger() < 70F;
        return hungry || cannotHeal;
    }

    private void distribute(ServerLevel level, ClaimGovernorData data, List<AbstractRecruitEntity> garrison, Map<UUID, AbstractRecruitEntity> byId) {
        Set<UUID> busy = new HashSet<>();
        for (GovernorPost post : data.getPosts()) if (post.getAssigned() != null) busy.add(post.getAssigned());
        for (ClaimGovernorData.Patrol patrol : data.patrols) busy.addAll(patrol.members);
        busy.addAll(breaks.keySet());

        List<AbstractRecruitEntity> free = new ArrayList<>();
        for (AbstractRecruitEntity recruit : garrison) if (!busy.contains(recruit.getUUID())) free.add(recruit);

        // 1. fill free guard posts with the nearest fitting free recruit; already assigned recruits are never re-sorted
        for (GovernorPost post : data.getPosts()) {
            if (!post.isFree()) continue;
            Vec3 stand = post.getStandPos();
            AbstractRecruitEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (AbstractRecruitEntity recruit : free) {
                if (!fits(recruit, post.getType())) continue;
                double dist = recruit.position().distanceToSqr(stand);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = recruit;
                }
            }
            if (best != null) {
                post.setAssigned(best.getUUID());
                free.remove(best);
            }
        }

        // 2. keep guards exactly on their posts
        for (GovernorPost post : data.getPosts()) {
            if (post.getAssigned() == null) continue;
            AbstractRecruitEntity recruit = byId.get(post.getAssigned());
            if (recruit != null) holdAt(recruit, post.getStandPos(), 0.1D);
        }

        // 3. patrols: the members walk to the current point of their route, side by side
        for (ClaimGovernorData.Patrol patrol : data.patrols) {
            BlockPos current = patrol.getCurrentPoint();
            if (current == null) continue;
            Vec3 point = Vec3.atBottomCenterOf(current);
            for (int i = 0; i < patrol.members.size(); i++) {
                AbstractRecruitEntity recruit = byId.get(patrol.members.get(i));
                if (recruit != null) holdAt(recruit, point.add(patrolOffset(i, patrol.members.size())), 0.5D);
            }
        }

        // 4. everyone else gathers at the nearest reserve, or around the Governor's post if there is none
        List<GovernorPost> reserves = data.getPosts(GovernorPost.Type.RESERVE);
        Vec3 governorPost = getGovernorPost(level, data);
        for (AbstractRecruitEntity recruit : free) {
            Vec3 center;
            if (reserves.isEmpty()) {
                center = governorPost;
            } else {
                GovernorPost nearest = data.getNearest(GovernorPost.Type.RESERVE, recruit.position());
                center = nearest != null ? nearest.getStandPos() : governorPost;
            }
            holdNear(recruit, center);
        }

        lastFree.clear();
        lastFree.addAll(free);
    }

    private static void holdAt(AbstractRecruitEntity recruit, Vec3 pos, double tolerance) {
        Vec3 hold = recruit.getHoldPos();
        if (recruit.getFollowState() != 3 || hold == null || hold.distanceToSqr(pos) > tolerance * tolerance) {
            recruit.setHoldPos(pos);
            recruit.setFollowState(3);
        }
    }

    /** Spreads reserve recruits around the reserve point so they do not all fight for one block. */
    private static void holdNear(AbstractRecruitEntity recruit, Vec3 center) {
        Vec3 hold = recruit.getHoldPos();
        if (recruit.getFollowState() == 3 && hold != null && hold.distanceToSqr(center) <= 16D) return;

        int hash = recruit.getUUID().hashCode();
        double angle = ((hash & 0xFFFF) / 65535D) * Math.PI * 2D;
        double radius = 1.5D + ((hash >>> 16) & 3) * 0.6D;
        Vec3 pos = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        recruit.setHoldPos(pos);
        recruit.setFollowState(3);
    }

    /** Members of a patrol stand next to each other at a point, not on one block. */
    private static Vec3 patrolOffset(int index, int size) {
        if (size <= 1) return Vec3.ZERO;
        double angle = Math.PI * 2D * index / size;
        return new Vec3(Math.cos(angle) * 1.2D, 0, Math.sin(angle) * 1.2D);
    }

    /**
     * Now and then a few recruits of the reserve walk through all patrol points of the claim, in any order:
     * always the nearest point not visited yet. Afterwards they return to the reserve.
     */
    private void startPatrol(ServerLevel level, ClaimGovernorData data) {
        List<GovernorPost> points = data.getPosts(GovernorPost.Type.PATROL);
        int maxPatrols = RecruitsServerConfig.GovernorMaxPatrols.get();
        if (points.isEmpty() || data.patrols.size() >= maxPatrols) return;

        List<AbstractRecruitEntity> reserve = new ArrayList<>(lastFree);
        // only garrison recruits patrol, never the Governor
        reserve.removeIf(r -> !r.isAlive() || r.getTarget() != null || r instanceof GovernorEntity);
        if (reserve.size() < 2) return;

        Random random = new Random(level.getGameTime() ^ governor.getId());
        int amount = Math.max(1, Math.min(2 + random.nextInt(2), reserve.size() / 2));
        Collections.shuffle(reserve, random);
        List<AbstractRecruitEntity> members = new ArrayList<>(reserve.subList(0, amount));

        // route: nearest unvisited point next, starting where the group stands
        List<GovernorPost> left = new ArrayList<>(points);
        List<UUID> route = new ArrayList<>();
        List<BlockPos> routePos = new ArrayList<>();
        Vec3 from = members.get(0).position();
        while (!left.isEmpty()) {
            GovernorPost nearest = null;
            double best = Double.MAX_VALUE;
            for (GovernorPost post : left) {
                double dist = post.getStandPos().distanceToSqr(from);
                if (dist < best) {
                    best = dist;
                    nearest = post;
                }
            }
            left.remove(nearest);
            route.add(nearest.getId());
            routePos.add(nearest.getPos());
            from = nearest.getStandPos();
        }

        ClaimGovernorData.Patrol patrol = new ClaimGovernorData.Patrol(route, routePos);
        Vec3 target = Vec3.atBottomCenterOf(routePos.get(0));
        for (int i = 0; i < members.size(); i++) {
            AbstractRecruitEntity recruit = members.get(i);
            patrol.members.add(recruit.getUUID());
            holdAt(recruit, target.add(patrolOffset(i, members.size())), 0.5D);
            lastFree.remove(recruit);
        }
        data.patrols.add(patrol);
    }

    // ------------------------------------------------------------------ the Governor's own post

    /** Without a task the Governor stands right at the treasury chest. */
    private void keepGovernorAtPost(ServerLevel level, ClaimGovernorData data) {
        if (task != null) return;
        holdAt(governor, getGovernorPost(level, data), 0.1D);
    }

    /** Where the Governor stands: in front of the treasury chest, without treasury at his home position. */
    private Vec3 getGovernorPost(ServerLevel level, ClaimGovernorData data) {
        BlockPos treasury = data.getTreasuryPos();
        if (treasury != null) {
            boolean recompute = postStand == null || !treasury.equals(postTreasury) || intervalCounter % 30 == 0;
            if (recompute && level.isLoaded(treasury)) {
                postStand = findStandPos(level, treasury);
                postTreasury = treasury;
            }
            if (postStand != null && treasury.equals(postTreasury)) return postStand;
        }
        if (governor.getHomePos() == null) governor.setHomePos(governor.blockPosition());
        return Vec3.atBottomCenterOf(governor.getHomePos());
    }

    /**
     * A free block next to the container to stand on: the side the chest opens to first, then the other sides,
     * each also one block lower or higher. Falls back to the top of the container.
     */
    public static Vec3 findStandPos(Level level, BlockPos container) {
        List<Direction> order = new ArrayList<>();
        BlockState state = level.getBlockState(container);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) order.add(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!order.contains(direction)) order.add(direction);
        }
        int[] heights = {0, -1, 1};
        for (Direction direction : order) {
            BlockPos side = container.relative(direction);
            for (int dy : heights) {
                BlockPos stand = side.offset(0, dy, 0);
                if (canStandAt(level, stand)) return Vec3.atBottomCenterOf(stand);
            }
        }
        return Vec3.atBottomCenterOf(container.above());
    }

    private static boolean canStandAt(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getFluidState(pos).isEmpty()
                && !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    // ------------------------------------------------------------------ siege engineers

    private void setSiegeEngineerPriority(ServerLevel level, RecruitsClaim claim, List<AbstractRecruitEntity> garrison) {
        List<SiegeEngineerEntity> engineers = new ArrayList<>();
        for (AbstractRecruitEntity recruit : garrison) if (recruit instanceof SiegeEngineerEntity engineer) engineers.add(engineer);
        if (engineers.isEmpty() || FactionEvents.recruitsDiplomacyManager == null) return;

        String factionId = claim.getOwnerFactionStringID();
        AABB box = GovernorManager.getClaimBox(level, claim, 16);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive()
                && living.getTeam() != null
                && FactionEvents.recruitsDiplomacyManager.getRelation(factionId, living.getTeam().getName()) == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY);

        boolean siegeWeapon = false;
        int infantry = 0;
        int cavalry = 0;
        for (LivingEntity enemy : enemies) {
            Entity vehicle = enemy.getVehicle();
            if (vehicle != null && SiegeWeapon.isSiegeWeapon(vehicle)) siegeWeapon = true;
            else if (vehicle instanceof AbstractHorse) cavalry++;
            else infantry++;
        }

        IHasTargetPriority.TargetPriority priority = null;
        if (siegeWeapon) priority = IHasTargetPriority.TargetPriority.SIEGE_WEAPONS;
        else if (cavalry > infantry) priority = IHasTargetPriority.TargetPriority.CAVALRY;
        else if (infantry > 0) priority = IHasTargetPriority.TargetPriority.INFANTRY;
        if (priority == null) return;

        for (SiegeEngineerEntity engineer : engineers) {
            if (engineer.getTargetPriority() != priority.getIndex()) engineer.setTargetPriority(priority);
        }
    }

    // ------------------------------------------------------------------ upkeep and loadout

    private void manageUpkeepAndLoadout(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, List<AbstractRecruitEntity> garrison) {
        // the Governor eats and takes his pay at his post: the treasury is his upkeep
        BlockPos governorUpkeep = data.getTreasuryPos();
        if (governorUpkeep == null) {
            GovernorPost nearest = data.getNearest(GovernorPost.Type.UPKEEP, governor.position());
            if (nearest != null) governorUpkeep = nearest.getPos();
        }
        if (governorUpkeep != null && !governorUpkeep.equals(governor.getUpkeepPos())) {
            governor.setUpkeepPos(governorUpkeep);
            governor.clearUpkeepEntity();
        }

        List<GovernorPost> upkeeps = data.getPosts(GovernorPost.Type.UPKEEP);
        if (upkeeps.isEmpty()) return;

        long now = level.getGameTime();
        Map<BlockPos, Container> containers = new HashMap<>();

        for (AbstractRecruitEntity recruit : garrison) {
            Vec3 anchor = recruit.getHoldPos() != null ? recruit.getHoldPos() : recruit.position();
            GovernorPost nearest = data.getNearest(GovernorPost.Type.UPKEEP, anchor);
            if (nearest == null) continue;

            if (recruit.getUpkeepPos() == null || !recruit.getUpkeepPos().equals(nearest.getPos())) {
                recruit.setUpkeepPos(nearest.getPos());
                recruit.clearUpkeepEntity();
            }

            if (claim.isUnderSiege) continue;
            if (recruit.getTarget() != null || recruit.forcedUpkeep) continue;
            Long last = lastLoadoutCall.get(recruit.getUUID());
            if (last != null && now - last < LOADOUT_COOLDOWN) continue;

            Container container = containers.computeIfAbsent(nearest.getPos(), pos -> GovernorGarrison.getContainer(level, pos));
            if (container != null && GovernorGarrison.wantsLoadoutChange(recruit, data, container)) {
                recruit.forcedUpkeep = true;
                lastLoadoutCall.put(recruit.getUUID(), now);
            }
        }
        lastLoadoutCall.keySet().removeIf(id -> now - lastLoadoutCall.get(id) > LOADOUT_COOLDOWN * 4L);
    }

    // ------------------------------------------------------------------ tasks

    @Nullable
    public Vec3 getTaskTarget() {
        Task current = task;
        if (current == null) return null;
        if (current.entity != null && governor.getCommandSenderWorld() instanceof ServerLevel level) {
            Entity entity = level.getEntity(current.entity);
            return entity == null ? null : entity.position();
        }
        if (current.pos == null) return null;
        // chest tasks: walk to the stand position at the chest, not into it
        if ((current.type == TaskType.WITHDRAW || current.type == TaskType.DEPOSIT) && postStand != null && current.pos.equals(postTreasury)) {
            return postStand;
        }
        return Vec3.atCenterOf(current.pos);
    }

    public void onTaskTargetReached() {
        if (task == null || !(governor.getCommandSenderWorld() instanceof ServerLevel level)) return;
        GovernorManager manager = GovernorManager.get();
        RecruitsClaim claim = GovernorManager.getClaim(governor.getGovernedClaim());
        if (manager == null || claim == null || !governor.isRuling()) {
            abortTask();
            return;
        }
        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        Task current = task;
        long day = level.getDayTime() / 24000L;

        switch (current.type) {
            case WITHDRAW -> {
                Container container = GovernorGarrison.getContainer(level, data.getTreasuryPos());
                if (container == null) {
                    abortTask();
                    return;
                }
                int removed = GovernorGarrison.removeCurrency(container, current.cost);
                if (removed < current.cost) {
                    GovernorGarrison.addCurrency(container, removed);
                    data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
                    abortTask();
                    return;
                }
                data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
                governor.setPurse(governor.getPurse() + current.cost);
                data.addSpent(day, current.cost);
                task = current.next;
            }
            case HIRE -> {
                boolean hired = hire(level, claim, data, current);
                if (!hired) data.addSpent(day, -current.cost);
                task = null;
            }
            case COLLECT_TAX -> {
                collectTaxes(level, claim, data, day);
                task = null;
            }
            case QUEST -> {
                handleQuest(level, claim, current);
                task = null;
            }
            case DEPOSIT -> {
                Container container = GovernorGarrison.getContainer(level, data.getTreasuryPos());
                if (container != null) {
                    int left = GovernorGarrison.addCurrency(container, governor.getPurse());
                    governor.setPurse(left);
                    // items of a delivery that did not happen go back into the treasury
                    Iterator<ItemStack> iterator = governor.getCarried().iterator();
                    while (iterator.hasNext()) {
                        ItemStack stack = iterator.next();
                        ItemStack rest = GovernorGarrison.insert(container, stack);
                        if (rest.isEmpty()) iterator.remove();
                        else stack.setCount(rest.getCount());
                    }
                    container.setChanged();
                    data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
                }
                task = null;
            }
        }
    }

    private void abortTask() {
        task = null;
    }

    /** Called when the Governor is appointed or removed from office. */
    public void reset() {
        task = null;
        breaks.clear();
        breakCooldown.clear();
        wasRuling = false;
        postTreasury = null;
        postStand = null;
        lastFree.clear();
    }

    public boolean isTravelling() {
        return travelling;
    }

    /**
     * Returns true while the Governor is outside his claim: he walks there in steps of up to 48 blocks
     * (his chunk has to be loaded, like for every recruit). Inside the claim his home is set once.
     */
    private boolean travelToClaim(ServerLevel level, RecruitsClaim claim) {
        if (GovernorManager.isInClaim(claim, governor.blockPosition())) {
            travelling = false;
            if (governor.getHomePos() == null || !GovernorManager.isInClaim(claim, governor.getHomePos())) {
                governor.setHomePos(governor.blockPosition());
            }
            return false;
        }

        travelling = true;
        abortTask();

        ChunkPos nearest = null;
        double best = Double.MAX_VALUE;
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            double dx = chunk.getMiddleBlockX() - governor.getX();
            double dz = chunk.getMiddleBlockZ() - governor.getZ();
            double dist = dx * dx + dz * dz;
            if (dist < best) {
                best = dist;
                nearest = chunk;
            }
        }
        if (nearest == null) return true;

        Vec3 from = governor.position();
        Vec3 to = new Vec3(nearest.getMiddleBlockX() + 0.5D, from.y, nearest.getMiddleBlockZ() + 0.5D);
        Vec3 step = to;
        if (from.distanceTo(to) > 48D) {
            step = from.add(to.subtract(from).normalize().scale(48D));
        }
        BlockPos stepPos = BlockPos.containing(step);
        double y = from.y;
        if (level.isLoaded(stepPos)) {
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, stepPos.getX(), stepPos.getZ());
        }
        holdAt(governor, new Vec3(step.x, y, step.z), 4D);
        return true;
    }

    private void planTask(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, List<AbstractRecruitEntity> garrison) {
        BlockPos treasury = data.getTreasuryPos();
        boolean treasuryOk = treasury != null && !data.treasuryMissing;
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long time = dayTime % 24000L;

        // the only time he leaves his post (apart from hiring): the villagers' meeting
        if (treasuryOk && time >= MEETING_START && time < MEETING_END) {
            if (planQuestTurnIn(level, claim, data)) return;
            if (planTaxes(level, claim, data, day, time)) return;
            if (planQuestAccept(level, claim, data, day)) return;
        }

        // carry money and items back first (taxes, refunds, rewards)
        if (governor.getPurse() > 0 || governor.hasCarried()) {
            if (treasuryOk) task = new Task(TaskType.DEPOSIT, treasury, null, null, 0, false);
            return;
        }

        if (treasuryOk) planRecruiting(level, claim, data, garrison, day);
        else data.setRecruitStatus("no_treasury", "");
    }

    // ------------------------------------------------------------------ taxes

    /** Taxes once a day at the meeting time, collected at the bell of the nearest village of the claim. */
    private boolean planTaxes(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, long day, long time) {
        if (data.getTaxLevel() <= 0 || data.getLastTaxDay() == day || time > 11000) return false;
        List<RecruitsVillage> villages = GovernorManager.getVillages(claim);
        if (villages.isEmpty()) return false;

        BlockPos bell = null;
        double best = Double.MAX_VALUE;
        for (RecruitsVillage village : villages) {
            double dist = village.getCenter().distSqr(governor.blockPosition());
            if (dist < best) {
                best = dist;
                bell = village.getCenter();
            }
        }
        data.setLastTaxDay(day);
        data.setTaxToday(0);
        task = new Task(TaskType.COLLECT_TAX, bell, null, null, 0, false);
        return true;
    }

    private void collectTaxes(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, long day) {
        int taxLevel = data.getTaxLevel();
        int inhabitants = GovernorManager.getInhabitants(level, claim);
        int amount = (inhabitants / 10) * taxLevel * RecruitsServerConfig.GovernorTaxPer10Inhabitants.get();
        data.setLastTaxDay(day);
        data.setTaxToday(amount);
        governor.setPurse(governor.getPurse() + amount);
        governor.addGovernorXp(XP_TAX);

        int penalty = switch (taxLevel) {
            case 1 -> RecruitsServerConfig.GovernorTaxReputationPenaltyLow.get();
            case 2 -> RecruitsServerConfig.GovernorTaxReputationPenaltyMedium.get();
            case 3 -> RecruitsServerConfig.GovernorTaxReputationPenaltyHigh.get();
            default -> 0;
        };
        UUID owner = governor.getOwnerUUID();
        if (owner == null) return;

        // the villagers do not like taxes: the owner loses reputation in every village of the claim
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (penalty > 0 && villageManager != null) {
            RecruitsVillageManager.Actor actor = new RecruitsVillageManager.Actor(owner, RecruitsVillageManager.getFactionOf(level.getServer(), owner));
            for (RecruitsVillage village : GovernorManager.getVillages(claim)) {
                villageManager.addReputation(village, actor, -penalty);
            }
        }
        MinecraftForge.EVENT_BUS.post(new GovernorEvent.TaxCollected(governor, claim, level, owner, taxLevel, amount, inhabitants, penalty));
    }

    // ------------------------------------------------------------------ village quests

    /** A finished quest of the owner from a village of the claim: go to a noble there and turn it in. */
    private boolean planQuestTurnIn(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        UUID owner = governor.getOwnerUUID();
        if (data.getQuestMode() <= 0 || villageManager == null || owner == null) return false;

        NobleQuestProgress progress = villageManager.getActiveQuest(owner);
        if (progress == null || !progress.completed || progress.isExpired(level.getGameTime())) return false;

        RecruitsVillage village = findClaimVillage(claim, progress.villageId);
        if (village == null) return false;
        VillagerNobleEntity noble = findNearestNoble(level, villageManager, village);
        if (noble == null) return false;

        Task turnIn = new Task(TaskType.QUEST, null, noble.getUUID(), null, 0, false);
        turnIn.village = village.getId();
        task = turnIn;
        return true;
    }

    /**
     * Once a day, when the owner's reputation in a village of the claim is below the chosen threshold and he has
     * no quest running: take a quest of that village for him. A delivery is preferred if the treasury holds the
     * items (the Governor brings them right away), otherwise a monster hunt the garrison can do. Never a bounty.
     */
    private boolean planQuestAccept(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, long day) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        UUID owner = governor.getOwnerUUID();
        if (data.getQuestMode() <= 0 || villageManager == null || owner == null || !RecruitsVillageManager.isQuestsEnabled()) return false;
        if (data.getLastQuestDay() == day) return false;

        NobleQuestProgress active = villageManager.getActiveQuest(owner);
        if (active != null && !active.isExpired(level.getGameTime())) return false;

        String faction = RecruitsVillageManager.getFactionOf(level.getServer(), owner);
        Container treasury = GovernorGarrison.getContainer(level, data.getTreasuryPos());

        for (RecruitsVillage village : GovernorManager.getVillages(claim)) {
            if (village.offersDay != day || village.offers.isEmpty()) continue;
            int reputation = villageManager.getEffectiveReputation(village, owner, faction);
            if (!data.wantsQuest(reputation)) continue;
            if (RecruitsVillageManager.isReputationEnabled() && NobleReputation.getTier(reputation) == NobleReputation.Tier.HOSTILE) continue;

            VillagerNobleEntity noble = findNearestNoble(level, villageManager, village);
            if (noble == null) continue;

            NobleQuest delivery = null;
            NobleQuest hunt = null;
            for (NobleQuest offer : village.offers) {
                if (offer.takenBy.contains(owner)) continue;
                if (offer.type == NobleQuest.Type.DELIVERY && delivery == null && treasury != null && countItems(treasury, offer) >= offer.count) delivery = offer;
                if (offer.type == NobleQuest.Type.MONSTER_HUNT && hunt == null) hunt = offer;
            }
            NobleQuest chosen = delivery != null ? delivery : hunt;
            if (chosen == null) continue;

            if (chosen == delivery) takeItems(treasury, chosen);

            data.setLastQuestDay(day);
            Task accept = new Task(TaskType.QUEST, null, noble.getUUID(), null, 0, false);
            accept.village = village.getId();
            accept.offer = chosen.id;
            task = accept;
            return true;
        }
        return false;
    }

    private void handleQuest(ServerLevel level, RecruitsClaim claim, Task current) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        UUID owner = governor.getOwnerUUID();
        if (villageManager == null || owner == null || current.village == null) return;
        RecruitsVillage village = findClaimVillage(claim, current.village);
        if (village == null) return;
        String faction = RecruitsVillageManager.getFactionOf(level.getServer(), owner);
        String name = governor.getName().getString();

        if (current.offer != null) {
            NobleQuestProgress progress = villageManager.acceptQuestFor(level, owner, faction, village, current.offer);
            if (progress == null) return;
            notifyOwner(level, Component.translatable("chat.recruits.governor.quest_accepted", name, progress.quest.getDescription()));
            if (progress.quest.type != NobleQuest.Type.DELIVERY) return;
            // he brought the items: hand them over right away
            governor.getCarried().clear();
        }

        NobleQuestProgress progress = villageManager.getActiveQuest(owner);
        if (progress == null) return;
        Component description = progress.quest.getDescription();
        int reward = villageManager.turnInQuestFor(level, owner, faction, village, current.offer != null);
        if (reward < 0) return;
        governor.setPurse(governor.getPurse() + reward);
        governor.addGovernorXp(XP_QUEST);
        notifyOwner(level, Component.translatable("chat.recruits.governor.quest_turned_in", name, description, claim.getName()));
    }

    @Nullable
    private static RecruitsVillage findClaimVillage(RecruitsClaim claim, @Nullable UUID villageId) {
        if (villageId == null) return null;
        for (RecruitsVillage village : GovernorManager.getVillages(claim)) {
            if (village.getId().equals(villageId)) return village;
        }
        return null;
    }

    @Nullable
    private VillagerNobleEntity findNearestNoble(ServerLevel level, RecruitsVillageManager villageManager, RecruitsVillage village) {
        VillagerNobleEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (VillagerNobleEntity noble : villageManager.getLoadedNobles(level, village)) {
            double dist = noble.distanceToSqr(governor);
            if (dist < bestDist) {
                bestDist = dist;
                best = noble;
            }
        }
        return best;
    }

    private static int countItems(Container container, NobleQuest quest) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (quest.matchesDeliveryItem(stack)) count += stack.getCount();
        }
        return count;
    }

    /** Moves the items of a delivery quest from the treasury into the Governor's hands. */
    private void takeItems(Container container, NobleQuest quest) {
        int left = quest.count;
        for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (!quest.matchesDeliveryItem(stack)) continue;
            int take = Math.min(left, stack.getCount());
            ItemStack taken = stack.copy();
            taken.setCount(take);
            governor.getCarried().add(taken);
            stack.shrink(take);
            if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
            left -= take;
        }
        container.setChanged();
    }

    // ------------------------------------------------------------------ recruiting

    private void planRecruiting(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, List<AbstractRecruitEntity> garrison, long day) {
        int[] counts = new int[GovernorUnitType.COUNT];
        for (AbstractRecruitEntity recruit : garrison) {
            GovernorUnitType type = GovernorUnitType.of(recruit);
            if (type != null) counts[type.getIndex()]++;
        }
        List<GovernorUnitType> needed = new ArrayList<>();
        for (GovernorUnitType type : GovernorUnitType.values()) {
            if (data.getQuota(type) > counts[type.getIndex()]) needed.add(type);
        }
        if (needed.isEmpty()) {
            data.setRecruitStatus(data.getTotalQuota() > 0 ? "quota_met" : "no_quota", "");
            return;
        }
        needed.sort(Comparator.comparingInt((GovernorUnitType type) -> data.getQuota(type) - counts[type.getIndex()]).reversed());

        int cap = GovernorManager.getGarrisonCap(claim);
        if (garrison.size() >= cap) {
            data.setRecruitStatus("cap_reached", cap);
            return;
        }
        String factionId = claim.getOwnerFactionStringID();
        if (!GovernorGarrison.canFactionAddNPC(factionId)) {
            data.setRecruitStatus("faction_limit", "");
            return;
        }
        int budget = data.getRemainingBudget(day);
        if (budget <= 0) {
            data.setRecruitStatus(data.getBudgetPerDay() <= 0 ? "no_budget" : "budget_spent", "");
            return;
        }
        int coins = data.lastKnownTreasury;
        if (coins <= 0) {
            data.setRecruitStatus("no_coins", "");
            return;
        }
        int limit = Math.min(budget, coins);
        UUID owner = governor.getOwnerUUID();
        RecruitsVillageManager villageManager = VillageEvents.villageManager;

        // free recruits in the claim
        AABB box = GovernorManager.getClaimBox(level, claim, 0);
        List<AbstractRecruitEntity> freeRecruits = new ArrayList<>();
        Set<VillagerNobleEntity> nobles = new LinkedHashSet<>();
        for (AbstractRecruitEntity entity : level.getEntitiesOfClass(AbstractRecruitEntity.class, box,
                e -> e.isAlive() && GovernorManager.isInClaim(claim, e.blockPosition()))) {
            if (entity instanceof VillagerNobleEntity noble) nobles.add(noble);
            else if (!entity.isOwned() && entity.canBeHired()) freeRecruits.add(entity);
        }
        // and the nobles of the villages of the claim (they may stand a little outside of it)
        if (villageManager != null) {
            for (RecruitsVillage village : GovernorManager.getVillages(claim)) nobles.addAll(villageManager.getLoadedNobles(level, village));
        }
        freeRecruits.sort(Comparator.comparingDouble(r -> r.distanceToSqr(governor)));

        boolean nobleWithTrade = false;
        boolean tooExpensive = false;
        boolean locked = false;
        boolean noVillagers = false;
        int cheapest = Integer.MAX_VALUE;

        for (GovernorUnitType type : needed) {
            // 1. a free recruit in the claim
            for (AbstractRecruitEntity candidate : freeRecruits) {
                if (GovernorUnitType.of(candidate) != type) continue;
                if (candidate.getCost() <= limit) {
                    startHire(data, candidate.getUUID(), type, candidate.getCost(), false);
                    data.setRecruitStatus("hiring", type.getIndex());
                    return;
                }
                tooExpensive = true;
                cheapest = Math.min(cheapest, candidate.getCost());
            }

            // 2. a noble of a village in the claim
            for (VillagerNobleEntity noble : nobles) {
                RecruitsHireTrade trade = findTrade(noble, type);
                if (trade == null) continue;
                nobleWithTrade = true;
                int price = getPrice(level, owner, noble, trade);
                if (price < 0) {
                    locked = true;
                    continue;
                }
                if (price > limit) {
                    tooExpensive = true;
                    cheapest = Math.min(cheapest, price);
                    continue;
                }
                if (RecruitsServerConfig.NobleVillagerNeedsVillagers.get() && pickVillager(level, noble) == null) {
                    noVillagers = true;
                    continue;
                }
                startHire(data, noble.getUUID(), type, price, true);
                data.setRecruitStatus("hiring", type.getIndex());
                return;
            }
        }

        if (noVillagers) data.setRecruitStatus("no_villagers", RecruitsVillageManager.MIN_JOBLESS_FOR_HIRE);
        else if (tooExpensive) data.setRecruitStatus("too_expensive", cheapest == Integer.MAX_VALUE ? "?" : cheapest);
        else if (locked) data.setRecruitStatus("locked", "");
        else if (nobles.isEmpty() && freeRecruits.isEmpty()) data.setRecruitStatus("no_noble", "");
        else if (!nobleWithTrade) data.setRecruitStatus("no_trade", "");
        else data.setRecruitStatus("no_source", "");
    }

    private void startHire(ClaimGovernorData data, UUID source, GovernorUnitType type, int cost, boolean fromNoble) {
        Task withdraw = new Task(TaskType.WITHDRAW, data.getTreasuryPos(), null, type, cost, fromNoble);
        withdraw.next = new Task(TaskType.HIRE, null, source, type, cost, fromNoble);
        task = withdraw;
    }

    /** Price for the owner: the noble's reputation price, −1 if the owner may not buy it (embargo, reputation). */
    private int getPrice(ServerLevel level, @Nullable UUID owner, VillagerNobleEntity noble, RecruitsHireTrade trade) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (villageManager == null || owner == null) return isEmbargoed(noble) ? -1 : trade.cost;
        return villageManager.getHirePrice(level.getServer(), owner, noble, trade);
    }

    @Nullable
    private static Villager pickVillager(ServerLevel level, VillagerNobleEntity noble) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (villageManager == null) return null;
        return villageManager.pickVillagerForHire(level, noble);
    }

    @Nullable
    private static RecruitsHireTrade findTrade(VillagerNobleEntity noble, GovernorUnitType type) {
        for (RecruitsHireTrade trade : noble.getTrades()) {
            if (trade != null && trade.uses > 0 && type.getEntityId().equals(trade.resourceLocation)) return trade;
        }
        return null;
    }

    private boolean isEmbargoed(VillagerNobleEntity noble) {
        UUID owner = governor.getOwnerUUID();
        String nobleTeam = noble.getEmbargoTeamID();
        if (owner == null || nobleTeam == null || nobleTeam.isEmpty() || FactionEvents.recruitsDiplomacyManager == null) return false;
        return FactionEvents.recruitsDiplomacyManager.hasEmbargo(owner, nobleTeam);
    }

    private boolean hire(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, Task current) {
        UUID owner = governor.getOwnerUUID();
        if (owner == null || current.unitType == null || current.entity == null) return false;
        String factionId = claim.getOwnerFactionStringID();
        if (!GovernorGarrison.canFactionAddNPC(factionId)) return false;
        if (GovernorManager.findGarrison(level, claim, owner).size() >= GovernorManager.getGarrisonCap(claim)) return false;

        String ownerName = governor.getOwnerName();
        Entity source = level.getEntity(current.entity);
        AbstractRecruitEntity recruit;
        int paid = current.cost;

        if (!current.fromNoble) {
            if (!(source instanceof AbstractRecruitEntity free) || !free.isAlive() || free.isOwned()
                    || GovernorUnitType.of(free) != current.unitType) return false;
            recruit = free;
            GovernorGarrison.hireIntoGarrison(level, recruit, owner, ownerName, factionId, claim.getUUID());
        } else {
            if (!(source instanceof VillagerNobleEntity noble) || !noble.isAlive()) return false;
            RecruitsHireTrade trade = findTrade(noble, current.unitType);
            if (trade == null) return false;
            int price = getPrice(level, owner, noble, trade);
            if (price < 0 || price > governor.getPurse()) return false;
            paid = price;

            Villager villager = null;
            if (RecruitsServerConfig.NobleVillagerNeedsVillagers.get()) {
                villager = pickVillager(level, noble);
                if (villager == null) return false;
            }

            recruit = current.unitType.getEntityType().create(level);
            if (recruit == null) return false;
            recruit.initSpawn();

            if (villager != null) {
                AbstractRecruitEntity.applyVariantFromVillager(recruit, villager);
                if (villager.getCustomName() != null && !villager.getCustomName().getString().isEmpty()) {
                    recruit.setCustomName(villager.getCustomName());
                }
                if (villager.isSleeping()) villager.stopSleeping();
                recruit.copyPosition(villager);
                for (ItemStack stack : villager.getInventory().items) {
                    if (!stack.isEmpty()) recruit.getInventory().addItem(stack.copy());
                }
                if (RecruitsServerConfig.RecruitTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
                villager.releasePoi(MemoryModuleType.HOME);
                villager.releasePoi(MemoryModuleType.MEETING_POINT);
                villager.discard();
            } else {
                recruit.copyPosition(noble);
            }

            GovernorGarrison.hireIntoGarrison(level, recruit, owner, ownerName, factionId, claim.getUUID());
            level.addFreshEntity(recruit);

            if (recruit instanceof ICompanion) {
                for (int i = 0; i < 4; i++) {
                    recruit.addXp(RecruitsServerConfig.RecruitsMaxXpForLevelUp.get());
                    recruit.checkLevel();
                }
            }
            noble.doTrade(trade.resourceLocation);
        }

        governor.setPurse(governor.getPurse() - paid);
        // paid less than withdrawn (cheaper by now): the rest goes back with the next deposit, the budget only counts what was paid
        if (paid < current.cost) data.addSpent(level.getDayTime() / 24000L, paid - current.cost);
        governor.addGovernorXp(XP_HIRE);
        recruit.setHoldPos(governor.position());
        recruit.setFollowState(3);
        data.setRecruitStatus("hired", current.unitType.getIndex());
        return true;
    }
}
