package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
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
import javax.swing.*;
import java.util.Iterator;
import java.util.Random;

public class RecruitPatrolSpawn {
    private final Random random = new Random();
    private final ServerLevel world;
    private int timer;
    private int delay;
    private int chance;

    public RecruitPatrolSpawn(ServerLevel level) {
        this.world = level;
        this.timer = 1200;
        this.delay = 300;
        this.chance = 100;
    }

    public void tick() {
        //Main.LOGGER.debug("timer: " + timer);
        //Main.LOGGER.debug("delay: " + delay);

        if (RecruitsModConfig.ShouldRecruitPatrolsSpawn.get() && --this.timer <= 0) {
            this.timer = 1200;
            this.delay -= 1200;
            if(delay < 0){
                delay = 0;
            }
            if (this.delay <= 0) {
                this.delay = 300;
                if (this.world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                    int i = this.chance;
                    this.chance = Mth.clamp(this.chance, 5, 100);
                    if (this.random.nextInt(100) <= i && this.attemptSpawnPatrol()) {
                        //Main.LOGGER.debug("SPAWNED new PATROL");
                        this.chance = 100;
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
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.distSqr(blockpos) > 225) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());

                RecruitEntity patrolRecruit = ModEntityTypes.RECRUIT.get().create(world);
                patrolRecruit.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
                patrolRecruit.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
                setAdvancedEquipment(patrolRecruit);
                patrolRecruit.setHunger(100);
                patrolRecruit.setMoral(100);
                patrolRecruit.setCost(30);
                patrolRecruit.restrictTo(upPos, 16);

                world.addFreshEntity(patrolRecruit);
                return true;
            }
            return false;
        }
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

    public static void setAdvancedEquipment(RecruitEntity recruit) {
        Random random = new Random();
        recruit.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        recruit.inventory.setItem(11, new ItemStack(Items.CHAINMAIL_HELMET));
        recruit.inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.inventory.setItem(13, new ItemStack(Items.IRON_LEGGINGS));
        recruit.inventory.setItem(14, new ItemStack(Items.IRON_BOOTS));


        int i = random.nextInt(8);
        if (i == 1) {
            recruit.inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
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
        recruit.inventory.addItem(food);
    }
}