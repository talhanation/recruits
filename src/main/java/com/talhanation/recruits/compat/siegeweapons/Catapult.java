package com.talhanation.recruits.compat.siegeweapons;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Catapult extends SiegeWeapon {
    public Catapult(Entity entity, SiegeEngineerEntity siegeEngineer) {
        this.siegeEngineer = siegeEngineer;
        this.entity = entity;
    }

    public static boolean isCatapult(Entity entity) {
        if(entity == null) return false;
        try {
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if (catapultClass.isInstance(entity)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {

        }
        return false;
    }

    public boolean isLoaded(){
        try{
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if(catapultClass.isInstance(entity)){
                Object catapult = catapultClass.cast(entity);
                Method getState = catapultClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(catapult);
                return state.ordinal() == 1;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("CatapultClass was not found");
        }
        return false;
    }
    public boolean isProjectileLoaded(){
        try{
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if(catapultClass.isInstance(entity)){
                Object catapult = catapultClass.cast(entity);
                Method getState = catapultClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(catapult);
                return state.ordinal() == 2;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("CatapultClass was not found");
        }
        return false;
    }

    public boolean isShot(){
        try{
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if(catapultClass.isInstance(entity)){
                Object catapult = catapultClass.cast(entity);
                Method getState = catapultClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(catapult);
                return state.ordinal() == 4;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("CatapultClass was not found");
        }
        return false;
    }

    public boolean isLoading(){
        try{
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if(catapultClass.isInstance(entity)){
                Object catapult = catapultClass.cast(entity);
                Method getState = catapultClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(catapult);
                return state.ordinal() == 0;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("CatapultClass was not found");
        }
        return false;
    }

    public boolean isShooting(){
        try{
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if(catapultClass.isInstance(entity)){
                Object catapult = catapultClass.cast(entity);
                Method getState = catapultClass.getMethod("getState");
                Enum<?> state = (Enum<?>) getState.invoke(catapult);
                return state.ordinal() == 3;
            }

        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("CatapultClass was not found");
        }
        return false;
    }

    public void trigger(boolean trigger) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setTriggering = siegeweaponClass.getDeclaredMethod("setTriggering", boolean.class);

                setTriggering.invoke(siegeweapon, trigger);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    public void setRange(float range) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setRange = siegeweaponClass.getDeclaredMethod("setRange", float.class);

                setRange.invoke(siegeweapon, range);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    @Override
    public boolean canShoot() {
        return isProjectileLoaded();
    }

    @Override
    public void loadProjectile(int projectileIndex) {
        try {
            Class<?> catapultClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity");
            Class<?> projectileEnumClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity$CatapultProjectiles");
            Class<?> catapultStateEnumClass = Class.forName("com.talhanation.siegeweapons.entities.CatapultEntity$CatapultState");

            if (catapultClass.isInstance(entity)) {
                Object catapult = catapultClass.cast(entity);

                Object projectile = projectileEnumClass.getEnumConstants()[projectileIndex];

                Method setProjectile = catapultClass.getMethod("setProjectile", projectileEnumClass);
                setProjectile.invoke(catapult, projectile);

                Object state = catapultStateEnumClass.getEnumConstants()[2];
                Method setState = catapultClass.getMethod("setState", catapultStateEnumClass);
                setState.invoke(catapult, state);

                Method playSound = catapultClass.getMethod("playLoadedSound");
                playSound.invoke(catapult);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 IllegalArgumentException e) {
            Main.LOGGER.error("Error setting catapult projectile: " + e.getMessage());
        }
    }

    public int getProjectile(SiegeEngineerEntity siegeEngineer) {
        SimpleContainer inventory = siegeEngineer.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack stack = inventory.getItem(i);
            String name = stack.getDescriptionId();
            if(name.contains("siegeweapons") || stack.is(Items.COBBLESTONE)){
                if(stack.is(Items.COBBLESTONE)){
                    stack.shrink(1);
                    return 1;
                }
                else if(name.contains("fire_pot_item")){
                    stack.shrink(1);
                    return 2;
                }
                else if(name.contains("explosive_pot_item")){
                    stack.shrink(1);
                    return 3;
                }
                else if(name.contains("cobble_cluster_item")){
                    stack.shrink(1);
                    return 4;
                }
            }
        }

        return 0;
    }
}
