package com.talhanation.recruits.init;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.Main;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModProfessions {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Main.MOD_ID);

    public static final RegistryObject<VillagerProfession> RECRUIT = createProfession("recruit", ModPois.POI_RECRUIT);
    public static final RegistryObject<VillagerProfession> SHIELDMAN = createProfession("shieldman", ModPois.POI_SHIELDMAN);
    public static final RegistryObject<VillagerProfession> BOWMAN = createProfession("bowman", ModPois.POI_BOWMAN);

    private static RegistryObject<VillagerProfession> createProfession(String name, RegistryObject<PoiType> pointOfInterest) {
        logger.info("Registering profession for {} with POI {}", name, pointOfInterest);
        return PROFESSIONS.register(name,
                () -> new VillagerProfession(name, poi -> poi.get() == pointOfInterest.get(),
                        poi -> poi.get() == pointOfInterest.get(), ImmutableSet.of(), ImmutableSet.of(),
                        SoundEvents.VILLAGER_CELEBRATE));
    }
}
