package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;



public class PatrolLeaderAttackAI extends Goal {
    private final AbstractLeaderEntity leader;

    public PatrolLeaderAttackAI(AbstractLeaderEntity recruit) {
        this.leader = recruit;
    }

    public boolean canUse() {
        return leader.commandCooldown == 0 && leader.getTarget() != null;
    }

    public void start(){
        this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();

        if(this.leader.currentRecruitsInCommand.size() > 0){
            attackCommandsToRecruits(this.leader.getTarget());
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.leader.setRecruitsClearTargets();
        this.leader.setRecruitsToFollow();
    }

    public boolean canContinueToUse() {
        return leader.commandCooldown != 0;
    }

    //TODO:
    private void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide()){
            Vec3 toTarget = target.position().subtract(this.leader.position());
            int rnd = leader.getRandom().nextInt(7);
            Main.LOGGER.debug("PatrolLeader Attack ID: "+ rnd);
            this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();
            leader.commandCooldown = 350;

            switch (rnd){
                case 0 -> {
                    //Ranged hold pos else move freely
                    this.leader.setRecruitsWanderFreely();

                    this.leader.setTypedRecruitsSetAndHoldPos(this.leader.getOnPos().above(), ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(this.leader.getOnPos().above(), ModEntityTypes.CROSSBOWMAN.get());
                }

                case 1 -> {
                    //Ranged and Infantry follow
                    this.leader.setRecruitsToFollow();

                    //Cavalry move freely
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                }
                case 2 -> {
                    //All freely
                    this.leader.setRecruitsWanderFreely();
                }

                case 3 -> {
                    //Ranged left, Cavalry right,

                    Vec3 moveVecRanged = toTarget.yRot(-90).normalize();
                    Vec3 moveRanged = this.leader.position().add(moveVecRanged.scale(10D));
                    BlockPos movePosRanged = new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z);

                    this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    Vec3 moveVecCav = toTarget.yRot(90).normalize();
                    Vec3 moveCav = this.leader.position().add(moveVecCav.scale(10D));
                    BlockPos movePosCav = new BlockPos(moveCav.x, moveCav.y, moveCav.z);

                    this.leader.setTypedRecruitsToMove(movePosCav, ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToMove(movePosCav, ModEntityTypes.HORSEMAN.get());

                    Vec3 moveVecInf = toTarget.yRot(0).normalize();
                    Vec3 moveInf = this.leader.position().add(moveVecInf.scale(5D));
                    BlockPos movePosInf = new BlockPos(moveInf.x, moveInf.y, moveInf.z);

                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                }
                case 4 -> {
                    //infantry freely, ranged go back pos enemy, cav freely
                    Vec3 moveVecInf = toTarget.yRot(180).normalize();
                    Vec3 moveInf = this.leader.position().add(moveVecInf.scale(5D));
                    BlockPos movePosInf = new BlockPos(moveInf.x, moveInf.y, moveInf.z);

                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                }

                case 5 ->{
                    //infantry hold enemy pos, ranged hold pos, cav freely
                    Vec3 moveVecInf = toTarget.yRot(0).normalize();
                    Vec3 moveInf = this.leader.position().add(moveVecInf.scale(5D));
                    BlockPos movePosInf = new BlockPos(moveInf.x, moveInf.y, moveInf.z);

                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsSetAndHoldPos(this.leader.getOnPos().above(), ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(this.leader.getOnPos().above(), ModEntityTypes.CROSSBOWMAN.get());

                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                }

                case 6 -> {
                    //infantry and ranged flank, cav freely
                    Vec3 moveVecRanged = toTarget.yRot(-90).normalize();
                    Vec3 moveRanged = target.position().add(moveVecRanged.scale(15D));
                    BlockPos movePosRanged = new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z);

                    this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());


                    Vec3 moveVecInf = toTarget.yRot(90).normalize();
                    Vec3 moveInf = target.position().add(moveVecInf.scale(15D));
                    BlockPos movePosInf = new BlockPos(moveInf.x, moveInf.y, moveInf.z);

                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(movePosInf, ModEntityTypes.RECRUIT_SHIELDMAN.get());


                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                }
            }
        }
    }

}


