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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class SiegeWeaponCatapultController implements ISiegeController {
    public final boolean DEBUG = true;
    public static final int REACH = 115;
    public static final int RECALCULATION_TIME = 300;
    public static final float REPAIR_THRESHOLD = 90F;
    public static final int REPAIR_COOLDOWN = 40;
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

    // Projectile feedback
    private float rangeAdjustment = 0;
    private Vec3 lastShotTargetPos;
    private float lastShotRange;
    private int lastShotTick = -1;
    private static final int PROJECTILE_FLIGHT_TIME = 80;
    private static final float MAX_RANGE_ADJUSTMENT = 8F;
    private static final float RANGE_ADJUSTMENT_STEP = 1.5F;

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
                // Target is behind - move backward with minimal rotation
                Vec3 toTargetReversed = toTarget.scale(-1);
                double phiBackward = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, toTargetReversed);

                double ref = 90;
                double tolerance = 2.0;

                // Inverted steering for backward movement
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
                // Target is in front or to the side - normal forward movement
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
            if(path != null && (!(path instanceof com.talhanation.recruits.pathfinding.AsyncPath ap) || ap.isProcessed())){
                try {
                    this.currentNode = path.getNextNode();
                } catch (IndexOutOfBoundsException e) {
                    this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
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

                if (!(path instanceof com.talhanation.recruits.pathfinding.AsyncPath ap2) || ap2.isProcessed()) {
                    try {
                        this.currentNode = path.getNextNode();
                    } catch (IndexOutOfBoundsException e) {
                        this.currentNode = path.nodes.isEmpty() ? null : path.nodes.get(path.nodes.size() - 1);
                    }
                }
            }

            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": FOLLOWING PATH"));
        }
    }

    // ========================= FACE ROTATION =========================

    public void startFaceRotation(float yaw) {
        this.faceYaw = yaw;
        this.faceTicks = 200;
        resetSteering();
        this.forward = false;
        this.backward = false;
    }

    private void updateFaceRotation() {
        Vec3 targetDir = new Vec3(-Math.sin(Math.toRadians(faceYaw)), 0, Math.cos(Math.toRadians(faceYaw)));
        Vec3 catapultFacing = catapult.getEntity().getForward().yRot((float) (Math.PI/2)).normalize();

        double angle = Kalkuel.horizontalAngleBetweenVectors(catapultFacing, targetDir);

        if(Math.abs(angle) < 5) {
            faceTicks = 0;
            resetSteering();
            return;
        }

        Vec3 cross = catapultFacing.cross(targetDir);
        left = cross.y > 0;
        right = cross.y < 0;

        this.forward = false;
        this.backward = false;
        faceTicks--;
    }

    // ========================= PROJECTILE FEEDBACK =========================

    private void updateProjectileFeedback() {
        if(lastShotTick < 0) return;

        int ticksSinceShot = siegeEngineer.tickCount - lastShotTick;
        if(ticksSinceShot < PROJECTILE_FLIGHT_TIME) return;

        if(lastShotTargetPos != null){
            // Check if enemies are still alive near the target position
            boolean enemiesStillAlive = !siegeEngineer.getCommandSenderWorld()
                    .getEntitiesOfClass(LivingEntity.class, siegeEngineer.getBoundingBox().inflate(200D)).stream()
                    .filter(target -> siegeEngineer.shouldAttack(target) && target.distanceToSqr(lastShotTargetPos) < 100)
                    .toList().isEmpty();

            if(enemiesStillAlive){
                // Shot missed - adjust range
                float currentRange = lastShotRange + rangeAdjustment;
                float idealRange = IRangedRecruit.calcBaseRangeForCatapult((float) catapult.entity.distanceToSqr(lastShotTargetPos.x, catapult.entity.position().y, lastShotTargetPos.z));

                if(currentRange < idealRange){
                    // Undershooting - increase range
                    rangeAdjustment += RANGE_ADJUSTMENT_STEP;
                }
                else {
                    // Overshooting - decrease range
                    rangeAdjustment -= RANGE_ADJUSTMENT_STEP;
                }

                // Clamp adjustment
                rangeAdjustment = Math.max(-MAX_RANGE_ADJUSTMENT, Math.min(MAX_RANGE_ADJUSTMENT, rangeAdjustment));

                if(DEBUG && siegeEngineer.getOwner() != null){
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                            siegeEngineer.getName().getString() + ": MISSED! Adjusting range by " + String.format("%.1f", rangeAdjustment)));
                }
            }
            else {
                // Hit! Slowly reduce adjustment back toward 0
                rangeAdjustment *= 0.5F;
                if(Math.abs(rangeAdjustment) < 0.5F) rangeAdjustment = 0;

                if(DEBUG && siegeEngineer.getOwner() != null){
                    siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                            siegeEngineer.getName().getString() + ": HIT! Range adjustment: " + String.format("%.1f", rangeAdjustment)));
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
    private boolean wasJustLoaded = false;
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

        // Range setzen mit Feedback-Adjustment
        range = this.calcRange(distanceToTarget);
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

        // Fire and record shot for feedback
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
                    siegeEngineer.getName().getString() + ": Repairing siege weapon. Health: " + String.format("%.0f", catapult.getHealth())));
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

            if(distanceToTarget < 900 || distanceToTarget > 19000){
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
