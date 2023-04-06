package com.talhanation.recruits.items;

import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


public class RecruitSpawnEgg extends SpawnEggItem {

    private final Supplier<EntityType<?>> entityType;

    public RecruitSpawnEgg(Supplier<EntityType<?>> entityType, int primaryColor, int secondaryColor, Properties properties){
        super(ModEntityTypes.RECRUIT.get(), primaryColor, secondaryColor, properties);
        this.entityType = entityType;
    }
    @Override
    public @NotNull EntityType<?> getType(CompoundTag compound){
        if(compound != null && compound.contains("EntityTag", 10)) {
            CompoundTag entityTag = compound.getCompound("EntityTag");

            if(entityTag.contains("id", 8)) {
                return EntityType.byString(entityTag.getString("id")).orElse(this.entityType.get());
            }
            //TODO: add recruit nbt here
        }
        return this.entityType.get();
    }
}