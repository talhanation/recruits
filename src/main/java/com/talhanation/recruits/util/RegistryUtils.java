package com.talhanation.recruits.util;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.items.RecruitsSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class RegistryUtils {

    public static DeferredHolder<Item, SpawnEggItem> createSpawnEggItem(String entityName, Supplier<? extends EntityType<? extends AbstractRecruitEntity>> supplier, int primaryColor, int secondaryColor) {
        DeferredHolder<Item, SpawnEggItem> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> new RecruitsSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties()));
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}
