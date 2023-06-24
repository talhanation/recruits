package com.talhanation.recruits.entities;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.RecruitRangedCrossbowAttackGoal;
import com.talhanation.recruits.entities.ai.compat.RecruitRangedMusketAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

import static com.talhanation.recruits.Main.isMusketModLoaded;


public class CrossBowmanEntity extends AbstractRecruitEntity implements CrossbowAttackMob{

    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(CrossBowmanEntity.class, EntityDataSerializers.BOOLEAN);
    //public IWeapon weapon;

    public CrossBowmanEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);

    }

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_CHARGING_CROSSBOW, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        if(isMusketModLoaded){
            this.goalSelector.addGoal(0, new RecruitRangedMusketAttackGoal(this));
        }
        this.goalSelector.addGoal(0, new RecruitRangedCrossbowAttackGoal(this));
    }


    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }


    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Crossbowman"));
        this.setCost(8);
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        this.setGroup(2);
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof ShieldItem) || itemStack.getItem() instanceof CrossbowItem;
    }
    public void performRangedAttack(@NotNull LivingEntity target, float v) {
        this.damageMainHandItem();
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
            this.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.0D);
        }
    }

    @Override
    public boolean wantsToPickUp(@NotNull ItemStack itemStack) {
        if(isMusketModLoaded && IWeapon.isMusketModWeapon(itemStack)) return true;

        if (itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof ProjectileWeaponItem){
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
        return RecruitsModConfig.CrossbowmanHandEquipment.get();
    }

    //Pillager
    @Override
    public void shootCrossbowProjectile(@NotNull LivingEntity target, @NotNull ItemStack stack, @NotNull Projectile projectile, float f) {
        this.shootCrossbowProjectile(this, target, projectile, f, 1.6F);
    }

    private boolean getChargingCrossbow() {
        return this.entityData.get(DATA_IS_CHARGING_CROSSBOW);
    }

    public void setChargingCrossbow(boolean is) {
        this.entityData.set(DATA_IS_CHARGING_CROSSBOW, is);
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem weaponItem) {
        return weaponItem.equals(Items.CROSSBOW);
    }
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

}
