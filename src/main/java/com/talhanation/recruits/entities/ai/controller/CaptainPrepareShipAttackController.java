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
        if(this.captain.getCommandSenderWorld().isClientSide()) return;

        // When not on a ship, fall back to standard land attack behaviour
        if(captain.smallShipsController.ship == null) {
            super.start();
            return;
        }

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

        if(captain.enemyArmy != null && captain.army != null && !captain.retreating) {
            this.captain.army.updateArmy();
            this.captain.enemyArmy.updateArmy();
            this.captain.enemyArmy.getAllUnits().removeIf(Entity::isUnderWater);

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
