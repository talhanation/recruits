package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

import static com.talhanation.recruits.entities.ai.RecruitPickupWantedItemGoal.State.*;


public class RecruitPickupWantedItemGoal extends Goal{

    public AbstractRecruitEntity recruit;
    public State state;
    public List<ItemEntity> itemEntityList = new ArrayList<>();
    public ItemEntity itemEntity;
    private byte timer;
    public boolean wasHoldingPos;

    public RecruitPickupWantedItemGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        if(!this.recruit.isFollowing() && !recruit.getFleeing() && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos()){
            if(recruit.getTarget() != null){
                return !recruit.getTarget().isAlive();
            }
            else
                return true;

        }
        else return false;
    }
    @Override
    public void start(){
        super.start();
        timer = 0;
        state = SEARCH;
        wasHoldingPos = recruit.getShouldHoldPos();
    }

    @Override
    public void stop() {
        super.stop();
        recruit.setCanPickUpLoot(false);
    }

    @Override
    public void tick() {
        Main.LOGGER.info("State: " + state);
        switch (state){
            case SEARCH -> {
                List<ItemEntity> list = recruit.level.getEntitiesOfClass(ItemEntity.class, recruit.getBoundingBox().inflate(16.0D, 3.0D, 16.0D), recruit.getAllowedItems());
                if (!list.isEmpty()) {
                    for(ItemEntity itemEntity : list){
                        if((itemEntity.getItem().isEdible() && recruit.getHunger() < 30) || (recruit.canEquipItem(itemEntity.getItem()))){
                            this.itemEntityList.add(itemEntity);
                        }
                    }
                }

                if(itemEntityList.isEmpty()){
                    state = SEARCH;
                }
                else state = SELECT;
            }

            case SELECT -> {
                if(!itemEntityList.isEmpty()){
                    //TODO: sort to distance
                    this.itemEntity = itemEntityList.get(0);
                    this.state = MOVE;
                }
                else state = SEARCH;
            }

            case MOVE -> {
                if(wasHoldingPos) recruit.setShouldHoldPos(false);
                if(itemEntity != null){
                    recruit.getNavigation().moveTo(itemEntity, 1F);
                    recruit.maxUpStep = 1.25F;
                    if(recruit.distanceTo(itemEntity) < 3F) {
                        this.state = PICKUP;
                        recruit.maxUpStep = 1F;

                    }
                }
                else state = SELECT;
            }

            case PICKUP -> {
                recruit.getMoveControl().setWantedPosition(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 1F);
                this.recruit.setCanPickUpLoot(true);
                if(++this.timer > 30){
                    this.itemEntityList.remove(0);
                    this.recruit.setCanPickUpLoot(false);
                    this.timer = 0;
                    if(wasHoldingPos) recruit.setShouldHoldPos(true);
                    this.state = SELECT;
                }
            }
        }
    }


    enum State{
        SEARCH,
        SELECT,
        MOVE,
        PICKUP
    }
}