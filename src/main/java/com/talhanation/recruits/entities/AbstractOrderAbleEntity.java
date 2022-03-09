package com.talhanation.recruits.entities;
//ezgi&talha kantar

import com.talhanation.recruits.AssassinEvents;
import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractOrderAbleEntity extends AbstractInventoryEntity{
    private static final DataParameter<Integer> DATA_REMAINING_ANGER_TIME = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> HOME_POS = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Integer> XP = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> LEVEL = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> KILLS = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> isEating = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> isInOrder = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FLEEING = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.BOOLEAN);

    //private static final DataParameter<ItemStack> OFFHAND_ITEM_SAVE = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.ITEM_STACK);

    public ItemStack beforeFoodItem;

    public AbstractOrderAbleEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setOwned(false);
        this.xpReward = 12;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }
    private void resetItemInHand() {
        this.setItemInHand(Hand.OFF_HAND, this.beforeFoodItem);
        this.setSlot(10, this.beforeFoodItem);
        this.beforeFoodItem = null;
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();

        if (this.getIsEating() && !this.isUsingItem()) {
            if (beforeFoodItem != null) resetItemInHand();
            setIsEating(false);
        }

    }

    public void rideTick() {
        super.rideTick();
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance diff, SpawnReason reason, @Nullable ILivingEntityData spawnData, @Nullable CompoundNBT nbt) {
        return spawnData;
    }


    public void setDropEquipment(){
        this.dropEquipment();
    }

    ////////////////////////////////////REGISTER////////////////////////////////////

    protected void registerGoals() {
        //this.goalSelector.addGoal(0, new RecruitQuaffGoal(this));

        this.goalSelector.addGoal(0, new FleeTNT(this));
        this.goalSelector.addGoal(0, new FleeTarget(this));
        this.goalSelector.addGoal(0, new FleeFire(this));
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(1, new UseShield(this));
        //this.goalSelector.addGoal(1, new RecruitEatGoal(this));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(7, new MoveTowardsTargetGoal(this, 1.15D, 32.0F));
        this.goalSelector.addGoal(8, new RecruitPickupWantedItemGoal(this));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(12, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(0, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllagerEntity.class, false));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(FLEEING, false);
        this.entityData.define(XP, 0);
        this.entityData.define(KILLS, 0);
        this.entityData.define(LEVEL, 1);
        this.entityData.define(isEating, true);
        this.entityData.define(isInOrder,false);
        //IS IN ORDER
        //IS LEADER
        //UUID OPFER optional

    }
    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("isInOrder", this.getIsInOrder());
        nbt.putBoolean("Fleeing", this.getFleeing());
        nbt.putBoolean("isEating", this.getIsEating());
        nbt.putInt("Xp", this.getXp());
        nbt.putInt("Level", this.getXpLevel());
        nbt.putInt("Kills", this.getKills());

    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        this.setIsInOrder(nbt.getBoolean("isInOrder"));
        this.setXpLevel(nbt.getInt("Level"));
        this.setFleeing(nbt.getBoolean("Fleeing"));
        this.setIsEating(nbt.getBoolean("isEating"));
        this.setXp(nbt.getInt("Xp"));
        this.setKills(nbt.getInt("Kills"));
    }


    ////////////////////////////////////GET////////////////////////////////////

    public boolean getIsInOrder() {
        return entityData.get(isInOrder);
    }

    public BlockPos getAssassinOnPos(){
        return getOnPos();
    }

    public boolean getFleeing() {
        return entityData.get(FLEEING);
    }

    public int getKills() {
        return entityData.get(KILLS);
    }

    public int getXpLevel() {
        return entityData.get(LEVEL);
    }

    public int getXp() {
        return entityData.get(XP);
    }

    public boolean getIsEating() {
        return entityData.get(isEating);
    }


    public SoundEvent getHurtSound(DamageSource ds) {
        if (this.isBlocking())
            return SoundEvents.SHIELD_BLOCK;
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(Pose pos, EntitySize size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public BlockPos getHomePos(){
        return entityData.get(HOME_POS).orElse(null);
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setIsInOrder(boolean bool){
        entityData.set(isInOrder, bool);
    }


    public void setFleeing(boolean bool){
        entityData.set(FLEEING, bool);
    }

    public void addXpLevel(int level){
        int currentLevel = this.getXpLevel();
        int newLevel = currentLevel + level;
        makelevelUpSound();
        this.entityData.set(LEVEL, newLevel);
    }

    public void setKills(int kills){
        this.entityData.set(KILLS, kills);
    }

    public void setXpLevel(int XpLevel){
        this.entityData.set(LEVEL, XpLevel);
    }

    public void setXp(int xp){
        this. entityData.set(XP, xp);
    }

    public void addXp(int xp){
        int currentXp = this.getXp();
        int newXp = currentXp + xp;

        this. entityData.set(XP, newXp);
    }

    public void setIsEating(boolean bool){
        entityData.set(isEating, bool);
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }

    public void setEquipment(){}


    public boolean playerHasEnoughEmeralds(PlayerEntity player, int price) {
        int emeraldCount = AssassinEvents.playerGetEmeraldsInInventory(player);
        if (emeraldCount >= price){
            return true;
        }
        if (player.isCreative()){
            return true;
        }
        else return false;
    }

    ////////////////////////////////////ATTACK FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amt = (amt + 1.0F) / 2.0F;
            }
            this.addXp(1);
            this.checkLevel();

            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }
        this.addXp(2);
        this.checkLevel();
        this.damageMainHandItem();
        return flag;
    }

    public void addLevelBuffs(){
        int level = getXpLevel();
        if(level <= 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 3D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.15D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.01D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.01D, AttributeModifier.Operation.ADDITION));
        }
        if(level > 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
        }
    }

    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
            if (target instanceof AbstractOrderAbleEntity) {
                AbstractOrderAbleEntity otherRecruit = (AbstractOrderAbleEntity)target;
                return otherRecruit.getOwner() != owner;
            } else if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity)owner).canHarmPlayer((PlayerEntity)target)) {
                return false;
            } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity)target).isTamed()) {
                return false;
            } else if (target instanceof RecruitHorseEntity) {
                return false;
            } else {
                return !(target instanceof TameableEntity) || !((TameableEntity)target).isTame();
            }
        } else {
            return false;
        }
    }


    public void die(DamageSource dmg) {
        super.die(dmg);
    }


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////


    public void checkLevel(){
        int currentXp = this.getXp();
        if (currentXp >= 1000){
            this.addXpLevel(1);
            this.setXp(0);
            this.addLevelBuffs();
            this.heal(10F);
        }
    }

    public void makelevelUpSound() {
        this.level.playSound(null, this.getX(), this.getY() + 4 , this.getZ(), SoundEvents.VILLAGER_YES, this.getSoundSource(), 15.0F, 0.8F + 0.4F * this.random.nextFloat());
        this.level.playSound(null, this.getX(), this.getY() + 4 , this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 15.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }



    protected void hurtArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 11; i < 15; ++i) {//11,12,13,14 = armor
                ItemStack itemstack = this.inventory.getItem(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireResistant()) && itemstack.getItem() instanceof ArmorItem) {
                    itemstack.setDamageValue((int) damage);
                }
            }
        }
    }

    protected void damageMainHandItem() {
        ItemStack itemstack = this.inventory.getItem(9);// 10 = hoffhand slot
        if (itemstack.getItem().isDamageable(itemstack)) {
            itemstack.setDamageValue(1);
        }
    }


    @Override
    public void killed(ServerWorld p_241847_1_, LivingEntity p_241847_2_) {
        super.killed(p_241847_1_, p_241847_2_);
        this.addXp(2);
        this.setKills(this.getKills() + 1);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.isShield(this)) {
            int i = 1 + MathHelper.floor(damage);
            Hand hand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
            if (this.useItem.isEmpty()) {
                if (hand == Hand.MAIN_HAND) {
                    this.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
                    this.setSlot(9, ItemStack.EMPTY);
                } else {
                    this.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
                    this.setSlot(10, ItemStack.EMPTY);
                }
                this.useItem = ItemStack.EMPTY;
                this.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
                this.setSlot(10, ItemStack.EMPTY);
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            }

            ItemStack itemstack = this.inventory.getItem(10);// 10 = hoffhand slot
            if (itemstack.getItem() instanceof ShieldItem) {
                itemstack.setDamageValue((int) damage);
            }
        }
    }

    @Override
    public abstract void openGUI(PlayerEntity player);

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
                if (player.isCrouching()) {
                    openGUI(player);
                    return ActionResultType.SUCCESS;
                }
                if(!player.isCrouching()) {
                    int i = this.random.nextInt(5);
                    switch (i) {
                        case 0:
                            player.sendMessage(new StringTextComponent(this.getName().getString() + ": " +" Hello my Friend."), player.getUUID());
                            break;
                        case 1:
                            player.sendMessage(new StringTextComponent(this.getName().getString() + ": " +"Life is only worth as much as emeralds..."), player.getUUID());
                            break;
                        default:
                            player.sendMessage(new StringTextComponent(this.getName().getString() + ": " +"Pay me I'll get rid of your headache!"), player.getUUID());
                            break;
                    }
                    return ActionResultType.SUCCESS;
                }
            return super.mobInteract(player, hand);
        }
    }

}
