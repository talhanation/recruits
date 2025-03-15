package com.talhanation.recruits.world;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.entities.ai.villager.FollowCaravanOwner;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.NaturalSpawner;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

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
        if(timer > 0) --timer;

        if(timer <= 0){
            if (world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                double rnd = random.nextInt(100);

                if (rnd <= chance && attemptSpawnPatrol(world)){}//To avoid multiple method call
            }
            timer = getSpawnInterval();
        }
    }

    public static boolean attemptSpawnPatrol(ServerLevel world) {
        Player player = world.getRandomPlayer();
        if (player == null) {
            return true;
        } else {
            if(!player.getCommandSenderWorld().dimensionType().hasRaids()){
                player = world.getRandomPlayer();
            }
            BlockPos blockpos = new BlockPos(player.getOnPos());
            BlockPos blockpos2 = func_221244_a(blockpos, 90, random, world);

            if (blockpos2 != null && func_226559_a_(blockpos2, world) && blockpos2.distSqr(blockpos) > 200) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());




                int i = random.nextInt(13);
                switch(i) {
                    default -> spawnCaravan(upPos, world);

                    case 9,0 -> spawnSmallPatrol(upPos, world);
                    case 1,2 -> spawnLargePatrol(upPos, world);
                    case 3,4 -> spawnHugePatrol(upPos, world);
                    case 5,6 -> spawnTinyPatrol(upPos, world);
                    case 7,8 -> spawnRoadPatrol(upPos, world);
                    case 10,11 -> spawnMediumPatrol(upPos, world);


                }

                return true;
            }
            return false;
        }
    }

    public static int getSpawnInterval(){
        //1200 == 1 min
        int minutes = RecruitsServerConfig.RecruitPatrolSpawnInterval.get(); //minutes
        return 1200 * minutes;
    }
    public static void spawnCaravan(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Caravan Leader");
        createVillager(world, upPos, patrolLeader);
        Villager villagerGuide = createVillager(world, upPos, patrolLeader);
        createLlama(world, upPos, villagerGuide);
        createLlama(world, upPos, villagerGuide);

        Villager villagerGuide2 = createVillager(world, upPos, patrolLeader);
        createMule(world, upPos, villagerGuide2);
        createMule(world, upPos, villagerGuide2);

        Villager villagerGuide3 = createVillager(world, upPos, patrolLeader);
        createHorse(world, upPos, villagerGuide3);
        createHorse(world, upPos, villagerGuide3);

        Villager villagerGuide4 = createVillager(world, upPos, patrolLeader);
        createMule(world, upPos, villagerGuide4);
        createMule(world, upPos, villagerGuide4);

        createPatrolRecruit(world, upPos, patrolLeader, "Caravan Guard");
        createPatrolRecruit(world, upPos, patrolLeader, "Caravan Guard");
        createPatrolRecruit(world, upPos, patrolLeader, "Caravan Guard");

        createPatrolShieldman(world, upPos, patrolLeader, "Caravan Guard", false);
        createPatrolShieldman(world, upPos, patrolLeader, "Caravan Guard", true);

        createPatrolHorseman(world,upPos, patrolLeader, "Caravan Guard", true);
        createPatrolHorseman(world,upPos, patrolLeader, "Caravan Guard", false);
        createPatrolHorseman(world,upPos, patrolLeader, "Caravan Guard", false);

        createPatrolNomad(world, upPos, patrolLeader, "Caravan Guard");
        createPatrolNomad(world, upPos, patrolLeader, "Caravan Guard");
        createPatrolNomad(world, upPos, patrolLeader, "Caravan Guard");

        createVillager(world, upPos, patrolLeader);
        createVillager(world, upPos, patrolLeader);

        createWanderingTrader(world, upPos, patrolLeader);
        createWanderingTrader(world, upPos, patrolLeader);
    }

    public static void createWanderingTrader(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader) {
        WanderingTrader villager = EntityType.WANDERING_TRADER.create(world);

        villager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();

        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, patrolLeader.getUUID()));
        world.addFreshEntity(villager);
    }

    public static void createHorse(ServerLevel world, BlockPos upPos, Villager villager) {
        Horse horse = EntityType.HORSE.create(world);

        horse.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        horse.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        horse.setPersistenceRequired();
        horse.setTamed(true);
        horse.equipSaddle(null);
        horse.setLeashedTo(villager, true);
        world.addFreshEntity(horse);
    }

    public static void createLlama(ServerLevel world, BlockPos upPos, Villager villager) {
        Llama llama = EntityType.LLAMA.create(world);

        llama.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        llama.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        llama.setPersistenceRequired();
        llama.setTamed(true);
        llama.setChest(true);
        llama.getPersistentData().putInt("Strength", 5);
        llama.createInventory();
        llama.setLeashedTo(villager, true);
        llama.getPersistentData().putBoolean("Caravan", true);
        world.addFreshEntity(llama);

        for(int x = 0; x < 4; x++){
            int k = random.nextInt(4);
            int count = random.nextInt(64);
            ItemStack food;
            switch (k) {
                default -> food = new ItemStack(Items.WHEAT);
                case 1 -> food = new ItemStack(Items.WHEAT_SEEDS);
                case 2 -> food = new ItemStack(Items.MELON_SEEDS);
                case 3 -> food = new ItemStack(Items.POTATO);

            }
            food.setCount(count);
            llama.inventory.addItem(food);
        }

        for(int x = 0; x < 4; x++) {
            int j = random.nextInt(4);
            int count = random.nextInt(64);
            ItemStack resources;
            switch (j) {
                default -> resources = new ItemStack(Items.STRING);
                case 1 -> resources = new ItemStack(Items.LEATHER);
                case 2 -> resources = new ItemStack(Items.ARROW);
                case 3 -> resources = new ItemStack(Items.CHAIN);
            }
            resources.setCount(count);
            llama.inventory.addItem(resources);
        }

        for(int x = 0; x < 4; x++) {
            int j = random.nextInt(4);
            int count = random.nextInt(64);
            ItemStack resources;
            switch (j) {
                default -> resources = new ItemStack(Items.COBBLESTONE);
                case 1 -> resources = new ItemStack(Items.WHITE_WOOL);
                case 2 -> resources = new ItemStack(Items.OAK_WOOD);
                case 3 -> resources = new ItemStack(Items.BRICK);
            }
            resources.setCount(count);
            llama.inventory.addItem(resources);
        }
    }

    public static Villager createVillager(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader) {
        Villager villager = EntityType.VILLAGER.create(world);

        villager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();

        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, patrolLeader.getUUID()));
        world.addFreshEntity(villager);

        return villager;
    }

    public static void createMule(ServerLevel world, BlockPos upPos, LivingEntity villager) {
        Mule mule = EntityType.MULE.create(world);

        mule.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        mule.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        mule.setPersistenceRequired();
        mule.setTamed(true);
        mule.setChest(true);
        mule.createInventory();
        mule.setLeashedTo(villager, true);
        mule.getPersistentData().putBoolean("Caravan", true);

        for(int x = 0; x < 4; x++){
            int k = random.nextInt(4);
            int count = random.nextInt(64);
            ItemStack food;
            switch (k) {
                default -> food = new ItemStack(Items.BREAD);
                case 1 -> food = new ItemStack(Items.COOKED_BEEF);
                case 2 -> food = new ItemStack(Items.COOKED_CHICKEN);
                case 3 -> food = new ItemStack(Items.COOKED_MUTTON);
            }
            food.setCount(count);
            mule.inventory.setItem(16 - x, food);
        }


        for(int x = 0; x < 4; x++) {
            int j = random.nextInt(5);
            int count = random.nextInt(64);
            ItemStack resources;
            switch (j) {
                default -> resources = new ItemStack(Items.COAL);
                case 1 -> resources = new ItemStack(Items.IRON_INGOT);
                case 2 -> resources = new ItemStack(Items.COPPER_INGOT);
                case 3 -> resources = new ItemStack(Items.CHAIN);
                case 4 -> resources = new ItemStack(Items.CLAY);
            }
            resources.setCount(count);
            mule.inventory.setItem(12 - x, resources);
        }

        for(int x = 0; x < 4; x++) {
            int j = random.nextInt(4);
            ItemStack resources;
            int count = random.nextInt(64);
            switch (j) {
                default -> resources = new ItemStack(Items.STONE);
                case 1 -> resources = new ItemStack(Items.WHITE_WOOL);
                case 2 -> resources = new ItemStack(Items.OAK_WOOD);
                case 3 -> resources = new ItemStack(Items.BRICK);
            }
            resources.setCount(count);
            mule.inventory.setItem(8 - x, resources);
        }

        for(int x = 0; x < 3; x++) {
            int j = random.nextInt(4);
            ItemStack resources;
            int count = random.nextInt(64);
            switch (j) {
                default -> resources = new ItemStack(Items.SAND);
                case 1 -> resources = new ItemStack(Items.SANDSTONE);
                case 2 -> resources = new ItemStack(Items.GLASS);
                case 3 -> resources = new ItemStack(Items.BARREL);
            }
            resources.setCount(count);
            mule.inventory.setItem(4 -x, resources);
        }

        //mule.goalSelector.addGoal(0, new FollowCaravanOwner(mule, villager.getUUID()));
        world.addFreshEntity(mule);
    }

    public static void spawnHugePatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");

        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);

        createPatrolBowman(world, upPos, patrolLeader);
        createPatrolBowman(world, upPos, patrolLeader);
        createPatrolBowman(world, upPos, patrolLeader);

        createPatrolCrossbowman(world, upPos, patrolLeader);
        createPatrolCrossbowman(world, upPos, patrolLeader);
        createPatrolCrossbowman(world, upPos, patrolLeader);

        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", false);
        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", false);
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");

    }
    public static void spawnLargePatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");

        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);

        createPatrolBowman(world, upPos, patrolLeader);
        createPatrolBowman(world, upPos, patrolLeader);

        createPatrolCrossbowman(world, upPos, patrolLeader);
        createPatrolCrossbowman(world, upPos, patrolLeader);

        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
    }

    public static void spawnMediumPatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");

        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);

        createPatrolBowman(world, upPos, patrolLeader);

        createPatrolCrossbowman(world, upPos, patrolLeader);

        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");

    }

    public static void spawnSmallPatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolBowman(world, upPos, patrolLeader);
        createPatrolBowman(world, upPos, patrolLeader);
    }
    public static void spawnTinyPatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");
        createPatrolShieldman(world, upPos, patrolLeader, "Patrol", true);
        createPatrolBowman(world, upPos, patrolLeader);
    }

    public static void spawnRoadPatrol(BlockPos upPos, ServerLevel world) {
        RecruitEntity patrolLeader = createPatrolLeader(world, upPos, "Patrol Leader");

        createPatrolRecruit(world, upPos, patrolLeader, "Patrol");

        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
        createPatrolHorseman(world,upPos, patrolLeader, "Patrol", true);
        createPatrolNomad(world, upPos, patrolLeader, "Patrol");
    }

    @Nullable
    public static BlockPos func_221244_a(BlockPos p_221244_1_, int spread, Random random, ServerLevel world) {
        BlockPos blockpos = null;

        for(int i = 0; i < 10; ++i) {
            int j = p_221244_1_.getX() + random.nextInt(spread * 2) - spread;
            int k = p_221244_1_.getZ() + random.nextInt(spread * 2) - spread;
            int l = world.getHeight(Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, world, blockpos1, EntityType.WANDERING_TRADER)) {
                blockpos = blockpos1;
                break;
            }
        }

        return blockpos;
    }

    public static boolean func_226559_a_(BlockPos p_226559_1_, ServerLevel world) {
        Iterator var2 = BlockPos.betweenClosed(p_226559_1_, p_226559_1_.offset(1, 2, 1)).iterator();

        BlockPos blockpos;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            blockpos = (BlockPos)var2.next();
        } while(world.getBlockState(blockpos).getBlockSupportShape(world, blockpos).isEmpty() && world.getFluidState(blockpos).isEmpty());

        return false;
    }

    public static void setPatrolLeaderEquipment(RecruitEntity recruit) {
        Random random = new Random();
        recruit.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        int j = random.nextInt(16);
        ItemStack item = new ItemStack(Items.EMERALD);
        item.setCount(8 + j);
        recruit.inventory.setItem(8, item);

        int i = random.nextInt(8);
        if (i == 1) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        }
        else if(i == 4 || i == 5) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }

        else {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    public static void setPatrolRecruitEquipment(RecruitEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        int i = random.nextInt(8);
        if (i == 1) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        else {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }

        int j = random.nextInt(8);

        if (j >= 4){
            recruit.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }

    }

    public static void setPatrolShieldmanEquipment(RecruitShieldmanEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        int i = random.nextInt(8);
        if (i == 1) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        else {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }
    }

    public static void setPatrolBowmanEquipment(AbstractRecruitEntity recruit) {
        recruit.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));

        setRangedArrows(recruit);
    }

    public static void setRangedArrows(AbstractRecruitEntity recruit) {
        int i = random.nextInt(32);
        ItemStack arrows = new ItemStack(Items.ARROW);
        arrows.setCount(24 + i);
        recruit.inventory.setItem(6, arrows);
    }

    public static void setRangedCartriges(AbstractRecruitEntity recruit) {
        int i = random.nextInt(32);
        ItemStack arrows = new ItemStack(Items.ARROW);
        arrows.setCount(24 + i);
        recruit.inventory.setItem(6, arrows);
    }

    public static void setRecruitFood(AbstractRecruitEntity recruit){
        setRecruitFood(recruit, 0);
    }
    public static void setRecruitFood(AbstractRecruitEntity recruit, int bonus){
        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_COD);
            case 2 -> food = new ItemStack(Items.MELON_SLICE);
            case 3 -> food = new ItemStack(Items.COOKED_RABBIT);
            case 4 -> food = new ItemStack(Items.COOKED_BEEF);
            case 5 -> food = new ItemStack(Items.COOKED_CHICKEN);
            case 6 -> food = new ItemStack(Items.COOKED_MUTTON);
            case 7 -> food = new ItemStack(Items.BAKED_POTATO);
        }
        int i = random.nextInt(14);
        food.setCount(6 + i + bonus);
        recruit.inventory.setItem(7, food);
    }
    public static void setPatrolCrossbowmanEquipment(AbstractRecruitEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));

        setRangedArrows(recruit);
    }

    //CREATE//

    public static RecruitEntity createPatrolLeader(ServerLevel world, BlockPos upPos, String name){
        RecruitEntity patrolLeader = ModEntityTypes.RECRUIT.get().create(world);
        patrolLeader.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        patrolLeader.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolLeaderEquipment(patrolLeader);
        patrolLeader.setPersistenceRequired();

        patrolLeader.setXpLevel(1 + random.nextInt(2));
        patrolLeader.addLevelBuffsForLevel(patrolLeader.getXpLevel());
        patrolLeader.setHunger(100);
        patrolLeader.setMoral(100);
        patrolLeader.setCost(55);
        patrolLeader.setXp(random.nextInt(200));
        patrolLeader.setCustomName(Component.literal(name));
        patrolLeader.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        patrolLeader.setProtectUUID(Optional.of(patrolLeader.getUUID()));


        setRecruitFood(patrolLeader);

        world.addFreshEntity(patrolLeader);

        return patrolLeader;
    }

    public static void createPatrolRecruit(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader, String name) {
        RecruitEntity recruitEntity = ModEntityTypes.RECRUIT.get().create(world);
        recruitEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        recruitEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolRecruitEquipment(recruitEntity);
        recruitEntity.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        recruitEntity.setPersistenceRequired();

        recruitEntity.setXpLevel(Math.max(1, random.nextInt(3)));
        recruitEntity.addLevelBuffsForLevel(recruitEntity.getXpLevel());
        recruitEntity.setHunger(80);
        recruitEntity.setMoral(65);
        recruitEntity.setCost(9);
        recruitEntity.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        recruitEntity.setShouldProtect(true);
        recruitEntity.setXp(random.nextInt(80));

        recruitEntity.setCustomName(Component.literal(name));

        setRecruitFood(recruitEntity);

        world.addFreshEntity(recruitEntity);
    }

    public static void createPatrolBowman(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader) {
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(world);
        bowman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        bowman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolBowmanEquipment(bowman);
        bowman.setPersistenceRequired();
        bowman.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        bowman.setXpLevel(Math.max(1, random.nextInt(3)));
        bowman.addLevelBuffsForLevel(bowman.getXpLevel());
        bowman.setHunger(80);
        bowman.setMoral(65);
        bowman.setCost(16);
        bowman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        bowman.setShouldProtect(true);
        bowman.setXp(random.nextInt(120));

        bowman.setCustomName(Component.literal("Patrol"));

        setRecruitFood(bowman);

        world.addFreshEntity(bowman);
    }

    public static void createPatrolShieldman(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader, String name, boolean banner) {
        RecruitShieldmanEntity shieldmanEntity = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(world);
        shieldmanEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        shieldmanEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolShieldmanEquipment(shieldmanEntity);
        shieldmanEntity.setPersistenceRequired();
        shieldmanEntity.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        shieldmanEntity.setXpLevel(Math.max(1, random.nextInt(3)));
        shieldmanEntity.addLevelBuffsForLevel(shieldmanEntity.getXpLevel());
        shieldmanEntity.setHunger(80);
        shieldmanEntity.setMoral(65);
        shieldmanEntity.setCost(12);
        shieldmanEntity.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        shieldmanEntity.setShouldProtect(true);
        shieldmanEntity.setXp(random.nextInt(120));

        shieldmanEntity.setCustomName(Component.literal(name));



        if(banner) {
            ItemStack stack = new ItemStack(Items.GREEN_BANNER);
            stack.setCount(1);

            shieldmanEntity.setItemSlot(EquipmentSlot.HEAD, stack);

        }

        setRecruitFood(shieldmanEntity);

        world.addFreshEntity(shieldmanEntity);
    }

    public static void createPatrolHorseman(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader, String name, boolean banner) {
        HorsemanEntity horseman = ModEntityTypes.HORSEMAN.get().create(world);

        horseman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        horseman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolShieldmanEquipment(horseman);
        horseman.setPersistenceRequired();
        horseman.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;
        horseman.isPatrol = true;
        horseman.setXpLevel(Math.max(1, random.nextInt(3)));
        horseman.addLevelBuffsForLevel(horseman.getXpLevel());
        horseman.setHunger(80);
        horseman.setMoral(75);
        horseman.setCost(30);
        horseman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        horseman.setShouldProtect(true);
        horseman.setXp(random.nextInt(120));
        horseman.setCustomName(Component.literal(name));

        if(banner) {
            ItemStack stack = new ItemStack(Items.GREEN_BANNER);
            stack.setCount(1);

            horseman.setItemSlot(EquipmentSlot.HEAD, stack);

        }

        setRecruitFood(horseman);

        world.addFreshEntity(horseman);
    }

    public static void createPatrolNomad(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader, String name) {
        NomadEntity nomad = ModEntityTypes.NOMAD.get().create(world);
        nomad.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        nomad.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolBowmanEquipment(nomad);
        nomad.setPersistenceRequired();
        nomad.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;
        nomad.isPatrol = true;
        nomad.setXpLevel(1 + random.nextInt(3));
        nomad.addLevelBuffsForLevel(nomad.getXpLevel());
        nomad.setHunger(80);
        nomad.setMoral(75);
        nomad.setCost(30);
        nomad.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        nomad.setShouldProtect(true);
        nomad.setXp(random.nextInt(120));

        nomad.setCustomName(Component.literal(name));

        setRecruitFood(nomad);

        world.addFreshEntity(nomad);
    }

    public static void createPatrolCrossbowman(ServerLevel world, BlockPos upPos, RecruitEntity patrolLeader) {
        CrossBowmanEntity crossBowman = ModEntityTypes.CROSSBOWMAN.get().create(world);
        crossBowman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        crossBowman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolCrossbowmanEquipment(crossBowman);
        crossBowman.setPersistenceRequired();
        crossBowman.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        crossBowman.setXpLevel(Math.max(1, random.nextInt(3)));
        crossBowman.addLevelBuffsForLevel(crossBowman.getXpLevel());
        crossBowman.setHunger(80);
        crossBowman.setMoral(65);
        crossBowman.setCost(16);
        crossBowman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        crossBowman.setShouldProtect(true);
        crossBowman.setXp(random.nextInt(120));

        crossBowman.setCustomName(Component.literal("Patrol"));

        setRecruitFood(crossBowman);

        world.addFreshEntity(crossBowman);
    }

    public static void createRecruit(ServerLevel world, BlockPos upPos, AbstractLeaderEntity leader){
        RecruitEntity recruitEntity = ModEntityTypes.RECRUIT.get().create(world);
        recruitEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        recruitEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        if(random.nextInt(2) == 0) setPatrolRecruitEquipment(recruitEntity);
        recruitEntity.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        recruitEntity.setPersistenceRequired();

        recruitEntity.setXpLevel(Math.max(1, random.nextInt(3)));
        recruitEntity.addLevelBuffsForLevel(recruitEntity.getXpLevel());
        recruitEntity.setHunger(80);
        recruitEntity.setMoral(65);
        recruitEntity.setCost(9);
        recruitEntity.setXp(random.nextInt(80));

        recruitEntity.setCustomName(Component.literal("Recruit"));

        setRecruitFood(recruitEntity);

        //ICompanion.assignToLeaderCompanion(leader, recruitEntity);

        world.addFreshEntity(recruitEntity);
    }

    public static PatrolLeaderEntity createCompanionPatrolLeader(BlockPos upPos, ServerLevel world){
        PatrolLeaderEntity leader = ModEntityTypes.PATROL_LEADER.get().create(world);
        leader.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        leader.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        AbstractRecruitEntity.applySpawnValues(leader);

        setPatrolLeaderEquipment(leader);

        leader.setXpLevel(Math.max(1, random.nextInt(4)));
        leader.despawnTimer = RecruitsServerConfig.RecruitPatrolDespawnTime.get() * 20 * 60;

        setRecruitFood(leader);

        leader.addLevelBuffsForLevel(leader.getXpLevel());
        leader.setHunger(80);
        leader.setMoral(65);
        leader.setCost(50);
        leader.setXp(random.nextInt(120));
        leader.setState(1);
        leader.setCustomName(Component.literal("Patrol Leader"));

        return leader;
    }

    public static void spawnPatrol(BlockPos upPos, ServerLevel world) {
        PatrolLeaderEntity leader = createCompanionPatrolLeader(upPos, world);

        createRecruit(world, upPos, leader);
        createRecruit(world, upPos, leader);
        createRecruit(world, upPos, leader);
        createRecruit(world, upPos, leader);
        createRecruit(world, upPos, leader);

        world.addFreshEntity(leader);
    }

    public static void setPatrolLeaderEquipment(PatrolLeaderEntity recruit) {
        Random random = new Random();
        recruit.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        int j = random.nextInt(16);
        ItemStack item = new ItemStack(Items.EMERALD);
        item.setCount(8 + j);
        recruit.inventory.setItem(8, item);

        int i = random.nextInt(8);
        if (i == 1) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        }
        else if(i == 4 || i == 5) {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }

        else {
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }
}