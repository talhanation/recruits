package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    public boolean canResetPaymentTimer = false;

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

    private boolean isFoodInEntity(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack foodItem = container.getItem(i);
            if (recruit.canEatItemStack(foodItem)) {
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
        if (entity.isPresent()) {
            this.pos = this.entity.get().getOnPos();

            if (entity.get() instanceof AbstractHorse horse) {
                this.container = horse.inventory;
                //Main.LOGGER.debug("found horse");
            } else if (entity.get() instanceof InventoryCarrier carrier) {
                this.container = carrier.getInventory();
                //Main.LOGGER.debug("found carrier");
            } else if (entity.get() instanceof Container containerEntity) {
                this.container = containerEntity;
                //Main.LOGGER.debug("found containerEntity");
            }
        }
        else {
            if (recruit.getOwner() != null && messageNotInRange) {
                recruit.getOwner().sendSystemMessage(TEXT_NOT_IN_RANGE(recruit.getName().getString()));
                messageNotInRange = false;
            }
            recruit.clearUpkeepEntity();
            this.stop();
        }
    }

    @Override
    public void tick() {
        super.tick();
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

                if(recruit.paymentTimer == 0){
                    recruit.checkPayment(container);
                    canResetPaymentTimer = true;
                }

                this.recruit.upkeepReequip(container);

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
                                recruit.getOwner().sendSystemMessage(TEXT_NO_PLACE(recruit.getName().getString()));
                                message = false;
                            }
                        }
                    }
                    this.stop(); //stop taking food out of the container
                    return;
                } else {
                    if (recruit.getOwner() != null && message) {
                        recruit.getOwner().sendSystemMessage(TEXT_FOOD(recruit.getName().getString()));
                        message = false;
                    }
                    this.stop();
                    return;
                }
            }
        } else {
            if (recruit.getOwner() != null && messageNotInRange) {
                recruit.getOwner().sendSystemMessage(TEXT_NOT_IN_RANGE(recruit.getName().getString()));
                messageNotInRange = false;

                recruit.clearUpkeepEntity();
                this.stop();
            }
        }

    }

    private void checkIfMounted(Entity entity) {
        Entity vehicle = this.recruit.getVehicle();
        if (vehicle != null) {
            if (vehicle.getUUID().equals(entity.getUUID())) {
                this.recruit.stopRiding();
            } else if (vehicle.getVehicle() != null && vehicle.getVehicle().getUUID().equals(entity.getUUID())) {
                vehicle.stopRiding();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
        recruit.forcedUpkeep = false;
        if(recruit.paymentTimer == 0 && canResetPaymentTimer){
            canResetPaymentTimer = false;
            recruit.resetPaymentTimer();
        }
    }

    private Optional<Entity> findEntity() {
        if (this.recruit.getUpkeepUUID() == null) return Optional.empty();

        List<Entity> entities = recruit.getCommandSenderWorld().getEntitiesOfClass(
                Entity.class,
                recruit.getBoundingBox().inflate(100.0D),
                (entity) -> entity.getUUID().equals(recruit.getUpkeepUUID())
        );

        return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }


    @Nullable
    private ItemStack getFoodFromInv(Container inv) {
        ItemStack itemStack = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (recruit.canEatItemStack(inv.getItem(i))) {
                itemStack = inv.getItem(i);
                break;
            }
        }
        return itemStack;
    }


    private boolean canAddFood() {
        for (int i = 6; i < 14; i++) {
            if (recruit.getInventory().getItem(i).isEmpty())
                return true;
        }
        return false;
    }

    private MutableComponent TEXT_NO_PLACE(String name) {
        return Component.translatable("chat.recruits.text.noPlaceInInv", name);
    }

    private MutableComponent TEXT_NOT_IN_RANGE(String name) {
        return Component.translatable("chat.recruits.text.cantFindEntity", name);
    }

    private MutableComponent TEXT_FOOD(String name) {
        return Component.translatable("chat.recruits.text.noFoodInUpkeep", name);
    }
}
