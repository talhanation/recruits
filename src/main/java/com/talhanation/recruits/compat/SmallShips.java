package com.talhanation.recruits.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.util.Kalkuel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

import static com.talhanation.recruits.util.Kalkuel.*;

public class SmallShips {

    private final Boat boat;
    private final CaptainEntity captain;

    public SmallShips(Boat boat, CaptainEntity captain) {
        this.boat = boat;
        this.captain = captain;
    }

    public Boat getBoat(){
        return boat;
    }

    public static boolean isSmallShip(Entity entity) {
        if(entity == null) return false;
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(entity)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {

        }
        return false;
    }

    public boolean isCaptainDriver(){
        List<Entity> passengers = boat.getPassengers();
        return !passengers.isEmpty() && passengers.get(0).equals(captain);
    }

    public float getShipSpeed() {
        float speed = 0;
        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method shipClassGetSpeed = shipClass.getMethod("getSpeed");
                speed = (float) shipClassGetSpeed.invoke(ship);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }

        return speed;
    }

    public void setSailState(int state) {
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(boat)) {
                Field coolDownField = shipClass.getField("sailStateCooldown");
                int coolDown = coolDownField.getInt(boat);
                if (coolDown == 0) {
                    Class<?> sailableClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Sailable");
                    if (sailableClass.isInstance(boat)) {
                        Object sailable = sailableClass.cast(boat);
                        Method getSailStateCooldown = sailableClass.getMethod("getSailStateCooldown");
                        int configCoolDown = (int) getSailStateCooldown.invoke(sailable);
                        Method setSailState = sailableClass.getMethod("setSailState", byte.class);
                        Method getSailState = sailableClass.getMethod("getSailState");
                        byte currentSail = (byte) getSailState.invoke(sailable);
                        if (currentSail != (byte) state) {
                            setSailState.invoke(sailable, (byte) state);
                        }
                        coolDownField.setInt(boat, configCoolDown);
                    }
                }
            }
        } catch (Exception e) {
            Main.LOGGER.info("shipClass oder Sailable-Klasse nicht gefunden: " + e.getMessage());
        }
    }

    /*


    public void steerLeft() {
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method setRight = shipClass.getDeclaredMethod("setRight", boolean.class);
                Method setLeft = shipClass.getDeclaredMethod("setLeft", boolean.class);
                setRight.invoke(ship, false);
                setLeft.invoke(ship, true);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }
     */

    /*
    public void steerRight() {
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method setRight = shipClass.getDeclaredMethod("setRight", boolean.class);
                Method setLeft = shipClass.getDeclaredMethod("setLeft", boolean.class);
                setRight.invoke(ship, true);
                setLeft.invoke(ship, false);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

     */

    public void rotateShip(boolean inputLeft, boolean inputRight) {
        float maxRotSp = 2.0F;
        float rotAcceleration = 0.35F;
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);
                Method getRotSpeed = shipClass.getMethod("getRotSpeed");
                Method setRotSpeed = shipClass.getMethod("setRotSpeed", float.class);
                Method updateControls = shipClass.getMethod("updateControls", boolean.class, boolean.class, boolean.class, boolean.class, net.minecraft.world.entity.player.Player.class);

                float boatRotSpeed = (float) getRotSpeed.invoke(ship);
                updateControls.invoke(ship, false, false, inputLeft, inputRight, null);

                float rotationSpeed = subtractToZero(boatRotSpeed, getVelocityResistance() * 2.5F);
                if (inputRight) {
                    if (rotationSpeed < maxRotSp) {
                        rotationSpeed = Math.min(rotationSpeed + rotAcceleration / 8, maxRotSp);
                    }
                }
                if (inputLeft) {
                    if (rotationSpeed > -maxRotSp) {
                        rotationSpeed = Math.max(rotationSpeed - rotAcceleration / 8, -maxRotSp);
                    }
                }
                boat.deltaRotation = rotationSpeed;
                boat.setYRot(boat.getYRot() + boat.deltaRotation);
                setRotSpeed.invoke(ship, rotationSpeed);
            }
        } catch (Exception e) {
            Main.LOGGER.info("shipClass nicht gefunden während rotateShip: " + e.getMessage());
        }
    }

    // Hilfsmethode, um den Rotationswert langsam in Richtung 0 zu ziehen
    private static float subtractToZero(float value, float amount) {
        if (value > 0) {
            return Math.max(value - amount, 0);
        } else if (value < 0) {
            return Math.min(value + amount, 0);
        }
        return 0;
    }

    private static float getVelocityResistance() {
        return 0.007F;
    }

    public void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor) {
        // Beispielhafte Logik:
        // - Falls der Kapitän in Slot 0 sitzt, wird weiter gemacht.
        if (boat.getPassengers().get(0).equals(captain)) {
            String id = boat.getEncodeId();
            if (Main.isSmallShipsLoaded && Main.isSmallShipsCompatible && id.contains("smallships")) {
                //updateSmallShipsRotation(posX, posZ);
            } else {
                updateVanillaBoatControl(posX, posZ, speedFactor, turnFactor);
            }
        }
    }
    public void updateControl() {
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if (shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method controlBoat = shipClass.getDeclaredMethod("controlBoat");
                controlBoat.setAccessible(true);
                controlBoat.invoke(ship);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("" + e );
        }
    }
    public void updateSmallShipsControl(boolean inputLeft, boolean inputRight, int state) {
        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);
                Method shipClassIsLeashed = shipClass.getMethod("isShipLeashed");

                boolean isLeashed = (boolean) shipClassIsLeashed.invoke(ship);

                if(!boat.isInWater() || isLeashed) return;

                rotateShip(inputLeft, inputRight);
                setSailState(state);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }
    public void updateSmallShipsControl(double posX, double posZ, int state) {
        Vec3 forward = boat.getForward().yRot(-90).normalize();
        Vec3 target = new Vec3(posX, 0, posZ);
        Vec3 toTarget = boat.position().subtract(target);

        double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
        double ref = 63.334F;
        boolean inputLeft = (phi < ref);
        boolean inputRight = (phi > ref);

        double deviation = Math.abs(phi - ref);
        double stopThreshold = ref * 0.80F;
        double slowThreshold = ref * 0.35F;

        if (deviation > stopThreshold) {
            state = 0;
        } else if (deviation > slowThreshold) {
            state = 1;
        }

        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);
                Method shipClassIsLeashed = shipClass.getMethod("isShipLeashed");

                boolean isLeashed = (boolean) shipClassIsLeashed.invoke(ship);

                if(!boat.isInWater() || isLeashed) return;

                rotateShip(inputLeft, inputRight);
                setSailState(state);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }
    public static void rotateSmallShip(Boat boat, boolean inputLeft, boolean inputRight){
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

    static boolean rotateShipTowardsPos(Boat boat, Vec3 targetVec){
        boolean rotated = false;

        if(targetVec != null) {
            Vec3 forward = boat.getForward().normalize();
            Vec3 target = new Vec3(targetVec.x, 0, targetVec.y);
            Vec3 toTarget = boat.position().subtract(target).normalize();

            double phi = Kalkuel.horizontalAngleBetweenVectors(forward, toTarget);
            double ref = 63.334F;
            boolean inputLeft = (phi < ref);
            boolean inputRight = (phi > ref);

            rotateSmallShip(boat, inputLeft, inputRight);

            if (Math.abs(phi - ref) <= ref * 0.35F) {
                rotated = true;
            }
        }
        return rotated;
    }

    private void setSmallShipsSailState(Boat boat, int state){
        try {
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");

            Field coolDownFlied = shipClass.getField("sailStateCooldown");
            if(coolDownFlied != null){
                int coolDown = coolDownFlied.getInt(boat);

                if(coolDown == 0){
                    try{
                        Class<?> sailableClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Sailable");
                        if(sailableClass.isInstance(boat)){
                            Object sailable = sailableClass.cast(boat);
                            if(sailable != null){
                                Method sailableClassGetSailStateCooldown = sailableClass.getMethod("getSailStateCooldown");
                                int configCoolDown = (int) sailableClassGetSailStateCooldown.invoke(sailable);


                                Method sailableClassSetSailState = sailableClass.getMethod("setSailState", byte.class);
                                Method sailableClassGetSailState = sailableClass.getMethod("getSailState");
                                byte currentSail = (byte) sailableClassGetSailState.invoke(sailable);
                                if(currentSail != (byte) state) sailableClassSetSailState.invoke(sailable, (byte) state);

                                coolDownFlied.setInt(boat, configCoolDown);
                            }
                        }
                    }
                    catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        Main.LOGGER.info("SailableClass was not found");
                    }
                }
            }

        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e ) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    // Standardsteuerung für Vanilla-Boote
    private void updateVanillaBoatControl(double posX, double posZ, double speedFactor, double turnFactor) {
        boat.setDeltaMovement(calculateMotionX((float) speedFactor, boat.getYRot()),
                boat.getDeltaMovement().y,
                calculateMotionZ((float) speedFactor, boat.getYRot()));
    }


    public boolean hasCannons() {
        try{
            Class<?> cannonAbleClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Cannonable");
            if(cannonAbleClass.isInstance(boat)){
                Object cannonAble = cannonAbleClass.cast(boat);
                Method cannonAbleClassGetCannons = cannonAbleClass.getMethod("getCannons");

                List<?> list = (List<?>) cannonAbleClassGetCannons.invoke(cannonAble);
                return list.size() > 0;
            }
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("Cannonable Class was not found");
        }
        return false;
    }
    public boolean canShootCannons() {
        return canShootCannons(boat);
    }
    public static boolean canShootCannons(Entity vehicle) {
        if(vehicle instanceof Boat boat) {
            try{
                Class<?> cannonAbleClass = Class.forName("com.talhanation.smallships.world.entity.ship.abilities.Cannonable");
                if(cannonAbleClass.isInstance(boat)){
                    Object cannonAble = cannonAbleClass.cast(boat);
                    Method cannonAbleClassCanShootCannons = cannonAbleClass.getMethod("canShoot");

                    return (boolean) cannonAbleClassCanShootCannons.invoke(cannonAble);
                }
            }
            catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                Main.LOGGER.info("Cannonable Class was not found");
            }
        }
        return false;
    }


    public static void shootCannonsSmallShip(CaptainEntity driver, Boat boat, Entity target, boolean leftSide){
        if(boat == null || target == null || driver == null) return;

        double distanceToTarget = driver.distanceToSqr(target);
        //Main.LOGGER.info("Distance: " + distanceToTarget);
        double speed = 3.2F;
        double accuracy = 2F;// 0 = 100% left right accuracy
        float rotation = leftSide ? (3.14F / 2) : -(3.14F / 2);

        Vec3 shootVec = boat.getForward().yRot(rotation).normalize();
        double heightDiff = target.getY() - driver.getY();
        double angle = IRangedRecruit.getCannonAngleDistanceModifier(distanceToTarget, 2) + IRangedRecruit.getCannonAngleHeightModifier(distanceToTarget, heightDiff)/ 100;
        double yShootVec = shootVec.y() + angle;
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

    @Nullable
    public static ItemStack getSmallShipsItem() {
        return ForgeRegistries.ITEMS.getDelegateOrThrow(ResourceLocation.tryParse("smallships:oak_cog")).get().getDefaultInstance();
    }

    public void repairShip(CaptainEntity captain) {
        int amount = (10 + captain.getCommandSenderWorld().random.nextInt(5));
        try{
            if (Main.isSmallShipsLoaded && Main.isSmallShipsCompatible && boat.getEncodeId().contains("smallships")) {
                Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
                if(shipClass.isInstance(boat)) {
                    Object ship = shipClass.cast(boat);

                    Method getDamageMethod = shipClass.getMethod("getDamage");
                    float damage = (float) getDamageMethod.invoke(ship);

                    if(damage > 5) {
                        Method shipRepairMethod = shipClass.getMethod("repairShip", int.class);
                        shipRepairMethod.invoke(ship, amount);

                        for (int i = 0; i < captain.getInventory().getContainerSize(); ++i) {
                            ItemStack itemStack = captain.getInventory().getItem(i);
                            if (itemStack.is(ItemTags.PLANKS)) {
                                itemStack.shrink(1);
                                break;
                            }
                        }

                        for (int i = 0; i < captain.getInventory().getContainerSize(); ++i) {
                            ItemStack itemStack = captain.getInventory().getItem(i);
                            if (itemStack.is(Items.IRON_NUGGET)) {
                                itemStack.shrink(1);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }
    }

    public float getDamage() {
        float damage = 0;
        try{
            Class<?> shipClass = Class.forName("com.talhanation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)) {
                Object ship = shipClass.cast(boat);

                Method shipClassGetDamage = shipClass.getMethod("getDamage");
                damage = (float) shipClassGetDamage.invoke(ship);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("shipClass was not found");
        }

        return damage;
    }


    public boolean isGalley() {
        return boat.getEncodeId().contains("galley");
    }
}

