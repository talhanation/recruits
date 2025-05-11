package com.talhanation.recruits.compat.siegeweapons;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public abstract class SiegeWeapon {
    public Entity entity;
    public SiegeEngineerEntity siegeEngineer;
    public abstract boolean canShoot();
    public abstract void loadProjectile(int projectileIndex);
    public abstract void trigger(boolean trigger);

    public Entity getEntity(){
        return entity;
    }

    public static boolean isSiegeWeapon(Entity entity) {
        if(entity == null) return false;
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if (siegeweaponClass.isInstance(entity)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {

        }
        return false;
    }

    public boolean isSiegeEngineerDriver(){
        List<Entity> passengers = entity.getPassengers();
        return !passengers.isEmpty() && passengers.get(0).equals(siegeEngineer);
    }


    public void steerLeft(boolean left) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setLeft = siegeweaponClass.getDeclaredMethod("setLeft", boolean.class);
                setLeft.invoke(siegeweapon, left);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    public void steerRight(boolean right) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setRight = siegeweaponClass.getDeclaredMethod("setRight", boolean.class);
                setRight.invoke(siegeweapon, right);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    public void forward(boolean forward) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setForward = siegeweaponClass.getDeclaredMethod("setForward", boolean.class);
                setForward.invoke(siegeweapon, forward);

            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    public void backward(boolean backward) {
        try {
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if (siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method setBackward = siegeweaponClass.getDeclaredMethod("setBackward", boolean.class);
                setBackward.invoke(siegeweapon, backward);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }


    public void repairSiegeWeapon(SiegeEngineerEntity siegeEngineer) {
        int amount = (10 + siegeEngineer.getCommandSenderWorld().random.nextInt(5));
        try{
            if (Main.isSmallShipsLoaded && Main.isSmallShipsCompatible && entity.getEncodeId().contains("siegeweapons")) {
                Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
                if(siegeweaponClass.isInstance(entity)) {
                    Object siegeweapon = siegeweaponClass.cast(entity);

                    Method getHealthMethod = siegeweaponClass.getMethod("getHealth");
                    float health = (float) getHealthMethod.invoke(siegeweapon);

                    if(health < 90) {
                        Method siegeweaponRepairMethod = siegeweaponClass.getMethod("repairVehicle", int.class);
                        siegeweaponRepairMethod.invoke(siegeweapon, amount);

                        for (int i = 0; i < siegeEngineer.getInventory().getContainerSize(); ++i) {
                            ItemStack itemStack = siegeEngineer.getInventory().getItem(i);
                            if (itemStack.is(ItemTags.PLANKS)) {
                                itemStack.shrink(1);
                                break;
                            }
                        }

                        for (int i = 0; i < siegeEngineer.getInventory().getContainerSize(); ++i) {
                            ItemStack itemStack = siegeEngineer.getInventory().getItem(i);
                            if (itemStack.is(Items.IRON_NUGGET)) {
                                itemStack.shrink(1);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }
    }

    public float getHealth() {
        float damage = 0;
        try{
            Class<?> siegeweaponClass = Class.forName("com.talhanation.siegeweapons.entities.AbstractVehicleEntity");
            if(siegeweaponClass.isInstance(entity)) {
                Object siegeweapon = siegeweaponClass.cast(entity);

                Method getHealthMethod = siegeweaponClass.getMethod("getHealth");
                damage = (float) getHealthMethod.invoke(siegeweapon);

            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Main.LOGGER.info("siegeweaponClass was not found");
        }

        return damage;
    }


    @Nullable
    public static ItemStack getSiegeWeaponItem() {
        return ForgeRegistries.ITEMS.getDelegateOrThrow(ResourceLocation.tryParse("siegeweapons:catapult")).get().getDefaultInstance();
    }
}
