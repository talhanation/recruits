package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.DefendVillageGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IronGolemEntity;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class RecruitBaseEntity extends VillagerEntity implements IAngerable {

    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);

    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;


    public RecruitBaseEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }


    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(2, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(4, new PatrolVillageGoal(this, 0.6D));
        //this.goalSelector.addGoal(5, new SaluteVillagerGoal(this));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new DefendVillageGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, MobEntity.class, 5, false, false, (p_234199_0_) -> {
            return p_234199_0_ instanceof IMob && !(p_234199_0_ instanceof CreeperEntity);
        }));
        this.targetSelector.addGoal(4, new ResetAngerGoal<>(this, false));
    }
/*
    @Override
    @Nullable
    public ILivingEntityData onInitialSpawn(@Nonnull IServerWorld world, @Nonnull DifficultyInstance difficulty, @Nonnull SpawnReason spawnReason, @Nullable ILivingEntityData livingdata, @Nullable CompoundNBT nbt) {
        livingdata = super.onInitialSpawn(world, difficulty, spawnReason, livingdata, nbt);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getWolfMaxHealth());
        this.getAttributeManager().createInstanceIfAbsent(Attributes.ATTACK_DAMAGE).setBaseValue(this.getWolfAttack());

        if (this.rand.nextDouble() <= 0.25D && System.currentTimeMillis() > lastAlphaTime + 100) {
            this.setVariant(1);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getWolfMaxHealth());
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getWolfAttack());
            this.setHealth(this.getWolfMaxHealth());
            this.experienceValue = 12;
            lastAlphaTime = System.currentTimeMillis();
        } else {
            this.setVariant(0);
        }
        return livingdata;
    }
*/

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

    private float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
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
}
