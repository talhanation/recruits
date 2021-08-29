package com.talhanation.recruits.client.events;


import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.Main;

import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.SoundEvents;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;

public class VillagerEvents {
    public static final DeferredRegister<PointOfInterestType> POINT_OF_INTEREST_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Main.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, Main.MOD_ID);


    public static final RegistryObject<PointOfInterestType> RECRUIT_POI = POINT_OF_INTEREST_TYPES.register("recruit",
            () -> new PointOfInterestType(Main.MOD_ID, PointOfInterestType.getBlockStates(ModBlocks.RECRUIT_BLOCK.get()), 1, 1));
    public static final RegistryObject<VillagerProfession> RECRUIT = VILLAGER_PROFESSIONS.register("recruit",
            () -> new VillagerProfession(Main.MOD_ID, RECRUIT_POI.get(), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE));

    public static final RegistryObject<PointOfInterestType> BOWMAN_POI = POINT_OF_INTEREST_TYPES.register("bowman",
            () -> new PointOfInterestType(Main.MOD_ID, PointOfInterestType.getBlockStates(ModBlocks.BOWMAN_BLOCK.get()), 1, 1));
    public static final RegistryObject<VillagerProfession> BOWMAN = VILLAGER_PROFESSIONS.register("bowman",
            () -> new VillagerProfession(Main.MOD_ID, RECRUIT_POI.get(), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE));

    public static void registerRecruitPOI() {
        try {
            ObfuscationReflectionHelper.findMethod(PointOfInterestType.class, "registerBlockStates", PointOfInterestType.class).invoke(null, RECRUIT_POI.get());
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        Entity entity = event.getEntityLiving();
        if (entity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (clientPlayerEntity == null) {
                return;
            }

            if (profession == VillagerEvents.RECRUIT.get()) {
                createRecruit(villager);
            }

            if (profession == VillagerEvents.BOWMAN.get()){
                createBowman(villager);
            }
        }

    }
    private static void createRecruit(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        recruit.copyPosition(villager);
        recruit.setEquipment();
        recruit.setRandomSpawnBonus();
        recruit.setPersistenceRequired();
        villager.remove();
        villager.level.addFreshEntity(recruit);

    }

    private static void createBowman(LivingEntity entity){
        BowmanEntity recruit = ModEntityTypes.BOWMAN.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        recruit.copyPosition(villager);
        recruit.setEquipment();
        recruit.setRandomSpawnBonus();
        recruit.setPersistenceRequired();
        villager.remove();
        villager.level.addFreshEntity(recruit);

    }
}
