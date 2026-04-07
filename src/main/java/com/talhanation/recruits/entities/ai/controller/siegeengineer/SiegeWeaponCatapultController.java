package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.siegeweapons.Catapult;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPath;
import com.talhanation.recruits.util.Kalkuel;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class SiegeWeaponCatapultController implements ISiegeController {
    public final boolean DEBUG = false;
    public static final int REACH = 115;
    public static final int RECALCULATION_TIME = 300;
    public static final float REPAIR_THRESHOLD = 90F;
    public static final int REPAIR_COOLDOWN = 40;
    public static final float MIN_ENGAGE_DISTANCE = 900;
    public static final float MAX_ENGAGE_DISTANCE = 19000;
    public final SiegeEngineerEntity siegeEngineer;
    public Catapult catapult;
    private final Level world;
    private final AsyncGroundPathNavigation pathNavigation;
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

    // Face rotation
    private float faceYaw;
    private int faceTicks;


    // Repair
    private int repairCooldown;

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
        catapult.backward(backward);
        catapult.steerLeft(left);
        catapult.steerRight(right);

        this.siegeEngineer.getLookControl().setLookAt(this.catapult.getEntity());

        // Face rotation command
        if(faceTicks > 0){
            updateFaceRotation();
            return;
        }

        // Repair check
        if(needsRepair() && canRepair()){
            if(--repairCooldown <= 0){
                tryRepair();
                repairCooldown = REPAIR_COOLDOWN;
            }
            resetSteering();
            return;
        }

        if(updateAttacking()) return;

        calculateMovementPos();
        if(movementPos == null) return;

        distanceToMovementPos = siegeEngineer.distanceToSqr(movementPos.x(), siegeEngineer.getY(), movementPos.z());
        reach = siegeEngineer.getFollowState() == 1 || siegeEngineer.getFollowState() == 5 ? REACH * 2 : REACH;
        if(distanceToMovementPos < reach ){
            currentNode = null;
            path = null;
            this.forward = false;
            this.backward = false;
            this.right = false;
            this.left = false;
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": REACHED POS"));
            return;
        }

        if (movementPos != null) {
            Vec3 toTarget = new Vec3(movementPos.x - catapult.getEntity().getX(), 0, movementPos.z - catapult.getEntity().getZ()).normalize();

            // Use raw forward (without yRot offset) for front/behind detection
            Vec3 rawForward = catapult.getEntity().getForward().normalize();
            double dot = rawForward.x * toTarget.x + rawForward.z * toTarget.z;
            boolean targetBehind = dot < -0.3;

            // Use yRot offset forward for left/right steering (ref=90 system)
            Vec3 catapultFacing = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();

            if(targetBehind){
                // Steer with reversed target while going backward
                Vec3 toTargetReversed = toTarget.scale(-1);
                double phiBackward = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTargetReversed);

                double ref = 90;
                double tolerance = 2.0;

                left = phiBackward > (ref + tolerance);
                right = phiBackward < (ref - tolerance);

                if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": MOVING BACKWARD dot: " + String.format("%.2f", dot)));

                this.backward = true;
                this.forward = false;
            }
            else {
                double phi = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTarget);
                double ref = 90;
                double tolerance = 2.0;

                left = phi < (ref - tolerance);
                right = phi > (ref + tolerance);

                if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FORWARD phi: " + String.format("%.1f", phi) + " dot: " + String.format("%.2f", dot)));

                if(phi > (ref + 20) || phi < (ref - 20)){
                    this.forward = false;
                    this.backward = false;
                }
                else {
                    this.forward = true;
                    this.backward = false;
                }
            }
        }

        // Pathfinding
        if (--recalcPath <= 0) {
            recalcPath = RECALCULATION_TIME;
            if(movementPos == null) return;

            this.path = pathNavigation.createPath(this.movementPos.x, this.movementPos.y, this.movementPos.z, 0);
            if(DEBUG && siegeEngineer.getOwner() != null)
                this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": CREATING PATH"));

            if(path != null && (!(path instanceof AsyncPath ap) || ap.isProcessed())){
                try {
                    this.currentNode = path.getNextNode();
                }
                catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
                }
            }
        }

        if (path != null && currentNode != null) {
            double distanceToNode = this.siegeEngineer.getVehicle().distanceToSqr(currentNode.x, this.siegeEngineer.getVehicle().getY(), currentNode.z);

            if (distanceToNode < REACH) {
                path.advance();
                if (!(path instanceof AsyncPath ap2) || ap2.isProcessed()) {
                    try {
                        this.currentNode = path.getNextNode();
                    }
                    catch (IndexOutOfBoundsException e) {
                        this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
                    }
                }
            }

            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FOLLOWING PATH"));
        }
    }

    // ========================= FACE ROTATION (like SmallShipsController) =========================

    public void startFaceRotation(float yaw) {
        this.faceYaw = yaw;
        this.faceTicks = 200;
        resetSteering();
        this.forward = false;
        this.backward = false;
    }

    private void updateFaceRotation() {
        Vec3 targetDir = new Vec3(-Math.sin(Math.toRadians(faceYaw)), 0, Math.cos(Math.toRadians(faceYaw)));
        Vec3 weaponForward = catapult.getEntity().getForward().normalize();

        double angle = Kalkuel.horizontalAngleBetweenVectors(weaponForward, targetDir);

        if(Math.abs(angle) < 5) {
            faceTicks = 0;
            resetSteering();
            this.forward = false;
            this.backward = false;
            return;
        }

        Vec3 cross = weaponForward.cross(targetDir);
        left = cross.y > 0;
        right = cross.y < 0;

        this.forward = false;
        this.backward = false;
        faceTicks--;
    }


    // ========================= ATTACKING =========================

    private static final int LOAD_DELAY_TICKS = 50;
    private int loadDelay = 0;
    public Vec3 targetPos;
    public float distanceToTarget;
    public boolean noAmmoMessage = false;
    public int range;
    public boolean updateAttacking() {
        calculateTargetPos();

        boolean isProjectileLoaded = catapult.isProjectileLoaded();
        boolean isLoaded = catapult.isLoaded();
        boolean isShot = catapult.isShot();
        boolean isLoading = catapult.isLoading();
        boolean isShooting = catapult.isShooting();

        if (isShooting) {
            resetSteering();
            return true;
        }

        if (isShot) {
            catapult.trigger(true);
            resetSteering();
            this.setTargetPos(null);
            return true;
        }

        if (isLoading) {
            catapult.trigger(true);
            resetSteering();
            return true;
        }

        if (targetPos == null) {
            catapult.trigger(false);
            return false;
        }

        if (isLoaded) {
            catapult.trigger(false);
        }

        // Rotation with lateral adjustment applied
        if (!rotateAndCheckAngle()) {
            return true;
        }

        range = this.calcRange(distanceToTarget);
        catapult.setRange(range);

        if (!isProjectileLoaded) {
            int index = catapult.getProjectileIndex(siegeEngineer);

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
                catapult.loadProjectileWithIndex(index);
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

    private int calcRange(float distanceToTarget) {
        float heightDiff = (float) (targetPos.y - catapult.entity.getY());
        return (int) (IRangedRecruit.calcCatapultRange(distanceToTarget, heightDiff));
    }

    private void resetSteering() {
        catapult.steerLeft(false);
        catapult.steerRight(false);
    }

    // ========================= REPAIR =========================

    @Override
    public boolean needsRepair() {
        if(catapult == null) return false;
        return catapult.getHealth() < REPAIR_THRESHOLD;
    }

    @Override
    public boolean canRepair() {
        return siegeEngineer.getInventory().hasAnyMatching(itemStack -> itemStack.is(Items.IRON_NUGGET))
                && siegeEngineer.getInventory().hasAnyMatching(itemStack -> itemStack.is(ItemTags.PLANKS));
    }

    @Override
    public void tryRepair() {
        if(catapult == null) return;
        catapult.repairSiegeWeapon(siegeEngineer);

        if(DEBUG && siegeEngineer.getOwner() != null){
            siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                    siegeEngineer.getName().getString() + ": Repairing catapult. Health: " + String.format("%.0f", catapult.getHealth())));
        }
    }

    // ========================= TARGET & MOVEMENT =========================

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

            if(distanceToTarget < MIN_ENGAGE_DISTANCE || distanceToTarget > MAX_ENGAGE_DISTANCE){
                targetPos = null;
            }
        }
    }

    private void calculateMovementPos() {
        movementPos = null;
        int movementState = siegeEngineer.getFollowState();

        switch (movementState) {
            case 0 -> { // Wander / Move to pos
                if(siegeEngineer.getShouldMovePos() && siegeEngineer.getMovePos() != null){
                    movementPos = siegeEngineer.getMovePos().getCenter();
                }
            }
            case 1 -> { // Follow
                LivingEntity owner = siegeEngineer.getOwner();
                if(owner != null) movementPos = owner.position();
            }
            case 2, 4, 3 -> { // Hold Position / Hold my position
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
        Vec3 catapultFacing = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
        Vec3 target = new Vec3(targetPos.x, 0, targetPos.z);

        Vec3 toTarget = target.subtract(catapult.getEntity().position()).normalize();

        double phi = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTarget);

        double ref = 90;
        double tolerance = 1.0;

        left = phi < (ref - tolerance);
        right = phi > (ref + tolerance);

        return !(phi > (ref + 2)) && !(phi < (ref - 2));
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
