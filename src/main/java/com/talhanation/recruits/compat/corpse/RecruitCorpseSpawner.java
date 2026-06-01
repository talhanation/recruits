package com.talhanation.recruits.compat.corpse;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.mixin.compat.corpse.CorpseEntityAccessor;
import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class RecruitCorpseSpawner {

    private static final int HEAD_SLOT = 0;
    private static final int CHEST_SLOT = 1;
    private static final int LEGS_SLOT = 2;
    private static final int FEET_SLOT = 3;
    private static final int OFFHAND_SLOT = 4;
    private static final int MAINHAND_SLOT = 5;

    private static final int[] EQUIPMENT_SLOTS = {MAINHAND_SLOT, OFFHAND_SLOT, FEET_SLOT, LEGS_SLOT, CHEST_SLOT, HEAD_SLOT};
    private static final int[] MAIN_INVENTORY_SLOTS = {MAINHAND_SLOT, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    private static final int[] ARMOR_SLOTS = {FEET_SLOT, LEGS_SLOT, CHEST_SLOT, HEAD_SLOT};
    private static final int[] OFFHAND_SLOTS = {OFFHAND_SLOT};

    private RecruitCorpseSpawner() {
    }

    public static boolean spawnCorpse(AbstractInventoryEntity recruit) {
        if (!(recruit instanceof AbstractRecruitEntity recruitEntity)) {
            return false;
        }
        if (!(recruit.level() instanceof ServerLevel level)) {
            return false;
        }

        try {
            byte poseType = selectPoseType(recruitEntity);
            CompoundTag deathTag = createDeathTag(recruitEntity, poseType);
            Death death = Death.fromNBT(deathTag);

            CorpseEntity corpse = new CorpseEntity(level);
            ((CorpseEntityAccessor) corpse).recruits$setDeath(death);
            corpse.setCorpseUUID(death.getPlayerUUID());
            corpse.setCorpseName(death.getPlayerName());
            corpse.setEquipment(death.getEquipment());
            corpse.setCorpseModel(death.getModel());
            corpse.setPos(death.getPosX(), Math.max(death.getPosY(), level.getMinBuildHeight()), death.getPosZ());
            corpse.setYRot(recruitEntity.getYRot());

            level.addFreshEntity(corpse);
            return true;
        } catch (Exception e) {
            Main.LOGGER.warn("Was not able to spawn recruit corpse for {}", recruit.getName().getString(), e);
            return false;
        }
    }

    private static CompoundTag createDeathTag(AbstractRecruitEntity recruit, byte poseType) {
        CompoundTag nbt = new CompoundTag();

        nbt.putUUID("PlayerUuid", RecruitCorpseAppearance.encode(recruit, poseType));
        nbt.putString("PlayerName", recruit.getName().getString());
        writeEquipment(nbt, recruit);
        writeSlottedInventory(nbt, "MainInventory", recruit, MAIN_INVENTORY_SLOTS);
        writeSlottedInventory(nbt, "ArmorInventory", recruit, ARMOR_SLOTS);
        writeSlottedInventory(nbt, "OffHandInventory", recruit, OFFHAND_SLOTS);

        nbt.putLong("Timestamp", System.currentTimeMillis());
        nbt.putInt("Experience", 6);
        nbt.putDouble("PosX", recruit.getX());
        nbt.putDouble("PosY", recruit.getY());
        nbt.putDouble("PosZ", recruit.getZ());
        nbt.putString("Dimension", recruit.level().dimension().location().toString());
        nbt.putByte("Model", RecruitCorpseAppearance.FULL_PLAYER_MODEL);

        return nbt;
    }

    private static void writeEquipment(CompoundTag nbt, AbstractRecruitEntity recruit) {
        ListTag items = new ListTag();
        // Corpse reads equipment by list index instead of the Slot tag, so empty positions must be preserved.
        for (int slot : EQUIPMENT_SLOTS) {
            items.add(saveEquipmentItem(recruit.getInventory().getItem(slot)));
        }
        nbt.put("Equipment", items);
    }

    private static void writeSlottedInventory(CompoundTag nbt, String key, AbstractRecruitEntity recruit, int[] slots) {
        ListTag items = new ListTag();

        for (int index = 0; index < slots.length; index++) {
            int inventorySlot = slots[index];
            int corpseSlot = index;
            Optional<CompoundTag> itemTag = saveItem(recruit.getInventory().getItem(inventorySlot));
            itemTag.ifPresent(tag -> {
                tag.putInt("Slot", corpseSlot);
                items.add(tag);
            });
        }

        nbt.put(key, items);
    }

    private static Optional<CompoundTag> saveItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag itemTag = new CompoundTag();
        stack.save(itemTag);
        if (!itemTag.contains("id")) {
            return Optional.empty();
        }
        return Optional.of(itemTag);
    }

    private static CompoundTag saveEquipmentItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            return saveItem(stack).orElseGet(RecruitCorpseSpawner::emptyItemTag);
        }
        return emptyItemTag();
    }

    private static CompoundTag emptyItemTag() {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", "minecraft:air");
        itemTag.putByte("Count", (byte) 0);
        return itemTag;
    }

    private static byte selectPoseType(AbstractRecruitEntity recruit) {
        int roll = recruit.getRandom().nextInt(100);
        if (roll < 40) {
            return 0;
        }
        return (byte) (roll < 70 ? 1 : 2);
    }
}
