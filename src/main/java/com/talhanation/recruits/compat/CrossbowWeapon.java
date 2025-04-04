package com.talhanation.recruits.compat;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IRangedRecruit;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.CallbackI;


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
        ItemStack weapon = this.getWeapon().getDefaultInstance();
        int quickChargeLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, weapon);
        return 20 - quickChargeLevel * 4;
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
        return new Arrow(shooter.getCommandSenderWorld(), shooter);
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
        double distance = shooter.distanceToSqr(x, y, z);
        double heightDiff = y - shooter.getY();

        double d0 = x - shooter.getX();
        double d1 = y - projectile.getY();
        double d2 = z - shooter.getZ();
        double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));

        float force = 2.25F;
        float accuracy = 0.2F; // 0 = 100%


        double angle = IRangedRecruit.getAngleDistanceModifier(distance, 85, 4) + IRangedRecruit.getCrossbowAngleHeightModifier(distance, heightDiff) / 100;

        projectile.shoot(d0, d1 + d3 * angle, d2, force, accuracy);

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
		
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, shooter.getMainHandItem());
        if (i > 0) {
            projectileEntity.setPierceLevel((byte)i);
        }

        int k = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, shooter.getMainHandItem());
        if (k > 0) {
            //TODO:
        }

        this.shootArrow(shooter, projectileEntity, x, y, z);

        shooter.playSound(this.getShootSound(), 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.getCommandSenderWorld().addFreshEntity(projectileEntity);

        if(RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get()){
            shooter.consumeArrow();
            projectileEntity.pickup = AbstractArrow.Pickup.ALLOWED;
        }

        shooter.damageMainHandItem();

    }

}
