package com.talhanation.recruits.entities;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.entities.ai.DefendVillageGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleStatus;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.schedule.Activity;
import net.minecraft.entity.ai.brain.schedule.Schedule;
import net.minecraft.entity.ai.brain.task.VillagerTasks;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

public class RecruitBaseEntity extends VillagerEntity implements IAngerable {

    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);

    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;


    public RecruitBaseEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(2, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(4, new PatrolVillageGoal(this, 0.6D));
        //this.goalSelector.addGoal(5, new SaluteVillagerGoal(this));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(0, new DefendVillageGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, PillagerEntity.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, MonsterEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, MobEntity.class, 5, false, false, (mob) -> {
            return mob instanceof IMob && !(mob instanceof CreeperEntity);
        }));
        this.targetSelector.addGoal(4, new ResetAngerGoal<>(this, false));
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
    public void setRemainingPersistentAngerTime(int p_230260_1_) {

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

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        //nbt.putBoolean("PlayerCreated", this.isPlayerCreated());
        this.addPersistentAngerSaveData(nbt);

    }

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

    @Override
    public void refreshBrain(ServerWorld world) {
        Brain<VillagerEntity> brain = this.getBrain();
        brain.stopAll(world, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    private void registerBrainGoals(Brain<VillagerEntity> villager) {
    /*
        VillagerProfession villagerprofession = this.getVillagerData().getProfession();

        villager.setSchedule(Schedule.VILLAGER_DEFAULT);
        villager.addActivityWithConditions(Activity.WORK, VillagerTasks.getWorkPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryModuleStatus.VALUE_PRESENT)));

        villager.addActivity(Activity.CORE, VillagerTasks.getCorePackage(villagerprofession, 0.5F));
        villager.addActivityWithConditions(Activity.MEET, VillagerTasks.getMeetPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryModuleStatus.VALUE_PRESENT)));
        villager.addActivity(Activity.REST, VillagerTasks.getRestPackage(villagerprofession, 0.5F));
        villager.addActivity(Activity.IDLE, VillagerTasks.getIdlePackage(villagerprofession, 0.5F));
        //villager.addActivity(Activity.PANIC, VillagerTasks.getPanicPackage(villagerprofession, 0.5F));
        //villager.addActivity(Activity.PRE_RAID, VillagerTasks.getPreRaidPackage(villagerprofession, 0.5F));
        //villager.addActivity(Activity.RAID, VillagerTasks.getRaidPackage(villagerprofession, 0.5F));
        //villager.addActivity(Activity.HIDE, VillagerTasks.getHidePackage(villagerprofession, 0.5F));
        villager.setCoreActivities(ImmutableSet.of(Activity.CORE));
        villager.setDefaultActivity(Activity.IDLE);
        villager.setActiveActivityIfPossible(Activity.IDLE);
        villager.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    */
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
}
