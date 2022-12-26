package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.helpers.NOPLogger;

public class RecruitUpkeepPosGoal extends Goal {
    public AbstractRecruitEntity recruit;
    public BlockPos chestPos;
    public Entity mobInv;
    public Container container;
    public boolean message;

    public RecruitUpkeepPosGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.needsToEat() && recruit.getUpkeepPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean hasFoodInInv(){
        return recruit.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    private boolean isFoodInChest(Container container){
        for(int i = 0; i < container.getContainerSize(); i++) {
            ItemStack foodItem = container.getItem(i);
            if(foodItem.isEdible()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        message = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.chestPos = findInvPos();

        if (chestPos != null && !this.hasFoodInInv()){
            BlockEntity entity = recruit.level.getBlockEntity(chestPos);

            if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            }

                this.recruit.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.15D);

                if (chestPos.closerThan(recruit.getOnPos(), 3) && container != null) {
                    this.recruit.getNavigation().stop();
                    this.recruit.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.recruit.getMaxHeadXRot());
                    if (isFoodInChest(container)) {
                        for (int i = 0; i < 3; i++) {
                            ItemStack foodItem = this.getFoodFromInv(container);
                            ItemStack food;
                            if (foodItem != null) {
                                food = foodItem.copy();
                                food.setCount(1);
                                recruit.getInventory().addItem(food);
                                foodItem.shrink(1);
                            } else {

                                //Main.LOGGER.debug("Chest empty");
                                break;
                            }
                        }
                    }
                    else {
                       if(recruit.getOwner() != null && message){
                           String name = recruit.getName().getString() + ": ";
                           String str = TEXT_NOFOOD.getString();
                           recruit.getOwner().sendMessage(new TextComponent(name + str), recruit.getOwner().getUUID());
                           message = false;
                       }
                    }
                }
                else stop();
        }
        else {
            this.chestPos = findInvPos();
            //Main.LOGGER.debug("Chest not found");
        }
    }

    @Nullable
    private BlockPos findInvPos() {
        if(this.recruit.getUpkeepPos() != null) {
            //Main.LOGGER.debug("up keep pos not null");
            BlockPos chestPos;
            int range = 8;

            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        chestPos = recruit.getUpkeepPos().offset(x, y, z);
                        BlockEntity block = recruit.level.getBlockEntity(chestPos);
                        if (block instanceof Container)
                            return chestPos;
                    }
                }
            }
        }
        //Main.LOGGER.debug("UpkeepPos NULL");
        //else entity around upkeepPos
        return null;
    }
    @Nullable
    private ItemStack getFoodFromInv(Container inv){
        ItemStack itemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++){
            if(inv.getItem(i).isEdible()){
                itemStack = inv.getItem(i);
                break;
            }
        }
        return itemStack;
    }

    private final TranslatableComponent TEXT_NOFOOD = new TranslatableComponent("chat.recruits.text.noFoodInUpkeep");
}
