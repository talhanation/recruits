package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;


public class CaptainAttackAI extends PatrolLeaderAttackAI {

    public CaptainAttackAI(AbstractLeaderEntity recruit) {
        super(recruit);
    }
    @Override
    public void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide()){
            double distanceToTarget = this.leader.distanceToSqr(target);

            if(distanceToTarget < 4000){
                targets = this.leader.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(70D)).stream()
                        .filter(living -> this.canAttack(living) && living.isAlive() && this.leader.getSensing().hasLineOfSight(living))
                        .toList();

                this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();

                double enemyArmor = calculateEnemyArmor(targets);
                double armor = calculateArmor();
                int enemySize = targets.size();

                int partySize = this.getPartySize();
                double sizeFactor = Math.abs((partySize + 1) / (enemySize + 1) );
                double armorFactor = Math.abs((armor + 1) / (enemyArmor + 1));


                if((sizeFactor + armorFactor)/2 <= 0.3){
                    if(this.leader.getOwner() != null) this.leader.getOwner().sendSystemMessage(Component.literal("Retreat!"));
                    this.leader.retreating = true;
                }

                Comparison comparisonOwnInfantry = getInfantryComparison();
                Comparison comparisonOwnRanged = getRangedComparison();

                Vec3 movePosInfantry = getPosTowardsTarget(target, 0.6);
                Vec3 movePosRanged;
                BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);

                /*
                //CHARGE INFANTRY
                if(comparisonOwnRanged == Comparison.BIGGER && comparisonOwnInfantry == Comparison.BIGGER){
                    BlockPos moveBlockPosInfantry = getBlockPosTowardsTarget(target, 0.9);
                    this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    movePosRanged = getPosTowardsTarget(target, 0.4);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(),   movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                }
                else if(comparisonOwnRanged == Comparison.SAME && comparisonOwnInfantry == Comparison.SAME){
                    movePosInfantry = getPosTowardsTarget(target, 0.75);
                    movePosRanged = getPosTowardsTarget(target, 0.5);

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                }

                else if(comparisonOwnRanged == Comparison.SMALLER && comparisonOwnInfantry == Comparison.BIGGER){
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                    this.leader.setRecruitsShields(true);

                }
                else if(comparisonOwnRanged == Comparison.SMALLER && comparisonOwnInfantry == Comparison.SMALLER){
                    movePosRanged = getPosTowardsTarget(target, -0.2);
                    movePosLeader = getBlockPosTowardsTarget(target, -0.3);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    movePosInfantry = getPosTowardsTarget(target, 0.3);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                }
                else {
                    movePosInfantry = getPosTowardsTarget(target, 0.6);
                    movePosRanged = getPosTowardsTarget(target, 0.4);

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                }
                this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                this.leader.setHoldPos(movePosLeader);
                this.leader.setFollowState(3);//LEADER HOLD POS


                 */

                this.leader.setRecruitsToFollow();
                for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand) recruit.setState(leader.getState());

                this.setRecruitsTargets();
                leader.commandCooldown = 400;
            }
        }
    }
}


