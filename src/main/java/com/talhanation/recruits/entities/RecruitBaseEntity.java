package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

public class RecruitBaseEntity extends AbstractHoldingEntity implements IAngerable {

    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);

    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;


    public RecruitBaseEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    /*
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new HoldGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(3, new RecruitFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));

        this.goalSelector.addGoal(4, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(4, new PatrolVillageGoal(this, 0.6D));
        //this.goalSelector.addGoal(5, new SaluteVillagerGoal(this));

        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new RecruitOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new DefendVillageGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, PillagerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, MonsterEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MobEntity.class, 5, false, false, (mob) -> {
            return mob instanceof IMob && !(mob instanceof CreeperEntity);
        }));
        this.targetSelector.addGoal(4, new ResetAngerGoal<>(this, false));
    }
    */

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new HoldGoal(this));
        //this.goalSelector.addGoal(3, new WolfEntity.AvoidEntityGoal(this, LlamaEntity.class, 24.0F, 1.5D, 1.5D));
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        //this.goalSelector.addGoal(6, new RecruitFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        //this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        //this.goalSelector.addGoal(9, new BegGoal(this, 8.0F));
        this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new RecruitOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, PillagerEntity.class, false));
        this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
    }

    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
    }

    protected SoundEvent getHurtSound(DamageSource p_184601_1_) {
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
    

    @Override
    public int getRemainingPersistentAngerTime() {
        return 0;
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {

    }

    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    public void setPersistentAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public void startPersistentAngerTimer() {

    }

    public boolean canAttackType(EntityType<?> target) {
       return super.canAttackType(target);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        //this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }
    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        //nbt.putBoolean("PlayerCreated", this.isPlayerCreated());
        this.addPersistentAngerSaveData(nbt);

    }
    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        //this.setPlayerCreated(p_70037_1_.getBoolean("PlayerCreated"));
        if(!level.isClientSide)
            this.readPersistentAngerSaveData((ServerWorld)this.level, nbt);
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }


    public void tick() {
        RecruitBaseEntity.this.setTarget(null);
        super.tick();
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance diff, SpawnReason reason, @Nullable ILivingEntityData spawnData, @Nullable CompoundNBT nbt) {
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("radom bonus on spawn", this.random.nextGaussian() * 0.05D, AttributeModifier.Operation.MULTIPLY_BASE));
        setEquipment();
        setCanPickUpLoot(true);
        return spawnData;
    }

    public void setEquipment() {
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();

        if (!this.isOwned() && item == Items.EMERALD ){
            this.recruit(player);
            itemstack.shrink(1);
            return ActionResultType.CONSUME;
        }
        else if (this.isOwnedBy(player)){
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToHold(true);
            return ActionResultType.SUCCESS;
        }
        else return ActionResultType.PASS;
    }

    public boolean wantsToAttack(LivingEntity lastHurt, LivingEntity owner) {
        if (!(lastHurt instanceof CreeperEntity) && !(lastHurt instanceof GhastEntity)) {
            if (lastHurt instanceof AbstractHoldingEntity) {
                AbstractHoldingEntity recruitEntity = (AbstractHoldingEntity)lastHurt;
                return !recruitEntity.isOwned() || recruitEntity.getOwner() != owner;
            } else if (lastHurt instanceof PlayerEntity &&
                    owner instanceof PlayerEntity &&
                    !((PlayerEntity)owner).canHarmPlayer((PlayerEntity)lastHurt)) {
                return false;
            } else if (lastHurt instanceof AbstractHorseEntity && ((AbstractHorseEntity)lastHurt).isTamed()) {
                return false;
            } else {
                return !(lastHurt instanceof TameableEntity) || !((TameableEntity)lastHurt).isTame();
            }
        } else {
            return false;
        }
    }
}
