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

    @Override
    public void loadProjectile(ItemStack projectile) {

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
    public void loadProjectileWithIndex(int projectileIndex) {
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

    public int getProjectileIndex(SiegeEngineerEntity siegeEngineer) {
        SimpleContainer inventory = siegeEngineer.getInventory();

        // Scan the whole inventory first and remember the slot of each ammo type, instead of
        // returning on the first matching slot. Otherwise cobblestone in an earlier slot would
        // always win and the special projectiles (fire / explosive / cobble cluster) could never
        // be loaded depending on inventory order.
        int fireSlot = -1;
        int explosiveSlot = -1;
        int clusterSlot = -1;
        int cobbleSlot = -1;

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack stack = inventory.getItem(i);
            if(stack.isEmpty()) continue;

            String name = stack.getDescriptionId();
            boolean isSiegeAmmo = name.contains("siegeweapons");

            if(isSiegeAmmo && name.contains("fire_pot_item")){
                if(fireSlot == -1) fireSlot = i;
            }
            else if(isSiegeAmmo && name.contains("explosion_pot_item")){
                if(explosiveSlot == -1) explosiveSlot = i;
            }
            else if(isSiegeAmmo && name.contains("cobble_cluster_item")){
                if(clusterSlot == -1) clusterSlot = i;
            }
            else if(stack.is(Items.COBBLESTONE)){
                if(cobbleSlot == -1) cobbleSlot = i;
            }
        }

        // Priority: special projectiles first, plain cobblestone only as a fallback.
        if(fireSlot != -1){
            inventory.getItem(fireSlot).shrink(1);
            return 2;
        }
        if(explosiveSlot != -1){
            inventory.getItem(explosiveSlot).shrink(1);
            return 3;
        }
        if(clusterSlot != -1){
            inventory.getItem(clusterSlot).shrink(1);
            return 4;
        }
        if(cobbleSlot != -1){
            inventory.getItem(cobbleSlot).shrink(1);
            return 1;
        }

        return 0;
    }

    @Override
    public ItemStack getProjectile(SiegeEngineerEntity siegeEngineer) {
        return null;
    }
}