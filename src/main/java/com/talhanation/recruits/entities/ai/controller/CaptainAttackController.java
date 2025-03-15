package com.talhanation.recruits.entities.ai.controller;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.util.RecruitCommanderUtil;

public class CaptainAttackController extends PatrolLeaderAttackController {

    public CaptainAttackController(AbstractLeaderEntity recruit) {
        super(recruit);
    }

    public void start() {
        if(!this.leader.getCommandSenderWorld().isClientSide() && leader.enemyArmy != null && leader.army != null){
            double distanceToTarget = this.leader.army.getPosition().distanceToSqr(leader.enemyArmy.getPosition());

            this.leader.army.updateArmy();
            this.leader.enemyArmy.updateArmy();

            if(leader.enemyArmy.size() == 0){
                // enemy army defeated
                this.leader.enemyArmy = null;
                return;
            }
            //To far from init pos -> enemy army is retreating
            if(initPos != null && initPos.distanceToSqr(this.leader.army.getPosition()) > 5000){
                this.leader.enemyArmy = null;
                return;
            }

            if(distanceToTarget < 4000){

                RecruitCommanderUtil.setRecruitsFollow(leader.army.getAllRecruitUnits(), leader.getUUID());
                RecruitCommanderUtil.setRecruitsAggroState(leader.army.getAllRecruitUnits(), leader.getState());

                this.setRecruitsTargets();
                leader.commandCooldown = 400;
            }
        }
    }
}


