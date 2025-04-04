package com.talhanation.recruits.util;

import com.talhanation.recruits.compat.SmallShips;
import com.talhanation.recruits.entities.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NPCArmy {
    private List<LivingEntity> allUnits;
    private List<LivingEntity> ranged;
    private List<LivingEntity> shieldmen;
    private List<LivingEntity> infantry;
    private List<LivingEntity> cavalry;
    public List<Boat> ships;
    private List<UUID> uuids;
    private final ServerLevel level;

    public NPCArmy(ServerLevel level, @Nullable List<LivingEntity> units, @Nullable List<UUID> uuids) {
        this.level = level;
        this.allUnits = units;
        this.uuids = uuids;

        if(this.uuids == null && this.allUnits == null){
            this.initLists();
            return;
        }

        initRecruits(this.uuids != null && allUnits == null);
    }

    // Get the total number of units in the army
    public int getTotalUnits() {
        return allUnits.size();
    }

    // Get the average health of all units in the army
    public double getAverageHealth() {
        if (allUnits.isEmpty()) return 0; // Avoid division by zero
        double totalHealth = 0;
        for (LivingEntity recruit : allUnits) {
            totalHealth += recruit.getHealth();
        }
        return totalHealth / allUnits.size();
    }

    // Get the average armor of all units in the army
    public double getAverageArmor() {
        if (allUnits.isEmpty()) return 0; // Avoid division by zero
        double totalArmor = 0;
        for (LivingEntity recruit : allUnits) {
            totalArmor += recruit.getArmorValue();
        }
        return totalArmor / allUnits.size();
    }

    // Get the percentage of units with health below a certain threshold
    public double getLowHealthPercentage(double threshold) {
        if (allUnits.isEmpty()) return 0; // Avoid division by zero
        int lowHealthCount = 0;
        for (LivingEntity recruit : allUnits) {
            if (recruit.getHealth() < threshold) {
                lowHealthCount++;
            }
        }
        return (double) lowHealthCount / allUnits.size() * 100;
    }

    // Get the morale of the army (example implementation)
    public double getAverageMorale() {
        if (allUnits.isEmpty()) return 0; // Avoid division by zero
        double totalMorale = 0;
        for (LivingEntity living : allUnits) {
            if(living instanceof AbstractRecruitEntity recruit){
                totalMorale += recruit.getMorale();
            }
            else{
                totalMorale += 40;
            }
        }
        return totalMorale / allUnits.size();
    }

    public boolean isInfantry(LivingEntity recruit) {
        if (recruit.getVehicle() instanceof AbstractHorse) {
            return false;
        } else if (recruit.getMainHandItem().getItem() instanceof BowItem
                || recruit.getMainHandItem().getItem() instanceof CrossbowItem) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isShieldmen(LivingEntity recruit) {
        if (recruit.getVehicle() instanceof AbstractHorse) {
            return false;
        } else if (recruit.getOffhandItem().getItem() instanceof ShieldItem) {
            return true;
        } else {
            return false;
        }
    }

    // Check if a recruit is ranged
    public static boolean isRanged(LivingEntity recruit) {
        return recruit.getMainHandItem().getItem() instanceof BowItem
                || recruit.getMainHandItem().getItem() instanceof CrossbowItem;
    }

    // Check if a recruit is cavalry
    public boolean isCavalry(LivingEntity recruit) {
        if (recruit.getMainHandItem().getItem() instanceof BowItem
                || recruit.getMainHandItem().getItem() instanceof CrossbowItem) {
            return false;
        } else {
            return recruit.getVehicle() instanceof AbstractHorse;
        }
    }


    public void updateArmy() {
        allUnits = allUnits.stream()
                .filter(LivingEntity::isAlive)
                .collect(Collectors.toList());

        initRecruits(false);
        updateShips();
    }

    public void updateShips() {
        ships = ships.stream()
                .filter(boat -> SmallShips.isSmallShip(boat) && !boat.isUnderWater() && hasOnBoard(boat))
                .collect(Collectors.toList());


        List<Boat> nearbyBoats = level.getEntitiesOfClass(Boat.class, getShipSearchArea());
        for (Boat boat : nearbyBoats) {
            if (SmallShips.isSmallShip(boat) && !ships.contains(boat) && hasOnBoard(boat)) {
                ships.add(boat);
            }
        }
    }

    private AABB getShipSearchArea() {
        Vec3 center = getPosition();
        double radius = 100.0;
        return new AABB(center.x - radius, center.y - 10, center.z - radius,
                center.x + radius, center.y + 10, center.z + radius);
    }


    private boolean hasOnBoard(Boat boat) {
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof LivingEntity livingEntity) {
                if (allUnits.contains(livingEntity)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Get a list of all recruits in the army
    public List<AbstractRecruitEntity> getAllRecruitUnits() {
        return getRecruitFromList(allUnits);
    }

    public List<AbstractRecruitEntity> getRecruitInfantry() {
        return getRecruitFromList(infantry);
    }

    public List<AbstractRecruitEntity> getRecruitShieldmen() {
        return getRecruitFromList(shieldmen);
    }

    public List<AbstractRecruitEntity> getRecruitRanged() {
        return getRecruitFromList(ranged);
    }

    public List<AbstractRecruitEntity> getRecruitCavalry() {
        return getRecruitFromList(cavalry);
    }

    private List<AbstractRecruitEntity> getRecruitFromList(List<LivingEntity> list){
        List<AbstractRecruitEntity> recruits = new ArrayList<>();
        for (LivingEntity living: list) {
            if(living instanceof AbstractRecruitEntity recruit) recruits.add(recruit);
        }
        return recruits;
    }

    public List<LivingEntity> getAllUnits() {
        return allUnits;
    }

    public List<LivingEntity> getInfantry() {
        return infantry;
    }

    public List<LivingEntity> getShieldmen() {
        return shieldmen;
    }

    public List<LivingEntity> getRanged() {
        return ranged;
    }

    public List<LivingEntity> getCavalry() {
        return cavalry;
    }

    public void save(CompoundTag nbt) {
        // Save recruit UUIDs
        if(uuids != null){
            ListTag recruitsTag = new ListTag();
            for (UUID recruitId : uuids) {
                CompoundTag recruitTag = new CompoundTag();
                recruitTag.putUUID("Recruit", recruitId);
                recruitsTag.add(recruitTag);
            }
            nbt.put("Recruits", recruitsTag);
        }

    }

    public static NPCArmy load(ServerLevel serverLevel, CompoundTag nbt) {
        // Load recruit UUIDs
        ListTag recruitsTag = nbt.getList("Recruits", Tag.TAG_COMPOUND);
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < recruitsTag.size(); i++) {
            CompoundTag recruitTag = recruitsTag.getCompound(i);
            uuids.add(recruitTag.getUUID("Recruit"));
        }

        return new NPCArmy(serverLevel, null, uuids);
    }


    // Helper method to get actual recruit entities from UUIDs
    public List<LivingEntity> getFromUUIDListRecruits(List<UUID> recruitUUIDs) {
        List<LivingEntity> recruits = new ArrayList<>();
        for (UUID uuid : recruitUUIDs) {
            Entity entity = this.level.getEntity(uuid);
            if (entity instanceof LivingEntity recruit) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }

    // Helper method to get actual recruit entities from UUIDs
    public List<UUID> getUUIDListFromRecruits(List<LivingEntity> recruits) {
        return recruits.stream()
                .map(LivingEntity::getUUID)
                .collect(Collectors.toList());
    }

    private void initLists(){
        if(allUnits == null)this.allUnits = new ArrayList<>();
        this.ranged = new ArrayList<>();
        this.shieldmen = new ArrayList<>();
        this.infantry = new ArrayList<>();
        this.cavalry = new ArrayList<>();
        this.ships = new ArrayList<>();
    }
    public void initRecruits(boolean fromUUID) {
        this.initLists();
        if(uuids == null || uuids.isEmpty() && allUnits != null && !allUnits.isEmpty()) this.uuids = getUUIDListFromRecruits(allUnits);
        if(fromUUID) this.allUnits = getFromUUIDListRecruits(uuids);

        for (LivingEntity recruit : allUnits) {
            if (isRanged(recruit)) {
                ranged.add(recruit);
            } else if (isCavalry(recruit)) {
                cavalry.add(recruit);
            } else if (isShieldmen(recruit)) {
                shieldmen.add(recruit);
            } else if (isInfantry(recruit)) {
                infantry.add(recruit);
            }
        }

        updateShips();
    }

    public Vec3 getPosition() {
        return FormationUtils.getCenterOfPositions(this.allUnits, this.level);
    }

    public int size() {
        return allUnits.size();
    }
}