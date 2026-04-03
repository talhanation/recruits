package com.talhanation.recruits.compat.siegeweapons;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public boolean isLoaded(){
        try{
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if(ballistaClass.isInstance(entity)){
                Object ballista = ballistaClass.cast(entity);
                Method getState = ballistaClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(ballista);
                return state.ordinal() == 2;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
        return false;
    }

    public boolean isProjectileLoaded(){
        try{
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if(ballistaClass.isInstance(entity)){
                Object ballista = ballistaClass.cast(entity);
                Method getState = ballistaClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(ballista);
                return state.ordinal() == 3;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
        return false;
    }

    public boolean isShot(){
        try{
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if(ballistaClass.isInstance(entity)){
                Object ballista = ballistaClass.cast(entity);
                Method getState = ballistaClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(ballista);
                return state.ordinal() == 0;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
        return false;
    }

    public boolean isLoading(){
        try{
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            if(ballistaClass.isInstance(entity)){
                Object ballista = ballistaClass.cast(entity);
                Method getState = ballistaClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(ballista);
                return state.ordinal() == 1;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("BallistaClass was not found");
        }
        return false;
    }

    @Override
    public boolean isShooting() {
        return false;
    }

    @Override
    public void setRange(float range) {

    }

    @Override
    public int getProjectileIndex(SiegeEngineerEntity siegeEngineer) {
        return 0;
    }

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
    public boolean canShoot() {
        return isProjectileLoaded();
    }

    @Override
    public void loadProjectileWithIndex(int projectileIndex) {

    }

    @Override
    public void loadProjectile(ItemStack projectile) {
        try {
            Class<?> ballistaClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity");
            Class<?> ballistaStateEnumClass = Class.forName("com.talhanation.siegeweapons.entities.BallistaEntity$BallistaState");

            if (ballistaClass.isInstance(entity)) {
                Object ballista = ballistaClass.cast(entity);

                Method setProjectile = ballistaClass.getDeclaredMethod("loadProjectile", ItemStack.class);
                setProjectile.invoke(ballista, projectile);

                Object state = ballistaStateEnumClass.getEnumConstants()[3];
                Method setState = ballistaClass.getMethod("setState", ballistaStateEnumClass);
                setState.invoke(ballista, state);

                Method playSound = ballistaClass.getMethod("playLoadedSound");
                playSound.invoke(ballista);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 IllegalArgumentException e) {
            Main.LOGGER.error("Error setting ballista projectile: " + e.getMessage());
        }
    }
    @Nullable
    public ItemStack getProjectile(SiegeEngineerEntity siegeEngineer) {
        SimpleContainer inventory = siegeEngineer.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack stack = inventory.getItem(i);
            String name = stack.getDescriptionId();

            if(name.contains("siegeweapons")){
                if(name.contains("ballista_projectile_item")){
                    stack.shrink(1);
                    return stack;
                }
            }
        }

        return null;
    }
}
