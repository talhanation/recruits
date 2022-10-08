package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class RecruitUpkeepGoal extends Goal {
    public AbstractRecruitEntity recruit;
    public BlockPos chestPos;
    public Entity mobInv;
    public Container container;

    public RecruitUpkeepGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return true;
    } // && recruit.needsToEat()

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean hasFoodInInv(){
        return recruit.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    @Override
    public void start() {
        super.start();
        Main.LOGGER.debug("upkeep started");


    }

    @Override
    public void tick() {
        super.tick();
        this.chestPos = findInvPos();
        Main.LOGGER.debug("searching upkeep");
        if (chestPos != null) {
            Main.LOGGER.debug("Moving to chest");
            this.recruit.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.15D);
            BlockEntity entity = recruit.level.getBlockEntity(chestPos);

            if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            }

            if (chestPos.closerThan(recruit.getOnPos(), 3) && container != null) {

                this.recruit.getNavigation().stop();
                this.recruit.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.recruit.getMaxHeadXRot());

                Main.LOGGER.debug("Getting food from chest");
                ItemStack foodItem = this.getFoodFromInv(container);
                if(foodItem != null) recruit.getInventory().addItem(foodItem);
                else Main.LOGGER.debug("Chest empty");
            }
        }else {
            this.chestPos = findInvPos();
            Main.LOGGER.debug("Chest not found");
        }
    }

    @Nullable
    private BlockPos findInvPos() {
        if(this.recruit.getUpkeepPos() != null) {
            Main.LOGGER.debug("up keep pos not null");
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
        Main.LOGGER.debug("UpkeepPos NULL");
        //else entity around upkeepPos
        return null;
    }
    @Nullable
    private ItemStack getFoodFromInv(Container inv){
        ItemStack itemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++){
            if(inv.getItem(i).isEdible()){
                itemStack = inv.getItem(i);
                itemStack.shrink(1);
                break;
            }
        }
        return itemStack;
    }
}
