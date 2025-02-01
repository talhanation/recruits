package com.talhanation.recruits.mixin;

import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorInteractGoal.class)
public class DoorInteractGoalMixin {
    @Shadow
    protected Mob mob;

    @Shadow
    protected BlockPos doorPos = BlockPos.ZERO;

    @Shadow
    protected boolean hasDoor;

    @Inject(method = "canUse", at = @At(value = "HEAD", target = "Lnet/minecraft/world/entity/ai/goal/DoorInteractGoal;canUse()Z"), cancellable = true)
    public void canUse(CallbackInfoReturnable<Boolean> ci) {
        if (!GoalUtils.hasGroundPathNavigation(this.mob) || !this.mob.horizontalCollision) {
            ci.setReturnValue(false);
            ci.cancel();
        }

        Path path = null;
        boolean canOpenDoors = false;

        if (this.mob.getNavigation() instanceof GroundPathNavigation navigation) {
            path = navigation.getPath();
            canOpenDoors = navigation.canOpenDoors();
        } else if (this.mob.getNavigation() instanceof AsyncGroundPathNavigation navigation) {
            path = navigation.getPath();
            canOpenDoors = navigation.canOpenDoors();
        }

        if (path != null && !path.isDone() && canOpenDoors) {
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                Node node = path.getNode(i);
                this.doorPos = new BlockPos(node.x, node.y + 1, node.z);

                if (this.mob.distanceToSqr(this.doorPos.getX(), this.mob.getY(), this.doorPos.getZ()) > 2.25D) {
                    continue;
                }

                this.hasDoor = DoorBlock.isWoodenDoor(this.mob.getCommandSenderWorld(), this.doorPos);
                if (this.hasDoor) {
                    ci.setReturnValue(true);
                    ci.cancel();
                }
            }
        } else {
            ci.setReturnValue(false);
            ci.cancel();
        }

        this.doorPos = this.mob.blockPosition().above();
        this.hasDoor = DoorBlock.isWoodenDoor(this.mob.getCommandSenderWorld(), this.doorPos);
        ci.setReturnValue(this.hasDoor);
        ci.cancel();
    }
}
