package com.talhanation.recruits.world;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.entities.ai.villager.FollowCaravanOwner;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.util.NPCArmy;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.WorldDimensions;

import javax.annotation.Nullable;
import java.util.*;

public class RecruitsPatrolSpawn {
    public static final Random random = new Random();
    public static int timer;
    public static double chance;
    public final ServerLevel world;

    public RecruitsPatrolSpawn(ServerLevel level) {
        world = level;
        timer = getSpawnInterval();
        chance = RecruitsServerConfig.RecruitPatrolsSpawnChance.get();
    }

    public void tick() {
        if (timer > 0) --timer;

        if (timer <= 0) {
            if (world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                double rnd = random.nextInt(100);
                if (rnd <= chance && attemptSpawnPatrol(world)) {}
            }
            timer = getSpawnInterval();
        }
    }

    public static boolean attemptSpawnPatrol(ServerLevel world) {
        Player player = world.getRandomPlayer();
        if (player == null) return true;

        BlockPos playerPos = player.getOnPos();
        BlockPos spawnPos  = func_221244_a(playerPos, 90, random, world);

        if (spawnPos == null) return false;
        if (!func_226559_a_(spawnPos, world)) return false;
        if (spawnPos.distSqr(playerPos) <= 200) return false;

        BlockPos upPos = new BlockPos(spawnPos.getX(), spawnPos.getY() + 2, spawnPos.getZ());

        int i = random.nextInt(13);
        switch (i) {
            case 9, 0  -> spawnSmallPatrol(upPos, world);
            case 1, 2  -> spawnLargePatrol(upPos, world);
            case 3, 4  -> spawnHugePatrol(upPos, world);
            case 5, 6  -> spawnTinyPatrol(upPos, world);
            case 7, 8  -> spawnRoadPatrol(upPos, world);
            case 10,11 -> spawnMediumPatrol(upPos, world);
            default    -> spawnCaravan(upPos, world);
        }

        return true;
    }

    public static int getSpawnInterval() {
        int minutes = RecruitsServerConfig.RecruitPatrolSpawnInterval.get();
        return 1200 * minutes;
    }

    // -------------------------------------------------------------------------
    // Route generation
    // -------------------------------------------------------------------------

    /**
     * Generates a simple patrol route around the spawn position.
     * Prefers open flat areas by sampling up to 5 candidate positions per
     * waypoint and choosing the one with the most air blocks around it.
     */
    @Nullable
    public static RecruitsRoute generatePatrolRoute(BlockPos center, ServerLevel world, int waypointCount, int spread) {
        List<BlockPos> waypoints = new ArrayList<>();

        for (int i = 0; i < waypointCount; i++) {
            BlockPos best = null;
            int bestOpenness = -1;

            // Sample several candidates and pick the most open one
            for (int attempt = 0; attempt < 5; attempt++) {
                int ox = random.nextInt(spread * 2) - spread;
                int oz = random.nextInt(spread * 2) - spread;
                int x  = center.getX() + ox;
                int z  = center.getZ() + oz;
                int y  = world.getHeight(Types.WORLD_SURFACE, x, z);
                BlockPos candidate = new BlockPos(x, y, z);

                // Skip positions that are underwater or inside blocks
                if (!world.getBlockState(candidate).isAir()) continue;
                if (world.getFluidState(candidate.below()).isEmpty() == false) continue;

                int openness = countOpenBlocks(world, candidate, 3);
                if (openness > bestOpenness) {
                    bestOpenness = openness;
                    best = candidate;
                }
            }

            if (best != null) {
                waypoints.add(best);
            }
        }

        if (waypoints.isEmpty()) return null;

        RecruitsRoute route = new RecruitsRoute("Patrol_" + Long.toHexString(center.asLong()));
        for (int i = 0; i < waypoints.size(); i++) {
            route.addWaypoint(new RecruitsRoute.Waypoint("WP " + (i + 1), waypoints.get(i), null));
        }
        return route;
    }

    /** Counts how many blocks in a radius×radius×2 box around pos are air. */
    private static int countOpenBlocks(ServerLevel world, BlockPos pos, int radius) {
        int open = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos check = pos.offset(dx, 0, dz);
                if (world.getBlockState(check).isAir()) open++;
                if (world.getBlockState(check.above()).isAir()) open++;
            }
        }
        return open;
    }

    // -------------------------------------------------------------------------
    // Commander + group creation
    // -------------------------------------------------------------------------

    /**
     * Creates a CommanderEntity patrol leader, registers an NPC-only group for it,
     * assigns all provided recruits to it, and gives it a random route.
     */
    public static CommanderEntity createCommanderWithGroup(BlockPos upPos, ServerLevel world, String name, List<AbstractRecruitEntity> recruits) {
        CommanderEntity leader = createCompanionPatrolLeader(upPos, world);
        leader.setCustomName(Component.literal(name));

        // Create a standalone group (no owner player — use a fixed sentinel UUID
        // so it persists in the group manager without being tied to a player).
        UUID groupId = UUID.randomUUID();
        RecruitsGroup group = new RecruitsGroup(name, groupId, name, 0);
        group.leaderUUID = leader.getUUID();

        // Register group in manager so it persists and can be queried
        RecruitEvents.recruitsGroupsManager.addPatrolGroup(group, world);

        leader.setGroupUUID(group.getUUID());

        // Assign recruits to the leader
        List<LivingEntity> armyUnits = new ArrayList<>();
        for (AbstractRecruitEntity recruit : recruits) {
            recruit.setGroupUUID(group.getUUID());
            group.addMember(recruit.getUUID());
            ICompanion.assignToLeaderCompanion(leader, recruit);
            armyUnits.add(recruit);
        }
        group.addMember(leader.getUUID());

        // Build the army so the leader can issue commands immediately
        leader.army = new NPCArmy(world, armyUnits, null);

        // Generate and assign a patrol route
        RecruitsRoute route = generatePatrolRoute(upPos, world, 4 + random.nextInt(3), 50);
        if (route != null) {
            List<BlockPos> positions = new ArrayList<>();
            List<Integer> waits     = new ArrayList<>();
            for (RecruitsRoute.Waypoint wp : route.getWaypoints()) {
                positions.add(wp.getPosition());
                waits.add(0);
            }
            leader.loadRouteWaypointsFromData(positions, waits);
            leader.setPatrolState(AbstractLeaderEntity.State.PATROLLING);
        }

        world.addFreshEntity(leader);
        return leader;
    }

    // -------------------------------------------------------------------------
    // Spawn methods — now using CommanderEntity
    // -------------------------------------------------------------------------

    public static void spawnCaravan(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Caravan Guard"));
        recruits.add(buildPatrolRecruit(world, upPos, "Caravan Guard"));
        recruits.add(buildPatrolRecruit(world, upPos, "Caravan Guard"));
        recruits.add(buildPatrolShieldman(world, upPos, "Caravan Guard", false));
        recruits.add(buildPatrolShieldman(world, upPos, "Caravan Guard", true));
        recruits.add(buildPatrolHorseman(world, upPos, "Caravan Guard", true));
        recruits.add(buildPatrolHorseman(world, upPos, "Caravan Guard", false));
        recruits.add(buildPatrolHorseman(world, upPos, "Caravan Guard", false));
        recruits.add(buildPatrolNomad(world, upPos, "Caravan Guard"));
        recruits.add(buildPatrolNomad(world, upPos, "Caravan Guard"));
        recruits.add(buildPatrolNomad(world, upPos, "Caravan Guard"));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);

        CommanderEntity leader = createCommanderWithGroup(upPos, world, "Caravan Leader", recruits);

        // Spawn the civilian caravan entities following the leader
        createVillager(world, upPos, leader);
        Villager g1 = createVillager(world, upPos, leader); createLlama(world, upPos, g1); createLlama(world, upPos, g1);
        Villager g2 = createVillager(world, upPos, leader); createMule(world, upPos, g2);  createMule(world, upPos, g2);
        Villager g3 = createVillager(world, upPos, leader); createHorse(world, upPos, g3); createHorse(world, upPos, g3);
        Villager g4 = createVillager(world, upPos, leader); createMule(world, upPos, g4);  createMule(world, upPos, g4);
        createVillager(world, upPos, leader);
        createVillager(world, upPos, leader);
        createWanderingTrader(world, upPos, leader);
        createWanderingTrader(world, upPos, leader);
    }

    public static void spawnHugePatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol")); recruits.add(buildPatrolRecruit(world, upPos, "Patrol")); recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true)); recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true)); recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolBowman(world, upPos)); recruits.add(buildPatrolBowman(world, upPos)); recruits.add(buildPatrolBowman(world, upPos));
        recruits.add(buildPatrolCrossbowman(world, upPos)); recruits.add(buildPatrolCrossbowman(world, upPos)); recruits.add(buildPatrolCrossbowman(world, upPos));
        recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true)); recruits.add(buildPatrolHorseman(world, upPos, "Patrol", false)); recruits.add(buildPatrolHorseman(world, upPos, "Patrol", false));
        recruits.add(buildPatrolNomad(world, upPos, "Patrol")); recruits.add(buildPatrolNomad(world, upPos, "Patrol")); recruits.add(buildPatrolNomad(world, upPos, "Patrol"));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    public static void spawnLargePatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol")); recruits.add(buildPatrolRecruit(world, upPos, "Patrol")); recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true)); recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolBowman(world, upPos)); recruits.add(buildPatrolBowman(world, upPos));
        recruits.add(buildPatrolCrossbowman(world, upPos)); recruits.add(buildPatrolCrossbowman(world, upPos));
        recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true)); recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolNomad(world, upPos, "Patrol")); recruits.add(buildPatrolNomad(world, upPos, "Patrol"));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    public static void spawnMediumPatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true)); recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolBowman(world, upPos));
        recruits.add(buildPatrolCrossbowman(world, upPos));
        recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolNomad(world, upPos, "Patrol"));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    public static void spawnSmallPatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol")); recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolBowman(world, upPos)); recruits.add(buildPatrolBowman(world, upPos));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    public static void spawnTinyPatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolShieldman(world, upPos, "Patrol", true));
        recruits.add(buildPatrolBowman(world, upPos));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    public static void spawnRoadPatrol(BlockPos upPos, ServerLevel world) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        recruits.add(buildPatrolRecruit(world, upPos, "Patrol"));
        recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true)); recruits.add(buildPatrolNomad(world, upPos, "Patrol"));
        recruits.add(buildPatrolHorseman(world, upPos, "Patrol", true)); recruits.add(buildPatrolNomad(world, upPos, "Patrol"));
        for (AbstractRecruitEntity r : recruits) world.addFreshEntity(r);
        createCommanderWithGroup(upPos, world, "Patrol Leader", recruits);
    }

    // -------------------------------------------------------------------------
    // Builder helpers — return the entity without adding to world
    // -------------------------------------------------------------------------

    private static AbstractRecruitEntity buildPatrolRecruit(ServerLevel world, BlockPos pos, String name) {
        RecruitEntity e = ModEntityTypes.RECRUIT.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        if (random.nextInt(2) == 0) setPatrolRecruitEquipment(e);
        applyCommonValues(e, 9, 80, 65, name);
        return e;
    }

    private static AbstractRecruitEntity buildPatrolShieldman(ServerLevel world, BlockPos pos, String name, boolean banner) {
        RecruitShieldmanEntity e = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        if (random.nextInt(2) == 0) setPatrolShieldmanEquipment(e);
        applyCommonValues(e, 12, 80, 65, name);
        if (banner) e.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GREEN_BANNER));
        return e;
    }

    private static AbstractRecruitEntity buildPatrolBowman(ServerLevel world, BlockPos pos) {
        BowmanEntity e = ModEntityTypes.BOWMAN.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        if (random.nextInt(2) == 0) setPatrolBowmanEquipment(e);
        applyCommonValues(e, 16, 80, 65, "Patrol");
        return e;
    }

    private static AbstractRecruitEntity buildPatrolCrossbowman(ServerLevel world, BlockPos pos) {
        CrossBowmanEntity e = ModEntityTypes.CROSSBOWMAN.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        setPatrolCrossbowmanEquipment(e);
        applyCommonValues(e, 16, 80, 65, "Patrol");
        return e;
    }

    private static AbstractRecruitEntity buildPatrolHorseman(ServerLevel world, BlockPos pos, String name, boolean banner) {
        HorsemanEntity e = ModEntityTypes.HORSEMAN.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        if (random.nextInt(2) == 0) setPatrolShieldmanEquipment(e);
        e.isPatrol = true;
        applyCommonValues(e, 30, 80, 75, name);
        if (banner) e.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GREEN_BANNER));
        return e;
    }

    private static AbstractRecruitEntity buildPatrolNomad(ServerLevel world, BlockPos pos, String name) {
        NomadEntity e = ModEntityTypes.NOMAD.get().create(world);
        e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        e.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null, null);
        if (random.nextInt(2) == 0) setPatrolBowmanEquipment(e);
        e.isPatrol = true;
        applyCommonValues(e, 30, 80, 75, name);
        return e;
    }

    private static void applyCommonValues(AbstractRecruitEntity e, int cost, int hunger, int morale, String name) {
        e.setPersistenceRequired();
        e.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;
        e.setXpLevel(Math.max(1, random.nextInt(3)));
        e.addLevelBuffsForLevel(e.getXpLevel());
        e.setHunger(hunger);
        e.setMoral(morale);
        e.setCost(cost);
        e.setXp(random.nextInt(120));
        e.setCustomName(Component.literal(name));
        AbstractRecruitEntity.applyBiomeAndVariant(e);
        setRecruitFood(e);
    }

    // -------------------------------------------------------------------------
    // Civilian entity creators (unchanged from original)
    // -------------------------------------------------------------------------

    public static void createWanderingTrader(ServerLevel world, BlockPos upPos, AbstractLeaderEntity leader) {
        WanderingTrader villager = EntityType.WANDERING_TRADER.create(world);
        villager.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();
        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, leader.getUUID()));
        world.addFreshEntity(villager);
    }

    public static void createHorse(ServerLevel world, BlockPos upPos, Villager villager) {
        Horse horse = EntityType.HORSE.create(world);
        horse.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        horse.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        horse.setPersistenceRequired();
        horse.setTamed(true);
        horse.equipSaddle(null);
        horse.setLeashedTo(villager, true);
        world.addFreshEntity(horse);
    }

    public static Villager createVillager(ServerLevel world, BlockPos upPos, AbstractLeaderEntity leader) {
        Villager villager = EntityType.VILLAGER.create(world);
        villager.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();
        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, leader.getUUID()));
        world.addFreshEntity(villager);
        return villager;
    }

    public static void createLlama(ServerLevel world, BlockPos upPos, Villager villager) {
        Llama llama = EntityType.LLAMA.create(world);
        llama.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        llama.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        llama.setPersistenceRequired();
        llama.setTamed(true);
        llama.setChest(true);
        llama.getPersistentData().putInt("Strength", 5);
        llama.createInventory();
        llama.setLeashedTo(villager, true);
        llama.getPersistentData().putBoolean("Caravan", true);
        fillLlamaInventory(llama);
        world.addFreshEntity(llama);
    }

    private static void fillLlamaInventory(Llama llama) {
        ItemStack[] foods = {
            new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS),
            new ItemStack(Items.MELON_SEEDS), new ItemStack(Items.POTATO)
        };
        ItemStack[] goods = {
            new ItemStack(Items.STRING), new ItemStack(Items.LEATHER),
            new ItemStack(Items.ARROW), new ItemStack(Items.CHAIN)
        };
        ItemStack[] building = {
            new ItemStack(Items.COBBLESTONE), new ItemStack(Items.WHITE_WOOL),
            new ItemStack(Items.OAK_WOOD), new ItemStack(Items.BRICK)
        };
        for (int i = 0; i < 4; i++) {
            ItemStack s = foods[random.nextInt(foods.length)].copy();   s.setCount(random.nextInt(64)); llama.inventory.addItem(s);
            ItemStack g = goods[random.nextInt(goods.length)].copy();   g.setCount(random.nextInt(64)); llama.inventory.addItem(g);
            ItemStack b = building[random.nextInt(building.length)].copy(); b.setCount(random.nextInt(64)); llama.inventory.addItem(b);
        }
    }

    public static void createMule(ServerLevel world, BlockPos upPos, LivingEntity owner) {
        Mule mule = EntityType.MULE.create(world);
        mule.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        mule.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        mule.setPersistenceRequired();
        mule.setTamed(true);
        mule.setChest(true);
        mule.createInventory();
        mule.setLeashedTo(owner, true);
        mule.getPersistentData().putBoolean("Caravan", true);
        fillMuleInventory(mule);
        world.addFreshEntity(mule);
    }

    private static void fillMuleInventory(Mule mule) {
        ItemStack[] foods    = { new ItemStack(Items.BREAD), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.COOKED_CHICKEN), new ItemStack(Items.COOKED_MUTTON) };
        ItemStack[] metal    = { new ItemStack(Items.COAL), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.CHAIN), new ItemStack(Items.CLAY) };
        ItemStack[] building = { new ItemStack(Items.STONE), new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.OAK_WOOD), new ItemStack(Items.BRICK) };
        ItemStack[] misc     = { new ItemStack(Items.SAND), new ItemStack(Items.SANDSTONE), new ItemStack(Items.GLASS), new ItemStack(Items.BARREL) };
        for (int i = 0; i < 4; i++) { ItemStack s = foods[random.nextInt(foods.length)].copy();    s.setCount(random.nextInt(64)); mule.inventory.setItem(16 - i, s); }
        for (int i = 0; i < 4; i++) { ItemStack s = metal[random.nextInt(metal.length)].copy();    s.setCount(random.nextInt(64)); mule.inventory.setItem(12 - i, s); }
        for (int i = 0; i < 4; i++) { ItemStack s = building[random.nextInt(building.length)].copy(); s.setCount(random.nextInt(64)); mule.inventory.setItem(8 - i, s); }
        for (int i = 0; i < 3; i++) { ItemStack s = misc[random.nextInt(misc.length)].copy();      s.setCount(random.nextInt(64)); mule.inventory.setItem(4 - i, s); }
    }

    // -------------------------------------------------------------------------
    // Commander creation (unchanged from original)
    // -------------------------------------------------------------------------

    public static CommanderEntity createCompanionPatrolLeader(BlockPos upPos, ServerLevel world) {
        CommanderEntity leader = ModEntityTypes.PATROL_LEADER.get().create(world);
        leader.moveTo(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5, random.nextFloat() * 360 - 180, 0);
        leader.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        AbstractRecruitEntity.applySpawnValues(leader);

        setPatrolLeaderEquipment(leader);

        leader.setXpLevel(Math.max(1, random.nextInt(4)));
        leader.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;
        leader.setPersistenceRequired();

        setRecruitFood(leader);
        leader.addLevelBuffsForLevel(leader.getXpLevel());
        leader.setHunger(80);
        leader.setMoral(65);
        leader.setCost(50);
        leader.setXp(random.nextInt(120));
        leader.setAggroState(0);

        return leader;
    }

    // -------------------------------------------------------------------------
    // Equipment helpers (unchanged)
    // -------------------------------------------------------------------------

    public static void setPatrolLeaderEquipment(AbstractRecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.IRON_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.IRON_BOOTS));

        ItemStack item = new ItemStack(Items.EMERALD);
        item.setCount(8 + random.nextInt(16));
        recruit.inventory.setItem(8, item);

        int i = random.nextInt(8);
        if      (i == 1)          recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        else if (i == 2 || i == 3) recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        else if (i == 4 || i == 5) recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        else                       recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    public static void setPatrolRecruitEquipment(RecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD,  ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.CHAINMAIL_BOOTS));

        int i = random.nextInt(8);
        if      (i == 1)          recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        else if (i == 2 || i == 3) recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        else                       recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));

        if (random.nextInt(8) >= 4) recruit.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    public static void setPatrolShieldmanEquipment(AbstractRecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD,  ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.CHAINMAIL_BOOTS));

        int i = random.nextInt(8);
        if      (i == 1)          recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        else if (i == 2 || i == 3) recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        else                       recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
    }

    public static void setPatrolBowmanEquipment(AbstractRecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD,  ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.CHAINMAIL_BOOTS));
        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        setRangedArrows(recruit);
    }

    public static void setPatrolCrossbowmanEquipment(AbstractRecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD,  ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.CHAINMAIL_BOOTS));
        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        setRangedArrows(recruit);
    }

    public static void setRangedArrows(AbstractRecruitEntity recruit) {
        ItemStack arrows = new ItemStack(Items.ARROW);
        arrows.setCount(24 + random.nextInt(32));
        recruit.inventory.setItem(6, arrows);
    }

    public static void setRecruitFood(AbstractRecruitEntity recruit) {
        setRecruitFood(recruit, 0);
    }

    public static void setRecruitFood(AbstractRecruitEntity recruit, int bonus) {
        ItemStack[] foods = {
            new ItemStack(Items.BREAD), new ItemStack(Items.COOKED_COD),
            new ItemStack(Items.MELON_SLICE), new ItemStack(Items.COOKED_RABBIT),
            new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.COOKED_CHICKEN),
            new ItemStack(Items.COOKED_MUTTON), new ItemStack(Items.BAKED_POTATO)
        };
        ItemStack food = foods[random.nextInt(foods.length)].copy();
        food.setCount(6 + random.nextInt(14) + bonus);
        recruit.inventory.setItem(7, food);
    }

    // -------------------------------------------------------------------------
    // Position helpers (unchanged)
    // -------------------------------------------------------------------------

    @Nullable
    public static BlockPos func_221244_a(BlockPos center, int spread, Random random, ServerLevel world) {
        for (int i = 0; i < 10; i++) {
            int x = center.getX() + random.nextInt(spread * 2) - spread;
            int z = center.getZ() + random.nextInt(spread * 2) - spread;
            int y = world.getHeight(Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, world, pos, EntityType.WANDERING_TRADER)) {
                return pos;
            }
        }
        return null;
    }

    public static boolean func_226559_a_(BlockPos pos, ServerLevel world) {
        for (BlockPos p : BlockPos.betweenClosed(pos, pos.offset(1, 2, 1))) {
            if (!world.getBlockState(p).getBlockSupportShape(world, p).isEmpty()
                    || !world.getFluidState(p).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
