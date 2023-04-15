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

import java.util.Optional;


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
        return recruit.needsToGetFood();
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
        if(recruit.getUpkeepTimer() == 0){

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
                            if (foodItem != null && canAddFood()){

                                food = foodItem.copy();
                                food.setCount(1);
                                recruit.getInventory().addItem(food);
                                foodItem.shrink(1);
                            } else {
//<<<<<<< HEAD

//=======
                                if(recruit.getOwner() != null && message){
                                    recruit.getOwner().sendSystemMessage(TEXT_NO_PLACE(recruit.getName().getString()));
                                    message = false;

                                }
//>>>>>>> 649df22 (fixed recruits dont get food from upkeep)
                                //Main.LOGGER.debug("Chest empty");
                                this.stop();
                            }
                        }
                    }
                    else {
//<<<<<<< HEAD
                       if(recruit.getOwner() != null && message){
                           String name = recruit.getName().getString() + ": ";
                           String str = TEXT_NOFOOD.getString();
                           recruit.getOwner().sendMessage(new TextComponent(name + str), recruit.getOwner().getUUID());
                           message = false;
                       }

//=======
                        if(recruit.getOwner() != null && message){
                            recruit.getOwner().sendSystemMessage(TEXT_FOOD(recruit.getName().getString()));
                            message = false;
                            this.stop();
                        }
//>>>>>>> 649df22 (fixed recruits dont get food from upkeep)
                    }
                    this.stop();
                }
            }
            else {
                this.chestPos = findInvPos();
                //Main.LOGGER.debug("Chest not found");
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
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
    private boolean canAddFood(){
        for(int i = 6; i < 14; i++){
            if(recruit.getInventory().getItem(i).isEmpty())
                return true;
        }
        return false;
    }
}
