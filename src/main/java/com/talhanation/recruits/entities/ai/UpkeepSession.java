package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.governor.GovernorGarrison;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Upkeep at the container, paced like the deposit order: the recruit stands at the container and
 * takes one slot per second - first the Governor's equipment template (garrison recruits), then better
 * equipment / ammunition / captain material, then food (one item per step, up to {@link #MAX_FOOD_ITEMS}).
 * Shared by the block and the entity upkeep goal.
 */
public class UpkeepSession {

    public enum Result { RUNNING, DONE, NO_FOOD, NO_PLACE }

    private static final int TICKS_PER_SLOT = 20;
    private static final int MAX_FOOD_ITEMS = 3;
    private static final int FIRST_BACKPACK_SLOT = 6;
    private static final int LAST_FOOD_SLOT = 14;

    private final AbstractRecruitEntity recruit;
    private final Container container;
    private int slotTimer;
    private int foodTaken;
    private boolean equipmentDone;

    public UpkeepSession(AbstractRecruitEntity recruit, Container container) {
        this.recruit = recruit;
        this.container = container;
    }

    public Container getContainer() {
        return container;
    }

    /** Call every tick while the recruit stands at the container. */
    public Result tick() {
        if (++slotTimer < TICKS_PER_SLOT) return Result.RUNNING;
        slotTimer = 0;

        if (!equipmentDone) {
            // garrison: swap a slot to the template item, the old one goes into the chest
            if (recruit.isGarrison() && GovernorGarrison.applyLoadoutStep(recruit, container)) {
                feedback();
                return Result.RUNNING;
            }
            if (recruit.upkeepTakeOne(container)) {
                feedback();
                return Result.RUNNING;
            }
            equipmentDone = true;
        }

        if (foodTaken >= MAX_FOOD_ITEMS) return Result.DONE;

        ItemStack food = findFood();
        if (food == null) return foodTaken > 0 ? Result.DONE : Result.NO_FOOD;
        if (!canAddFood()) return Result.NO_PLACE;

        ItemStack one = food.copy();
        one.setCount(1);
        recruit.getInventory().addItem(one);
        food.shrink(1);
        container.setChanged();
        foodTaken++;
        feedback();
        return foodTaken >= MAX_FOOD_ITEMS ? Result.DONE : Result.RUNNING;
    }

    private void feedback() {
        recruit.swing(InteractionHand.MAIN_HAND);
        recruit.playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.0F + (recruit.getRandom().nextFloat() - recruit.getRandom().nextFloat()) * 0.4F);
    }

    @Nullable
    private ItemStack findFood() {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && recruit.canEatItemStack(stack)) return stack;
        }
        return null;
    }

    private boolean canAddFood() {
        for (int i = FIRST_BACKPACK_SLOT; i < LAST_FOOD_SLOT; i++) {
            if (recruit.getInventory().getItem(i).isEmpty()) return true;
        }
        return false;
    }
}
