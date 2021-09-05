package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<Block> RECRUIT_BLOCK = BLOCKS.register("recruit_block",
            () -> new Block(AbstractBlock.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> BOWMAN_BLOCK = BLOCKS.register("bowman_block",
            () -> new Block(AbstractBlock.Properties.copy(Blocks.FLETCHING_TABLE)));
/*
    public static final RegistryObject<Block> NOMAD_BLOCK = BLOCKS.register("nomad_block",
            () -> new Block(AbstractBlock.Properties.copy(Blocks.FLETCHING_TABLE)));
*/
    public static final RegistryObject<Block> RECRUIT_SHIELD_BLOCK = BLOCKS.register("recruit_shield_block",
            () -> new Block(AbstractBlock.Properties.copy(Blocks.FLETCHING_TABLE)));

}
