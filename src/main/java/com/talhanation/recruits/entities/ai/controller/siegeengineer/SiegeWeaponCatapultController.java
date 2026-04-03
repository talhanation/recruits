package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.siegeweapons.Catapult;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
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

import java.util.List;

public class SiegeWeaponCatapultController implements ISiegeController {
    public final boolean DEBUG = true;
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

    // Projectile feedback - range and lateral
    private float rangeAdjustment = 0;
    private float lateralAdjustment = 0; // positive = adjust right, negative = adjust left (in degrees)
    private Vec3 lastShotTargetPos;
    private float lastShotRange;
    private int lastShotTick = -1;
    private static final int PROJECTILE_FLIGHT_TIME = 80;
    private static final float MAX_RANGE_ADJUSTMENT = 8F;
    private static final float RANGE_ADJUSTMENT_STEP = 1.5F;
    private static final float MAX_LATERAL_ADJUSTMENT = 3F;
    private static final float LATERAL_ADJUSTMENT_STEP = 0.5F;

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

        // Projectile feedback check
        updateProjectileFeedback();

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
            Vec3 catapultFacing = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();
            Vec3 toTarget = new Vec3(movementPos.x - catapult.getEntity().getX(), 0, movementPos.z - catapult.getEntity().getZ()).normalize();

            // Smart backward movement: check if target is behind the catapult
            double dotForward = catapultFacing.x * toTarget.x + catapultFacing.z * toTarget.z;
            boolean targetBehind = dotForward < -0.3;

            if(targetBehind){
                Vec3 toTargetReversed = toTarget.scale(-1);
                double phiBackward = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTargetReversed);

                double ref = 90;
                double tolerance = 2.0;

                left = phiBackward > (ref + tolerance);
                right = phiBackward < (ref - tolerance);

                if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": MOVING BACKWARD"));

                if(phiBackward > (ref + 20) || phiBackward < (ref - 20)){
                    this.backward = false;
                    this.forward = false;
                    return;
                }
                else {
                    this.backward = true;
                    this.forward = false;
                }
            }
            else {
                double phi = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTarget);
                if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal("phi: " + phi));
                double ref = 90;
                double tolerance = 2.0;

                left = phi < (ref - tolerance);
                right = phi > (ref + tolerance);

                if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": ROTATING"));
                if(phi > (ref + 20) || phi < (ref - 20)){
                    this.forward = false;
                    this.backward = false;
                    return;
                }
                else {
                    this.forward = true;
                    this.backward = false;
                }
            }
        }

        //DEFAULT PATHFINDING
        if (--recalcPath <= 0) {
            recalcPath = RECALCULATION_TIME;

            if(movementPos == null) return;

            this.path = pathNavigation.createPath(this.movementPos.x,this.movementPos.y, this.movementPos.z, 0);
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": CREATING PATH"));
            if(path != null){
                try {
                    this.currentNode = path.getNextNode();
                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.get(path.nodes.size() - 1);
                }
            }
        }

        if (path != null && currentNode != null) {
            double distanceToNode = this.siegeEngineer.getVehicle().distanceToSqr(currentNode.x, this.siegeEngineer.getVehicle().getY(), currentNode.z);

            if (distanceToNode < REACH) {
                path.advance();
                try {
                    this.currentNode = path.getNextNode();
                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.get(path.nodes.size() - 1);
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
        // Same approach as SmallShipsController.updateFaceRotation()
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

    // ========================= PROJECTILE FEEDBACK (range + lateral) =========================

    private void updateProjectileFeedback() {
        if(lastShotTick < 0) return;

        int ticksSinceShot = siegeEngineer.tickCount - lastShotTick;
        if(ticksSinceShot < PROJECTILE_FLIGHT_TIME) return;

        if(lastShotTargetPos != null){
            // Find enemies still alive near the target position
            List<LivingEntity> nearbyEnemies = siegeEngineer.getCommandSenderWorld()
                    .getEntitiesOfClass(LivingEntity.class, siegeEngineer.getBoundingBox().inflate(200D)).stream()
                    .filter(target -> siegeEngineer.shouldAttack(target) && target.distanceToSqr(lastShotTargetPos) < 100)
                    .toList();

            if(!nearbyEnemies.isEmpty()){
                // Shot missed
                LivingEntity closestEnemy = nearbyEnemies.get(0);
                Vec3 catapultPos = catapult.entity.position();

                // Range adjustment
                float currentRange = lastShotRange + rangeAdjustment;
                float idealRange = IRangedRecruit.calcBaseRangeForCatapult((float) catapult.entity.distanceToSqr(lastShotTargetPos.x, catapultPos.y, lastShotTargetPos.z));

                if(currentRange < idealRange){
                    rangeAdjustment += RANGE_ADJUSTMENT_STEP;
                }
                else {
                    rangeAdjustment -= RANGE_ADJUSTMENT_STEP;
                }
                rangeAdjustment = Math.max(-MAX_RANGE_ADJUSTMENT, Math.min(MAX_RANGE_ADJUSTMENT, rangeAdjustment));

                // Lateral adjustment: check if enemy is left or right of our aim direction
                Vec3 aimDir = new Vec3(lastShotTargetPos.x - catapultPos.x, 0, lastShotTargetPos.z - catapultPos.z).normalize();
                Vec3 toEnemy = new Vec3(closestEnemy.getX() - catapultPos.x, 0, closestEnemy.getZ() - catapultPos.z).normalize();
                Vec3 cross = aimDir.cross(toEnemy);

                if(cross.y > 0.01){
                    // Enemy is to the left of our aim - adjust left (negative)
                    lateralAdjustment -= LATERAL_ADJUSTMENT_STEP;
                }
                else if(cross.y < -0.01){
                    // Enemy is to the right of our aim - adjust right (positive)
                    lateralAdjustment += LATERAL_ADJUSTMENT_STEP;
                }
                lateralAdjustment = Math.max(-MAX_LATERAL_ADJUSTMENT, Math.min(MAX_LATERAL_ADJUSTMENT, lateralAdjustment));

                if(DEBUG && siegeEngineer.getOwner() != null){
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                            siegeEngineer.getName().getString() + ": MISSED! Range adj: " + String.format("%.1f", rangeAdjustment)
                                    + " Lateral adj: " + String.format("%.1f", lateralAdjustment) + "°"));
                }
            }
            else {
                // Hit
                rangeAdjustment *= 0.5F;
                lateralAdjustment *= 0.5F;
                if(Math.abs(rangeAdjustment) < 0.5F) rangeAdjustment = 0;
                if(Math.abs(lateralAdjustment) < 0.2F) lateralAdjustment = 0;

                if(DEBUG && siegeEngineer.getOwner() != null){
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                            siegeEngineer.getName().getString() + ": HIT!"));
                }
            }
        }

        lastShotTick = -1;
        lastShotTargetPos = null;
    }

    private void recordShot(float range) {
        this.lastShotTick = siegeEngineer.tickCount;
        this.lastShotRange = range;
        this.lastShotTargetPos = targetPos != null ? new Vec3(targetPos.x, targetPos.y, targetPos.z) : null;
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

        if (isShot || isLoading) {
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

        recordShot(range);
        catapult.trigger(true);
        return true;
    }

    private int calcRange(float distanceToTarget) {
        int heightDiff = (int) (targetPos.y - catapult.entity.getY());
        float baseRange = IRangedRecruit.calcBaseRangeForCatapult(distanceToTarget);

        float heightCorrection = 0;
        if(heightDiff > 0){
            heightCorrection = IRangedRecruit.applyPositiveHeightCorrection(distanceToTarget, heightDiff);
        }
        else heightCorrection = IRangedRecruit.applyNegativeHeightCorrection(distanceToTarget, heightDiff);

        return (int) (baseRange + heightCorrection + rangeAdjustment);
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
            case 2, 4 -> { // Hold Position / Hold my position
                Vec3 holdPos = siegeEngineer.getHoldPos();
                if(holdPos != null) movementPos = holdPos;
            }
            case 3 -> { // Back to position (forward/backward commands)
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

        // Apply lateral adjustment: rotate the target position slightly
        Vec3 adjustedTarget;
        if(Math.abs(lateralAdjustment) > 0.1){
            double adjRad = Math.toRadians(lateralAdjustment);
            double tx = targetPos.x - catapult.getEntity().getX();
            double tz = targetPos.z - catapult.getEntity().getZ();
            double rotX = tx * Math.cos(adjRad) - tz * Math.sin(adjRad);
            double rotZ = tx * Math.sin(adjRad) + tz * Math.cos(adjRad);
            adjustedTarget = new Vec3(catapult.getEntity().getX() + rotX, 0, catapult.getEntity().getZ() + rotZ);
        }
        else {
            adjustedTarget = new Vec3(targetPos.x, 0, targetPos.z);
        }

        Vec3 toTarget = adjustedTarget.subtract(catapult.getEntity().position()).normalize();

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
