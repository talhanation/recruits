package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;

public class CrossBowmanEntity extends AbstractRecruitEntity implements CrossbowAttackMob{
    private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(CrossBowmanEntity.class, EntityDataSerializers.BOOLEAN);

    public CrossBowmanEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_CHARGING_CROSSBOW, false);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return false;
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RecruitFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(3, new RecruitRangedCrossbowAttackGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(4, new RecruitMoveToPosGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(5, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(6, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(7, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new RecruitDefendVillageGoal(this));
        this.targetSelector.addGoal(2, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new RecruitAggresiveNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, false));
        //this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
        this.targetSelector.addGoal(10, new RecruitDefendVillageGoal(this));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

    }

    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.setEquipment();
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setCanPickUpLoot(true);
        this.dropEquipment();
        //this.reassessWeaponGoal();
        this.setGroup(2);
        return ilivingentitydata;

    }

    public void setEquipment() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
    }

    @Override
    public void initSpawn() {

    }

    @Override
    public int recruitCosts() {
        return 5;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        return !(itemStack.getItem() instanceof SwordItem ||  itemStack.getItem() instanceof ShieldItem || itemStack.getItem() instanceof BowItem);
    }

    @Nullable
    @Override
    public AgableMob getBreedOffspring(ServerLevel p_241840_1_, AgableMob p_241840_2_) {
        return null;
    }


    public void shootCrossbowProjectile(LivingEntity livingEntity, ItemStack itemStack, Projectile projectile, float x) {
        this.shootCrossbowProjectile(this, livingEntity, projectile, x, 1.6F);
    }

    @OnlyIn(Dist.CLIENT)
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isChargingCrossbow()) {
            return AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE;
        } else if (this.isHolding(Items.CROSSBOW)) {
            return AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
        } else {
            return this.isAggressive() ? AbstractIllager.IllagerArmPose.ATTACKING : AbstractIllager.IllagerArmPose.NEUTRAL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isChargingCrossbow() {
        return this.entityData.get(IS_CHARGING_CROSSBOW);
    }

    public void setChargingCrossbow(boolean bool) {
        this.entityData.set(IS_CHARGING_CROSSBOW, bool);
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem shootableItem) {
        return shootableItem == Items.CROSSBOW;
    }

    public void performRangedAttack(LivingEntity livingEntity, float x) {
        this.performCrossbowAttack(this, 1.6F);
    }
}
