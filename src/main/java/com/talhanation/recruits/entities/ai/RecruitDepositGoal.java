package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * "Anti-upkeep": the recruit walks to the ordered container (block or entity) and puts everything
 * he carries into it, one slot per second - backpack, weapons and armor. Afterwards he is naked.
 */
public class RecruitDepositGoal extends Goal {

    private static final int FIRST_BACKPACK_SLOT = 6;
    private static final int OFFHAND_SLOT = 4;
    private static final int MAINHAND_SLOT = 5;
    private static final int TICKS_PER_SLOT = 20;
    private static final int TIMEOUT_TICKS = 20 * 60;
    private static final double MAX_DISTANCE_SQR = 100.0D * 100.0D;

    private final AbstractRecruitEntity recruit;
    @Nullable private Container container;
    @Nullable private BlockPos chestPos;
    @Nullable private Entity containerEntity;
    private int ticks;
    private int slotTimer;
    private int timeToRecalcPath;
    private boolean opened;

    public RecruitDepositGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (recruit.depositPos == null && recruit.depositEntityUUID == null) return false;
        LivingEntity target = recruit.getTarget();
        return (target == null || !target.isAlive()) && !recruit.isPassenger();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && container != null && ticks < TIMEOUT_TICKS;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.slotTimer = 0;
        this.timeToRecalcPath = 0;
        this.opened = false;
        this.container = null;
        this.chestPos = null;
        this.containerEntity = null;

        if (recruit.depositPos != null) {
            resolveBlockContainer(recruit.depositPos);
        } else if (recruit.depositEntityUUID != null) {
            resolveEntityContainer(recruit.depositEntityUUID);
        }

        if (container == null || getTargetPos().distanceToSqr(recruit.position()) > MAX_DISTANCE_SQR) {
            if (recruit.getOwner() != null) {
                recruit.getOwner().sendSystemMessage(Component.translatable("chat.recruits.text.cantInteract", recruit.getName().getString()));
            }
            finishOrder();
        }
    }

    @Override
    public void stop() {
        setChestOpen(false);
        if (container != null) container.setChanged();
        this.container = null;
        this.containerEntity = null;
        this.chestPos = null;
        recruit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (container == null) return;
        ticks++;

        if (containerEntity != null && containerEntity.isRemoved()) {
            finishOrder();
            return;
        }

        Vec3 targetPos = getTargetPos();
        if (targetPos.distanceToSqr(recruit.position()) > 3.0D * 3.0D) {
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = adjustedTickDelay(10);
                if (containerEntity != null) recruit.getNavigation().moveTo(containerEntity, 1.15D);
                else recruit.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.15D);
            }
            if (recruit.horizontalCollision || recruit.minorHorizontalCollision) recruit.getJumpControl().jump();
            return;
        }

        recruit.getNavigation().stop();
        recruit.getLookControl().setLookAt(targetPos.x, targetPos.y + 0.5D, targetPos.z, 10.0F, (float) recruit.getMaxHeadXRot());
        setChestOpen(true);

        if (++slotTimer < TICKS_PER_SLOT) return;
        slotTimer = 0;

        if (!depositOneSlot()) {
            finishOrder();
        }
    }

    // -------------------------------------------------------------------------

    private void finishOrder() {
        recruit.depositPos = null;
        recruit.depositEntityUUID = null;
        this.container = null;
    }

    /**
     * Unloading order: backpack first, then both hands, then the armor - the recruit ends up naked.
     * Equipment slots are cleared on the entity too, not only in the inventory.
     */
    private static final int[] DEPOSIT_ORDER = {MAINHAND_SLOT, OFFHAND_SLOT, 0, 1, 2, 3};

    /** Moves one slot into the container. Returns false when nothing is left (or no room). */
    private boolean depositOneSlot() {
        Container inventory = recruit.getInventory();
        // A slot that does not fit (container full for that item) is skipped, the next one is tried.
        for (int i = FIRST_BACKPACK_SLOT; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty() && depositSlot(inventory, i)) return true;
        }
        for (int i : DEPOSIT_ORDER) {
            if (!inventory.getItem(i).isEmpty() && depositSlot(inventory, i)) return true;
        }
        return false;
    }

    private boolean depositSlot(Container inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        ItemStack rest = insert(container, stack.copy());
        if (rest.getCount() == stack.getCount()) return false; // container full

        inventory.setItem(slot, rest);
        if (slot < FIRST_BACKPACK_SLOT) {
            EquipmentSlot equipmentSlot = recruit.getEquipmentSlotIndex(slot);
            if (equipmentSlot != null) recruit.setItemSlot(equipmentSlot, rest);
        }
        container.setChanged();
        recruit.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        recruit.playSound(SoundEvents.ITEM_PICKUP, 0.4F, 0.8F);
        return true;
    }

    /** Merges into matching stacks first, then fills empty slots. Returns what did not fit. */
    private static ItemStack insert(Container target, ItemStack stack) {
        for (int i = 0; i < target.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = target.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack) && target.canPlaceItem(i, stack)) {
                int max = Math.min(existing.getMaxStackSize(), target.getMaxStackSize());
                int move = Math.min(stack.getCount(), max - existing.getCount());
                if (move > 0) {
                    existing.grow(move);
                    stack.shrink(move);
                }
            }
        }
        for (int i = 0; i < target.getContainerSize() && !stack.isEmpty(); i++) {
            if (target.getItem(i).isEmpty() && target.canPlaceItem(i, stack)) {
                int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), target.getMaxStackSize()));
                ItemStack placed = stack.split(move);
                target.setItem(i, placed);
            }
        }
        return stack;
    }

    private Vec3 getTargetPos() {
        if (containerEntity != null) return containerEntity.position();
        if (chestPos != null) return Vec3.atCenterOf(chestPos);
        return recruit.position();
    }

    private void resolveBlockContainer(BlockPos pos) {
        BlockEntity blockEntity = recruit.getCommandSenderWorld().getBlockEntity(pos);
        BlockState state = recruit.getCommandSenderWorld().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            this.container = ChestBlock.getContainer(chestBlock, state, recruit.getCommandSenderWorld(), pos, false);
        } else if (blockEntity instanceof Container blockContainer) {
            this.container = blockContainer;
        }
        if (this.container != null) this.chestPos = pos;
    }

    private void resolveEntityContainer(java.util.UUID uuid) {
        if (!(recruit.getCommandSenderWorld() instanceof ServerLevel serverLevel)) return;
        Entity entity = serverLevel.getEntity(uuid);
        if (entity == null) return;

        if (entity instanceof AbstractHorse horse) this.container = horse.inventory;
        else if (entity instanceof InventoryCarrier carrier) this.container = carrier.getInventory();
        else if (entity instanceof Container entityContainer) this.container = entityContainer;

        if (this.container != null) this.containerEntity = entity;
    }

    /** Lid animation for chests, mirroring the upkeep goal. */
    private void setChestOpen(boolean open) {
        if (chestPos == null || opened == open) return;
        BlockState state = recruit.getCommandSenderWorld().getBlockState(chestPos);
        if (!(state.getBlock() instanceof ChestBlock)) return;
        Block block = state.getBlock();
        recruit.getCommandSenderWorld().blockEvent(chestPos, block, 1, open ? 1 : 0);
        recruit.getCommandSenderWorld().playSound(null, chestPos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                recruit.getSoundSource(), 0.7F, 0.8F + 0.4F * recruit.getRandom().nextFloat());
        opened = open;
    }
}
