package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface IBoatController {

    default CaptainEntity getCaptain() {
        return (CaptainEntity) this;
    }

    BlockPos getSailPos();

    float getPrecisionMin();
    float getPrecisionMax();
    void setSailPos(BlockPos pos);

    default void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor, Path path){
        if(getCaptain().getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.getCaptain())) {
            String string = boat.getEncodeId();
            if (Main.isSmallShipsLoaded && Main.isSmallShipsCompatible && (string.contains("smallships"))) {
                boolean onPosIsDeep = getWaterDepth(boat.getOnPos(), this.getCaptain()) >= 7;
                boolean following = getCaptain().getFollowState() == 1 && getCaptain().getOwner() != null;
                BlockPos targetPos = new BlockPos(posX, getCaptain().getY(), posZ);
                if(following){
                    boolean ownerOnPosIsDeep = getWaterDepth(getCaptain().getOwner().getOnPos(), this.getCaptain()) >= 7;
                    boolean ownerFar = getCaptain().distanceToSqr(getCaptain().getOwner()) > 50;
                    if (ownerOnPosIsDeep)
                        updateSmallShipsBoatControl(getCaptain(), boat, getCaptain().getOwner().getX(), getCaptain().getOwner().getZ(), ownerFar && onPosIsDeep);
                    else
                        updateSmallShipsBoatControl(getCaptain(), boat, posX, posZ, ownerFar && onPosIsDeep);
                }
                //MOVING TO POSITION / HOLD POS / MOVE / TARGET
                else if(onPosIsDeep && path != null && !boat.horizontalCollision){

                    boolean targetIsDeep = getWaterDepth(targetPos, this.getCaptain()) >= 7;
                    updateSmallShipsBoatControl(getCaptain(), boat, targetPos.getX(), targetPos.getZ(), targetIsDeep);
                }
                else
                    updateSmallShipsBoatControl(getCaptain(), boat, posX, posZ, false);
            }
            else
                updateVanillaBoatControl(boat, posX, posZ, speedFactor, turnFactor);
        }
    }
    default void updateSmallShipsBoatControl(CaptainEntity captainEntity, Boat boat, double posX, double posZ, boolean fast) {
        Vec3 forward = boat.getForward().yRot(-90).normalize();
        Vec3 target = new Vec3(posX, 0, posZ);
        Vec3 toTarget = boat.position().subtract(target).normalize();

        double phi = horizontalAngleBetweenVectors(forward, toTarget);
        //Main.LOGGER.info("phi: " + phi);
        double ref = 63.5F;
        boolean inputLeft =  (phi < ref);
        boolean inputRight = (phi > ref);
        boolean inputUp = Math.abs(phi - ref) <= ref * 0.35F;
        boolean inAngleForSail = Math.abs(phi - ref) <= ref * 0.80;

        float acceleration = 0.005F;
        float setPoint = 0;
        float boatSpeed = 0;

        /*
        try {
            Class<?> shipClass = Class.forName("talhnation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)){
                Object ship = shipClass.cast(boat);

                Method getSpeedMethod = ship.getClass().getMethod("getSpeed", float.class);
                boatSpeed = getSpeedMethod.
            }
        } catch (ClassNotFoundException e) {
            Main.LOGGER.info("smallShipsShipClass was not found");
        } catch (NoSuchMethodException e) {
            Main.LOGGER.info("setSpeedMethod was not found");
        } catch (InvocationTargetException | IllegalAccessException e) {
            Main.LOGGER.info("setSpeedMethod could not invocation");
        }
        */

        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method shipClassGetSpeed = shipClass.getMethod("getSpeed");
                Method shipClassUpdateControls = shipClass.getMethod("updateControls", boolean.class, boolean.class, boolean.class, boolean.class, Player.class);

                boatSpeed = (float) shipClassGetSpeed.invoke(ship);

                shipClassUpdateControls.invoke(ship,inputUp, false, inputLeft, inputRight, null);

                //TODO if(this.isInWater() && !((BoatLeashAccess) this).isLeashed()){ need smallsips update

                if(!inAngleForSail){
                    setSmallShipsSailState((Boat) ship,0);
                    setPoint = 0.02F;
                }
                else if (inputUp) {
                    double distance = toTarget.distanceToSqr(boat.position());
                    byte state = 3;
                    if(fast){
                        state = 4;
                        setPoint = 0.3F;
                    }
                    else if(distance > 20){
                        setPoint = 0.15F;
                    }
                    else{
                        setPoint = 0.075F;
                    }
                    setSmallShipsSailState((Boat) ship,state);
                }
                else if(getCaptain().getFollowState() == 1) {
                    setSmallShipsSailState((Boat) ship,1);
                    setPoint = 0.025F;
                }
                else{
                    setSmallShipsSailState((Boat) ship,0);
                    setPoint = 0.0F;
                }

                this.calculateSpeed(boat, boatSpeed, acceleration, setPoint);

                rotateSmallShip(boat, inputLeft, inputRight);

                //SET
                boat.setDeltaMovement(calculateMotionX(boatSpeed, boat.getYRot()), 0.0F, calculateMotionZ(boatSpeed, boat.getYRot()));
                //}


            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    static void rotateSmallShip(Boat boat, boolean inputLeft, boolean inputRight){
        float maxRotSp = 2.0F;
        float boatRotSpeed = 0;
        float rotAcceleration = 0.35F;
        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method shipClassSetRotSpeed = shipClass.getMethod("setRotSpeed", float.class);
                Method shipClassGetRotSpeed = shipClass.getMethod("getRotSpeed");
                Method shipClassUpdateControls = shipClass.getMethod("updateControls", boolean.class, boolean.class, boolean.class, boolean.class, Player.class);

                boatRotSpeed = (float) shipClassGetRotSpeed.invoke(ship);

                shipClassUpdateControls.invoke(ship,false, false, inputLeft, inputRight, null);

                //CALCULATE ROTATION SPEED//
                float rotationSpeed = subtractToZero(boatRotSpeed, getVelocityResistance() * 2.5F);

                if (inputRight) {
                    if (rotationSpeed < maxRotSp) {
                        rotationSpeed = Math.min(rotationSpeed + rotAcceleration * 1 / 8, maxRotSp);
                    }
                }

                if (inputLeft) {
                    if (rotationSpeed > -maxRotSp) {
                        rotationSpeed = Math.max(rotationSpeed - rotAcceleration * 1 / 8, -maxRotSp);
                    }
                }

                //ship.setRotSpeed(rotationSpeed);

                boat.deltaRotation = rotationSpeed;
                boat.setYRot(boat.getYRot() + boat.deltaRotation);

                shipClassSetRotSpeed.invoke(ship, rotationSpeed);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    default void setSmallShipsSailState(Boat boat, int state){
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            Field coolDownFlied = shipClass.getField("sailStateCooldown");
            int coolDown = coolDownFlied.getInt(boat);

            if(coolDown == 0){
                try{
                    Class<?> sailableClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Sailable");
                    if(sailableClass.isInstance(boat)){
                        Object sailable = sailableClass.cast(boat);
                        Method sailableClassGetSailStateCooldown = sailableClass.getMethod("getSailStateCooldown");
                        int configCoolDown = (int) sailableClassGetSailStateCooldown.invoke(sailable);


                        Method sailableClassSetSailState = sailableClass.getMethod("setSailState", byte.class);
                        Method sailableClassGetSailState = sailableClass.getMethod("getSailState");
                        byte currentSail = (byte) sailableClassGetSailState.invoke(sailable);
                        if(currentSail != (byte) state) sailableClassSetSailState.invoke(sailable, (byte) state);

                        coolDownFlied.setInt(boat, configCoolDown);
                    }

                }
                catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    Main.LOGGER.info("SailableClass was not found");
                }
            }

        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e ) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    static float getVelocityResistance(){
        return 0.007F;
    }

    static void shootCannonsSmallShip(CaptainEntity driver, Boat boat, LivingEntity target){
        double distanceToTarget = driver.distanceToSqr(target);
        double speed = 2.2F;
        double accuracy = 5F;// 0 = 100%
        Vec3 shootVec = getShootVector(boat.getForward(), driver);
        double yShootVec = shootVec.y() + distanceToTarget/48;
        try{
            Class<?> cannonAbleClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Cannonable");
            if(cannonAbleClass.isInstance(boat)){
                Object cannonAble = cannonAbleClass.cast(boat);
                Method cannonAbleClassTriggerCannons = cannonAbleClass.getMethod("triggerCannons", Vec3.class, double.class, LivingEntity.class, double.class, double.class);

                cannonAbleClassTriggerCannons.invoke(cannonAble,  shootVec, yShootVec, driver, speed, accuracy);
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("Cannonable Class was not found");
        }
    }

    default void updateVanillaBoatControl(Boat boat, double posX, double posZ, double speedFactor, double turnFactor){
        Vec3 forward = boat.getForward().yRot(-90).normalize();
        Vec3 target = new Vec3(posX, 0, posZ);
        Vec3 toTarget = boat.position().subtract(target).normalize();

        double phi = horizontalAngleBetweenVectors(forward, toTarget);
        //Main.LOGGER.info("phi: " + phi);
        double ref = 63.5F;
        boolean inputLeft =  (phi < ref);
        boolean inputRight = (phi > ref);
        boolean inputUp = Math.abs(phi - ref) <= ref * 0.15F;

        float f = 0.0F;

        if (inputLeft) {
            boat.setYRot((float) (boat.getYRot() - 2.5F));
        }

        if (inputRight) {
            boat.setYRot((float) (boat.getYRot() + 2.5F));
        }

        if (inputRight != inputLeft && !inputUp) {
            f += 0.005F * speedFactor;
        }

        if (inputUp) {
            f += 0.02F * speedFactor;
        }

        boat.setDeltaMovement(boat.getDeltaMovement().add((double)(Mth.sin(-boat.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(boat.getYRot() * ((float)Math.PI / 180F)) * f)));
        boat.setPaddleState(inputRight || inputUp, inputLeft || inputUp);
    }

    //Taken from Smallships/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void calculateSpeed(Boat boat, float speed, float acceleration, float setPoint) {
        if (speed < setPoint) {
            speed = addToSetPoint(speed, acceleration, setPoint);
        } else
            speed = subtractToZero(speed, getVelocityResistance() * 2.2F);

        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method shipClassSetSpeed = shipClass.getMethod("setSpeed", float.class);
                shipClassSetSpeed.invoke(ship, speed);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    /**
     * Adds from the provided number, but does not cross the set point
     *
     * @param current the current number
     * @param positiveChange the amount to add
     * @param setPoint the amount to not cross
     * @return the resulting number
     */
    private float addToSetPoint(float current, float positiveChange, float setPoint) {
        if (current < setPoint) {
            current = current + positiveChange;
        }
        return current;
    }


    private double calculateMotionX(float speed, float rotationYaw) {
        return Mth.sin(-rotationYaw * 0.017453292F) * speed;
    }

    private double calculateMotionZ(float speed, float rotationYaw) {
        return Mth.cos(rotationYaw * 0.017453292F) * speed;
    }

    /**
     * Subtracts from the provided number, but does not cross zero
     *
     * @param num the number
     * @param sub the amount to subtract
     * @return the resulting number
     */
    static float subtractToZero(float num, float sub) {
        float erg;
        if (num < 0F) {
            erg = num + sub;
            if (erg > 0F) {
                erg = 0F;
            }
        }
        else {
            erg = num - sub;
            if (erg < 0F) {
                erg = 0F;
            }
        }
        return erg;
    }

    static double horizontalAngleBetweenVectors(Vec3 vector1, Vec3 vector2) {
        double dotProduct = vector1.x * vector2.x + vector1.z * vector2.z;
        double magnitude1 = Math.sqrt(vector1.x * vector1.x + vector1.z * vector1.z);
        double magnitude2 = Math.sqrt(vector2.x * vector2.x + vector2.z * vector2.z);

        double cosTheta = dotProduct / (magnitude1 * magnitude2);

        return Math.toDegrees(Math.acos(cosTheta));
    }

    public static int getWaterDepth(BlockPos pos, LivingEntity cap){
        int depth = 0;
        for(int i = 0; i < 10; i++){
            BlockState state = cap.level.getBlockState(pos.below(i));
            if(state.is(Blocks.WATER) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.KELP)){
                depth++;
            }
            else break;
        }
        return depth;
    }

    //From smallships
    static Vec3 getShootVector(Vec3 forward, LivingEntity driver) {
        Vec3 VecRight = forward.yRot(-3.14F / 2).normalize();
        Vec3 VecLeft = forward.yRot(3.14F / 2).normalize();

        Vec3 playerVec = driver.getLookAngle().normalize();

        if (playerVec.distanceTo(VecLeft) > playerVec.distanceTo(VecRight)) {
            return VecRight;
        }

        if (playerVec.distanceTo(VecLeft) < playerVec.distanceTo(VecRight)) {
            return VecLeft;
        }
        return null;
    }
}
