package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.blocks.WeaponRack;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<Block> WEAPONRACK = BLOCKS.register("weaponrack",
            () -> new WeaponRack(AbstractBlock.Properties.copy(Blocks.FLETCHING_TABLE)));
}
