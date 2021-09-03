package com.talhanation.recruits.init;

import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.util.RegistryUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();

    public static final RegistryObject<Item> RECRUIT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("recruit", ModEntityTypes.RECRUIT::get, 16755200, 16777045);
    public static final RegistryObject<Item> BOWMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("bowman", ModEntityTypes.BOWMAN::get, 16755200, 16777045);
    public static final RegistryObject<Item> NOMAD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("nomad", ModEntityTypes.NOMAD::get, 16755200, 16777045);

    public static final RegistryObject<BlockItem> RECRUIT_BLOCK = ITEMS.register("recruit_block", () -> new BlockItem(ModBlocks.RECRUIT_BLOCK.get(), (new Item.Properties()).tab(ItemGroup.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> BOWMAN_BLOCK = ITEMS.register("bowman_block", () -> new BlockItem(ModBlocks.BOWMAN_BLOCK.get(), (new Item.Properties()).tab(ItemGroup.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> NOMAD_BLOCK = ITEMS.register("nomad_block", () -> new BlockItem(ModBlocks.NOMAD_BLOCK.get(), (new Item.Properties()).tab(ItemGroup.TAB_DECORATIONS)));

}