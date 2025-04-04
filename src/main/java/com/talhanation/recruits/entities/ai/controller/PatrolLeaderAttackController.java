package com.talhanation.recruits.entities.ai.controller;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.util.NPCArmy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class PatrolLeaderAttackController implements IAttackController {

    public final AbstractLeaderEntity leader;

    public int timeOut = 1000;
    public Vec3 initPos;

    public PatrolLeaderAttackController(AbstractLeaderEntity recruit) {
        this.leader = recruit;
    }

    public void start(){
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

            RecruitCommanderUtil.setRecruitsAggroState(this.leader.army.getAllRecruitUnits(), leader.getState());
            RecruitCommanderUtil.setRecruitsMoveSpeed( this.leader.army.getAllRecruitUnits(),1.0F);

            this.setRecruitsTargets();

            if(distanceToTarget < 2500) {
                if(isArmyScattered()){
                    regroupArmy();
                    return;
                }
                leader.commandCooldown = 400;
                commandArmy(this.leader.army, this.leader.enemyArmy);
            }
            else{
                if(leader.getOwner() != null) this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": Enemy contact! Im advancing, their size is " + leader.enemyArmy.size()));
                forwarding();
                leader.commandCooldown = 250;
            }
        }
    }

    public void tick() {
        if(leader.commandCooldown == 0){
            leader.commandCooldown = 400;
            start();
        }
    }

    @Override
    public void setInitPos(Vec3 pos) {
        initPos = pos;
    }

    @Override
    public boolean isTargetInRange() {
        return false;
    }

    private boolean isArmyScattered() {
        List<AbstractRecruitEntity> recruits = this.leader.army.getAllRecruitUnits();
        if (recruits.isEmpty()) return false;

        Vec3 commanderPos = this.leader.position();
        double maxDistance = 500.0;

        int scatteredCount = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            double distance = recruit.position().distanceToSqr(commanderPos);
            if (distance > maxDistance) {
                scatteredCount++;
            }
        }


        return scatteredCount >= (recruits.size() / 2);
    }
    public void commandArmy(NPCArmy playerArmy, NPCArmy enemyArmy) {
        double distance = playerArmy.getPosition().distanceTo(enemyArmy.getPosition());
        int ownArmySize = playerArmy.getTotalUnits();
        int enemyArmySize = enemyArmy.getTotalUnits();
        double ownMorale = playerArmy.getAverageMorale();
        double enemyMorale = enemyArmy.getAverageMorale();
        int ownRangedUnits = playerArmy.getRanged().size();
        int enemyRangedUnits = enemyArmy.getRanged().size();
        int ownCavalry = playerArmy.getCavalry().size();
        int enemyCavalry = enemyArmy.getCavalry().size();
        int ownShieldmen = playerArmy.getShieldmen().size();
        int enemyShieldmen = enemyArmy.getShieldmen().size();

        // Health-related factors
        double ownAverageHealth = playerArmy.getAverageHealth();
        double enemyAverageHealth = enemyArmy.getAverageHealth();

        // Decision-making logic
        if (ownArmySize >= 2 * enemyArmySize || ownAverageHealth > 50) {
            sendToOwner("We have overwhelming advantage! Charging!");
            if(distance < 1000) charge();
            else forwarding();
        }
        else if (ownMorale > 70 && enemyMorale < 30) {
            sendToOwner("We have a morale advantage! Advancing!");
            forwarding();
        }
        else if (enemyArmySize >= 2 * ownArmySize || ownMorale < 20 || enemyAverageHealth > 50) {
            sendToOwner("We are at a disadvantage! Retreating!");
            back();
        }
        else if (enemyRangedUnits > ownCavalry + ownShieldmen) {
            sendToOwner("Enemy has ranged superiority! Need assistance!");
            back();
        }
        else {
            sendToOwner("Default attacking!");
            if(distance < 1000) defaultAttack();
            else forwarding();
        }

        if(distance < 200){
            RecruitCommanderUtil.setRecruitsShields(this.leader.army.getRecruitShieldmen(), false);
        }
        else if(enemyRangedUnits >= ownShieldmen){
            RecruitCommanderUtil.setRecruitsShields(this.leader.army.getRecruitShieldmen(), true);
        }

    }

    public void shieldWall(){
        Vec3 target = leader.enemyArmy.getPosition();
        Vec3 toTarget = leader.position().vectorTo(target).normalize();
        Vec3 movePosInfantry = getPosTowardsTarget(target, 0.05);
        Vec3 movePosRanged = getPosTowardsTarget(target, -0.1);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitShieldmen(), movePosInfantry, 30, 1.0);
        FormationUtils.lineFormation(toTarget, leader.army.getRecruitInfantry(), movePosInfantry, 30, 1.0);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitRanged(), movePosRanged, 20, 1.8);

        RecruitCommanderUtil.setRecruitsWanderFreely(this.leader.army.getRecruitCavalry());
    }

    public void charge(){
        BlockPos movePosLeader = getBlockPosTowardsTarget(this.leader.enemyArmy.getPosition(), 0.2);
        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS

        RecruitCommanderUtil.setRecruitsWanderFreely(this.leader.army.getAllRecruitUnits());

        this.setRecruitsTargets();
    }
    public void defaultAttack(){
        Vec3 target = leader.enemyArmy.getPosition();
        Vec3 toTarget = leader.position().vectorTo(target).normalize();
        Vec3 movePosRanged = getPosTowardsTarget(target, 0.4);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);
        Vec3 movePosInfantry = getPosTowardsTarget(target, 0.6);

        FormationUtils.lineFormation(toTarget, this.leader.army.getRecruitInfantry(), movePosInfantry, 20, 3.25);
        RecruitCommanderUtil.setRecruitsWanderFreely(this.leader.army.getRecruitShieldmen());

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitRanged(), movePosRanged, 20, 3.25);

        RecruitCommanderUtil.setRecruitsWanderFreely(this.leader.army.getRecruitCavalry());

        this.setRecruitsTargets();

        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }

    public void regroupArmy(){
        sendToOwner("Recruits regroup!");
        Vec3 target = leader.enemyArmy.getPosition();
        Vec3 toTarget = leader.position().vectorTo(target).normalize();
        Vec3 movePosInfantry = getPosTowardsTarget(target, 0.1);
        Vec3 movePosRanged = getPosTowardsTarget(target, -0.1);
        Vec3 movePosCav = getPosTowardsTarget(target, 0.0);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitInfantry(), movePosInfantry, 20, 1.75);
        FormationUtils.lineFormation(toTarget, leader.army.getRecruitShieldmen(), movePosInfantry, 10, 2.25);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitRanged(), movePosRanged, 20, 3.0);
        FormationUtils.squareFormation(toTarget, leader.army.getRecruitCavalry(), movePosCav, 2.0);

    }

    public void forwarding(){
        Vec3 target = leader.enemyArmy.getPosition();
        Vec3 toTarget = leader.position().vectorTo(target).normalize();
        Vec3 movePosInfantry = getPosTowardsTarget(target, 0.6);
        Vec3 movePosRanged = getPosTowardsTarget(target, 0.4);
        Vec3 movePosCav = getPosTowardsTarget(target, 0.2);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.3);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitInfantry(), movePosInfantry, 20, 1.75);
        FormationUtils.lineFormation(toTarget, leader.army.getRecruitShieldmen(), movePosInfantry, 10, 2.25);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitRanged(), movePosRanged, 20, 3.0);
        FormationUtils.squareFormation(toTarget, leader.army.getRecruitCavalry(), movePosCav, 2.0);

        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }

    public void back(){
        Vec3 target = leader.enemyArmy.getPosition();
        Vec3 toTarget = leader.position().vectorTo(target).normalize();
        Vec3 movePosInfantry = getPosTowardsTarget(target, -0.4);
        Vec3 movePosRanged = getPosTowardsTarget(target, -0.6);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, -0.7);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitInfantry(), movePosInfantry, 20, 1.25);
        FormationUtils.lineFormation(toTarget, leader.army.getRecruitShieldmen(), movePosInfantry, 20, 1.25);

        FormationUtils.lineFormation(toTarget, leader.army.getRecruitRanged(), movePosRanged, 20, 2.25);
        FormationUtils.lineFormation(toTarget, leader.army.getRecruitCavalry(), movePosRanged, 20, 2.25);


        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }

    public BlockPos getBlockPosTowardsTarget(Vec3 target, double x){
        Vec3 pos = leader.position().lerp(target, x);
        return FormationUtils.getPositionOrSurface(leader.getCommandSenderWorld(), new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
    }
    public Vec3 getPosTowardsTarget(Vec3 target, double x){
        return leader.position().lerp(target, x);
    }

    public void setRecruitsTargets() {
        for(int i = 0; i < this.leader.army.getAllRecruitUnits().size(); i++){
            AbstractRecruitEntity recruit = this.leader.army.getAllRecruitUnits().get(i);
            if(this.leader.enemyArmy.size() > i) recruit.setTarget(this.leader.enemyArmy.getAllUnits().get(i));
        }
    }

    public boolean canAttack(LivingEntity living) {
        int aggroState = this.leader.getState();
        switch(aggroState){
            case 0 -> { //Neutral
                if(living instanceof Monster){
                    return this.leader.canAttack(living);
                }
            }
            case 1 -> { //AGGRO
                if(living instanceof Player || living instanceof AbstractRecruitEntity || living instanceof Monster){
                    return this.leader.canAttack(living);
                }
            }
            case 2 -> { //RAID
                return this.leader.canAttack(living);
            }

            default -> {
                return false;
            }
        }
        return false;
    }

    public void sendToOwner(String string){
        if(leader.getOwner() != null)
            this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": " + string));

    }

}


