package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<Block> RECRUIT_BLOCK = BLOCKS.register("recruit_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> BOWMAN_BLOCK = BLOCKS.register("bowman_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> NOMAD_BLOCK = BLOCKS.register("nomad_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> CROSSBOWMAN_BLOCK = BLOCKS.register("crossbowman_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> HORSEMAN_BLOCK = BLOCKS.register("horseman_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> RECRUIT_SHIELD_BLOCK = BLOCKS.register("recruit_shield_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

}
