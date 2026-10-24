package com.talhanation.recruits.governor;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.GovernorEvent;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
 * upkeep/loadout, and plans one task at a time (withdraw, hire, collect taxes, deposit).
 */
public class GovernorController {

    private static final int TASK_TIMEOUT = 1200;
    private static final int LOADOUT_COOLDOWN = 600;
    private static final int PATROL_TIMEOUT = 2400;

    private enum TaskType {WITHDRAW, HIRE, COLLECT_TAX, DEPOSIT}

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
    @Nullable
    private Task task;
    private int intervalCounter;
    private boolean wasRuling;
    private boolean travelling;

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
        governor.setRuling(true);
        wasRuling = true;
        if (governor.getListen()) governor.setListen(false);

        if (task != null && task.entity != null && level.getEntity(task.entity) == null) abortTask();

        // appointed from somewhere else: walk to the claim first, then start governing
        if (travelToClaim(level, claim)) return;

        boolean slowTick = intervalCounter % Math.max(1, 200 / interval) == 0;
        if (slowTick || intervalCounter == 1) checkContainers(level, data);
        checkSiege(level, claim, data);

        UUID owner = governor.getOwnerUUID();
        List<AbstractRecruitEntity> garrison = GovernorManager.findGarrison(level, claim, owner);
        Map<UUID, AbstractRecruitEntity> byId = new HashMap<>();
        for (AbstractRecruitEntity recruit : garrison) byId.put(recruit.getUUID(), recruit);

        cleanupAssignments(level, data, byId);
        updatePatrols(data, byId, interval);
        distribute(data, garrison, byId);

        int patrolInterval = RecruitsServerConfig.GovernorPatrolInterval.get();
        if (intervalCounter % Math.max(1, patrolInterval / interval) == 0) startPatrol(level, data);

        setSiegeEngineerPriority(level, claim, garrison);
        if (slowTick) manageUpkeepAndLoadout(level, claim, data, garrison);
        keepGovernorAtHome(data);

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
            if (patrol.members.isEmpty() || data.getPost(patrol.pointId) == null) iterator.remove();
        }
    }

    private static boolean fits(AbstractRecruitEntity recruit, GovernorPost.Type postType) {
        GovernorUnitType type = GovernorUnitType.of(recruit);
        return type != null && type.fitsPost(postType);
    }

    private void updatePatrols(ClaimGovernorData data, Map<UUID, AbstractRecruitEntity> byId, int interval) {
        int dwell = RecruitsServerConfig.GovernorPatrolDwellTicks.get();
        Iterator<ClaimGovernorData.Patrol> iterator = data.patrols.iterator();
        while (iterator.hasNext()) {
            ClaimGovernorData.Patrol patrol = iterator.next();
            patrol.ageTicks += interval;
            Vec3 point = Vec3.atBottomCenterOf(patrol.point);

            if (!patrol.arrived) {
                for (UUID id : patrol.members) {
                    AbstractRecruitEntity member = byId.get(id);
                    if (member != null && member.position().distanceToSqr(point) < 16D) {
                        patrol.arrived = true;
                        patrol.dwellLeft = dwell;
                        break;
                    }
                }
            } else {
                patrol.dwellLeft -= interval;
            }

            if ((patrol.arrived && patrol.dwellLeft <= 0) || patrol.ageTicks > PATROL_TIMEOUT + dwell) {
                iterator.remove();
            }
        }
    }

    private void distribute(ClaimGovernorData data, List<AbstractRecruitEntity> garrison, Map<UUID, AbstractRecruitEntity> byId) {
        Set<UUID> busy = new HashSet<>();
        for (GovernorPost post : data.getPosts()) if (post.getAssigned() != null) busy.add(post.getAssigned());
        for (ClaimGovernorData.Patrol patrol : data.patrols) busy.addAll(patrol.members);

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

        // 2. keep guards on their posts
        for (GovernorPost post : data.getPosts()) {
            if (post.getAssigned() == null) continue;
            AbstractRecruitEntity recruit = byId.get(post.getAssigned());
            if (recruit != null) holdAt(recruit, post.getStandPos(), 0.75D);
        }

        // 3. patrols
        for (ClaimGovernorData.Patrol patrol : data.patrols) {
            Vec3 point = Vec3.atBottomCenterOf(patrol.point);
            for (UUID id : patrol.members) {
                AbstractRecruitEntity recruit = byId.get(id);
                if (recruit != null) holdAt(recruit, point, 2.5D);
            }
        }

        // 4. everyone else gathers at the nearest reserve, or at the governor if there is none
        List<GovernorPost> reserves = data.getPosts(GovernorPost.Type.RESERVE);
        for (AbstractRecruitEntity recruit : free) {
            Vec3 center;
            if (reserves.isEmpty()) {
                center = governor.position();
            } else {
                GovernorPost nearest = data.getNearest(GovernorPost.Type.RESERVE, recruit.position());
                center = nearest != null ? nearest.getStandPos() : governor.position();
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

    private void startPatrol(ServerLevel level, ClaimGovernorData data) {
        List<GovernorPost> points = data.getPosts(GovernorPost.Type.PATROL);
        int maxPatrols = RecruitsServerConfig.GovernorMaxPatrols.get();
        if (points.isEmpty() || data.patrols.size() >= maxPatrols) return;

        List<AbstractRecruitEntity> reserve = new ArrayList<>(lastFree);
        reserve.removeIf(r -> !r.isAlive() || r.getTarget() != null);
        if (reserve.size() < 2) return;

        Random random = new Random(level.getGameTime() ^ governor.getId());
        List<GovernorPost> candidates = new ArrayList<>(points);
        if (candidates.size() > 1 && data.lastPatrolPoint != null) {
            candidates.removeIf(p -> p.getId().equals(data.lastPatrolPoint));
        }
        GovernorPost point = candidates.get(random.nextInt(candidates.size()));

        int amount = Math.max(1, Math.min(1 + random.nextInt(3), reserve.size() / 2));
        Collections.shuffle(reserve, random);

        ClaimGovernorData.Patrol patrol = new ClaimGovernorData.Patrol(point.getId(), point.getPos(), RecruitsServerConfig.GovernorPatrolDwellTicks.get());
        Vec3 target = point.getStandPos();
        for (int i = 0; i < amount; i++) {
            AbstractRecruitEntity recruit = reserve.get(i);
            patrol.members.add(recruit.getUUID());
            holdAt(recruit, target, 2.5D);
            lastFree.remove(recruit);
        }
        data.patrols.add(patrol);
        data.lastPatrolPoint = point.getId();
    }

    private void keepGovernorAtHome(ClaimGovernorData data) {
        if (task != null) return;
        if (governor.getHomePos() == null) governor.setHomePos(governor.blockPosition());
        Vec3 home = Vec3.atBottomCenterOf(governor.getHomePos());
        GovernorPost reserve = data.getNearest(GovernorPost.Type.RESERVE, home);
        holdAt(governor, reserve != null ? reserve.getStandPos() : home, 1.5D);
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
        List<GovernorPost> upkeeps = data.getPosts(GovernorPost.Type.UPKEEP);
        if (upkeeps.isEmpty()) return;

        long now = level.getGameTime();
        Map<BlockPos, Container> containers = new HashMap<>();

        List<AbstractRecruitEntity> all = new ArrayList<>(garrison);
        all.add(governor);
        for (AbstractRecruitEntity recruit : all) {
            Vec3 anchor = recruit.getHoldPos() != null ? recruit.getHoldPos() : recruit.position();
            GovernorPost nearest = data.getNearest(GovernorPost.Type.UPKEEP, anchor);
            if (nearest == null) continue;

            if (recruit.getUpkeepPos() == null || !recruit.getUpkeepPos().equals(nearest.getPos())) {
                recruit.setUpkeepPos(nearest.getPos());
                recruit.clearUpkeepEntity();
            }

            if (recruit == governor || claim.isUnderSiege) continue;
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
        return current.pos == null ? null : Vec3.atCenterOf(current.pos);
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
            case DEPOSIT -> {
                Container container = GovernorGarrison.getContainer(level, data.getTreasuryPos());
                if (container != null) {
                    int left = GovernorGarrison.addCurrency(container, governor.getPurse());
                    governor.setPurse(left);
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
        wasRuling = false;
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

        // carry money back first (taxes, refunds)
        if (governor.getPurse() > 0) {
            if (treasuryOk) task = new Task(TaskType.DEPOSIT, treasury, null, null, 0, false);
            return;
        }

        // taxes once a day at the villager meeting time
        if (treasuryOk && data.getTaxLevel() > 0 && data.getLastTaxDay() != day && time >= 9000 && time <= 11000) {
            List<BlockPos> bells = GovernorManager.findBells(level, claim);
            if (!bells.isEmpty()) {
                data.setLastTaxDay(day);
                data.setTaxToday(0);
                BlockPos bell = bells.stream().min(Comparator.comparingDouble(pos -> pos.distSqr(governor.blockPosition()))).get();
                task = new Task(TaskType.COLLECT_TAX, bell, null, null, 0, false);
                return;
            }
        }

        if (treasuryOk) planRecruiting(level, claim, data, garrison, day);
    }

    private void planRecruiting(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, List<AbstractRecruitEntity> garrison, long day) {
        int coins = data.lastKnownTreasury;
        if (coins <= 0) return;
        if (garrison.size() >= GovernorManager.getGarrisonCap(claim)) return;
        String factionId = claim.getOwnerFactionStringID();
        if (!GovernorGarrison.canFactionAddNPC(factionId)) return;
        int budget = data.getRemainingBudget(day);
        if (budget <= 0) return;

        int[] counts = new int[GovernorUnitType.COUNT];
        for (AbstractRecruitEntity recruit : garrison) {
            GovernorUnitType type = GovernorUnitType.of(recruit);
            if (type != null) counts[type.getIndex()]++;
        }

        List<GovernorUnitType> needed = new ArrayList<>();
        for (GovernorUnitType type : GovernorUnitType.values()) {
            if (data.getQuota(type) > counts[type.getIndex()]) needed.add(type);
        }
        if (needed.isEmpty()) return;
        needed.sort(Comparator.comparingInt((GovernorUnitType type) -> data.getQuota(type) - counts[type.getIndex()]).reversed());

        int limit = Math.min(budget, coins);
        AABB box = GovernorManager.getClaimBox(level, claim, 0);

        // one scan for both sources
        List<AbstractRecruitEntity> freeRecruits = new ArrayList<>();
        List<VillagerNobleEntity> nobles = new ArrayList<>();
        for (AbstractRecruitEntity entity : level.getEntitiesOfClass(AbstractRecruitEntity.class, box,
                e -> e.isAlive() && GovernorManager.isInClaim(claim, e.blockPosition()))) {
            if (entity instanceof VillagerNobleEntity noble) nobles.add(noble);
            else if (!entity.isOwned() && entity.canBeHired()) freeRecruits.add(entity);
        }
        freeRecruits.sort(Comparator.comparingDouble(r -> r.distanceToSqr(governor)));

        for (GovernorUnitType type : needed) {
            // 1. a free recruit in the claim
            AbstractRecruitEntity free = null;
            for (AbstractRecruitEntity candidate : freeRecruits) {
                if (GovernorUnitType.of(candidate) == type) {
                    free = candidate;
                    break;
                }
            }
            if (free != null && free.getCost() <= limit) {
                startHire(data, free.getUUID(), type, free.getCost(), false);
                return;
            }

            // 2. a noble of the village in the claim
            for (VillagerNobleEntity noble : nobles) {
                RecruitsHireTrade trade = findTrade(noble, type);
                if (trade == null || trade.cost > limit) continue;
                if (isEmbargoed(noble)) continue;
                if (RecruitsServerConfig.NobleVillagerNeedsVillagers.get() && findJoblessVillagers(level, noble).size() <= 2) continue;
                startHire(data, noble.getUUID(), type, trade.cost, true);
                return;
            }
        }
    }

    private void startHire(ClaimGovernorData data, UUID source, GovernorUnitType type, int cost, boolean fromNoble) {
        Task withdraw = new Task(TaskType.WITHDRAW, data.getTreasuryPos(), null, type, cost, fromNoble);
        withdraw.next = new Task(TaskType.HIRE, null, source, type, cost, fromNoble);
        task = withdraw;
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
        if (owner == null || nobleTeam == null || FactionEvents.recruitsDiplomacyManager == null) return false;
        String embargoes = FactionEvents.recruitsDiplomacyManager.embargoMap.get(owner);
        if (embargoes == null) return false;
        for (String team : embargoes.split(",")) {
            if (team.trim().equals(nobleTeam)) return true;
        }
        return false;
    }

    private static List<Villager> findJoblessVillagers(ServerLevel level, VillagerNobleEntity noble) {
        return level.getEntitiesOfClass(Villager.class, noble.getBoundingBox().inflate(32D),
                v -> v.isAlive() && !v.isBaby() && v.getVillagerData().getProfession().equals(VillagerProfession.NONE));
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

        if (!current.fromNoble) {
            if (!(source instanceof AbstractRecruitEntity free) || !free.isAlive() || free.isOwned()
                    || GovernorUnitType.of(free) != current.unitType) return false;
            recruit = free;
            GovernorGarrison.hireIntoGarrison(level, recruit, owner, ownerName, factionId, claim.getUUID());
        } else {
            if (!(source instanceof VillagerNobleEntity noble) || !noble.isAlive()) return false;
            RecruitsHireTrade trade = findTrade(noble, current.unitType);
            if (trade == null) return false;

            recruit = current.unitType.getEntityType().create(level);
            if (recruit == null) return false;
            recruit.initSpawn();

            if (RecruitsServerConfig.NobleVillagerNeedsVillagers.get()) {
                List<Villager> villagers = findJoblessVillagers(level, noble);
                if (villagers.size() <= 2) return false;
                villagers.sort(Comparator.comparingDouble(v -> v.distanceToSqr(noble)));
                Villager villager = villagers.get(0);

                AbstractRecruitEntity.applyVariantFromVillager(recruit, villager);
                if (villager.getCustomName() != null && !villager.getCustomName().getString().isEmpty()) {
                    recruit.setCustomName(villager.getCustomName());
                }
                recruit.copyPosition(villager);
                for (ItemStack stack : villager.getInventory().items) {
                    if (!stack.isEmpty()) recruit.getInventory().addItem(stack.copy());
                }
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

        governor.setPurse(governor.getPurse() - current.cost);
        recruit.setHoldPos(governor.position());
        recruit.setFollowState(3);
        return true;
    }

    private void collectTaxes(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, long day) {
        int taxLevel = data.getTaxLevel();
        int inhabitants = GovernorManager.findVillagers(level, claim).size();
        int amount = (inhabitants / 10) * taxLevel * RecruitsServerConfig.GovernorTaxPer10Inhabitants.get();
        data.setLastTaxDay(day);
        data.setTaxToday(amount);
        governor.setPurse(governor.getPurse() + amount);

        int penalty = switch (taxLevel) {
            case 1 -> RecruitsServerConfig.GovernorTaxReputationPenaltyLow.get();
            case 2 -> RecruitsServerConfig.GovernorTaxReputationPenaltyMedium.get();
            case 3 -> RecruitsServerConfig.GovernorTaxReputationPenaltyHigh.get();
            default -> 0;
        };
        UUID owner = governor.getOwnerUUID();
        if (owner != null) {
            MinecraftForge.EVENT_BUS.post(new GovernorEvent.TaxCollected(governor, claim, level, owner, taxLevel, amount, inhabitants, penalty));
        }
    }
}
