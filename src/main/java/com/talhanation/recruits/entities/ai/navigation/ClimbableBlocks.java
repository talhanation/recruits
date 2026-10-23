package com.talhanation.recruits.entities.ai.navigation;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which blocks recruits climb: the blocks and tags from the "ClimbableBlocks" config, plus - like vanilla -
 * a trapdoor directly on top of a ladder with the same facing (wooden ones count even when closed,
 * {@link RecruitClimbGoal} opens them). Immutable, so the async path workers can use it.
 */
public final class ClimbableBlocks {

    private static ClimbableBlocks cached;
    private static List<String> cachedSource;

    private final Set<Block> blocks = new HashSet<>();
    private final List<TagKey<Block>> tags = new ArrayList<>();

    /** Server thread only (reads the config). Rebuilt only when the config list changed. */
    public static ClimbableBlocks get() {
        List<String> source = RecruitsServerConfig.RecruitClimbableBlocks.get();
        if (cached == null || source != cachedSource) {
            cached = new ClimbableBlocks(source);
            cachedSource = source;
        }
        return cached;
    }

    private ClimbableBlocks(List<String> entries) {
        for (String entry : entries) {
            boolean isTag = entry.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
            if (id == null) continue;

            if (isTag) this.tags.add(TagKey.create(Registries.BLOCK, id));
            else if (ForgeRegistries.BLOCKS.containsKey(id)) this.blocks.add(ForgeRegistries.BLOCKS.getValue(id));
        }
    }

    public boolean canClimbAt(BlockGetter level, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        return this.isClimbable(state) || isTrapdoorOnLadder(level, pos, state);
    }

    private boolean isClimbable(BlockState state) {
        if (this.blocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : this.tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private static boolean isTrapdoorOnLadder(BlockGetter level, BlockPos.MutableBlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) return false;
        if (!state.getValue(TrapDoorBlock.OPEN) && !state.is(BlockTags.WOODEN_TRAPDOORS)) return false;

        BlockState below = level.getBlockState(pos.move(Direction.DOWN));
        return below.getBlock() instanceof LadderBlock && below.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
    }

    public static boolean isClosedWoodenTrapdoor(BlockState state) {
        return state.getBlock() instanceof TrapDoorBlock && state.is(BlockTags.WOODEN_TRAPDOORS) && !state.getValue(TrapDoorBlock.OPEN);
    }
}
