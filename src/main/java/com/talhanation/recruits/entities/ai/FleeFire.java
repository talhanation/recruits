package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;
import java.util.Random;

public class FleeFire extends Goal {

    CreatureEntity entity;
    BlockPos fleePos;

    public FleeFire(CreatureEntity entity) {
    this.entity = entity;
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
            Vector3d vecRec = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
            Vector3d fleeDir = vecRec.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            Vector3d fleePos1 = new Vector3d(vecRec.x + fleeDir.x * fleeDistance, vecRec.y + fleeDir.y * fleeDistance, vecRec.z + fleeDir.z * fleeDistance);
            entity.getNavigation().moveTo(fleePos1.x, fleePos1.y, fleePos1.z, 1.15D);
            if (entity instanceof AbstractRecruitEntity) {
                AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
                recruit.setFleeing(true);
            }
            if (entity instanceof AssassinEntity) {
                AssassinEntity recruit = (AssassinEntity) entity;
                recruit.setFleeing(true);
            }
        }
        else
        if (entity instanceof AbstractRecruitEntity) {
            AbstractRecruitEntity recruit = (AbstractRecruitEntity) entity;
            recruit.setFleeing(false);
        }
        if (entity instanceof AssassinEntity) {
            AssassinEntity recruit = (AssassinEntity) entity;
            recruit.setFleeing(false);
        }
    }


public boolean isNearLava() {
        Random random = new Random();
        int range = 4;

            for(int i = 0; i < 15; i++){
                BlockPos blockPos = new BlockPos(entity.getX(), entity.getY(), entity.getZ());
                BlockPos blockpos1 = blockPos.offset(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
                while(this.entity.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1){
                blockpos1 = blockpos1.below();
                }
                if(this.entity.level.getFluidState(blockpos1).is(FluidTags.LAVA) || this.entity.level.getBlockState(blockpos1).isBurning(entity.level, blockpos1)){
                    this.fleePos = blockpos1;

                    return true;
                }
            }

        return false;
        }
}
