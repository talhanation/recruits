package com.talhanation.recruits.init;

import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.util.RegistryUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();

    public static final RegistryObject<Item> RECRUIT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("recruit", ModEntityTypes.RECRUIT::get, 16755200, 16777045);
    public static final RegistryObject<Item> BOWMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("bowman", ModEntityTypes.BOWMAN::get, 16755200, 16777045);
    public static final RegistryObject<Item> NOMAD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("nomad", ModEntityTypes.NOMAD::get, 16755200, 16777045);
    public static final RegistryObject<Item> RECRUIT_SHIELD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("recruit_shieldman", ModEntityTypes.RECRUIT_SHIELDMAN::get, 16755200, 16777045);
    public static final RegistryObject<Item> HORSEMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("horseman", ModEntityTypes.HORSEMAN::get, 16755200, 16777045);
    public static final RegistryObject<Item> CROSSBOWMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("crossbowman", ModEntityTypes.CROSSBOWMAN::get, 16755200, 16777045);

    public static final RegistryObject<BlockItem> RECRUIT_BLOCK = ITEMS.register("recruit_block", () -> new BlockItem(ModBlocks.RECRUIT_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> BOWMAN_BLOCK = ITEMS.register("bowman_block", () -> new BlockItem(ModBlocks.BOWMAN_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> RECRUIT_SHIELD_BLOCK = ITEMS.register("recruit_shield_block", () -> new BlockItem(ModBlocks.RECRUIT_SHIELD_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> CROSSBOWMAN_BLOCK = ITEMS.register("crossbowman_block", () -> new BlockItem(ModBlocks.CROSSBOWMAN_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> HORSEMAN_BLOCK = ITEMS.register("horseman_block", () -> new BlockItem(ModBlocks.HORSEMAN_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> NOMAD_BLOCK = ITEMS.register("nomad_block", () -> new BlockItem(ModBlocks.NOMAD_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));

    //public static final RegistryObject<Item> HELD_BANNER_ITEM = ITEMS.register("held_banner_item",() -> new HeldBannerItem((new Item.Properties()).tab(ItemGroup.TAB_COMBAT)));
}