package com.talhanation.recruits.compat;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class CrossbowWeapon implements IWeapon {
    @Override
    @Nullable
    public Item getWeapon() {
        return Items.CROSSBOW;
    }

    @Override
    public double getMoveSpeedAmp() {
        return 0.4D;
    }

    @Override
    public int getAttackCooldown() {
        return 30;
    }

    @Override
    public int getWeaponLoadTime() {
        return 20;
    }

    @Override
    public float getProjectileSpeed() {
        return 2.0F;
    }

    @Override
    public AbstractHurtingProjectile getProjectile(LivingEntity shooter) {
        return null;
    }
    @Override
    public AbstractArrow getProjectileArrow(LivingEntity shooter) {
        return new Arrow(shooter.level, shooter);
    }

    public boolean isLoaded(ItemStack itemStack) {
        if(itemStack.getItem() instanceof CrossbowItem){
            return CrossbowItem.isCharged(itemStack);
        }
        else
            return false;
    }

    @Override
    public void setLoaded(ItemStack stack, boolean loaded) {

    }

    @Override
    public AbstractHurtingProjectile shoot(LivingEntity shooter, AbstractHurtingProjectile projectile, double x, double y, double z) {
        return null;
    }

    @Override
    public AbstractArrow shootArrow(LivingEntity shooter, AbstractArrow projectile, double x, double y, double z) {
        double d3 = Mth.sqrt((float) (x * x + z * z));
        Vec3 vec3 = (new Vec3(x, y + d3 * (double) 0.1, z)).normalize().scale(1F);

        projectile.setDeltaMovement(vec3);
        projectile.shoot(x, y + d3 * (double) 0.1, z, 3.0F, (float) (2));

        return projectile;
    }

    @Override
    public SoundEvent getShootSound() {
        return SoundEvents.ARROW_SHOOT;
    }
    
    @Override
    public SoundEvent getLoadSound() {
        return SoundEvents.CROSSBOW_LOADING_END;
    }

    @Override
    public boolean isGun() {
        return false;
    }

    @Override
    public boolean canMelee() {
        return false;
    }

    @Override
    public boolean isBow(){
        return false;
    }

    @Override
    public boolean isCrossBow() {
        return true;
    }

    @Override
    public void performRangedAttackIWeapon(AbstractRecruitEntity shooter, double x, double y, double z, float projectileSpeed) {
        AbstractArrow projectileEntity = this.getProjectileArrow(shooter);
        double d0 = x - shooter.getX();
        double d1 = y + 0.5D - projectileEntity.getY();
        double d2 = z - shooter.getZ();


        this.shootArrow(shooter, projectileEntity, d0, d1, d2);

        shooter.playSound(this.getShootSound(), 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.level.addFreshEntity(projectileEntity);

        shooter.damageMainHandItem();
    }

}
