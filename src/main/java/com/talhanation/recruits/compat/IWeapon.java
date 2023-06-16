package com.talhanation.recruits.compat;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IWeapon {
    Item getWeapon();
    double getMoveSpeedAmp();
    int getAttackCooldown();
    int getWeaponLoadTime();
    float getProjectileSpeed();
    Entity getProjectile(LivingEntity shooter);
    Entity shoot(LivingEntity shooter, Entity projectile, double x, double y, double z);
    SoundEvent getShootSound();
    SoundEvent getLoadSound();
    boolean isGun();
    boolean canMelee();
    boolean isBow();
    boolean isCrossBow();
    void performRangedAttackIWeapon(AbstractRecruitEntity shooter, LivingEntity target, float projectileSpeed);

    static boolean isMusketModWeapon(ItemStack stack){
        return stack.getDescriptionId().equals("item.musketmod.musket") ||
                stack.getDescriptionId().equals("item.musketmod.cartridge") ||
                stack.getDescriptionId().equals("item.musketmod.pistol");
    }

    boolean isLoaded(ItemStack stack);

    void setLoaded(ItemStack stack, boolean loaded);
}
