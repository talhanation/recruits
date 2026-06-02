package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

record MapSample(BlockPos pos, BlockState state, int height) {
}
