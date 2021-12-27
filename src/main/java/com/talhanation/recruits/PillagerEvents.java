package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.pillager.PillagerMeleeAttackGoal;
import com.talhanation.recruits.entities.ai.pillager.PillagerUseShield;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class PillagerEvents {
    protected final Random random = new Random();

    @SubscribeEvent
    public void attackRecruit(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof PillagerEntity) {
            PillagerEntity pillager = (PillagerEntity) entity;
            pillager.goalSelector.addGoal(0, new PillagerMeleeAttackGoal(pillager, 1.15D, true));
        }

        if (entity instanceof AbstractIllagerEntity) {
            AbstractIllagerEntity illager = (AbstractIllagerEntity) entity;
            illager.goalSelector.addGoal(0, new PillagerUseShield(illager));
            illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AbstractRecruitEntity.class, true));
            if (RecruitsModConfig.PillagerAttackMonsters.get()){
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, ZombieEntity.class, true));
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AbstractSkeletonEntity.class, true));
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, SpiderEntity.class, true));
            }
            if (RecruitsModConfig.ShouldPillagersRaidNaturally.get()){
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AnimalEntity.class, true));
            }
        }

        if (entity instanceof MonsterEntity) {
            MonsterEntity monster = (MonsterEntity) entity;
            monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractRecruitEntity.class, true));
        }

        if (entity instanceof ZombieEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                ZombieEntity monster = (ZombieEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }
        if (entity instanceof AbstractSkeletonEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                AbstractSkeletonEntity monster = (AbstractSkeletonEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }
        if (entity instanceof SpiderEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                SpiderEntity monster = (SpiderEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }

        if (entity instanceof VindicatorEntity) {
            VindicatorEntity vindicator = (VindicatorEntity) entity;

            vindicator.setPersistenceRequired();
            vindicator.setCanPickUpLoot(true);
            vindicator.setCanJoinRaid(true);
            int i = this.random.nextInt(3);
            if (i == 2) vindicator.setItemInHand(Hand.MAIN_HAND, Items.IRON_AXE.getDefaultInstance());
            else {
                vindicator.setItemInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                vindicator.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
            }
        }

        if (entity instanceof PillagerEntity) {
            PillagerEntity pillager = (PillagerEntity) entity;

            pillager.setPersistenceRequired();
            pillager.setCanPickUpLoot(true);
            pillager.setCanJoinRaid(true);

            int i = this.random.nextInt(6);
            if (i == 2) pillager.setItemInHand(Hand.MAIN_HAND, Items.IRON_AXE.getDefaultInstance());
            else if (i == 1) pillager.setItemInHand(Hand.MAIN_HAND, Items.CROSSBOW.getDefaultInstance());
            else if (i == 3 || i == 4) pillager.setItemInHand(Hand.MAIN_HAND, Items.CROSSBOW.getDefaultInstance());
            else {
                pillager.setItemInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                pillager.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
            }
        }
        /*
        if (entity instanceof VindicatorEntity) {
            VindicatorEntity vindicator = (VindicatorEntity) entity;

            List<PillagerEntity> list1 = entity.level.getEntitiesOfClass(PillagerEntity.class, vindicator.getBoundingBox().inflate(64));
            int max = 2 + random.nextInt(10);
            if (list1.size() > 1) {
                vindicator.remove();
            }
            else {
                for (int k = 0; k < max; k++) createPillager(vindicator);
            }
        }
        */
    }

    @SubscribeEvent
    public void onBiomeLoadingPillager(BiomeLoadingEvent event) {
        Biome.Category category = event.getCategory();
        if (category != Biome.Category.NETHER && category != Biome.Category.THEEND && category != Biome.Category.NONE && category != Biome.Category.OCEAN && category != Biome.Category.RIVER) {
            //event.getSpawns().getSpawner(EntityClassification.MONSTER).add(new MobSpawnInfo.Spawners(EntityType.VINDICATOR, 3, 1, 2));
        }
    }

    private void createPillager(LivingEntity entity){
        PillagerEntity pillager = EntityType.PILLAGER.create(entity.level);
        pillager.copyPosition(entity);
        entity.level.addFreshEntity(pillager);
    }
}

