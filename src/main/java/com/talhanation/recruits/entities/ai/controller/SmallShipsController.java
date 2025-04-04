package com.talhanation.recruits.entities.ai.controller;

import com.talhanation.recruits.compat.SmallShips;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import com.talhanation.recruits.util.Kalkuel;
import com.talhanation.recruits.util.WaterObstacleScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class SmallShipsController {
    public static final boolean DEBUG = false;
    public static final int REACH = 115;
    public static final int RECALCULATION_TIME = 300;
    private Path path;
    private final Level world;
    private final SailorPathNavigation pathNavigation;
    private final CaptainEntity captain;
    public SmallShips ship;
    private int recalcPath;
    private Node currentNode;
    public double distanceToSailPos;
    public WaterObstacleScanner waterObstacleScanner;

    public SmallShipsController(CaptainEntity captain, Level world) {
        pathNavigation = new SailorPathNavigation(captain, world);
        this.world = world;
        this.captain = captain;
    }

    public void tryMountShip(Entity entity) {
        if (entity instanceof Boat boat && SmallShips.isSmallShip(boat)) {
            ship = new SmallShips(boat, this.captain);
        }
    }

    public void tryDisMountShip() {
        Entity entity = this.captain.getVehicle();
        if (entity instanceof Boat boat && SmallShips.isSmallShip(boat)) {
            ship = null;
        }
        waterObstacleScanner = null;
    }
    public void calculatePath() {
        this.recalcPath = 0;
    }
    private int sailState;
    public boolean right;
    public boolean left;
    public double reach;

    public void tick() {
        if(this.world.isClientSide() || this.captain.level().isClientSide()) return;
        if(captain.getVehicle() == null || captain.getSailPos() == null || ship == null) return;
        if(!ship.isCaptainDriver()) return;

        if(updateAttacking()) return;

        BlockPos sailPos = this.captain.getSailPos();
        distanceToSailPos = captain.distanceToSqr(sailPos.getX(), captain.getY(), sailPos.getZ());
        reach = captain.getFollowState() == 1 || captain.getFollowState() == 5 ? REACH * 2 : REACH;
        if(distanceToSailPos < reach || captain.attackController.isTargetInRange()){
            currentNode = null;
            path = null;
            ship.setSailState(0);
            if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": REACHED SAILPOS"));
            return;
        }

        //CHECK IF CORRECTLY ALIGNED
        if (currentNode != null) {
            Vec3 forward = ship.getBoat().getForward().yRot(-90).normalize();
            Vec3 target = new Vec3(currentNode.x, 0, currentNode.z);
            Vec3 toTarget = target.subtract(ship.getBoat().position()).normalize();

            double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
            double ref = 63.334F;
            double stopThreshold = ref * 0.80F;

            if (Math.abs(phi) < stopThreshold) {
                left = phi < ref;
                right = phi > ref;

                ship.updateSmallShipsControl(right, left, 0);
                if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": ROTATING"));

                return;
            }
        }

        if (path != null && currentNode != null) {
            double distanceToNode = this.captain.getVehicle().distanceToSqr(currentNode.x, this.captain.getVehicle().getY(), currentNode.z);

            if (distanceToNode < reach) {
                calculatePath();
            }
        }

        //CHECK IF OBSTRACLES ARE NEAR
        if(waterObstacleScanner != null){
            int obstaclesLeft = waterObstacleScanner.getObstaclesLeft();
            int obstaclesRight = waterObstacleScanner.getObstaclesRight();
            int obstaclesFront = waterObstacleScanner.getObstaclesFront(10);

            if(obstaclesRight != 0 || obstaclesLeft != 0){
                if(obstaclesLeft < obstaclesRight){
                    right = true;
                    left = false;
                    sailState = 2;
                }
                else if(obstaclesLeft > obstaclesRight){
                    right = false;
                    left = true;
                    sailState = 2;
                }
                else if(obstaclesFront > 0){
                    sailState = 0;
                    right = true;
                    left = false;
                }
                else{
                    right = false;
                    left = false;
                }

                ship.updateSmallShipsControl(right, left, sailState);
                if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": OBSTRACLES ARE NEAR"));

                return;
            }

            //CHECK IF FOLLOW
            if(this.captain.getFollowState() == 1 || this.captain.getFollowState() == 5) {
                if(obstaclesFront <= 0){
                    if(distanceToSailPos < 500){
                        ship.setSailState(2);
                    }
                    else{
                        ship.setSailState(4);
                    }

                    ship.updateSmallShipsControl(this.captain.getSailPos().getX(), this.captain.getSailPos().getZ(), sailState);
                    if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": FOLLOWING"));
                    return;
                }
            }

            //DEFAULT PATHFINDING
            if (--recalcPath <= 0) {
                recalcPath = RECALCULATION_TIME;
                this.path = pathNavigation.createPath(this.captain.getSailPos(), 32, false, 0);
                if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": CREATING PATH"));
                if(path != null){
                    try {
                        this.currentNode = path.getEndNode();// FIX for "IndexOutOfBoundsException: Index 23 out of bounds for length 23" or "Index 1 out of bounds for length 1"

                    } catch (IndexOutOfBoundsException e) {
                        this.currentNode = path.nodes.get(path.nodes.size() - 1);
                    }
                    sailState = 4;
                    reach = REACH;
                }
            }

            if(path != null && DEBUG){
                for(Node node : this.path.nodes) {
                    captain.getCommandSenderWorld().setBlock(new BlockPos(node.x, (int) (captain.getY() + 4), node.z), Blocks.ICE.defaultBlockState(), 3);
                }
            }

            if (path != null && currentNode != null) {
                double distanceToNode = this.captain.getVehicle().distanceToSqr(currentNode.x, this.captain.getVehicle().getY(), currentNode.z);

                if (distanceToNode < reach) {
                    calculatePath();
                }
                if(DEBUG && captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": FOLLOWING PATH"));
                ship.updateSmallShipsControl(currentNode.x, currentNode.z, sailState);

            }
        }
        else{
            this.waterObstacleScanner = new WaterObstacleScanner(captain.getCommandSenderWorld(), ship.getBoat());
        }
    }
    public Entity target;
    private final int attackRange = 4500;
    private final int followRange = 600;
    public double distanceToTarget;
    private boolean updateAttacking() {
        if(target == null || !target.isAlive() || target.isUnderWater() || ship.getBoat().getPassengers().contains(target) || !this.captain.getSensing().hasLineOfSight(target)){
            if(this.captain.tickCount % 20 == 0) checkForNextTarget();
            return false;
        }

        this.distanceToTarget = enemyDistanceToLeader(target);
        if(distanceToTarget > attackRange) return false;

        if(isTooFarFromMovementRange()){
            target = null;
            this.captain.commandCooldown = 100;
            return false;
        }

        if(ship.canShootCannons()) {
            if(this.rotateAndCheckAngle()) {
                this.shootCannons();
            }
        }

        return true;
    }

    public boolean checkForNextTarget() {
        if(captain.enemyArmy == null || captain.enemyArmy.ships == null || captain.enemyArmy.getAllUnits() == null) return false;

        if(captain.enemyArmy.ships.size() > 0){
            captain.enemyArmy.ships.sort(Comparator.comparing(this::enemyDistanceToLeader));
            this.target = captain.enemyArmy.ships.get(0);
            if(captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": Enemy Ship in contact, im counting " +  captain.enemyArmy.ships + "!"));
        }
        else if(captain.enemyArmy.getAllUnits().size() > 0){
            captain.enemyArmy.ships.sort(Comparator.comparing(this::enemyDistanceToLeader));
            this.target = captain.enemyArmy.getAllUnits().get(0);
            if(captain.getOwner() != null) this.captain.getOwner().sendSystemMessage(Component.literal(captain.getName().getString() + ": Enemies in contact, im counting " +  captain.enemyArmy.getAllUnits().size() + "!"));
        }
        else {
            //NO ENEMIES LEFT
            this.captain.enemyArmy = null;
            this.target = null;
            return false;
        }
        return true;
    }

    public boolean shouldGetInRange() {
        return ship.canShootCannons() && ship.getDamage() < 70 && captain.getFollowState() == 0 && !captain.getShouldMovePos();
    }

    private boolean isTooFarFromMovementRange(){
        Vec3 boatPos = ship.getBoat().position();
        int movementState = captain.getFollowState();

        switch (movementState) {
            case 0 -> { // Wander
                BlockPos movePos = captain.getMovePos();
                return captain.getShouldMovePos() && movePos != null && movePos.getCenter().distanceToSqr(boatPos) > (followRange * 0.5F);
            }
            case 1 -> { // Follow
                LivingEntity owner = captain.getOwner();
                return owner != null && owner.distanceToSqr(boatPos) > followRange;
            }
            case 2 -> { // Hold Position
                Vec3 holdPos = captain.getHoldPos();
                return holdPos != null && holdPos.distanceToSqr(boatPos) > (followRange * 0.5F);
            }
            case 5 -> { // Protect
                LivingEntity protect = captain.getProtectingMob();
                return protect != null && captain.getShouldProtect() && protect.distanceToSqr(boatPos) > followRange;
            }
        }
        return false;
    }

    private boolean rotateAndCheckAngle() {
        if (target == null || !target.isAlive() || target.isUnderWater() || captain.smallShipsController.ship.getBoat().getPassengers().contains(target)) {
            target = null;
            this.captain.commandCooldown = 0;
            return false;
        }

        Vec3 forward = captain.smallShipsController.ship.getBoat().getForward().normalize();

        double offsetDistance = captain.smallShipsController.ship.isGalley() ? 2.0 : 0;
        Vec3 cannonPosition = captain.smallShipsController.ship.getBoat().position().add(forward.scale(offsetDistance));

        Vec3 toTarget = cannonPosition.vectorTo(target.position());

        Vec3 vecRight = forward.yRot(-3.14F / 2).normalize();
        Vec3 vecLeft = forward.yRot(3.14F / 2).normalize();

        double distanceToLeft = toTarget.distanceTo(vecLeft);
        double distanceToRight = toTarget.distanceTo(vecRight);
        boolean shootLeftSide = distanceToLeft < distanceToRight;

        double alpha = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
        double phi = shootLeftSide ? -alpha : alpha;
        double ref = shootLeftSide ? -90 : 90;

        boolean inputLeft = (phi < ref);
        boolean inputRight = (phi > ref);

        captain.smallShipsController.ship.rotateShip(inputLeft, inputRight);

        double beta = shootLeftSide
                ? Kalkuel.horizontalAngleBetweenVectors(forward.yRot(3.14F / 2), toTarget)
                : Kalkuel.horizontalAngleBetweenVectors(forward.yRot(-3.14F / 2), toTarget);

        return beta < 15;
    }
    private void shootCannons(){
        if(captain.smallShipsController.ship.canShootCannons() && captain.getShouldRanged() && captain.getState() != 3){
            if(!target.isAlive()){
                target = null;
                return;
            }

            Vec3 toTarget = captain.smallShipsController.ship.getBoat().position().vectorTo(target.position());

            Vec3 forward = captain.smallShipsController.ship.getBoat().getForward().normalize();
            Vec3 VecRight = forward.yRot(-3.14F / 2).normalize();
            Vec3 VecLeft = forward.yRot(3.14F / 2).normalize();

            double distanceToLeft = toTarget.distanceTo(VecLeft);
            double distanceToRight = toTarget.distanceTo(VecRight);

            boolean shootLeftSide = distanceToLeft < distanceToRight;

            double alpha = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
            double phi = shootLeftSide ?  -alpha : alpha;
            double ref = shootLeftSide ? -90 : 90;

            boolean inputLeft =  (phi < ref);
            boolean inputRight = (phi > ref);

            captain.smallShipsController.ship.rotateShip(inputLeft, inputRight);

            double beta = shootLeftSide ? Kalkuel.horizontalAngleBetweenVectors(forward.yRot(3.14F / 2), toTarget) : Kalkuel.horizontalAngleBetweenVectors(forward.yRot(-3.14F / 2), toTarget);
            if(beta < 15){
                SmallShips.shootCannonsSmallShip(this.captain, (Boat) this.captain.getVehicle(), target, shootLeftSide);
            }
        }
    }

    public double enemyDistanceToLeader(Entity entity){
        return entity.distanceToSqr(this.captain.position());
    }

}
