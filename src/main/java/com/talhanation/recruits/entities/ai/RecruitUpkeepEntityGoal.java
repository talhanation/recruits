package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RecruitUpkeepEntityGoal extends Goal {
    public AbstractRecruitEntity recruit;
    public Optional<Entity> entity;
    public Container container;
    public boolean message;
    public boolean messageNotInRange;
    public BlockPos pos;
    public int timeToRecalcPath;
    private long lastCanUseCheck;

    public RecruitUpkeepEntityGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        long i = this.recruit.getCommandSenderWorld().getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;

            return recruit.needsToGetFood() && recruit.getUpkeepUUID() != null;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean isFoodInEntity(Container container){
        for(int i = 0; i < container.getContainerSize(); i++) {
            ItemStack foodItem = container.getItem(i);
            if(recruit.canEatItemStack(foodItem)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        timeToRecalcPath = 0;
        message = true;
        messageNotInRange = true;

        this.entity = findEntity();
        if(entity.isPresent()){
            this.pos = this.entity.get().getOnPos();

            if (entity.get() instanceof AbstractHorse horse) {
                this.container = horse.inventory;
                //Main.LOGGER.debug("found horse");
            }

            else if (entity.get() instanceof InventoryCarrier carrier) {
                this.container = carrier.getInventory();
                //Main.LOGGER.debug("found carrier");
            }

            else if (entity.get() instanceof Container containerEntity) {
                this.container = containerEntity;
                //Main.LOGGER.debug("found containerEntity");
            }
        }
        else
        if (recruit.getOwner() != null && messageNotInRange) {
            recruit.getOwner().sendMessage(TEXT_NOT_IN_RANGE(recruit.getName().getString()), recruit.getOwnerUUID());
            messageNotInRange = false;
            recruit.clearUpkeepEntity();
            this.stop();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (recruit.getUpkeepTimer() == 0) {
            //Main.LOGGER.debug("searching upkeep entity");
            if (entity.isPresent()) {
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay(10);
                    this.recruit.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.15D);
                }




                if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                    this.recruit.getJumpControl().jump();
                }
                double distance = this.recruit.position().distanceToSqr(Vec3.atCenterOf(pos));
                if (distance < 50 && container != null) {

                    this.checkIfMounted(entity.get());

                    this.recruit.getNavigation().stop();
                    this.recruit.getLookControl().setLookAt(entity.get().getX(), entity.get().getY() + 1, entity.get().getZ(), 10.0F, (float) this.recruit.getMaxHeadXRot());

                    if (isFoodInEntity(container)) {
                        for (int i = 0; i < 3; i++) {
                            ItemStack foodItem = this.getFoodFromInv(container);
                            ItemStack food;
                            if (foodItem != null && canAddFood()) {
                                food = foodItem.copy();
                                food.setCount(1);
                                recruit.getInventory().addItem(food);
                                foodItem.shrink(1);
                            } else {
                                if (recruit.getOwner() != null && message) {
                                    recruit.getOwner().sendMessage(TEXT_NO_PLACE(recruit.getName().getString()), recruit.getOwnerUUID());
                                    message = false;
                                }

                            }
                        }
                        this.stop();//stop taking food out of the container
                    }
                    else {
                        if (recruit.getOwner() != null && message) {
                            recruit.getOwner().sendMessage(TEXT_FOOD(recruit.getName().getString()), recruit.getOwnerUUID());
                            message = false;
                            this.stop();
                        }
                    }

                    //Try to reequip
                    for(int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack itemstack = container.getItem(i);
                        ItemStack equipment;
                        if(!recruit.canEatItemStack(itemstack) && recruit.wantsToPickUp(itemstack)){
                            if (recruit.canEquipItem(itemstack)) {
                                equipment = itemstack.copy();
                                equipment.setCount(1);
                                recruit.equipItem(equipment);
                                itemstack.shrink(1);
                            }
                            else if (recruit instanceof IRangedRecruit && itemstack.is(ItemTags.ARROWS)){ //all that are ranged
                                if(recruit.canTakeArrows()){
                                    equipment = itemstack.copy();
                                    recruit.inventory.addItem(equipment);
                                    itemstack.shrink(equipment.getCount());
                                }
                            }
                        }
                    }

                }
            }
            else {
                if (recruit.getOwner() != null && messageNotInRange) {
                    recruit.getOwner().sendMessage(TEXT_NOT_IN_RANGE(recruit.getName().getString()), recruit.getOwnerUUID());
                    messageNotInRange = false;

                    recruit.clearUpkeepEntity();
                    this.stop();
                }
            }
        }
    }

    private void checkIfMounted(Entity entity) {
        Entity vehicle = this.recruit.getVehicle();
        if(vehicle != null){
            if(vehicle.getUUID().equals(entity.getUUID())) {
                this.recruit.stopRiding();
            }
            else if(vehicle.getVehicle() != null && vehicle.getVehicle().getUUID().equals(entity.getUUID())){
                vehicle.stopRiding();
            }

        }
    }

    @Override
    public void stop() {
        super.stop();
        recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
        recruit.forcedUpkeep = false;
    }

    private Optional<Entity> findEntity() {
        if(this.recruit.getUpkeepUUID() != null) {
            return recruit.getCommandSenderWorld().getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(100.0D))
                    .stream()
                    .filter(entity -> entity.getUUID().equals(recruit.getUpkeepUUID())).findAny();
        }
        else return Optional.empty();
    }


    @Nullable
    private ItemStack getFoodFromInv(Container inv){
        ItemStack itemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++){
            if(recruit.canEatItemStack(inv.getItem(i))){
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
        return new TranslatableComponent("chat.recruits.text.noPlaceInInv", name);
    }

    private MutableComponent TEXT_NOT_IN_RANGE(String name) {
        return new TranslatableComponent("chat.recruits.text.cantFindEntity", name);
    }

    private MutableComponent TEXT_FOOD(String name) {
        return new TranslatableComponent("chat.recruits.text.noFoodInUpkeep", name);
    }
}
