package com.talhanation.recruits.entities;


import com.talhanation.recruits.IStrategicFire;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.RecruitStrategicFire;
import com.talhanation.recruits.entities.ai.RecruitRangedBowAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class BowmanEntity extends AbstractRecruitEntity implements RangedAttackMob, IStrategicFire {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    private static final EntityDataAccessor<Optional<BlockPos>> STRATEGIC_FIRE_POS = SynchedEntityData.defineId(BowmanEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> SHOULD_STRATEGIC_FIRE = SynchedEntityData.defineId(BowmanEntity.class, EntityDataSerializers.BOOLEAN);
    public BowmanEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STRATEGIC_FIRE_POS, Optional.empty());
        this.entityData.define(SHOULD_STRATEGIC_FIRE, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        if(this.StrategicFirePos() != null){

            nbt.putInt("StrategicFirePosX", this.StrategicFirePos().getX());
            nbt.putInt("StrategicFirePosY", this.StrategicFirePos().getY());
            nbt.putInt("StrategicFirePosZ", this.StrategicFirePos().getZ());
            nbt.putBoolean("ShouldStrategicFire", this.getShouldStrategicFire());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("StrategicFirePosX") && nbt.contains("StrategicFirePosY") && nbt.contains("StrategicFirePosZ")) {
            this.setStrategicFirePos(new BlockPos (
                    nbt.getInt("StrategicFirePosX"),
                    nbt.getInt("StrategicFirePosY"),
                    nbt.getInt("StrategicFirePosZ")));
            this.setShouldStrategicFire(nbt.getBoolean("ShouldStrategicFire"));
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new RecruitStrategicFire(this, 10, 20));
        this.goalSelector.addGoal(3, new RecruitRangedBowAttackGoal<>(this, 1.15D, 10, 20, 44.0F, getMeleeStartRange()));
    }
    @Override
    public double getMeleeStartRange() {
        return 3D;
    }


    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }


    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Bowman"));
        this.setCost(RecruitsModConfig.BowmanCost.get());
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setGroup(2);
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof ShieldItem || itemStack.getItem() instanceof CrossbowItem);
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float v) {
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack itemstack = this.getProjectile(this.getItemInHand(InteractionHand.MAIN_HAND));

            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, itemstack, v);
            arrow = ((net.minecraft.world.item.BowItem) this.getMainHandItem().getItem()).customArrow(arrow);

            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, itemstack);
            if (powerLevel > 0) arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D + this.arrowDamageModifier());

            int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, itemstack);
            if (punchLevel > 0) arrow.setKnockback(punchLevel);

            int fireLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, itemstack);
            if (fireLevel > 0) arrow.setSecondsOnFire(100);

            double d0 = target.getX() - this.getX();
            double d1 = target.getY(0.25D) - arrow.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
                                                    //angle   = 0.196F           //force     //accuracy 0 = 100%
            arrow.shoot(d0, d1 + d3 * (double) 0.196F, d2, 1.75F, (float) (0));

            this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level.addFreshEntity(arrow);


            this.damageMainHandItem();
        }
    }

    public double arrowDamageModifier() {
        return 0D;
    }

    public void performRangedAttackXYZ(double x, double y, double z, float v, float angle, float force) {
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack itemstack = this.getProjectile(this.getItemInHand(InteractionHand.MAIN_HAND));

            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, itemstack, v);
            arrow = ((net.minecraft.world.item.BowItem) this.getMainHandItem().getItem()).customArrow(arrow);

            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, itemstack);
            if (powerLevel > 0) arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D + this.arrowDamageModifier());

            int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, itemstack);
            if (punchLevel > 0) arrow.setKnockback(punchLevel);

            int fireLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, itemstack);
            if (fireLevel > 0) arrow.setSecondsOnFire(100);

            double d0 = x - this.getX();
            double d1 = y - this.getY();
            double d2 = z - this.getZ();
            double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));


                                         //angle            //force             //accuracy 0 = 100%
            arrow.shoot(d0, d1 + d3 + angle, d2, force + 1.95F, (float) (2.5));

            this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level.addFreshEntity(arrow);


            this.damageMainHandItem();
        }
    }

    public void fleeEntity(LivingEntity target) {
        if (target != null) {
            double fleeDistance = 10.0D;
            Vec3 vecTarget = new Vec3(target.getX(), target.getY(), target.getZ());
            Vec3 vecBowman = new Vec3(this.getX(), this.getY(), this.getZ());
            Vec3 fleeDir = vecBowman.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            double rnd = this.random.nextGaussian() * 1.2;
            Vec3 fleePos = new Vec3(vecBowman.x + rnd + fleeDir.x * fleeDistance, vecBowman.y + fleeDir.y * fleeDistance, vecBowman.z + rnd + fleeDir.z * fleeDistance);
            this.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.1D);
        }
    }

    public void setStrategicFirePos(BlockPos pos) {
        this.entityData.set(STRATEGIC_FIRE_POS, Optional.of(pos));
    }
    public BlockPos StrategicFirePos(){
        return this.entityData.get(STRATEGIC_FIRE_POS).orElse(null);
    }

    public void clearArrowsPos(){
        this.entityData.set(STRATEGIC_FIRE_POS, Optional.empty());
    }

    public void setShouldStrategicFire(boolean bool) {
        this.entityData.set(SHOULD_STRATEGIC_FIRE, bool);
    }
    public boolean getShouldStrategicFire(){
        return this.entityData.get(SHOULD_STRATEGIC_FIRE);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if ((itemStack.getItem() instanceof BowItem || itemStack.getItem() instanceof ProjectileWeaponItem || itemStack.getItem() instanceof SwordItem) && this.getMainHandItem().isEmpty()){
            return !hasSameTypeOfItem(itemStack);
        }
        else
            return super.wantsToPickUp(itemStack);
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    public List<String> getHandEquipment(){
        return RecruitsModConfig.BowmanHandEquipment.get();
    }
}