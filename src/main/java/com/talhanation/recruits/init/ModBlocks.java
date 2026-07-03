package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MOD_ID);

    public static final DeferredHolder<Block, Block> RECRUIT_BLOCK = BLOCKS.register("recruit_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> BOWMAN_BLOCK = BLOCKS.register("bowman_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> NOMAD_BLOCK = BLOCKS.register("nomad_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> CROSSBOWMAN_BLOCK = BLOCKS.register("crossbowman_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> HORSEMAN_BLOCK = BLOCKS.register("horseman_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> RECRUIT_SHIELD_BLOCK = BLOCKS.register("recruit_shield_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE)));

}
