package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);


    public static final RegistryObject<EntityType<RecruitEntity>> RECRUIT = ENTITY_TYPES.register("recruit",
            () -> EntityType.Builder.of(RecruitEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "recruit").toString()));

    public static final RegistryObject<EntityType<RecruitShieldmanEntity>> RECRUIT_SHIELDMAN = ENTITY_TYPES.register("recruit_shieldman",
            () -> EntityType.Builder.of(RecruitShieldmanEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "recruit_shield").toString()));


    public static final RegistryObject<EntityType<BowmanEntity>> BOWMAN = ENTITY_TYPES.register("bowman",
            () -> EntityType.Builder.of(BowmanEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "bowman").toString()));

    public static final RegistryObject<EntityType<CrossBowmanEntity>> CROSSBOWMAN = ENTITY_TYPES.register("crossbowman",
            () -> EntityType.Builder.of(CrossBowmanEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "crossbowman").toString()));
/*
    public static final RegistryObject<EntityType<ArcherEntity>> ARCHER = ENTITY_TYPES.register("archer",
            () -> EntityType.Builder.of(ArcherEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "archer").toString()));
*/
    public static final RegistryObject<EntityType<NomadEntity>> NOMAD = ENTITY_TYPES.register("nomad",
            () -> EntityType.Builder.of(NomadEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "nomad").toString()));


    public static final RegistryObject<EntityType<RecruitHorseEntity>> RECRUIT_HORSE = ENTITY_TYPES.register("recruit_horse",
            () -> EntityType.Builder.of(RecruitHorseEntity::new, EntityClassification.CREATURE)
                    .sized(1.3964844F, 1.6F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "recruit_horse").toString()));


    public static final RegistryObject<EntityType<AssassinEntity>> ASSASSIN = ENTITY_TYPES.register("assassin",
            () -> EntityType.Builder.of(AssassinEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "assassin").toString()));

    public static final RegistryObject<EntityType<AssassinLeaderEntity>> ASSASSIN_LEADER = ENTITY_TYPES.register("assassin_leader",
            () -> EntityType.Builder.of(AssassinLeaderEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "assassin_leader").toString()));


}
