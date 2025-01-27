package com.talhanation.recruits.mixin;

import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GoalUtils.class)
public class GoalUtilsMixin {
    @Inject(method = "hasGroundPathNavigation", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/ai/util/GoalUtils;hasGroundPathNavigation(Lnet/minecraft/world/entity/Mob;)Z"), cancellable = true)
    private static void hasGroundPathNavigation(Mob p_26895_, CallbackInfoReturnable<Boolean> cb) {
        cb.setReturnValue(p_26895_.getNavigation() instanceof GroundPathNavigation ||
                p_26895_.getNavigation() instanceof AsyncGroundPathNavigation);
        cb.cancel();
    }
}
