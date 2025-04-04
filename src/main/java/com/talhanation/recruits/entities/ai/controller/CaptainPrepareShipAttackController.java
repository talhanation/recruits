package com.talhanation.recruits.entities.ai.controller;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.util.WaterObstacleScanner;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CaptainPrepareShipAttackController extends PatrolLeaderAttackController {
    private final CaptainEntity captain;

    private int range;
    public CaptainPrepareShipAttackController(CaptainEntity recruit) {
        super(recruit);
        this.captain = recruit;
    }

    public void start() {
        if(!this.captain.getCommandSenderWorld().isClientSide() && captain.enemyArmy != null && captain.army != null && captain.smallShipsController.ship != null && !captain.retreating) {
            this.captain.army.updateArmy();
            this.captain.enemyArmy.updateArmy();
            this.captain.enemyArmy.getAllUnits().removeIf(Entity::isUnderWater);

            boolean captainBalls = this.captain.getCannonBallCount(this.captain.getInventory()) > 0;
            boolean shipContainer = captain.getVehicle() instanceof Container container && this.captain.getCannonBallCount(container) < 128;

            if(captainBalls && shipContainer){
                this.captain.refillCannonBalls();
            }

            if(this.captain.canRepair() && this.captain.smallShipsController.ship.getDamage() > 10){
                for(int i = 0; i < this.captain.getRandom().nextInt(3 ); i++){
                    this.captain.smallShipsController.ship.repairShip(this.captain);
                }
            }

            if(captain.enemyArmy.getPosition().distanceToSqr(captain.position()) < 500){
                this.setRecruitsTargets();
            }

            if(!captain.smallShipsController.checkForNextTarget()) return;


            if(!hasEnoughSpaceInWater()){
                this.captain.smallShipsController.ship.setSailState(0);
                return;
            }

            if(captain.smallShipsController.shouldGetInRange()){
                this.captain.setSailPos(this.captain.smallShipsController.target.getOnPos());
            }
        }
    }

    private boolean hasEnoughSpaceInWater() {
        Entity ship = this.captain.smallShipsController.ship.getBoat();
        Vec3 forward = ship.getForward();

        for(int i = 0; i < 4; i ++){
            if(WaterObstacleScanner.hasObstacle(forward.yRot(90 * i), ship, 30)){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTargetInRange() {
        return isTargetInRange;
    }
    public boolean isTargetInRange;
    @Override
    public void tick() {
        super.tick();

    }
}

/*
private void setMovementBehavior(double distanceToTarget) {
        int followState = captain.getFollowState();
        switch(followState){
            default -> this.captain.setSailPos(target.getOnPos());

            case 1,5 -> {
                if(captain.getShouldHoldPos() && captain.getHoldPos() != null ){
                    if(captain.distanceToSqr(captain.getHoldPos()) < distanceToTarget){
                        this.captain.setSailPos(new BlockPos((int) captain.getHoldPos().x, (int) captain.getHoldPos().y, (int) captain.getHoldPos().z));
                    }
                }

                LivingEntity followEntity = null;
                if(captain.getOwner() != null && followState == 1){
                    followEntity = captain.getOwner();
                }
                else if(captain.getProtectingMob() != null && followState == 5) {
                    followEntity = captain.getProtectingMob();
                }
                if(followEntity == null){
                    this.captain.setSailPos(target.getOnPos());
                    return;
                }

                if(captain.distanceToSqr(followEntity.position()) > 500){
                    this.captain.setSailPos(followEntity.getOnPos());
                }
            }

            case 2,3,4 -> {
                Vec3 pos = captain.getHoldPos();
                this.captain.setSailPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
            }
        }

    }
 */


