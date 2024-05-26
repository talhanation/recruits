package com.talhanation.recruits.compat;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import de.maxhenkel.corelib.death.Death;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class Corpse {

    public static void spawnCorpse(AbstractInventoryEntity recruit) {
        CompoundTag nbt = new CompoundTag();

        nbt.putUUID("PlayerUuid", recruit.getUUID());
        nbt.putString("PlayerName", recruit.getName().getString());

        List<ItemStack> list = new ArrayList<>(Arrays.asList(
                recruit.inventory.getItem(4),
                recruit.inventory.getItem(5),
                recruit.inventory.getItem(3),
                recruit.inventory.getItem(2),
                recruit.inventory.getItem(1),
                recruit.inventory.getItem(0)
        ));
        ListTag tagList = new ListTag();

        for (int i = 0; i < list.size(); ++i) {
            if (!(list.get(i)).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putInt("Slot", i);
                (list.get(i)).save(slot);
                tagList.add(slot);
            }
        }
        nbt.put("Equipment", tagList);

        List<ItemStack> list1 = new ArrayList<>(Arrays.asList(
                recruit.inventory.getItem(5),
                recruit.inventory.getItem(6),
                recruit.inventory.getItem(7),
                recruit.inventory.getItem(8),
                recruit.inventory.getItem(9),
                recruit.inventory.getItem(10),
                recruit.inventory.getItem(11),
                recruit.inventory.getItem(12),
                recruit.inventory.getItem(13),
                recruit.inventory.getItem(14)
        ));
        ListTag tagList1 = new ListTag();

        for (int k = 0; k < list1.size(); ++k) {
            if (!(list1.get(k)).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putInt("Slot", k);
                (list1.get(k)).save(slot);
                tagList1.add(slot);
            }
        }
        nbt.put("MainInventory", tagList1);


        List<ItemStack> list2 = new ArrayList<>(Arrays.asList(
                recruit.inventory.getItem(0),
                recruit.inventory.getItem(1),
                recruit.inventory.getItem(2),
                recruit.inventory.getItem(3)

        ));
        ListTag tagList2 = new ListTag();

        for (int k = 0; k < list2.size(); ++k) {
            if (!(list2.get(k)).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putInt("Slot", k);
                (list2.get(k)).save(slot);
                tagList2.add(slot);
            }
        }
        nbt.put("ArmorInventory", tagList2);

        List<ItemStack> list3 = new ArrayList<>(List.of(
                recruit.inventory.getItem(4)
        ));
        ListTag tagList3 = new ListTag();

        for (int k = 0; k < list3.size(); ++k) {
            if (!(list3.get(k)).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putInt("Slot", k);
                (list3.get(k)).save(slot);
                tagList3.add(slot);
            }
        }
        nbt.put("OffHandInventory", tagList3);

        nbt.putLong("Timestamp", System.currentTimeMillis());
        nbt.putInt("Experience", 6);

        nbt.putDouble("PosX", recruit.getX());
        nbt.putDouble("PosY", recruit.getY());
        nbt.putDouble("PosZ", recruit.getZ());
        nbt.putString("Dimension", recruit.getCommandSenderWorld().dimension().location().toString());
        nbt.putByte("Model", (byte) 0);

        Death death = Death.fromNBT(nbt);
        CompoundTag deathCompound = new CompoundTag();
        deathCompound.put("Death", death.toNBT(true));
        try {
            createCorpseFromDeath(recruit, death, deathCompound, (ServerLevel) recruit.getCommandSenderWorld());

        } catch (ClassNotFoundException e) {
            Main.LOGGER.warn("Was not able to spawn recruit corpse for " + recruit.getName().getString());
        }
    }

    public static void createCorpseFromDeath(AbstractInventoryEntity recruit, Death death, CompoundTag deathCompound, ServerLevel level) throws ClassNotFoundException {
        try {
            Class<?> corpseClass = Class.forName("de.maxhenkel.corpse.entities.CorpseEntity");

            Class<?>[] constructorParamTypes = {Level.class};
            Constructor<?> bulletConstructor = corpseClass.getConstructor(constructorParamTypes);

            Object corpseInstance = bulletConstructor.newInstance(level);

            Method readAdditionalSaveData = corpseClass.getDeclaredMethod("readAdditionalSaveData", CompoundTag.class);
            readAdditionalSaveData.setAccessible(true);
            readAdditionalSaveData.invoke(corpseInstance, deathCompound);

            Method setUUIDMethod = corpseClass.getMethod("setCorpseUUID", UUID.class);
            setUUIDMethod.invoke(corpseInstance, death.getPlayerUUID());

            Method setNameMethod = corpseClass.getMethod("setCorpseName", String.class);
            setNameMethod.invoke(corpseInstance, death.getPlayerName());

            Method setEquipmentMethod = corpseClass.getMethod("setEquipment", NonNullList.class);
            setEquipmentMethod.invoke(corpseInstance, death.getEquipment());

            Method setPosMethod = corpseClass.getMethod("setPos", double.class, double.class, double.class);
            setPosMethod.invoke(corpseInstance, death.getPosX(), Math.max(death.getPosY(), level.getMinBuildHeight()), death.getPosZ());

            Method setYRotMethod = corpseClass.getMethod("setYRot", float.class);
            setYRotMethod.invoke(corpseInstance, recruit.getYRot());

            if (corpseInstance instanceof Entity corpseEntity) {

                level.addFreshEntity(corpseEntity);
            }

        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            Main.LOGGER.warn("Was not able to spawn recruit corpse for " + recruit.getName().getString());
        }
    }
}