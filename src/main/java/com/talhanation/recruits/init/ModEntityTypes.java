package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);


    public static final RegistryObject<EntityType<RecruitEntity>> RECRUIT = ENTITY_TYPES.register("recruit",
            () -> EntityType.Builder.<RecruitEntity>of(RecruitEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build(new ResourceLocation(Main.MOD_ID, "recruit").toString()));

}
