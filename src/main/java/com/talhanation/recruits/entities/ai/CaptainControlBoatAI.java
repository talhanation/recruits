package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.IBoatController;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Optional;

import static com.talhanation.recruits.entities.ai.CaptainControlBoatAI.State.*;

public class CaptainControlBoatAI extends Goal {

    private final CaptainEntity captain;
    private State state;
    private Path path;
    private Node node;
    private BlockPos sailPos;
    private int timer;
    private float precision;
    private final boolean DEBUG = true;

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
        if (!captain.getLevel().isClientSide()) {
            if(DEBUG) {
                if (this.captain.getOwner() != null && captain.getOwner().isInWater()) {
                    captain.setSailPos(captain.getOwner().getOnPos());
                    this.state = IDLE;
                }
            }

            if(sailPosChanged()){
                this.state = CREATING_PATH;
            }

            switch (state) {

                case IDLE -> {
                    captain.setSailPos(Optional.empty());
                    captain.setSmallShipsSailState((Boat) captain.getVehicle(), 0);

                    if (captain.getSailPos() != null) {
                        this.state = CREATING_PATH;
                    }
                }

                case CREATING_PATH -> {
                    if (captain.getSailPos() != null) {
                        this.sailPos = captain.getSailPos();
                        SailorPathNavigation sailorPathNavigation = (SailorPathNavigation) captain.getNavigation();
                        this.path = sailorPathNavigation.createPath(this.sailPos, 16, false, 0);

                        if (path != null) {
                            this.node = this.path.getNextNode();

                            if(DEBUG){
                                for(Node node : this.path.nodes) {
                                    captain.level.setBlock(new BlockPos(node.x, captain.getY() + 4, node.z), Blocks.ICE.defaultBlockState(), 3);
                                }
                            }
                            state = State.MOVING_PATH;
                        }
                    } else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    double distance = this.captain.distanceToSqr(node.x, captain.getY(), node.z);
                    if(DEBUG) {
                        Main.LOGGER.info("################################");
                        Main.LOGGER.info("State: " + this.state);
                        Main.LOGGER.info("Precision: " + precision);
                        Main.LOGGER.info("distance to node: " + distance);
                        Main.LOGGER.info("################################");
                    }

                    if(distance <= precision){
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

                    if(distance >= 5F){
                        captain.updateBoatControl(node.x, node.z, 1.0F, 1.1F, path);
                    }

                    if(captain.distanceToSqr(sailPos.getX(), captain.getY(), sailPos.getZ()) < 25F){
                        node = null;
                        path = null;
                        state = IDLE;
                    }
                }
            }
        }
    }

    private boolean isNeighborsWater(Node node){
        for(int i = -2; i <= 2; i++) {
            for (int k = -2; k <= 2; k++) {
                BlockPos pos = new BlockPos(node.x, this.captain.getY(), node.z).offset(i, 0, k);
                BlockState state = this.captain.level.getBlockState(pos);

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
    }
    private boolean sailPosChanged(){
        return this.captain.getSailPos() != null && !this.captain.getSailPos().equals(this.sailPos);
    }
}
