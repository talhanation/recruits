package com.talhanation.recruits.entities.ai.navigation;


import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;


public abstract class RecruitsDoorInteractGoal extends Goal {
    protected AbstractRecruitEntity recruit;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private float doorOpenDirX;
    private float doorOpenDirZ;

    public RecruitsDoorInteractGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        } else {
            BlockState blockstate = this.recruit.getCommandSenderWorld().getBlockState(this.doorPos);
            if (!(blockstate.getBlock() instanceof DoorBlock)) {
                this.hasDoor = false;
                return false;
            } else {
                return blockstate.getValue(DoorBlock.OPEN);
            }
        }
    }

    protected void setOpen(boolean open) {
        if (this.hasDoor) {
            BlockState blockstate = this.recruit.getCommandSenderWorld().getBlockState(this.doorPos);

            if (blockstate.getBlock() instanceof DoorBlock) {
                ((DoorBlock) blockstate.getBlock()).setOpen(this.recruit, this.recruit.getCommandSenderWorld(), blockstate, this.doorPos, open);
            } else if (blockstate.getBlock() instanceof FenceGateBlock) {
                useGate(blockstate, this.recruit.getCommandSenderWorld(), doorPos, this.recruit);
            }

            for(Direction direction: Direction.values()){
                if(direction.equals(Direction.DOWN)) continue;

                BlockPos blockPos = this.doorPos.relative(direction);
                BlockState state = this.recruit.getCommandSenderWorld().getBlockState(blockPos);

                if (state.getBlock() instanceof DoorBlock) {
                    ((DoorBlock) blockstate.getBlock()).setOpen(this.recruit, this.recruit.getCommandSenderWorld(), blockstate, this.doorPos, open);
                } else if (state.getBlock() instanceof FenceGateBlock) {
                    useGate(blockstate, this.recruit.getCommandSenderWorld(), blockPos, this.recruit);
                }
            }
        }
    }

    public boolean canUse() {
        if (!GoalUtils.hasGroundPathNavigation(this.recruit)) {
            return false;
        } else if (!this.recruit.horizontalCollision) {
            return false;
        } else {
            RecruitPathNavigation groundpathnavigation = (RecruitPathNavigation)this.recruit.getNavigation();
            Path path = groundpathnavigation.getPath();
            if (path != null && !path.isDone() && groundpathnavigation.canOpenDoors()) {
                for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                    Node node = path.getNode(i);
                    this.doorPos = new BlockPos(node.x, node.y, node.z);
                    if (!(this.recruit.distanceToSqr((double)this.doorPos.getX(), this.recruit.getY(), (double)this.doorPos.getZ()) > 5D)) {
                        this.hasDoor = DoorBlock.isWoodenDoor(this.recruit.getCommandSenderWorld(), this.doorPos) || (this.recruit.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                        if (this.hasDoor) {
                            return true;
                        }
                    }
                }

                this.doorPos = this.recruit.blockPosition().above();
                this.hasDoor = DoorBlock.isWoodenDoor(this.recruit.getCommandSenderWorld(), this.doorPos) || (this.recruit.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                return this.hasDoor;
            } else {
                return false;
            }
        }
    }

    public boolean canContinueToUse() {
        return !this.passed;
    }

    public void start() {
        this.passed = false;
        this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5D - this.recruit.getX());
        this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5D - this.recruit.getZ());
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        float f = (float)((double)this.doorPos.getX() + 0.5D - this.recruit.getX());
        float f1 = (float)((double)this.doorPos.getZ() + 0.5D - this.recruit.getZ());
        float f2 = this.doorOpenDirX * f + this.doorOpenDirZ * f1;
        if (f2 < 0.0F) {
            this.passed = true;
        }

    }

    public void useGate(BlockState blockState, Level level, BlockPos blockPos, Entity entity) {
        if (blockState.getValue(FenceGateBlock.OPEN)) {
            blockState = blockState.setValue(FenceGateBlock.OPEN, Boolean.FALSE);
            level.setBlock(blockPos, blockState, 10);
        } else {
            Direction direction = entity.getDirection();
            if (blockState.getValue(FenceGateBlock.FACING) == direction.getOpposite()) {
                blockState = blockState.setValue(FenceGateBlock.FACING, direction);
            }

            blockState = blockState.setValue(FenceGateBlock.OPEN, Boolean.TRUE);
            level.setBlock(blockPos, blockState, 10);
        }

        boolean flag = blockState.getValue(FenceGateBlock.OPEN);
        //level.levelEvent(entity, flag ? 1008 : 1014, blockPos, 0);
        level.gameEvent(entity, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockPos);
    }

}
