package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.RecruitShieldmanEntity;
import com.talhanation.recruits.entities.ai.PatrolLeaderTargetAttackers;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

public class RecruitPatrolSpawn {
    private final Random random = new Random();
    private final ServerLevel world;
    private int timer;
    private int delay;
    private double chance;

    public RecruitPatrolSpawn(ServerLevel level) {
        this.world = level;
        this.timer = 7000;
        this.delay = 3000;
        this.chance = RecruitsModConfig.RecruitPatrolsSpawnChance.get();
    }

    public void tick() {
        if (RecruitsModConfig.ShouldRecruitPatrolsSpawn.get() && --this.timer <= 0) {
            this.timer = 7000;
            this.delay -= 7000;
            if(delay < 0){
                delay = 0;
            }
            if (this.delay <= 0) {
                this.delay = 3000;
                if (this.world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                    double i = this.chance;
                    this.chance = Mth.clamp(this.chance, 5, 100);
                    if (this.random.nextInt(100) <= i && this.attemptSpawnPatrol()) {
                        this.chance = RecruitsModConfig.RecruitPatrolsSpawnChance.get();
                    }
                }
            }
        }

    }

    private boolean attemptSpawnPatrol() {
        Player player = this.world.getRandomPlayer();
        if (player == null) {
            return true;
        } else if (this.random.nextInt(5) != 0) {
            return false;
        } else {
            BlockPos blockpos = new BlockPos(player.position());
            BlockPos blockpos2 = this.func_221244_a(blockpos, 90);
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.distSqr(blockpos) > 200) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());


                int i = random.nextInt(5);
                switch(i) {
                    default -> spawnSmallPatrol(upPos);
                    case 1,2 -> spawnMediumPatrol(upPos);
                    case 3 -> spawnLargePatrol(upPos);
                }
                Main.LOGGER.debug("PATROL SPAWNED");
                return true;
            }
            return false;
        }
    }

    private void spawnLargePatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos);

        this.createPatrolRecruit(upPos, patrolLeader);
        this.createPatrolRecruit(upPos, patrolLeader);
        this.createPatrolRecruit(upPos, patrolLeader);


        this.createPatrolShieldman(upPos, patrolLeader, true);
        this.createPatrolShieldman(upPos, patrolLeader, true);
        this.createPatrolShieldman(upPos, patrolLeader, true);

        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);

    }
    private void spawnMediumPatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos);

        this.createPatrolRecruit(upPos, patrolLeader);
        this.createPatrolRecruit(upPos, patrolLeader);
        this.createPatrolRecruit(upPos, patrolLeader);


        this.createPatrolShieldman(upPos, patrolLeader, true);
        this.createPatrolShieldman(upPos, patrolLeader, true);

        this.createPatrolBowman(upPos, patrolLeader);
        this.createPatrolBowman(upPos, patrolLeader);
    }

    private void spawnSmallPatrol(BlockPos upPos) {
        RecruitEntity patrolLeader = this.createPatrolLeader(upPos);

        this.createPatrolRecruit(upPos, patrolLeader);
        this.createPatrolShieldman(upPos, patrolLeader, true);
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
        recruit.inventory.setItem(11, new ItemStack(Items.IRON_HELMET));
        recruit.inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.inventory.setItem(13, new ItemStack(Items.IRON_LEGGINGS));
        recruit.inventory.setItem(14, new ItemStack(Items.IRON_BOOTS));

        int j = random.nextInt(32);
        ItemStack item = new ItemStack(Items.EMERALD);
        item.setCount(8 + j);
        recruit.inventory.setItem(5, item);

        int i = random.nextInt(8);
        if (i == 1) {
            recruit.inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.inventory.setItem(9, new ItemStack(Items.GOLDEN_AXE));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        }
        else if(i == 4 || i == 5) {
            recruit.inventory.setItem(9, new ItemStack(Items.GOLDEN_SWORD));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }

        else {
            recruit.inventory.setItem(9, new ItemStack(Items.IRON_SWORD));
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
        recruit.inventory.setItem(5, food);
    }

    public static void setPatrolRecruitEquipment(RecruitEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.inventory.setItem(13, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.inventory.setItem(14, new ItemStack(Items.CHAINMAIL_BOOTS));


        int i = random.nextInt(8);
        if (i == 1) {
            recruit.inventory.setItem(9, new ItemStack(Items.STONE_AXE));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.inventory.setItem(9, new ItemStack(Items.IRON_SWORD));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        else {
            recruit.inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
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
        recruit.inventory.setItem(5, food);


        int j = random.nextInt(8);

        if (j >= 4){
            recruit.inventory.setItem(10, new ItemStack(Items.SHIELD));
            recruit.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }

    }

    public static void setPatrolShieldmanEquipment(RecruitShieldmanEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.inventory.setItem(13, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.inventory.setItem(14, new ItemStack(Items.CHAINMAIL_BOOTS));


        int i = random.nextInt(8);
        if (i == 1) {
            recruit.inventory.setItem(9, new ItemStack(Items.STONE_AXE));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        }
        else if (i == 2 || i == 3) {
            recruit.inventory.setItem(9, new ItemStack(Items.IRON_SWORD));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        else {
            recruit.inventory.setItem(9, new ItemStack(Items.STONE_SWORD));
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
        recruit.inventory.setItem(5, food);
    }

    public static void setPatrolBowmanEquipment(BowmanEntity recruit) {
        Random random = new Random();

        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.inventory.setItem(12, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        recruit.inventory.setItem(13, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        recruit.inventory.setItem(14, new ItemStack(Items.CHAINMAIL_BOOTS));

        recruit.inventory.setItem(9, new ItemStack(Items.BOW));
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
        recruit.inventory.setItem(5, food);
    }



    public RecruitEntity createPatrolLeader(BlockPos upPos){
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
        patrolLeader.setCustomName(new TextComponent("Patrol Leader"));

        patrolLeader.setEscortUUID(Optional.of(patrolLeader.getUUID()));

        patrolLeader.targetSelector.addGoal(2, new PatrolLeaderTargetAttackers(patrolLeader));

        world.addFreshEntity(patrolLeader);
        return patrolLeader;
    }

    private void createPatrolRecruit(BlockPos upPos, RecruitEntity patrolLeader) {
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

        recruitEntity.setCustomName(new TextComponent("Patrol"));


        world.addFreshEntity(recruitEntity);
        Main.LOGGER.debug("SpawnPatrol: patrol spawned");
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

    private void createPatrolShieldman(BlockPos upPos, RecruitEntity patrolLeader, boolean banner) {
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

        shieldmanEntity.setCustomName(new TextComponent("Patrol"));



        if(banner) {
            ItemStack stack = new ItemStack(Items.GREEN_BANNER);
            stack.setCount(1);

            shieldmanEntity.setItemSlot(EquipmentSlot.HEAD, stack);
            shieldmanEntity.inventory.setItem(11, stack);

        }

        world.addFreshEntity(shieldmanEntity);
    }
}