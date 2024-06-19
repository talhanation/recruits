package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class CaptainAttackAI extends PatrolLeaderAttackAI {

    public CaptainAttackAI(AbstractLeaderEntity recruit) {
        super(recruit);
    }
    @Override
    public void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide() && target != null){
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

                this.leader.setRecruitsToFollow();
                for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand) recruit.setState(leader.getState());

                this.setRecruitsTargets();
                leader.commandCooldown = 400;
            }
        }
    }
}


