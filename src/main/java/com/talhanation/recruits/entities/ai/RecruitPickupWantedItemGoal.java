package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.talhanation.recruits.entities.ai.RecruitPickupWantedItemGoal.State.*;


public class RecruitPickupWantedItemGoal extends Goal {

    public AbstractRecruitEntity recruit;
    public State state;
    public List<ItemEntity> itemEntityList = new ArrayList<>();
    public ItemEntity itemEntity;
    private byte timer;

    public RecruitPickupWantedItemGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getTarget() == null && !this.recruit.isFollowing() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos() && !recruit.getShouldHoldPos();
    }

    @Override
    public void start() {
        super.start();
        timer = 0;
        state = SEARCH;
    }

    @Override
    public void stop() {
        super.stop();
        recruit.setCanPickUpLoot(false);
    }

    @Override
    public void tick() {
        switch (state) {
            case SEARCH -> {
                recruit.getCommandSenderWorld().getEntitiesOfClass(
                        ItemEntity.class,
                        recruit.getBoundingBox().inflate(16.0D, 3.0D, 16.0D),
                        (item) -> recruit.getAllowedItems().test(item) &&
                                recruit.distanceTo(itemEntity) < 25 &&
                                ((itemEntity.getItem().isEdible() && recruit.getHunger() < 30) ||
                                        (recruit.wantsToPickUp(itemEntity.getItem())))
                ).forEach((item) -> {
                    this.itemEntityList.add(itemEntity);
                });

                if (itemEntityList.isEmpty()) {
                    state = SEARCH;
                } else {
                    state = SELECT;
                }
            }

            case SELECT -> {
                if (!itemEntityList.isEmpty()) {
                    ItemEntity result = null;
                    double d0 = -1.0D;

                    for (ItemEntity item : itemEntityList) {
                        double d1 = recruit.distanceToSqr(item);
                        if (d0 == -1.0D || d0 < d1) {
                            result = item;
                            d0 = d1;
                        }
                    }

                    this.itemEntity = result;
                    this.state = MOVE;
                } else state = SEARCH;
            }

            case MOVE -> {
                if (itemEntity != null) {
                    recruit.getNavigation().moveTo(itemEntity, 1F);
                    recruit.setMaxUpStep(1.25F);
                    if (recruit.distanceTo(itemEntity) < 3F) {
                        this.state = PICKUP;
                        recruit.setMaxUpStep(1F);

                    }
                } else state = SELECT;
            }

            case PICKUP -> {
                recruit.getNavigation().moveTo(itemEntity, 1F);
                this.recruit.setCanPickUpLoot(true);
                if (++this.timer > 30) {
                    this.itemEntityList.clear();
                    this.recruit.setCanPickUpLoot(false);
                    this.timer = 0;
                    this.state = SELECT;
                }
            }
        }
    }


    enum State {
        SEARCH,
        SELECT,
        MOVE,
        PICKUP
    }
}