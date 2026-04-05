package com.talhanation.recruits.compat.siegeweapons;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection wrapper for BallistaEntity from the SiegeWeapons mod.
 *
 * BallistaEntity state machine (from source):
 *   UNLOADED(0) - needs winding. trigger(true) starts 100-tick loading -> LOADED
 *   LOADING(1)  - exists in enum but unused in tick
 *   LOADED(2)   - wound up, ready for projectile insertion
 *   PROJECTILE_LOADED(3) - ready to fire. trigger(true) -> shoots -> UNLOADED
 *
 * NPC Control (added methods):
 *   setNpcControlled(true) - disables driver look override, enables manual aim
 *   setPitchUp/setPitchDown - vertical aim control
 *   setLeft/setRight from AbstractVehicleEntity - yaw rotation (works when npcControlled)
 */
public class Ballista extends SiegeWeapon {
    public Ballista(Entity entity, SiegeEngineerEntity siegeEngineer) {
        this.siegeEngineer = siegeEngineer;
        this.entity = entity;
    }

    public static boolean isBallista(Entity entity) {
        if(entity == null) return false;
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {

        }
        return false;
    }

    // ========================= STATE CHECKS =========================
    // BallistaState: UNLOADED(0), LOADING(1), LOADED(2), PROJECTILE_LOADED(3)

    private int getStateIndex(){
        try{
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if(ballistaClass.isInstance(entity)){
                Object ballista = ballistaClass.cast(entity);
                Method getState = ballistaClass.getMethod("getState");
                Object state = getState.invoke(ballista);

                Method getIndex = state.getClass().getMethod("getIndex");
                return (int) getIndex.invoke(state);
            }
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
        return -1;
    }

    public boolean isShot(){
        return getStateIndex() == 0; // UNLOADED - after shooting, needs winding
    }

    public boolean isLoading(){
        return getStateIndex() == 1; // LOADING - unused in tick
    }

    public boolean isLoaded(){
        return getStateIndex() == 2; // LOADED - ready for projectile
    }

    public boolean isProjectileLoaded(){
        return getStateIndex() == 3; // PROJECTILE_LOADED - ready to fire
    }

    @Override
    public boolean isShooting() {
        return false; // Ballista has no SHOOTING animation state
    }

    // ========================= ACTIONS =========================

    public void trigger(boolean trigger) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method setTriggering = ballistaClass.getDeclaredMethod("setTriggering", boolean.class);
                setTriggering.invoke(ballista, trigger);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
    }

    @Override
    public void setRange(float range) {
        // No-op: Ballista trajectory determined by look direction, not range
    }

    @Override
    public boolean canShoot() {
        return isProjectileLoaded();
    }

    @Override
    public void loadProjectileWithIndex(int projectileIndex) {
        // No-op: Ballista uses ItemStack-based loading
    }

    @Override
    public int getProjectileIndex(SiegeEngineerEntity siegeEngineer) {
        return 0; // Not used for Ballista
    }

    // ========================= NPC CONTROL =========================

    public void setPitchUp(boolean pitchUp) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method method = ballistaClass.getDeclaredMethod("setPitchUp", boolean.class);
                method.invoke(ballista, pitchUp);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaEntity.setPitchUp was not found");
        }
    }

    public void setPitchDown(boolean pitchDown) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method method = ballistaClass.getDeclaredMethod("setPitchDown", boolean.class);
                method.invoke(ballista, pitchDown);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaEntity.setPitchDown was not found");
        }
    }

    public void setTurnRight(boolean turnRight) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method method = ballistaClass.getDeclaredMethod("setTurnRight", boolean.class);
                method.invoke(ballista, turnRight);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaEntity.setPitchUp was not found");
        }
    }

    public void setTurnLeft(boolean turnLeft) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method method = ballistaClass.getDeclaredMethod("setTurnLeft", boolean.class);
                method.invoke(ballista, turnLeft);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaEntity.setPitchDown was not found");
        }
    }

    // ========================= PROJECTILE LOADING =========================

    @Override
    public void loadProjectile(ItemStack projectile) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            Class<?> ballistaStateEnumClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity$BallistaState");

            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                // Set state to PROJECTILE_LOADED(3)
                Object projectileLoadedState = ballistaStateEnumClass.getEnumConstants()[3];
                Method setState = ballistaClass.getMethod("setState", ballistaStateEnumClass);
                setState.invoke(ballista, projectileLoadedState);

                Method playSound = ballistaClass.getMethod("playLoadedSound");
                playSound.invoke(ballista);

                projectile.shrink(1);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 IllegalArgumentException e) {
            Main.LOGGER.error("Error loading ballista projectile: " + e.getMessage());
        }
    }

    @Nullable
    public ItemStack getProjectile(SiegeEngineerEntity siegeEngineer) {
        SimpleContainer inventory = siegeEngineer.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack stack = inventory.getItem(i);
            String name = stack.getDescriptionId();

            if(name.contains("ballista_projectile")){
                return stack;
            }
        }

        return null;
    }
}
