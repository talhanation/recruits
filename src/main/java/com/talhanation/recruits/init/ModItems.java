package com.talhanation.recruits.init;

import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.util.RegistryUtils;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();

    public static final RegistryObject<Item> RECRUIT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("recruit", ModEntityTypes.RECRUIT::get, 16755200, 16777045);

}