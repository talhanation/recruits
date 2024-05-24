package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.IBoatController;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static com.talhanation.recruits.entities.ai.CaptainControlBoatAI.State.*;

public class CaptainControlBoatAI extends Goal {

    private final CaptainEntity captain;
    private LivingEntity target;
    private State state;
    private Path path;
    private Node node;
    private BlockPos sailPos;
    private int timer;
    private float precision;
    public final boolean DEBUG = false;
    private byte stoppingTimer = 0;
    private int attackingTimeOut = 0;
    public final int ATTACKING_TIME_OUT = 3000;

    public CaptainControlBoatAI(IBoatController sailor) {
        this.captain = sailor.getCaptain();

    }

    @Override
    public boolean canUse() {
        return this.captain.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.captain);
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean isInterruptable() {
        return false;
    }

    public void start(){
        state = IDLE;
        precision = 100F;
    }

    public void stop(){
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        //Main.LOGGER.info("State: " + state);

        if (!captain.getCommandSenderWorld().isClientSide()) {
            if(DEBUG) {
                if (this.captain.getOwner() != null && captain.getOwner().isInWater()) {
                    captain.setSailPos(captain.getOwner().getOnPos());
                    this.state = IDLE;
                }
            }

            if(captain.getFollowState() == 1 && captain.getOwner() != null){
                if(sailPos == null || captain.tickCount % 40 == 0){
                    captain.setSailPos(captain.getOwner().getOnPos());
                    this.sailPos = captain.getSailPos();
                    this.state = CREATING_PATH;
                }
            }
            else if (captain.getFollowState() == 5 && captain.getProtectingMob() != null){
                if(sailPos == null || captain.tickCount % 40 == 0){
                    captain.setSailPos(captain.getProtectingMob().getOnPos());
                    this.sailPos = captain.getSailPos();
                    this.state = CREATING_PATH;
                }
            }
            else if(sailPosChanged() && !captain.shipAttacking){
                this.sailPos = captain.getSailPos();
                this.state = CREATING_PATH;
            }

            switch (state) {

                case IDLE -> {

                    if(canAttackTarget()){
                        double distance = captain.distanceToSqr(target.getX(), captain.getY(), target.getZ());
                        this.captain.shipAttacking = true;
                        if(distance > 3500){
                            this.sailPos = captain.getTarget().getOnPos();
                            this.state = CREATING_PATH;
                        }
                        else{
                            this.state = ATTACKING;
                        }
                    }

                    else if (captain.getSailPos() != null) {
                        this.sailPos = captain.getSailPos();
                        this.state = CREATING_PATH;
                    }
                }

                case CREATING_PATH -> {
                    if (this.sailPos != null && captain.getNavigation() instanceof SailorPathNavigation sailorPathNavigation) {
                        this.path = sailorPathNavigation.createPath(this.sailPos, 32, false, 0);

                        if (path != null) {
                            this.node = this.path.getNextNode();

                            if(DEBUG){
                                for(Node node : this.path.nodes) {
                                    captain.getCommandSenderWorld().setBlock(new BlockPos(node.x, (int) (captain.getY() + 4), node.z), Blocks.ICE.defaultBlockState(), 3);
                                }
                            }
                            state = State.MOVING_PATH;
                        }
                    }
                    else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    if(canAttackTarget()){
                        double distanceToTarget = this.captain.distanceToSqr(target.getX(), captain.getY(), target.getZ());
                        if(distanceToTarget <= 3500){
                            this.captain.shipAttacking = true;
                            this.state = ATTACKING;
                            break;
                        }
                    }
                    double distanceToNode = this.captain.distanceToSqr(node.x, captain.getY(), node.z);

                    if(DEBUG) {
                        Main.LOGGER.info("################################");
                        Main.LOGGER.info("State: " + this.state);
                        Main.LOGGER.info("Precision: " + precision);
                        Main.LOGGER.info("distance to node: " + distanceToNode);
                        Main.LOGGER.info("################################");
                    }

                    if(distanceToNode <= precision){
                        path.advance();
                        if(isNeighborsWater(node)){
                            precision = captain.getPrecisionMax();
                        }
                        else precision = captain.getPrecisionMin();

                        if (path.getNodeCount() == path.getNextNodeIndex() - 1 || node.equals(path.getEndNode())) {
                            node = null;
                            state = State.CREATING_PATH;
                        }

                        try {
                            this.node = path.getNextNode();// FIX for "IndexOutOfBoundsException: Index 23 out of bounds for length 23" or "Index 1 out of bounds for length 1"

                        } catch (IndexOutOfBoundsException e) {
                            this.node = path.nodes.get(path.nodes.size() - 1);
                        }
                    }
                    else if (++timer > 50){
                        if(precision < 300) precision += 25;
                        else{
                            precision = 50F;
                            state = CREATING_PATH;
                        }
                        this.timer = 0;
                    }

                    if(distanceToNode >= 3F){
                        captain.updateBoatControl(node.x, node.z, 1.0F, 1.1F, path);
                    }
                    int reach = getTargetReach();
                    if(captain.distanceToSqr(sailPos.getX(), captain.getY(), sailPos.getZ()) < reach){
                        node = null;
                        path = null;
                        state = DONE;
                    }
                }

                case DONE -> {
                    captain.setSailPos(Optional.empty());
                    captain.setSmallShipsSailState((Boat) captain.getVehicle(), 0);
                    if(++this.stoppingTimer > 25){
                        this.stoppingTimer = 0;
                        state = captain.shipAttacking && canAttackTarget() ? ATTACKING : IDLE;
                    }
                }

                case ATTACKING -> {
                    if(canContinueAttack() && captain.shipAttacking && ++this.attackingTimeOut < ATTACKING_TIME_OUT){
                        Vec3 toTarget = target.getVehicle() != null ? captain.position().vectorTo(target.getVehicle().position()) : captain.position().vectorTo(target.position());
                        double distanceToTarget = this.captain.distanceToSqr(target.getX(), captain.getY(), target.getZ());
                        if(distanceToTarget > 5000){
                            this.captain.shipAttacking = false;
                            this.attackingTimeOut = 0;
                            this.target = null;
                            this.state = IDLE;
                            captain.setPatrolState(AbstractLeaderEntity.State.IDLE);
                            break;
                        }
                        captain.setSmallShipsSailState((Boat) captain.getVehicle(), 0);
                        Vec3 forward = this.captain.getVehicle().getForward().normalize();
                        Vec3 VecRight = forward.yRot(-3.14F / 2).normalize();
                        Vec3 VecLeft = forward.yRot(3.14F / 2).normalize();

                        double distanceToLeft = toTarget.distanceTo(VecLeft);
                        double distanceToRight = toTarget.distanceTo(VecRight);

                        boolean shootLeftSide = distanceToLeft < distanceToRight;

                        double alpha = IBoatController.horizontalAngleBetweenVectors(forward, toTarget);
                        double phi = shootLeftSide ?  -alpha : alpha;
                        double ref = shootLeftSide ? -90 : 90;

                        boolean inputLeft =  (phi < ref);
                        boolean inputRight = (phi > ref);

                        IBoatController.rotateSmallShip((Boat) this.captain.getVehicle(), inputLeft, inputRight);

                        double beta = shootLeftSide ? IBoatController.horizontalAngleBetweenVectors(forward.yRot(3.14F / 2), toTarget) : IBoatController.horizontalAngleBetweenVectors(forward.yRot(-3.14F / 2), toTarget);
                        if(beta < 10)
                            IBoatController.shootCannonsSmallShip(this.captain, (Boat) this.captain.getVehicle(), target, shootLeftSide);

                    }
                    else {
                        this.captain.shipAttacking = false;
                        this.attackingTimeOut = 0;
                        this.target = null;
                        captain.setPatrolState(AbstractLeaderEntity.State.IDLE);
                        this.state = IDLE;
                    }
                }
            }
        }
    }

    private boolean canAttackTarget(){
        if(captain.getTarget() != null){
            this.target = captain.getTarget();
            return captain.canAttackWhilePatrolling(target) && captain.getVehicle() != null && target != null && target.isAlive() && !target.isUnderWater();
        }
        return false;
    }

    private boolean canContinueAttack(){
        boolean notnull = target != null && target.isAlive();
        boolean can = captain.canAttackWhilePatrolling(target);
        boolean notUnderwater = !target.isUnderWater();
        //boolean seeSky = this.captain.getCommandSenderWorld().canSeeSky(target.getOnPos().above());
        return notnull && notUnderwater && can;
    }

    private int getTargetReach() {
        if(this.captain.getFollowState() == 1 || this.captain.getFollowState() == 5){
            return 1000;
        }
        else return captain.shipAttacking ? 3000 : 25;
    }

    private boolean isNeighborsWater(Node node){
        for(int i = -2; i <= 2; i++) {
            for (int k = -2; k <= 2; k++) {
                BlockPos pos = new BlockPos(node.x, (int) this.captain.getY(), node.z).offset(i, 0, k);
                BlockState state = this.captain.getCommandSenderWorld().getBlockState(pos);

                if(!state.is(Blocks.WATER) || (!state.is(Blocks.KELP_PLANT) || !state.is(Blocks.KELP)))
                    return false;
            }
        }
        return true;
    }
    enum State{
        IDLE,
        CREATING_PATH,
        MOVING_PATH,
        DONE,
        ATTACKING,
    }
    private boolean sailPosChanged(){
        return this.captain.getSailPos() != null && !this.captain.getSailPos().equals(this.sailPos);
    }
}
