package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.siegeweapons.Catapult;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.IRangedRecruit;
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

public class SiegeWeaponCatapultController implements ISiegeController {
    public final boolean DEBUG = true;
    public static final int REACH = 115;
    public static final int RECALCULATION_TIME = 300;
    public final SiegeEngineerEntity siegeEngineer;
    public Catapult catapult;
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
    public SiegeWeaponCatapultController(SiegeEngineerEntity siegeEngineer, Level world){
        pathNavigation = new RecruitPathNavigation(siegeEngineer, world);
        this.world = world;
        this.siegeEngineer = siegeEngineer;
        this.pathNavigation.setCanFloat(false);
    }

    public void tryMount(Entity entity) {
        if(Catapult.isCatapult(entity)){
            catapult = new Catapult(entity, this.siegeEngineer);
        }
        //else if(Ballista.isBallista(entity)){
        //  siegeWeapon = new Ballista(entity, this.siegeEngineer);
        //}
    }
    public void tryDismount() {
        this.reset();
        Entity entity = this.siegeEngineer.getVehicle();
        if (SiegeWeapon.isSiegeWeapon(entity)) {
            catapult = null;
        }
    }

    public void tick(){
        if(this.world.isClientSide() || this.siegeEngineer.level().isClientSide()) return;
        if(siegeEngineer.getVehicle() == null || catapult == null) return;
        if(!catapult.isSiegeEngineerDriver()) return;

        catapult.forward(forward);
        catapult.steerLeft(left);
        catapult.steerRight(right);

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
            Vec3 forward = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
            Vec3 target = new Vec3(movementPos.x, 0, movementPos.z);
            Vec3 toTarget = target.subtract(catapult.getEntity().position()).normalize();

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

    private static final int LOAD_DELAY_TICKS = 50; // 2 Sekunden bei 20 TPS
    private int loadDelay = 0;
    private boolean wasJustLoaded = false;
    public Vec3 targetPos;
    public float distanceToTarget;
    public boolean noAmmoMessage = false;

    public boolean updateAttacking() {
        calculateTargetPos();

        boolean isProjectileLoaded = catapult.isProjectileLoaded();
        boolean isLoaded = catapult.isLoaded();
        boolean isShot = catapult.isShot();
        boolean isLoading = catapult.isLoading();
        boolean isShooting = catapult.isShooting();

        // Block Steuerung während Schussanimation
        if (isShooting) {
            resetSteering();
            return true;
        }

        // Automatisches Nachladen nach dem Schuss
        if (isShot || isLoading) {
            catapult.trigger(true);
            resetSteering();
            return true;
        }


        if (targetPos == null) {
            catapult.trigger(false);
            return false;
        }

        // Falls geladen, erstmal Trigger aus
        if (isLoaded) {
            catapult.trigger(false);
        }

        // Rotation und Zielausrichtung
        if (!rotateAndCheckAngle()) {
            return true; // Noch in Bewegung, weiter warten
        }

        // Range setzen
        float range = IRangedRecruit.calcRangeForCatapult(siegeEngineer, distanceToTarget);
        catapult.setRange(range);

        // Noch kein Projektil geladen
        if (!isProjectileLoaded) {
            int index = catapult.getProjectile(siegeEngineer);

            if (index == 0) {
                if (!noAmmoMessage) {
                    noAmmoMessage = true;
                    if (siegeEngineer.getOwner() != null) {
                        siegeEngineer.getOwner().sendSystemMessage(
                                Component.literal(siegeEngineer.getName().getString() + ": I have no Ammo for the catapult.")
                        );
                    }
                }
                return false;
            }

            if (isLoaded) {
                catapult.loadProjectile(index);
                loadDelay = LOAD_DELAY_TICKS;
                noAmmoMessage = false;
            }

            return true;
        }

        if (--loadDelay > 0) {
            return true;
        }

        catapult.trigger(true);
        return true;
    }

    private void resetSteering() {
        catapult.steerLeft(false);
        catapult.steerRight(false);
    }

/*
        if(isTooFarFromMovementRange()){
            target = null;
            return false;
        }
*/



    public Vec3 getTargetPos(){
        return targetPos;
    }

    @Override
    public void setTargetPos(Vec3 vec3) {
        this.targetPos = vec3;
    }

    private void calculateTargetPos() {
        if(siegeEngineer.tickCount % 20 != 0) return;

        if(siegeEngineer.getShouldStrategicFire()){
            this.setTargetPos(siegeEngineer.getStrategicFirePos().getCenter());
        }
        else {
            this.siegeEngineer.checkForPotentialEnemies();
        }

        if(targetPos != null){
            this.distanceToTarget = (float) this.catapult.entity.distanceToSqr(targetPos.x, this.catapult.entity.position().y, targetPos.z);

            if(distanceToTarget < 900 || distanceToTarget > 15000){
                targetPos = null;
            }
        }

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
        Vec3 forward = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
        Vec3 target = new Vec3(targetPos.x, 0, targetPos.z);
        Vec3 toTarget = target.subtract(catapult.getEntity().position()).normalize();

        double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);

        double ref = 90;
        double tolerance = 2.0;

        left = phi < (ref - tolerance);
        right = phi > (ref + tolerance);

        return !(phi > (ref + 4)) && !(phi < (ref - 4));
    }
    public void calculatePath() {
        this.recalcPath = 0;
    }

    public void reset(){
        if(catapult != null){
            catapult.trigger(false);
            catapult.forward(false);
            catapult.steerLeft(false);
            catapult.steerRight(false);
            catapult.backward(false);
        }
    }

    @Override
    public Entity getSiegeEntity() {
        if(catapult == null) return null;
        else return catapult.getEntity();
    }
}
