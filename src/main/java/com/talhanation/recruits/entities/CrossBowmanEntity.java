package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class CrossBowmanEntity extends AbstractRecruitEntity implements ICrossbowUser{
    private static final DataParameter<Boolean> IS_CHARGING_CROSSBOW = EntityDataManager.defineId(CrossBowmanEntity.class, DataSerializers.BOOLEAN);

    public CrossBowmanEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
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
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new RecruitFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(3, new RecruitRangedCrossbowAttackGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(4, new RecruitMoveToPosGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(5, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(6, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(7, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(9, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(12, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(1, new RecruitDefendVillageGoal(this));
        this.targetSelector.addGoal(2, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new RecruitAggresiveNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllagerEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
        //this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
        this.targetSelector.addGoal(10, new RecruitDefendVillageGoal(this));
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
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
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.setEquipment();
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setCanPickUpLoot(true);
        this.dropEquipment();
        //this.reassessWeaponGoal();
        this.setGroup(2);
        return ilivingentitydata;

    }

    public void setEquipment() {
        this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.CROSSBOW));
        this.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlotType.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlotType.FEET, new ItemStack(Items.LEATHER_BOOTS));
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
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }


    public void shootCrossbowProjectile(LivingEntity livingEntity, ItemStack itemStack, ProjectileEntity projectile, float x) {
        this.shootCrossbowProjectile(this, livingEntity, projectile, x, 1.6F);
    }

    @OnlyIn(Dist.CLIENT)
    public AbstractIllagerEntity.ArmPose getArmPose() {
        if (this.isChargingCrossbow()) {
            return AbstractIllagerEntity.ArmPose.CROSSBOW_CHARGE;
        } else if (this.isHolding(Items.CROSSBOW)) {
            return AbstractIllagerEntity.ArmPose.CROSSBOW_HOLD;
        } else {
            return this.isAggressive() ? AbstractIllagerEntity.ArmPose.ATTACKING : AbstractIllagerEntity.ArmPose.NEUTRAL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isChargingCrossbow() {
        return this.entityData.get(IS_CHARGING_CROSSBOW);
    }

    public void setChargingCrossbow(boolean bool) {
        this.entityData.set(IS_CHARGING_CROSSBOW, bool);
    }

    public boolean canFireProjectileWeapon(ShootableItem shootableItem) {
        return shootableItem == Items.CROSSBOW;
    }

    public void performRangedAttack(LivingEntity livingEntity, float x) {
        this.performCrossbowAttack(this, 1.6F);
    }
}
