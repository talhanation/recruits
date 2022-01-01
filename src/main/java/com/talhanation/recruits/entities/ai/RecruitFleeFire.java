package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;
import java.util.Random;

public class RecruitFleeFire extends Goal {

    AbstractRecruitEntity recruit;
    BlockPos fleePos;

    public RecruitFleeFire(AbstractRecruitEntity recruit) {
    this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (isNearLava()) {
            double fleeDistance = 4D;
            Vector3d vecTarget = new Vector3d(fleePos.getX(), fleePos.getY(), fleePos.getZ());
            Vector3d vecRec = new Vector3d(recruit.getX(), recruit.getY(), recruit.getZ());
            Vector3d fleeDir = vecRec.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            Vector3d fleePos1 = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
            recruit.getNavigation().moveTo(fleePos1.x, fleePos1.y, fleePos1.z, 1.15D);
            recruit.setFleeing(true);
        }
        else
            recruit.setFleeing(false);
    }


public boolean isNearLava() {
        Random random = new Random();
        int range = 4;

            for(int i = 0; i < 15; i++){
                BlockPos blockpos1 = this.recruit.getRecruitOnPos().offset(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
                while(this.recruit.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1){
                blockpos1 = blockpos1.below();
                }
                if(this.recruit.level.getFluidState(blockpos1).is(FluidTags.LAVA) || this.recruit.level.getBlockState(blockpos1).isBurning(recruit.level, blockpos1)){
                    this.fleePos = blockpos1;

                    return true;
                }
            }

        return false;
        }
}
