package com.talhanation.recruits.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public final class RegistryLookup {
    private RegistryLookup() {
    }

    public static Optional<Holder<Item>> itemHolder(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getHolder(ResourceKey.create(Registries.ITEM, id)).map(holder -> holder);
    }
}
