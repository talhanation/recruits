package com.talhanation.recruits.entities.ai.controller;

import com.talhanation.recruits.compat.siegeweapons.Catapult;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.util.Kalkuel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class SiegeWeaponController {
    public final boolean DEBUG = true;
    public static final int REACH = 115;
    public static final int RECALCULATION_TIME = 300;
    public final SiegeEngineerEntity siegeEngineer;
    public SiegeWeapon siegeWeapon;
    private final Level world;
    private final PathNavigation pathNavigation;
    private Node currentNode;
    public double distanceToMovementPos;
    public Vec3 movementPos;
    public Path path;

    public boolean left;
    public boolean right;
    public boolean forward;
    public boolean backward;
    public double reach;
    private int recalcPath;
    public SiegeWeaponController(SiegeEngineerEntity siegeEngineer, Level world){
        pathNavigation = new RecruitPathNavigation(siegeEngineer, world);
        this.world = world;
        this.siegeEngineer = siegeEngineer;
        this.pathNavigation.setCanFloat(false);
    }

    public void tryMountSiegeWeapon(Entity entity) {
        if(Catapult.isCatapult(entity)){
            siegeWeapon = new Catapult(entity, this.siegeEngineer);
        }
        //else if(Ballista.isBallista(entity)){
        //  siegeWeapon = new Ballista(entity, this.siegeEngineer);
        //}
    }
    public void tryDismount() {
        Entity entity = this.siegeEngineer.getVehicle();
        if (SiegeWeapon.isSiegeWeapon(entity)) {
            siegeWeapon = null;
        }
    }

    public void tick(){
        if(this.world.isClientSide() || this.siegeEngineer.level().isClientSide()) return;
        if(siegeEngineer.getVehicle() == null || siegeWeapon == null) return;
        if(!siegeWeapon.isSiegeEngineerDriver()) return;

        siegeWeapon.forward(forward);
        siegeWeapon.steerLeft(left);
        siegeWeapon.steerRight(right);

        if(updateAttacking()) return;

        calculateMovementPos();
        if(movementPos == null) return;

        distanceToMovementPos = siegeEngineer.distanceToSqr(movementPos.x(), siegeEngineer.getY(), movementPos.z());
        reach = siegeEngineer.getFollowState() == 1 || siegeEngineer.getFollowState() == 5 ? REACH * 2 : REACH;
        if(distanceToMovementPos < reach ){//|| siegeEngineer.getTarget().isTargetInRange()
            currentNode = null;
            path = null;
            this.forward = false;
            this.right = false;
            this.left = false;
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": REACHED POS"));
            return;
        }

        if (movementPos != null) {
            Vec3 forward = siegeWeapon.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
            Vec3 target = new Vec3(movementPos.x, 0, movementPos.z);
            Vec3 toTarget = target.subtract(siegeWeapon.getEntity().position()).normalize();

            double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal("phi: " + phi));
            double ref = 90;
            double tolerance = 2.0;

            left = phi < (ref - tolerance);
            right = phi > (ref + tolerance);

            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": ROTATING"));
            if(phi > (ref + 20) || phi < (ref - 20)){
                this.forward = false;
                return;
            }
            else this.forward = true;
        }

        //DEFAULT PATHFINDING
        if (--recalcPath <= 0) {
            recalcPath = RECALCULATION_TIME;

            if(movementPos == null) return;

            this.path = pathNavigation.createPath(this.movementPos.x,this.movementPos.y, this.movementPos.z, 0);
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": CREATING PATH"));
            if(path != null){
                try {
                    this.currentNode = path.getNextNode();// FIX for "IndexOutOfBoundsException: Index 23 out of bounds for length 23" or "Index 1 out of bounds for length 1"

                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.get(path.nodes.size() - 1);
                }
            }
        }

        if(path != null && DEBUG){
            for(Node node : this.path.nodes) {
                //siegeEngineer.getCommandSenderWorld().setBlock(new BlockPos(node.x, (int) (siegeEngineer.getY() + 4), node.z), Blocks.ICE.defaultBlockState(), 3);
            }
        }

        if (path != null && currentNode != null) {
            double distanceToNode = this.siegeEngineer.getVehicle().distanceToSqr(currentNode.x, this.siegeEngineer.getVehicle().getY(), currentNode.z);

            if (distanceToNode < REACH) {
                path.advance();

                try {
                    this.currentNode = path.getNextNode();// FIX for "IndexOutOfBoundsException: Index 23 out of bounds for length 23" or "Index 1 out of bounds for length 1"

                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.get(path.nodes.size() - 1);
                }
            }

            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FOLLOWING PATH"));
        }

    }
    public Vec3 targetPos;
    public double distanceToTarget;
    private boolean updateAttacking() {
        getTargetPos();
        if(targetPos == null){
            return false;
        }

        if(siegeWeapon instanceof Catapult catapult){
            //if()

            if(!catapult.isProjectileLoaded()){
                if(!catapult.isLoaded()){
                    catapult.trigger(true);// load catapult
                }
                else{
                    catapult.trigger(false); //catapult is loaded need to add projectile

                    int index = catapult.getProjectile(siegeEngineer);
                    catapult.loadProjectile(index);
                }
            }
        }


        this.distanceToTarget = this.siegeWeapon.entity.distanceToSqr(targetPos.x, this.siegeWeapon.entity.position().y, targetPos.z);
        //if(distanceToTarget > attackRange) return false;

/*
        if(isTooFarFromMovementRange()){
            target = null;
            return false;
        }
*/

        if(this.rotateAndCheckAngle()) {
            if(siegeWeapon instanceof Catapult catapult){
                catapult.setRange(100); //Range 1 == 900distance;

                if(catapult.isProjectileLoaded()){
                    catapult.trigger(true);
                }
            }
        }


        return true;
    }

    private void getTargetPos() {
        if(siegeEngineer.tickCount % 20 == 0){

            if(siegeEngineer.getShouldStrategicFire()){
                this.targetPos = siegeEngineer.getStrategicFirePos().getCenter();
            }
        }
        //!target.isAlive() || target.isUnderWater() || !this.siegeEngineer.getSensing().hasLineOfSight(target)
        //this.target = ????;
    }

    private void calculateMovementPos() {
        movementPos = null;
        int movementState = siegeEngineer.getFollowState();

        switch (movementState) {
            case 0 -> { // Wander
                if(siegeEngineer.getShouldMovePos() && siegeEngineer.getMovePos() != null){
                    movementPos = siegeEngineer.getMovePos().getCenter();
                }
            }
            case 1 -> { // Follow
                LivingEntity owner = siegeEngineer.getOwner();
                if(owner != null) movementPos = owner.position();
            }
            case 2 -> { // Hold Position
                Vec3 holdPos = siegeEngineer.getHoldPos();
                if(holdPos != null) movementPos = holdPos;
            }
            case 5 -> { // Protect
                LivingEntity protect = siegeEngineer.getProtectingMob();
                if(protect != null) movementPos = protect.position();
            }
        }

    }

    private boolean rotateAndCheckAngle() {
        Vec3 forward = siegeWeapon.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
        Vec3 target = new Vec3(targetPos.x, 0, targetPos.z);
        Vec3 toTarget = target.subtract(siegeWeapon.getEntity().position()).normalize();

        double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);

        double ref = 90;
        double tolerance = 2.0;

        left = phi < (ref - tolerance);
        right = phi > (ref + tolerance);

        return !(phi > (ref + 20)) && !(phi < (ref - 20));
    }
    public void calculatePath() {
        this.recalcPath = 0;
    }

    public void resetSiegeWeapon(){
        if(siegeWeapon != null){
            siegeWeapon.trigger(false);
            siegeWeapon.forward(false);
            siegeWeapon.steerLeft(false);
            siegeWeapon.steerRight(false);
            siegeWeapon.backward(false);
        }
    }
}
