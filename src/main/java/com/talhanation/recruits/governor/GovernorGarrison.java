package com.talhanation.recruits.governor;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Recruit-level helpers of the Governor: joining and leaving the garrison, the loadout templates
 * and container/currency handling. Garrison recruits are tagged with the UUID of their claim,
 * so a successor Governor of the same owner takes them over automatically.
 *
 * Counting (Variante B): garrison recruits do not count towards the player limit, but they do count
 * for the faction (faction.npcs / maxNPCs).
 */
public final class GovernorGarrison {

    public static final EquipmentSlot[] LOADOUT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private GovernorGarrison() {
    }

    public static boolean isGarrisonOf(AbstractRecruitEntity recruit, UUID claimId, @Nullable UUID owner) {
        if (recruit == null || !recruit.isAlive() || !recruit.isOwned()) return false;
        if (recruit instanceof GovernorEntity) return false;
        if (!claimId.equals(recruit.getGarrisonClaim())) return false;
        return owner == null || owner.equals(recruit.getOwnerUUID());
    }

    /** Recruit types that can serve in a garrison (no companions, no nobles, no governors). */
    public static boolean canServe(AbstractRecruitEntity recruit) {
        if (recruit instanceof GovernorEntity || recruit instanceof VillagerNobleEntity) return false;
        GovernorUnitType type = GovernorUnitType.of(recruit);
        return type != null;
    }

    /**
     * Moves one of the owner's own recruits into the garrison of the claim.
     * The recruit leaves its group and stops counting towards the player limit.
     */
    public static void joinGarrison(ServerLevel level, AbstractRecruitEntity recruit, UUID claimId) {
        if (recruit.getGroup() != null) {
            RecruitEvents.recruitsGroupsManager.removeMember(recruit.getGroup(), recruit.getUUID(), level);
            recruit.setGroupUUID(null);
        }
        boolean wasCounted = !recruit.isGarrison();
        recruit.setGarrisonClaim(claimId);
        prepareGarrisonState(recruit);

        if (wasCounted && recruit.getOwnerUUID() != null) {
            RecruitEvents.recruitsPlayerUnitManager.removeRecruits(recruit.getOwnerUUID(), 1);
        }
    }

    /**
     * Hires a recruit directly into the garrison for the owner of the Governor.
     * Works while the owner is offline: owner, team and faction count are set from UUIDs.
     */
    public static void hireIntoGarrison(ServerLevel level, AbstractRecruitEntity recruit, UUID owner, String ownerName, String factionId, UUID claimId) {
        recruit.setOwnerUUID(Optional.of(owner));
        recruit.setIsOwned(true);
        recruit.setGarrisonClaim(claimId);
        recruit.despawnTimer = -1;
        recruit.setPersistenceRequired();
        recruit.resetPaymentTimer();
        recruit.setTarget(null);
        recruit.setGroupUUID(null);
        prepareGarrisonState(recruit);

        if (recruit instanceof ICompanion companion) {
            companion.setOwnerName(ownerName);
        }

        PlayerTeam team = level.getScoreboard().getPlayerTeam(factionId);
        if (team != null) {
            FactionEvents.addRecruitToTeam(recruit, team, level);
            FactionEvents.addNPCToData(level, factionId, 1);
        }
        recruit.needsTeamUpdate = true;
    }

    private static void prepareGarrisonState(AbstractRecruitEntity recruit) {
        recruit.setListen(false);
        recruit.setProtectUUID(Optional.empty());
        recruit.setShouldProtect(false);
        recruit.setShouldMount(false);
        recruit.setShouldRest(false);
        if (recruit.getState() == 3 || recruit.getState() == 2) recruit.setAggroState(0);
    }

    /**
     * Releases a garrison recruit back to the normal command of its owner.
     * Counts like hiring, so the player limit is checked.
     */
    public static boolean releaseFromGarrison(ServerLevel level, AbstractRecruitEntity recruit, @Nullable ServerPlayer actor) {
        if (!recruit.isGarrison()) return false;
        UUID owner = recruit.getOwnerUUID();
        if (owner == null) return false;

        String factionId = recruit.getTeam() != null ? recruit.getTeam().getName() : "";
        if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(factionId, owner)) {
            if (actor != null) actor.sendSystemMessage(Component.translatable("chat.recruits.governor.release_limit", recruit.getName().getString()));
            return false;
        }

        UUID claimId = recruit.getGarrisonClaim();
        recruit.setGarrisonClaim(null);
        recruit.setListen(true);
        recruit.setFollowState(2);
        RecruitEvents.recruitsPlayerUnitManager.addRecruits(owner, 1);

        if (claimId != null && GovernorManager.get() != null) {
            ClaimGovernorData data = GovernorManager.get().getData(claimId);
            if (data != null) GovernorManager.unassign(data, recruit.getUUID());
        }
        return true;
    }

    // ------------------------------------------------------------------ loadout

    /**
     * Swaps all managed slots of a garrison recruit to the template items at once
     * (matched by item type only, enchantments ignored). See {@link #applyLoadoutStep}.
     */
    public static void applyLoadout(AbstractRecruitEntity recruit, Container container) {
        for (int guard = 0; guard < LOADOUT_SLOTS.length && applyLoadoutStep(recruit, container); guard++) {
        }
    }

    /**
     * Called by the upkeep session when a garrison recruit stands at an upkeep chest: swaps ONE slot to its
     * template item. The old item is put into the chest (dropped next to it if the chest is full).
     * Slots without template ("keep") are left as they are; if the template item is not in the chest,
     * the recruit keeps what he has.
     *
     * @return true if a slot was swapped (the session waits a moment and calls again)
     */
    public static boolean applyLoadoutStep(AbstractRecruitEntity recruit, Container container) {
        if (!recruit.isGarrison() || recruit.getCommandSenderWorld().isClientSide()) return false;
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return false;
        ClaimGovernorData data = manager.getData(recruit.getGarrisonClaim());
        if (data == null) return false;
        GovernorUnitType type = GovernorUnitType.of(recruit);
        if (type == null) return false;

        for (int i = 0; i < LOADOUT_SLOTS.length; i++) {
            Item wanted = getItem(data.getLoadout(type, i));
            if (wanted == null) continue;
            EquipmentSlot slot = LOADOUT_SLOTS[i];
            ItemStack current = recruit.getItemBySlot(slot);
            if (!needsSwap(current, wanted)) continue;

            int sourceSlot = findUsable(container, wanted);
            if (sourceSlot < 0) continue;

            ItemStack source = container.getItem(sourceSlot);
            ItemStack replacement = source.copy();
            replacement.setCount(1);
            if (!recruit.canGarrisonHold(replacement)) continue;

            source.shrink(1);
            if (source.isEmpty()) container.setItem(sourceSlot, ItemStack.EMPTY);

            if (!current.isEmpty()) {
                ItemStack rest = insert(container, current.copy());
                if (!rest.isEmpty()) recruit.spawnAtLocation(rest);
            }
            recruit.setItemSlot(slot, replacement);
            recruit.inventory.setItem(recruit.getInventorySlotIndex(slot), replacement);
            container.setChanged();
            return true;
        }
        return false;
    }

    /** True if the recruit's equipment differs from its template and the chest can fix it. */
    public static boolean wantsLoadoutChange(AbstractRecruitEntity recruit, ClaimGovernorData data, @Nullable Container container) {
        GovernorUnitType type = GovernorUnitType.of(recruit);
        if (type == null) return false;
        for (int i = 0; i < LOADOUT_SLOTS.length; i++) {
            Item wanted = getItem(data.getLoadout(type, i));
            if (wanted == null) continue;
            ItemStack current = recruit.getItemBySlot(LOADOUT_SLOTS[i]);
            if (!needsSwap(current, wanted)) continue;
            // an item this unit can not hold (a bow for a swordsman) would send him to the chest again and again
            if (!recruit.canGarrisonHold(new ItemStack(wanted))) continue;
            if (container == null || findUsable(container, wanted) >= 0) return true;
        }
        return false;
    }

    private static boolean needsSwap(ItemStack current, Item wanted) {
        if (current.isEmpty() || current.getItem() != wanted) return true;
        return isWornOut(current);
    }

    private static boolean isWornOut(ItemStack stack) {
        if (!stack.isDamageableItem()) return false;
        int max = stack.getMaxDamage();
        return max > 0 && (max - stack.getDamageValue()) < max * 0.1F;
    }

    private static int findUsable(Container container, Item wanted) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == wanted && !isWornOut(stack)) return i;
        }
        return -1;
    }

    @Nullable
    public static Item getItem(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !ForgeRegistries.ITEMS.containsKey(location)) return null;
        return ForgeRegistries.ITEMS.getValue(location);
    }

    // ------------------------------------------------------------------ containers and currency

    @Nullable
    public static Container getContainer(Level level, @Nullable BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container chest = ChestBlock.getContainer(chestBlock, state, level, pos, false);
            if (chest != null) return chest;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof Container container) return container;
        return null;
    }

    public static boolean isContainer(Level level, BlockPos pos) {
        return getContainer(level, pos) != null;
    }

    public static Item getCurrency() {
        return FactionEvents.getCurrency().getItem();
    }

    public static int countCurrency(Container container) {
        Item currency = getCurrency();
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() == currency) count += stack.getCount();
        }
        return count;
    }

    /** Removes up to amount currency, returns how much was removed. */
    public static int removeCurrency(Container container, int amount) {
        Item currency = getCurrency();
        int removed = 0;
        for (int i = 0; i < container.getContainerSize() && removed < amount; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() != currency) continue;
            int take = Math.min(stack.getCount(), amount - removed);
            stack.shrink(take);
            if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
            removed += take;
        }
        container.setChanged();
        return removed;
    }

    /** Puts amount currency into the container, returns what did not fit. */
    public static int addCurrency(Container container, int amount) {
        Item currency = getCurrency();
        int left = amount;
        while (left > 0) {
            int size = Math.min(left, currency.getMaxStackSize());
            ItemStack stack = new ItemStack(currency, size);
            ItemStack rest = insert(container, stack);
            int inserted = size - rest.getCount();
            left -= inserted;
            if (inserted == 0) break;
        }
        container.setChanged();
        return left;
    }

    /** Inserts into existing stacks first, then empty slots. Returns the remainder. */
    public static ItemStack insert(Container container, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) continue;
            int max = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
            int move = Math.min(stack.getCount(), max - existing.getCount());
            if (move > 0) {
                existing.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (!container.getItem(i).isEmpty()) continue;
            if (!container.canPlaceItem(i, stack)) continue;
            int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), container.getMaxStackSize()));
            ItemStack placed = stack.copy();
            placed.setCount(move);
            container.setItem(i, placed);
            stack.shrink(move);
        }
        return stack;
    }

    public static boolean canFactionAddNPC(String factionId) {
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
        return faction == null || faction.canAddNPC();
    }
}
