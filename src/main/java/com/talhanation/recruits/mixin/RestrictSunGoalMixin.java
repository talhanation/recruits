package com.talhanation.recruits.mixin;

import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RestrictSunGoal.class, priority = Integer.MAX_VALUE)
public class RestrictSunGoalMixin {
    @Shadow
    @Final
    private PathfinderMob mob;

    @Inject(method = "start", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/ai/goal/RestrictSunGoal;start()V"), cancellable = true)
    public void start(CallbackInfoReturnable<Void> ci) {
        if(this.mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setAvoidSun(true);
        } else if (this.mob.getNavigation() instanceof AsyncGroundPathNavigation navigation) {
            navigation.setAvoidSun(true);
        }
        ci.cancel();
    }

    @Inject(method = "stop", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/ai/goal/RestrictSunGoal;stop()V"), cancellable = true)
    public void stop(CallbackInfoReturnable<Void> ci) {
        if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
            ci.cancel();
            return;
        }

        if(this.mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setAvoidSun(false);
        } else if (this.mob.getNavigation() instanceof AsyncGroundPathNavigation navigation) {
            navigation.setAvoidSun(false);
        }
        ci.cancel();
    }
}
