package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.compat.corpse.LootableCorpses;
import com.talhanation.recruits.compat.musketmod.IWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Field looting: when the Corpse or RpgZ mod is installed, an idle recruit with no enemies around
 * walks to a nearby corpse (players included) and loots it. He first kneels at the corpse for
 * 3 seconds, looking at it, then takes one slot per second until the corpse is empty or he can
 * carry nothing more.
 * Order: food first, then equipment that is better than what he wears (equipped directly, the old
 * piece goes into his backpack or back into the corpse), then everything else that fits.
 */
public class RecruitFieldLootGoal extends Goal {

    private static final double SEARCH_RADIUS = 8.0D;
    private static final double REACH_SQR = 2.5D * 2.5D;
    private static final int SEARCH_INTERVAL = 40;
    private static final int KNEEL_TICKS = 60;          // 3s looking at the corpse while crouching
    private static final int TICKS_PER_SLOT = 20;       // 1 slot per second
    private static final int TIMEOUT_TICKS = 20 * 90;   // a full player corpse has ~40 slots
    private static final int COOLDOWN_TICKS = 20 * 10;
    private static final int RECENT_DAMAGE_TICKS = 100;
    private static final int FIRST_BACKPACK_SLOT = 6;

    private final AbstractRecruitEntity recruit;
    @Nullable private Entity corpse;
    private int ticks;
    private int kneelTicks;
    private int slotTimer;
    private int cooldown;
    private boolean kneeling;

    public RecruitFieldLootGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if ((recruit.tickCount + recruit.getId()) % SEARCH_INTERVAL != 0) return false;
        if (!RecruitsServerConfig.RecruitsFieldLooting.get() || !LootableCorpses.isAnyCorpseModLoaded()) return false;
        if (!isCalm()) return false;

        this.corpse = findCorpse();
        return this.corpse != null;
    }

    @Override
    public boolean canContinueToUse() {
        return corpse != null && !corpse.isRemoved() && ticks < TIMEOUT_TICKS && isCalm()
                && recruit.distanceToSqr(corpse) <= (SEARCH_RADIUS + 4) * (SEARCH_RADIUS + 4)
                && isCloseToOrder(corpse);
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.kneelTicks = 0;
        this.slotTimer = 0;
        this.kneeling = false;
    }

    @Override
    public void stop() {
        if (kneeling) recruit.setPose(Pose.STANDING);
        this.kneeling = false;
        this.corpse = null;
        this.cooldown = COOLDOWN_TICKS;
        recruit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (corpse == null) return;
        ticks++;

        recruit.getLookControl().setLookAt(corpse.getX(), corpse.getY() + 0.2D, corpse.getZ(), 30.0F, 30.0F);

        if (recruit.distanceToSqr(corpse) > REACH_SQR) {
            if (kneeling) {
                recruit.setPose(Pose.STANDING);
                kneeling = false;
            }
            if (ticks % 10 == 1) recruit.getNavigation().moveTo(corpse, 1.0D);
            return;
        }

        recruit.getNavigation().stop();
        if (!kneeling) {
            recruit.setPose(Pose.CROUCHING);
            kneeling = true;
        }

        if (kneelTicks < KNEEL_TICKS) {
            kneelTicks++;
            return;
        }

        if (++slotTimer < TICKS_PER_SLOT) return;
        slotTimer = 0;

        Container container = LootableCorpses.getContainer(corpse);
        if (container == null || !lootOneSlot(container)) {
            // corpse empty or recruit can't carry more
            this.corpse = null;
        }
    }

    // -------------------------------------------------------------------------
    // Conditions

    /** No target, not recently hurt and no order that must not be interrupted. */
    private boolean isCalm() {
        LivingEntity target = recruit.getTarget();
        if (target != null && target.isAlive()) return false;
        if (recruit.getLastHurtByMob() != null && recruit.tickCount - recruit.getLastHurtByMobTimestamp() < RECENT_DAMAGE_TICKS) return false;
        if (recruit.isPassenger() || recruit.getFleeing() || recruit.getShouldRest()) return false;
        return !recruit.needsToGetFood() && !recruit.getShouldMovePos() && !recruit.getShouldProtect() && !recruit.getShouldMount();
    }

    /**
     * The goal runs above hold-pos/follow (which keep the MOVE flag while active), so it must not drag
     * the recruit away from his order: the corpse has to be near the hold position / the owner.
     */
    private boolean isCloseToOrder(Entity corpse) {
        if (recruit.getShouldHoldPos() && recruit.getHoldPos() != null) {
            return corpse.position().distanceToSqr(recruit.getHoldPos()) <= SEARCH_RADIUS * SEARCH_RADIUS;
        }
        if (recruit.getShouldFollow() && recruit.getOwner() != null) {
            return recruit.getOwner().distanceToSqr(corpse) <= 12.0D * 12.0D;
        }
        return true;
    }

    @Nullable
    private Entity findCorpse() {
        List<Entity> corpses = LootableCorpses.findCorpses(recruit.level(), recruit.getBoundingBox().inflate(SEARCH_RADIUS));
        corpses.sort(Comparator.comparingDouble(recruit::distanceToSqr));
        for (Entity candidate : corpses) {
            if (!isCloseToOrder(candidate)) continue;
            Container container = LootableCorpses.getContainer(candidate);
            if (container != null && pickSlot(container) >= 0) return candidate;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Looting

    /** Slot to loot next by priority: food, then better equipment, then anything that fits. -1 = none. */
    private int pickSlot(Container container) {
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && recruit.canEatItemStack(stack) && canStore(stack)) return i;
        }
        for (int i = 0; i < size; i++) {
            if (isEquipmentUpgrade(container.getItem(i))) return i;
        }
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && canStore(stack)) return i;
        }
        return -1;
    }

    /** Takes one slot. Returns false when nothing more can be looted. */
    private boolean lootOneSlot(Container container) {
        int slot = pickSlot(container);
        if (slot < 0) return false;

        ItemStack stack = container.getItem(slot);
        ItemStack taken;
        if (!recruit.canEatItemStack(stack) && isEquipmentUpgrade(stack)) {
            taken = equipFromCorpse(container, slot);
        } else {
            taken = storeFromCorpse(container, slot);
        }
        if (taken.isEmpty()) return false;

        LootableCorpses.onItemTaken(corpse, taken);
        container.setChanged();
        recruit.swing(InteractionHand.MAIN_HAND);
        recruit.playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.0F + (recruit.getRandom().nextFloat() - recruit.getRandom().nextFloat()) * 0.4F);
        return pickSlot(container) >= 0;
    }

    private ItemStack storeFromCorpse(Container container, int slot) {
        ItemStack stack = container.getItem(slot);
        ItemStack rest = recruit.getInventory().addItem(stack.copy());
        int moved = stack.getCount() - rest.getCount();
        if (moved <= 0) return ItemStack.EMPTY;

        ItemStack taken = stack.copy();
        taken.setCount(moved);
        container.setItem(slot, rest);
        return taken;
    }

    /** Puts a better piece on directly; the old piece goes into the backpack, or back into the corpse. */
    private ItemStack equipFromCorpse(Container container, int slot) {
        ItemStack stack = container.getItem(slot);
        EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(stack);
        ItemStack old = recruit.getItemBySlot(equipmentSlot).copy();

        ItemStack piece = stack.split(1);
        recruit.setItemSlot(equipmentSlot, piece);
        recruit.getInventory().setItem(recruit.getInventorySlotIndex(equipmentSlot), piece);

        ItemStack leftInCorpse = stack.isEmpty() ? ItemStack.EMPTY : stack;
        if (!old.isEmpty()) {
            ItemStack oldRest = recruit.getInventory().addItem(old);
            if (!oldRest.isEmpty()) {
                if (leftInCorpse.isEmpty()) leftInCorpse = oldRest;
                else recruit.spawnAtLocation(oldRest);
            }
        }
        container.setItem(slot, leftInCorpse);
        recruit.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 0.6F, 1.0F);
        return piece.copy();
    }

    private boolean isEquipmentUpgrade(ItemStack stack) {
        return !stack.isEmpty() && isGear(stack) && recruit.canEquipItem(stack);
    }

    private static boolean isGear(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof ShieldItem || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || (Main.isMusketModLoaded && IWeapon.isMusketModWeapon(stack));
    }

    /** Is there room in the backpack (free slot or a matching stack with space)? */
    private boolean canStore(ItemStack stack) {
        Container inventory = recruit.getInventory();
        for (int i = FIRST_BACKPACK_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack own = inventory.getItem(i);
            if (own.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(own, stack) && own.getCount() < Math.min(own.getMaxStackSize(), inventory.getMaxStackSize())) return true;
        }
        return false;
    }
}
