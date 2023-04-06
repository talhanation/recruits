package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.RecruitShieldmanEntity;
import com.talhanation.recruits.entities.ai.PatrolLeaderTargetAttackers;
import com.talhanation.recruits.entities.ai.villager.FollowCaravanOwner;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
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
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap.Types;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

public class RecruitsPatrolSpawn {
    private final Random random = new Random();
    private final ServerLevel world;
    private int timer;
    private double chance;

    public RecruitsPatrolSpawn(ServerLevel level) {
        this.world = level;
        this.timer = getSpawnInterval();
        this.chance = RecruitsModConfig.RecruitPatrolsSpawnChance.get();
    }

    public void tick() {
        if(timer > 0) --this.timer;

        if(this.timer <= 0){
            if (this.world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                double rnd = this.random.nextInt(100);

                if (rnd <= this.chance && this.attemptSpawnPatrol()){}//To avoid multiple method call
            }
            this.timer = getSpawnInterval();
        }
    }

    private boolean attemptSpawnPatrol() {
        Player player = this.world.getRandomPlayer();
        if (player == null) {
            return true;
        } else {
            BlockPos blockpos = new BlockPos(player.position());
            BlockPos blockpos2 = this.func_221244_a(blockpos, 90);
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.distSqr(blockpos) > 200) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());

                int i = random.nextInt(10);
                switch(i) {
                    default -> spawnCaravan(upPos);
                    case 8,9 -> spawnSmallPatrol(upPos);
                    case 1,2 -> spawnMediumPatrol(upPos);
                    case 3,4 -> spawnLargePatrol(upPos);
                }
                Main.LOGGER.info("New Recruit Patrol spawned at "+ upPos);
                return true;
            }
            return false;
        }
    }

    private int getSpawnInterval(){
        //1200 == 1 min
        int minutes = RecruitsModConfig.RecruitPatrolSpawnInterval.get(); //minutes
        return 1200 * minutes;
    }
    private void spawnCaravan(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos, "Caravan Leader");
        this.createVillager(upPos, patrolLeader);
        Villager villagerGuide = this.createVillager(upPos, patrolLeader);
        this.createLlama(upPos, villagerGuide);
        this.createLlama(upPos, villagerGuide);

        Villager villagerGuide2 = this.createVillager(upPos, patrolLeader);
        this.createMule(upPos, villagerGuide2);
        this.createMule(upPos, villagerGuide2);

        Villager villagerGuide3 = this.createVillager(upPos, patrolLeader);
        this.createHorse(upPos, villagerGuide3);
        this.createHorse(upPos, villagerGuide3);

        Villager villagerGuide4 = this.createVillager(upPos, patrolLeader);
        this.createMule(upPos, villagerGuide4);
        this.createMule(upPos, villagerGuide4);


        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");
        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");
        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");
        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");
        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");
        this.createPatrolRecruit(upPos, patrolLeader, "Caravan Guard");

        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", false);
        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);

        this.createVillager(upPos, patrolLeader);
        this.createVillager(upPos, patrolLeader);

        this.createWanderingTrader(upPos, patrolLeader);
        this.createWanderingTrader(upPos, patrolLeader);
    }

    private void createWanderingTrader(BlockPos upPos, RecruitEntity patrolLeader) {
        WanderingTrader villager = EntityType.WANDERING_TRADER.create(world);

        villager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();

        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, patrolLeader.getUUID()));
        world.addFreshEntity(villager);
    }

    private void createHorse(BlockPos upPos, Villager villager) {
        Horse horse = EntityType.HORSE.create(world);

        horse.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        horse.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        horse.setPersistenceRequired();
        horse.setTamed(true);
        horse.equipSaddle(null);
        horse.setLeashedTo(villager, true);
        world.addFreshEntity(horse);
    }

    private Llama createLlama(BlockPos upPos, Villager villager) {
        Llama llama = EntityType.LLAMA.create(world);

        llama.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        llama.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        llama.setPersistenceRequired();
        llama.setTamed(true);
        llama.setChest(true);
        llama.setLeashedTo(villager, true);
        world.addFreshEntity(llama);

        return llama;
    }

    private Villager createVillager(BlockPos upPos, RecruitEntity patrolLeader) {
        Villager villager = EntityType.VILLAGER.create(world);

        villager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        villager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        villager.setPersistenceRequired();

        villager.goalSelector.addGoal(0, new FollowCaravanOwner(villager, patrolLeader.getUUID()));
        world.addFreshEntity(villager);

        return villager;
    }

    private Mule createMule(BlockPos upPos, LivingEntity villager) {
        Mule mule = EntityType.MULE.create(world);

        mule.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        mule.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        mule.setPersistenceRequired();
        mule.setTamed(true);
        mule.setChest(true);
        mule.setLeashedTo(villager, true);
        world.addFreshEntity(mule);

        return mule;
    }

    private void spawnLargePatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos, "Patrol Leader");

        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");
        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");
        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");


        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);
        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);
        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);

        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);

    }
    private void spawnMediumPatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos, "Patrol Leader");

        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");
        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");
        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");


        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);
        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);

        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);
    }

    private void spawnSmallPatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos, "Patrol Leader");

        this.createPatrolRecruit(upPos, patrolLeader, "Patrol");
        this.createPatrolShieldman(upPos, patrolLeader, "Patrol", true);
        this.createPatrolBowman(upPos, patrolLeader);
    }

    @Nullable
    private BlockPos func_221244_a(BlockPos p_221244_1_, int p_221244_2_) {
        BlockPos blockpos = null;

        for(int i = 0; i < 10; ++i) {
            int j = p_221244_1_.getX() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int k = p_221244_1_.getZ() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int l = this.world.getHeight(Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, this.world, blockpos1, EntityType.WANDERING_TRADER)) {
                blockpos = blockpos1;
                break;
            }
        }

        return blockpos;
    }

    private boolean func_226559_a_(BlockPos p_226559_1_) {
        Iterator var2 = BlockPos.betweenClosed(p_226559_1_, p_226559_1_.offset(1, 2, 1)).iterator();

        BlockPos blockpos;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            blockpos = (BlockPos)var2.next();
        } while(this.world.getBlockState(blockpos).getBlockSupportShape(this.world, blockpos).isEmpty() && world.getFluidState(blockpos).isEmpty());

        return false;
    }

    public static void setPatrolLeaderEquipment(RecruitEntity recruit) {
        Random random = new Random();
        recruit.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        int j = random.nextInt(32);
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

        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_BEEF);
            case 2 -> food = new ItemStack(Items.COOKED_CHICKEN);
            case 3 -> food = new ItemStack(Items.COOKED_MUTTON);
        }
        food.setCount(16 + k);
        recruit.inventory.setItem(7, food);
    }

    public static void setPatrolRecruitEquipment(RecruitEntity recruit) {
        Random random = new Random();

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

        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_COD);
            case 2 -> food = new ItemStack(Items.CARROT);
            case 3 -> food = new ItemStack(Items.BAKED_POTATO);
        }
        food.setCount(6 + k);
        recruit.inventory.setItem(7, food);


        int j = random.nextInt(8);

        if (j >= 4){
            recruit.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }

    }

    public static void setPatrolShieldmanEquipment(RecruitShieldmanEntity recruit) {
        Random random = new Random();

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

        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_COD);
            case 2 -> food = new ItemStack(Items.CARROT);
            case 3 -> food = new ItemStack(Items.BAKED_POTATO);
        }
        food.setCount(6 + k);
        recruit.inventory.setItem(7, food);
    }

    public static void setPatrolBowmanEquipment(BowmanEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));

        int i = random.nextInt(13);
        ItemStack arrows = new ItemStack(Items.ARROW);
        arrows.setCount(8 + i);
        recruit.inventory.setItem(6, arrows);

        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_COD);
            case 2 -> food = new ItemStack(Items.MELON_SLICE);
            case 3 -> food = new ItemStack(Items.COOKED_RABBIT);
        }
        food.setCount(6 + k);
        recruit.inventory.setItem(7, food);
    }



    public RecruitEntity createPatrolLeader(BlockPos upPos, String name){
        RecruitEntity patrolLeader = ModEntityTypes.RECRUIT.get().create(world);
        patrolLeader.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        patrolLeader.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolLeaderEquipment(patrolLeader);
        patrolLeader.setPersistenceRequired();

        patrolLeader.setXpLevel(2 + random.nextInt(3));
        patrolLeader.addLevelBuffsForLevel(patrolLeader.getXpLevel());
        patrolLeader.setHunger(100);
        patrolLeader.setMoral(100);
        patrolLeader.setCost(55);
        patrolLeader.setXp(random.nextInt(200));
        patrolLeader.setCustomName(new TextComponent(name));

        patrolLeader.setEscortUUID(Optional.of(patrolLeader.getUUID()));

        patrolLeader.targetSelector.addGoal(2, new PatrolLeaderTargetAttackers(patrolLeader));

        world.addFreshEntity(patrolLeader);
        return patrolLeader;
    }

    private void createPatrolRecruit(BlockPos upPos, RecruitEntity patrolLeader, String name) {
        RecruitEntity recruitEntity = ModEntityTypes.RECRUIT.get().create(world);
        recruitEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        recruitEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolRecruitEquipment(recruitEntity);
        recruitEntity.setPersistenceRequired();

        recruitEntity.setXpLevel(1 + random.nextInt(3));
        recruitEntity.addLevelBuffsForLevel(recruitEntity.getXpLevel());
        recruitEntity.setHunger(80);
        recruitEntity.setMoral(65);
        recruitEntity.setCost(18);
        recruitEntity.setEscortUUID(Optional.of(patrolLeader.getUUID()));
        recruitEntity.setShouldEscort(true);
        recruitEntity.setXp(random.nextInt(80));

        recruitEntity.setCustomName(new TextComponent(name));


        world.addFreshEntity(recruitEntity);
        //Main.LOGGER.debug("SpawnPatrol: patrol spawned");
    }

    private void createPatrolBowman(BlockPos upPos, RecruitEntity patrolLeader) {
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(world);
        bowman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        bowman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolBowmanEquipment(bowman);
        bowman.setPersistenceRequired();

        bowman.setXpLevel(1 + random.nextInt(3));
        bowman.addLevelBuffsForLevel(bowman.getXpLevel());
        bowman.setHunger(80);
        bowman.setMoral(65);
        bowman.setCost(32);
        bowman.setEscortUUID(Optional.of(patrolLeader.getUUID()));
        bowman.setShouldEscort(true);
        bowman.setXp(random.nextInt(120));

        bowman.setCustomName(new TextComponent("Patrol"));



        world.addFreshEntity(bowman);
    }

    private void createPatrolShieldman(BlockPos upPos, RecruitEntity patrolLeader, String name, boolean banner) {
        RecruitShieldmanEntity shieldmanEntity = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(world);
        shieldmanEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        shieldmanEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setPatrolShieldmanEquipment(shieldmanEntity);
        shieldmanEntity.setPersistenceRequired();

        shieldmanEntity.setXpLevel(1 + random.nextInt(3));
        shieldmanEntity.addLevelBuffsForLevel(shieldmanEntity.getXpLevel());
        shieldmanEntity.setHunger(80);
        shieldmanEntity.setMoral(65);
        shieldmanEntity.setCost(24);
        shieldmanEntity.setEscortUUID(Optional.of(patrolLeader.getUUID()));
        shieldmanEntity.setShouldEscort(true);
        shieldmanEntity.setXp(random.nextInt(120));

        shieldmanEntity.setCustomName(new TextComponent(name));



        if(banner) {
            ItemStack stack = new ItemStack(Items.GREEN_BANNER);
            stack.setCount(1);

            shieldmanEntity.setItemSlot(EquipmentSlot.HEAD, stack);

        }

        world.addFreshEntity(shieldmanEntity);
    }
}