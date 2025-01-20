package com.talhanation.recruits.mixin;

import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GoalUtils.class)
public class GoalUtilsMixin {
    @Overwrite
    public static boolean hasGroundPathNavigation(Mob p_26895_) {
        return p_26895_.getNavigation() instanceof GroundPathNavigation || p_26895_.getNavigation() instanceof AsyncGroundPathNavigation;
    }
}
