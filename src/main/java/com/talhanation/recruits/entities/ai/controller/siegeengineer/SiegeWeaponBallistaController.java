package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.siegeweapons.Ballista;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncPath;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Controller for the Ballista siege weapon.
 * Unlike the Catapult, the Ballista aims where the driver looks.
 * Movement uses forward only - steering is done by changing the driver's look direction via LookControl.
 * The ballista rotates to match the driver's head rotation.
 */
public class SiegeWeaponBallistaController implements ISiegeController {
    public final boolean DEBUG = true;
    public static final int REACH = 80;
    public static final int RECALCULATION_TIME = 200;
    public static final float REPAIR_THRESHOLD = 90F;
    public static final int REPAIR_COOLDOWN = 40;
    public static final float MIN_ENGAGE_DISTANCE = 200;
    public static final float MAX_ENGAGE_DISTANCE = 8000;
    public final SiegeEngineerEntity siegeEngineer;
    public Ballista ballista;
    private final Level world;
    private final AsyncGroundPathNavigation pathNavigation;
    private Node currentNode;
    public double distanceToMovementPos;
    public Vec3 movementPos;
    public Path path;

    public boolean forward;
    public double reach;
    private int recalcPath;

    // Face rotation
    private float faceYaw;
    private int faceTicks;

    // Projectile feedback
    private float rangeAdjustment = 0;
    private Vec3 lastShotTargetPos;
    private float lastShotRange;
    private int lastShotTick = -1;
    private static final int PROJECTILE_FLIGHT_TIME = 40;
    private static final float MAX_RANGE_ADJUSTMENT = 6F;
    private static final float RANGE_ADJUSTMENT_STEP = 1.0F;

    // Repair
    private int repairCooldown;

    public SiegeWeaponBallistaController(SiegeEngineerEntity siegeEngineer, Level world){
        pathNavigation = new RecruitPathNavigation(siegeEngineer, world);
        this.world = world;
        this.siegeEngineer = siegeEngineer;
        this.pathNavigation.setCanFloat(false);
    }

    public void tryMount(Entity entity) {
        if(Ballista.isBallista(entity)){
            ballista = new Ballista(entity, this.siegeEngineer);
        }
    }
    public void tryDismount() {
        this.reset();
        Entity entity = this.siegeEngineer.getVehicle();
        if (SiegeWeapon.isSiegeWeapon(entity)) {
            ballista = null;
        }
    }

    public void tick(){
        if(this.world.isClientSide() || this.siegeEngineer.level().isClientSide()) return;
        if(siegeEngineer.getVehicle() == null || ballista == null) return;
        if(!ballista.isSiegeEngineerDriver()) return;

        ballista.forward(forward);

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
            forward = false;
            return;
        }

        // Projectile feedback check
        updateProjectileFeedback();

        if(updateAttacking()) return;

        // Movement
        calculateMovementPos();
        if(movementPos == null) return;

        distanceToMovementPos = siegeEngineer.distanceToSqr(movementPos.x(), siegeEngineer.getY(), movementPos.z());
        reach = siegeEngineer.getFollowState() == 1 || siegeEngineer.getFollowState() == 5 ? REACH * 2 : REACH;
        if(distanceToMovementPos < reach ){
            currentNode = null;
            path = null;
            this.forward = false;
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": REACHED POS"));
            return;
        }

        //DEFAULT PATHFINDING
        if (--recalcPath <= 0) {
            recalcPath = RECALCULATION_TIME;

            if(movementPos == null) return;

            this.path = pathNavigation.createPath(this.movementPos.x,this.movementPos.y, this.movementPos.z, 0);
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": CREATING PATH"));

            if(path != null && (!(path instanceof AsyncPath ap) || ap.isProcessed())){
                try {
                    this.currentNode = path.getNextNode();
                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
                }
            }
        }

        if (path != null && currentNode != null) {
            double distanceToNode = this.siegeEngineer.getVehicle().distanceToSqr(currentNode.x, this.siegeEngineer.getVehicle().getY(), currentNode.z);

            if (distanceToNode < REACH) {
                path.advance();
                // Selber Guard nach advance() — Pfad könnte immer noch unverarbeitet sein
                if (!(path instanceof AsyncPath ap2) || ap2.isProcessed()) {
                    try {
                        this.currentNode = path.getNextNode();
                    } catch (IndexOutOfBoundsException e) {
                        this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
                    }
                }
            }

            // Look at the current node - the ballista will rotate to match
            siegeEngineer.getLookControl().setLookAt(currentNode.x, siegeEngineer.getY(), currentNode.z, 30.0F, 30.0F);
            this.forward = true;

            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FOLLOWING PATH"));
        }
    }

    // ========================= FACE ROTATION =========================

    public void startFaceRotation(float yaw) {
        this.faceYaw = yaw;
        this.faceTicks = 100;
        this.forward = false;
    }

    private void updateFaceRotation() {
        // Look in the target direction - the ballista will follow
        double targetX = siegeEngineer.getX() + (-Math.sin(Math.toRadians(faceYaw))) * 10;
        double targetZ = siegeEngineer.getZ() + (Math.cos(Math.toRadians(faceYaw))) * 10;
        siegeEngineer.getLookControl().setLookAt(targetX, siegeEngineer.getY(), targetZ, 30.0F, 30.0F);

        this.forward = false;
        faceTicks--;

        if(faceTicks <= 0){
            faceTicks = 0;
        }
    }

    // ========================= PROJECTILE FEEDBACK =========================

    private void updateProjectileFeedback() {
        if(lastShotTick < 0) return;

        int ticksSinceShot = siegeEngineer.tickCount - lastShotTick;
        if(ticksSinceShot < PROJECTILE_FLIGHT_TIME) return;

        if(lastShotTargetPos != null){
            List<LivingEntity> nearbyEnemies = siegeEngineer.getCommandSenderWorld()
                    .getEntitiesOfClass(LivingEntity.class, siegeEngineer.getBoundingBox().inflate(200D)).stream()
                    .filter(target -> siegeEngineer.shouldAttack(target) && target.distanceToSqr(lastShotTargetPos) < 64)
                    .toList();

            if(!nearbyEnemies.isEmpty()){
                float currentRange = lastShotRange + rangeAdjustment;
                float idealRange = IRangedRecruit.calcBaseRangeForBallista((float) ballista.entity.distanceToSqr(lastShotTargetPos.x, ballista.entity.position().y, lastShotTargetPos.z));

                if(currentRange < idealRange){
                    rangeAdjustment += RANGE_ADJUSTMENT_STEP;
                }
                else {
                    rangeAdjustment -= RANGE_ADJUSTMENT_STEP;
                }
                rangeAdjustment = Math.max(-MAX_RANGE_ADJUSTMENT, Math.min(MAX_RANGE_ADJUSTMENT, rangeAdjustment));

                if(DEBUG && siegeEngineer.getOwner() != null){
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                            siegeEngineer.getName().getString() + ": MISSED! Range adj: " + String.format("%.1f", rangeAdjustment)));
                }
            }
            else {
                rangeAdjustment *= 0.5F;
                if(Math.abs(rangeAdjustment) < 0.5F) rangeAdjustment = 0;
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

    private static final int LOAD_DELAY_TICKS = 25;
    private int loadDelay = 0;
    public Vec3 targetPos;
    public float distanceToTarget;
    public boolean noAmmoMessage = false;
    public int range;
    public boolean updateAttacking() {
        calculateTargetPos();

        boolean isProjectileLoaded = ballista.isProjectileLoaded();
        boolean isLoaded = ballista.isLoaded();
        boolean isShot = ballista.isShot();
        boolean isLoading = ballista.isLoading();

        if (isShot || isLoading) {
            ballista.trigger(true);
            forward = false;
            return true;
        }

        if (targetPos == null) {
            ballista.trigger(false);
            return false;
        }

        if (isLoaded) {
            ballista.trigger(false);
        }

        // Aim at target using LookControl - the ballista follows the driver's look
        siegeEngineer.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 30.0F, 30.0F);
        forward = false; // Stop moving while aiming

        // Check if we're looking close enough to the target
        if(!isLookingAtTarget()){
            return true; // Still rotating
        }

        range = this.calcRange(distanceToTarget);
        ballista.setRange(range);

        if (!isProjectileLoaded) {
            ItemStack projectile = ballista.getProjectile(siegeEngineer);

            if (projectile == null) {
                if (!noAmmoMessage) {
                    noAmmoMessage = true;
                    if (siegeEngineer.getOwner() != null) {
                        siegeEngineer.getOwner().sendSystemMessage(
                                Component.literal(siegeEngineer.getName().getString() + ": I have no Ammo for the ballista.")
                        );
                    }
                }
                return false;
            }

            if (isLoaded) {
                ballista.loadProjectile(projectile);
                loadDelay = LOAD_DELAY_TICKS;
                noAmmoMessage = false;
            }

            return true;
        }

        if (--loadDelay > 0) {
            return true;
        }

        recordShot(range);
        ballista.trigger(true);
        return true;
    }

    private boolean isLookingAtTarget() {
        if(targetPos == null) return false;

        Vec3 lookDir = siegeEngineer.getViewVector(1.0F).normalize();
        Vec3 toTarget = new Vec3(targetPos.x - siegeEngineer.getX(), 0, targetPos.z - siegeEngineer.getZ()).normalize();
        Vec3 lookFlat = new Vec3(lookDir.x, 0, lookDir.z).normalize();

        double dot = lookFlat.x * toTarget.x + lookFlat.z * toTarget.z;
        // dot > 0.99 means within ~8 degrees
        return dot > 0.99;
    }

    private int calcRange(float distanceToTarget) {
        int heightDiff = (int) (targetPos.y - ballista.entity.getY());
        float baseRange = IRangedRecruit.calcBaseRangeForBallista(distanceToTarget);

        float heightCorrection = 0;
        if(heightDiff > 0){
            heightCorrection = IRangedRecruit.applyBallistaPositiveHeightCorrection(distanceToTarget, heightDiff);
        }
        else heightCorrection = IRangedRecruit.applyBallistaNegativeHeightCorrection(distanceToTarget, heightDiff);

        return (int) (baseRange + heightCorrection + rangeAdjustment);
    }

    // ========================= REPAIR =========================

    @Override
    public boolean needsRepair() {
        if(ballista == null) return false;
        return ballista.getHealth() < REPAIR_THRESHOLD;
    }

    @Override
    public boolean canRepair() {
        return siegeEngineer.getInventory().hasAnyMatching(itemStack -> itemStack.is(Items.IRON_NUGGET))
                && siegeEngineer.getInventory().hasAnyMatching(itemStack -> itemStack.is(ItemTags.PLANKS));
    }

    @Override
    public void tryRepair() {
        if(ballista == null) return;
        ballista.repairSiegeWeapon(siegeEngineer);

        if(DEBUG && siegeEngineer.getOwner() != null){
            siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                    siegeEngineer.getName().getString() + ": Repairing ballista. Health: " + String.format("%.0f", ballista.getHealth())));
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
            this.distanceToTarget = (float) this.ballista.entity.distanceToSqr(targetPos.x, this.ballista.entity.position().y, targetPos.z);

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

    public void calculatePath() {
        this.recalcPath = 0;
    }

    public void reset(){
        if(ballista != null){
            ballista.trigger(false);
            ballista.forward(false);
            ballista.steerLeft(false);
            ballista.steerRight(false);
            ballista.backward(false);
        }
    }

    @Override
    public Entity getSiegeEntity() {
        if(ballista == null) return null;
        else return ballista.getEntity();
    }
}