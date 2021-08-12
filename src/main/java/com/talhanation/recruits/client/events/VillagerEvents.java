package com.talhanation.recruits.client.events;


import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.init.ModBlocks;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.SoundEvents;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.lang.reflect.InvocationTargetException;

public class VillagerEvents {
    public static final DeferredRegister<PointOfInterestType> POINT_OF_INTEREST_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Main.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, Main.MOD_ID);


    public static final RegistryObject<PointOfInterestType> RECRUIT_POI = POINT_OF_INTEREST_TYPES.register("woodworker",
            () -> new PointOfInterestType("woodworker", PointOfInterestType.getBlockStates(ModBlocks.WEAPONRACK.get()), 1, 1));
    public static final RegistryObject<VillagerProfession> RECRUIT = VILLAGER_PROFESSIONS.register("woodworker",
            () -> new VillagerProfession("woodworker", RECRUIT_POI.get(), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE));

    public static void registerRecruitPOI() {
        try {
            ObfuscationReflectionHelper.findMethod(PointOfInterestType.class, "registerBlockStates", PointOfInterestType.class).invoke(null, RECRUIT_POI.get());
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
