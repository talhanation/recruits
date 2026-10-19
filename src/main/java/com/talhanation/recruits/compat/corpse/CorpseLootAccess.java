package com.talhanation.recruits.compat.corpse;

import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Corpse mod access (player and recruit corpses). Only loaded when the Corpse mod is present (guarded by Main.isCorpseLoaded
 * in {@link LootableCorpses}), because it links against Corpse classes.
 */
final class CorpseLootAccess {

    private CorpseLootAccess() {
    }

    static boolean isCorpseEntity(Entity entity) {
        return entity instanceof CorpseEntity;
    }

    @Nullable
    static Container getContainer(Entity entity) {
        if (!(entity instanceof CorpseEntity corpse)) return null;
        Death death = corpse.getDeath();
        if (death == null) return null;
        return new ListsContainer(List.of(
                death.getMainInventory(),
                death.getArmorInventory(),
                death.getOffHandInventory(),
                death.getAdditionalItems()));
    }

    /**
     * The corpse renders its worn equipment from a separate list. When a recruit takes a piece that is
     * shown there, remove it so the corpse does not keep wearing looted armor.
     */
    static void onItemTaken(Entity entity, ItemStack taken) {
        if (!(entity instanceof CorpseEntity corpse) || taken.isEmpty()) return;

        // The synced list may be the Death's own list instance: update the Death in place, then hand
        // the entity a fresh copy, otherwise SynchedEntityData sees "no change" and never syncs.
        Death death = corpse.getDeath();
        if (death != null) removeMatching(death.getEquipment(), taken);

        NonNullList<ItemStack> shown = corpse.getEquipment();
        NonNullList<ItemStack> copy = NonNullList.withSize(shown.size(), ItemStack.EMPTY);
        for (int i = 0; i < shown.size(); i++) copy.set(i, shown.get(i));
        if (death == null || shown != death.getEquipment()) removeMatching(copy, taken);
        corpse.setEquipment(copy);
    }

    private static void removeMatching(@Nullable NonNullList<ItemStack> equipment, ItemStack taken) {
        if (equipment == null) return;
        for (int i = 0; i < equipment.size(); i++) {
            if (!equipment.get(i).isEmpty() && ItemStack.isSameItemSameTags(equipment.get(i), taken)) {
                equipment.set(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    /** Container view over the Death's item lists; writes go straight into the lists. */
    private static final class ListsContainer implements Container {
        private final List<NonNullList<ItemStack>> lists;

        private ListsContainer(List<NonNullList<ItemStack>> lists) {
            this.lists = lists;
        }

        @Override
        public int getContainerSize() {
            int size = 0;
            for (NonNullList<ItemStack> list : lists) size += list.size();
            return size;
        }

        @Override
        public boolean isEmpty() {
            for (NonNullList<ItemStack> list : lists) {
                for (ItemStack stack : list) if (!stack.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            for (NonNullList<ItemStack> list : lists) {
                if (slot < list.size()) return list.get(slot);
                slot -= list.size();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack removed = stack.split(amount);
            if (stack.isEmpty()) setItem(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            for (NonNullList<ItemStack> list : lists) {
                if (slot < list.size()) {
                    list.set(slot, stack);
                    return;
                }
                slot -= list.size();
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (NonNullList<ItemStack> list : lists) list.replaceAll(s -> ItemStack.EMPTY);
        }
    }
}
