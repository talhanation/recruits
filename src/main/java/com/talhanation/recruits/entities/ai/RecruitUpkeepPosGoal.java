package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class RecruitUpkeepPosGoal extends Goal {
    public AbstractRecruitEntity recruit;
    public BlockPos chestPos;
    public Entity mobInv;
    public Container container;
    public boolean message;
    public boolean messageNotChest;

    public RecruitUpkeepPosGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.needsToGetFood() && recruit.getUpkeepPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
        messageNotChest = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.chestPos = findInvPos();
        if(recruit.getUpkeepTimer() == 0){

            if (chestPos != null && !recruit.hasFoodInInv()){
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
                                if(recruit.getOwner() != null && message){
                                    recruit.getOwner().sendSystemMessage(TEXT_NO_PLACE(recruit.getName().getString()));
                                    message = false;
                                }
                                //Main.LOGGER.debug("Chest empty");
                                this.stop();
                            }
                        }
                    }
                    else {
                        if(recruit.getOwner() != null && message){
                            recruit.getOwner().sendSystemMessage(TEXT_FOOD(recruit.getName().getString()));
                            message = false;
                            this.stop();
                        }
                    }


                    //Try to reequip
                    for(int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack itemstack = container.getItem(i);
                        ItemStack equipment;
                        if(!itemstack.isEdible() && recruit.wantsToPickUp(itemstack)){
                            if (recruit.canEquipItem(itemstack)) {
                                equipment = itemstack.copy();
                                equipment.setCount(1);
                                recruit.equipItem(equipment);
                                itemstack.shrink(1);
                            }
                        }
                    }
                    this.stop();
                }
            }
            else {
                this.chestPos = findInvPos();

                if(chestPos == null){
                    if(recruit.getOwner() != null && messageNotChest){
                        recruit.getOwner().sendMessage(TEXT_CANT_INTERACT(recruit.getName().getString()),recruit.getOwner().getUUID());
                        messageNotChest = false;

                        recruit.clearUpkeepPos();
                    }
                }
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

    private boolean canAddFood(){
        for(int i = 6; i < 14; i++){
            if(recruit.getInventory().getItem(i).isEmpty())
                return true;
        }
        return false;
    }

    private MutableComponent TEXT_NO_PLACE(String name) {
        return Component.translatable("chat.recruits.text.noPlaceInInv", name);
    }

    private MutableComponent TEXT_CANT_INTERACT(String name) {
        return new TranslatableComponent("chat.recruits.text.cantInteract", name);
    }

    private MutableComponent TEXT_FOOD(String name) {
        return Component.translatable("chat.recruits.text.noFoodInUpkeep", name);
    }
}
