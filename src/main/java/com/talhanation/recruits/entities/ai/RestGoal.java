package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RestGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private final MutableComponent NEED_BED = Component.translatable("chat.workers.needBed");
    private final MutableComponent CANT_FIND_BED = Component.translatable("chat.workers.cantFindBed");
    private final MutableComponent BED_OCCUPIED = Component.translatable("chat.workers.bedOccupied");
    private boolean messageCantFindBed;
    private boolean messageBedOccupied;
    private boolean noBed;
    public RestGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return false; //cruit.shouldRest();
    }
    /*
    public boolean canContinueToUse() {
        return recruit.shouldRest() && recruit.getTarget() == null && recruit.getFollowState() == 0;
    }

    @Override
    public void start() {
        super.start();
        this.messageCantFindBed = true;
        this.messageBedOccupied = true;

        if(recruit.getBedPos() == null) {
            if (recruit.getOwner() != null) {
                this.recruit.tellPlayer(recruit.getOwner(), Translatable.NEED_BED);
            }
            this.noBed = true;
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.recruit.stopSleeping();
        this.recruit.clearSleepingPos();
        if(this.recruit.getStatus() != AbstractWorkerEntity.Status.FOLLOW) this.recruit.setStatus(AbstractWorkerEntity.Status.IDLE);
        this.recruit.shouldDepositBeforeSleep = true;
    }

    @Override
    public void tick() {
        if (recruit.isSleeping()) {
            this.recruit.getNavigation().stop();
            this.recruit.heal(0.025F);
            if(this.recruit.getMoral() < 60) this.recruit.setMoral(this.recruit.getMoral() + 0.25F);
            return;
        }
        if(recruit.tickCount % 20 == 0){
            BlockPos sleepPos = this.getRandomBedPos();
            if(sleepPos != null){
                LivingEntity owner = recruit.getOwner();
                BlockEntity bedEntity = recruit.getCommandSenderWorld().getBlockEntity(sleepPos);
                if (bedEntity == null || !bedEntity.getBlockState().isBed(recruit.level, sleepPos, recruit)) {
                    if(messageCantFindBed && owner != null){
                        recruit.tellPlayer(owner, CANT_FIND_BED);
                        messageCantFindBed = false;
                        this.noBed = true;
                    }
                    return;
                }
                if (bedEntity.getBlockState().getValue(BlockStateProperties.OCCUPIED)) {
                    if(messageBedOccupied){
                        if(owner != null) recruit.tellPlayer(owner, BED_OCCUPIED);
                        messageBedOccupied = false;
                        this.noBed = true;
                    }
                }
                else {
                    this.goToBed(sleepPos);
                }
            }
        }

    }


    private void goToBed(BlockPos bedPos) {
        if (bedPos == null) {
            return;
        }
        // Move to the bed and stay there.
        PathNavigation pathFinder = this.recruit.getNavigation();
        pathFinder.moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.1D);
        this.recruit.getLookControl().setLookAt(
                bedPos.getX(),
                bedPos.getY() + 1,
                bedPos.getZ(),
                10.0F,
                (float) this.recruit.getMaxHeadXRot()
        );

        if (bedPos.distManhattan((Vec3i) recruit.getWorkerOnPos()) <= 5) {
            this.recruit.startSleeping(bedPos);
            this.recruit.setSleepingPos(bedPos);
            pathFinder.stop();
        }
    }


    /*
    @Nullable
    private BlockPos grabRandomBed() {
        BlockPos bedPos;
        int range = 16;

        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    bedPos = worker.getOnPos().offset(x, y, z);
                    BlockState state = worker.level.getBlockState(bedPos);

                    if (state.isBed(worker.level, bedPos, this.worker) &&
                        state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD &&
                        !state.getValue(BlockStateProperties.OCCUPIED)) {
                        return bedPos;
                    }
                }
            }
        }
        return null;
    }

     */
}