package com.talhanation.recruits.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.Main;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;


public class ModProfessions {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, Main.MOD_ID);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> RECRUIT = makeProfession("recruit", ModPois.POI_RECRUIT);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> SHIELDMAN = makeProfession("shieldman", ModPois.POI_SHIELDMAN);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> BOWMAN = makeProfession("bowman", ModPois.POI_BOWMAN);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> CROSSBOWMAN = makeProfession("crossbowman", ModPois.POI_CROSSBOWMAN);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> HORSEMAN = makeProfession("horseman", ModPois.POI_HORSEMAN);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> NOMAD = makeProfession("nomad", ModPois.POI_NOMAD);

    private static DeferredHolder<VillagerProfession, VillagerProfession> makeProfession(String name, Object pointOfInterest) {
        logger.info("Registering profession for {} with POI {}", name, pointOfInterest);
        return PROFESSIONS.register(name,
                () -> new VillagerProfession(name, poi -> poi.equals(pointOfInterest),
                        poi -> poi.equals(pointOfInterest), ImmutableSet.of(), ImmutableSet.of(),
                        SoundEvents.VILLAGER_CELEBRATE));
    }
}
