package com.talhanation.recruits.entities.ai.controller.siegeengineer;

import com.talhanation.recruits.compat.siegeweapons.Ballista;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class SiegeWeaponBallistaController implements ISiegeController {
    public final boolean DEBUG = false;
    public static final int REACH = 50;
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

    public boolean left;
    public boolean right;
    public boolean forward;
    public double reach;
    private int recalcPath;

    // Face rotation
    private float faceYaw;
    private int faceTicks;


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

        // Apply controls
        ballista.forward(forward);
        ballista.setTurnLeft(left);
        ballista.setTurnRight(right);

        this.siegeEngineer.getLookControl().setLookAt(this.ballista.getEntity());

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
            resetControls();
            return;
        }

        if(updateAttacking()) return;

        // Movement
        calculateMovementPos();
        if(movementPos == null) return;

        distanceToMovementPos = siegeEngineer.distanceToSqr(movementPos.x(), siegeEngineer.getY(), movementPos.z());
        reach = siegeEngineer.getFollowState() == 1 || siegeEngineer.getFollowState() == 5 ? REACH * 2 : REACH;
        if(distanceToMovementPos < reach){
            currentNode = null;
            path = null;
            this.forward = false;
            this.left = false;
            this.right = false;
            if(DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": REACHED POS"));
            return;
        }

        if (movementPos != null) {
            Vec3 forward = ballista.getEntity().getForward().yRot((float) (Math.PI / 2)).normalize();
            Vec3 target = new Vec3(movementPos.x, 0, movementPos.z);
            Vec3 toTarget = target.subtract(ballista.getEntity().position()).normalize();

            double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
            if (DEBUG && siegeEngineer.getOwner() != null) this.siegeEngineer.getOwner().sendSystemMessage(Component.literal("phi: " + phi));

            double ref = 90;
            double tolerance = 2.0;

            left = phi < (ref - tolerance);
            right = phi > (ref + tolerance);

            if (DEBUG && siegeEngineer.getOwner() != null)
                this.siegeEngineer.getOwner().sendSystemMessage(Component.literal(siegeEngineer.getName().getString() + ": ROTATING"));

            if(phi > (ref + 20) || phi < (ref - 20)){
                this.forward = false;
                return;
            }
            else {
                this.forward = true;
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

    private void steerTowardYaw(double targetX, double targetZ, double forwardTolerance) {
        Vec3 ballistaForward = ballista.getEntity().getForward().normalize();
        Vec3 toTarget = new Vec3(targetX - ballista.getEntity().getX(), 0, targetZ - ballista.getEntity().getZ()).normalize();

        double angle = Kalkuel.horizontalAngleBetweenVectors(
                new Vec3(ballistaForward.x, 0, ballistaForward.z),
                new Vec3(toTarget.x, 0, toTarget.z)
        );

        double tolerance = 0.1;

        if(angle > tolerance){
            Vec3 cross = ballistaForward.cross(new Vec3(toTarget.x, 0, toTarget.z));
            left = cross.y > 0;
            right = cross.y < 0;
        }
        else {
            left = false;
            right = false;
        }
    }

    private void steerPitchToward(double targetX, double targetY, double targetZ) {
        double dx = targetX - ballista.getEntity().getX();
        double dz = targetZ - ballista.getEntity().getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double heightDiff = targetY - ballista.getEntity().getY();
        double pitch = IRangedRecruit.calcBallistaPitchAngle(horizontalDist, heightDiff);

        float currentPitch = ballista.getEntity().getXRot();
        float pitchDiff = (float) (pitch - currentPitch);

        float pitchTolerance = 0.10F;

        if(pitchDiff > pitchTolerance){
            ballista.setPitchUp(false);
            ballista.setPitchDown(true);
        }
        else if(pitchDiff < -pitchTolerance){
            ballista.setPitchUp(true);
            ballista.setPitchDown(false);
        }
        else {
            ballista.setPitchUp(false);
            ballista.setPitchDown(false);
        }
    }

    private boolean isAlignedToTarget() {
        Vec3 aim = targetPos;
        if(aim == null) return false;

        Vec3 ballistaForward = ballista.getEntity().getForward().normalize();
        Vec3 toTarget = new Vec3(aim.x - ballista.getEntity().getX(), 0, aim.z - ballista.getEntity().getZ()).normalize();
        double yawAngle = Kalkuel.horizontalAngleBetweenVectors(
                new Vec3(ballistaForward.x, 0, ballistaForward.z),
                new Vec3(toTarget.x, 0, toTarget.z)
        );

        double dx = aim.x - ballista.getEntity().getX();
        double dz = aim.z - ballista.getEntity().getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double heightDiff = aim.y - ballista.getEntity().getY();
        float desiredPitch = IRangedRecruit.calcBallistaPitchAngle(horizontalDist, heightDiff);
        float pitchDiff = Math.abs(desiredPitch - ballista.getEntity().getXRot());

        return yawAngle < 0.2 && pitchDiff < 2.0;
    }

    private void resetControls() {
        forward = false;
        left = false;
        right = false;
        ballista.setPitchUp(false);
        ballista.setPitchDown(false);
    }

    // ========================= FACE ROTATION =========================

    public void startFaceRotation(float yaw) {
        this.faceYaw = yaw;
        this.faceTicks = 200;
        resetControls();
    }

    private void updateFaceRotation() {
        Vec3 targetDir = new Vec3(-Math.sin(Math.toRadians(faceYaw)), 0, Math.cos(Math.toRadians(faceYaw)));
        Vec3 ballistaForward = ballista.getEntity().getForward().normalize();

        double angle = Kalkuel.horizontalAngleBetweenVectors(
                new Vec3(ballistaForward.x, 0, ballistaForward.z),
                new Vec3(targetDir.x, 0, targetDir.z)
        );

        if(angle < 3.0) {
            faceTicks = 0;
            left = false;
            right = false;
            this.forward = false;
            return;
        }

        Vec3 cross = ballistaForward.cross(targetDir);
        left = cross.y > 0;
        right = cross.y < 0;

        this.forward = false;
        faceTicks--;
    }

    // ========================= ATTACKING =========================

    private static final int LOAD_DELAY_TICKS = 25;
    private int loadDelay = 0;
    public Vec3 targetPos;
    public float distanceToTarget;
    public boolean noAmmoMessage = false;

    public boolean updateAttacking() {
        calculateTargetPos();

        if(targetPos != null){
            steerTowardYaw(targetPos.x, targetPos.z, 1);
            steerPitchToward(targetPos.x, targetPos.y, targetPos.z);
        }

        boolean isProjectileLoaded = ballista.isProjectileLoaded();
        boolean isLoaded = ballista.isLoaded();
        boolean isShot = ballista.isShot();

        // UNLOADED -> trigger(true) to start winding
        if (isShot) {
            ballista.trigger(true);
            resetControls();
            return true;
        }

        if (targetPos == null) {
            ballista.trigger(false);
            ballista.setPitchUp(false);
            ballista.setPitchDown(false);
            return false;
        }

        // LOADED -> load projectile
        if (isLoaded) {
            ballista.trigger(false);

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
                ballista.setPitchUp(false);
                ballista.setPitchDown(false);
                return false;
            }

            ballista.loadProjectile(projectile);
            loadDelay = LOAD_DELAY_TICKS;
            noAmmoMessage = false;
            return true;
        }

        if (isProjectileLoaded) {
            if (--loadDelay > 0) {
                return true;
            }
            forward = false;

            if(!isAlignedToTarget()){
                return true;
            }

            // Aligned - fire
            left = false;
            right = false;
            ballista.setPitchUp(false);
            ballista.setPitchDown(false);
            ballista.trigger(true);
            this.setTargetPos(null);
            if(DEBUG && siegeEngineer.getOwner() != null){
                siegeEngineer.getOwner().sendSystemMessage(Component.literal(
                        siegeEngineer.getName().getString() + ": FIRING ballista!"));
            }

            return true;
        }

        return false;
    }

    // ========================= TARGET TRACKING =========================

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

    // ========================= MOVEMENT =========================

    public Vec3 getTargetPos(){
        return targetPos;
    }

    @Override
    public void setTargetPos(Vec3 vec3) {
        this.targetPos = vec3;
    }

    private void calculateMovementPos() {
        movementPos = null;
        int movementState = siegeEngineer.getFollowState();

        switch (movementState) {
            case 0 -> {
                if(siegeEngineer.getShouldMovePos() && siegeEngineer.getMovePos() != null){
                    movementPos = siegeEngineer.getMovePos().getCenter();
                }
            }
            case 1 -> {
                LivingEntity owner = siegeEngineer.getOwner();
                if(owner != null) movementPos = owner.position();
            }
            case 2, 4, 3 -> {
                Vec3 holdPos = siegeEngineer.getHoldPos();
                if(holdPos != null) movementPos = holdPos;
            }
            case 5 -> {
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
            ballista.setTurnLeft(false);
            ballista.setTurnRight(false);
            ballista.setPitchUp(false);
            ballista.setPitchDown(false);
        }
    }

    @Override
    public Entity getSiegeEntity() {
        if(ballista == null) return null;
        else return ballista.getEntity();
    }
}
