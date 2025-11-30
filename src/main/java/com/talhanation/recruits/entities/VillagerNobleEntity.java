package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.network.MessageToClientOpenNobleTradeScreen;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.recruits.world.RecruitsHireTradesRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class VillagerNobleEntity extends AbstractRecruitEntity {
    private static final EntityDataAccessor<CompoundTag> TRADES = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TRADER_PROGRESS = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRADER_LEVEL = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.STRING);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    private int restoreTimer;
    public boolean isTrading;
    public boolean needsNewTrades;
    public VillagerNobleEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRADES, new CompoundTag());
        this.entityData.define(TRADER_PROGRESS, 0);
        this.entityData.define(TRADER_LEVEL, 1);
        this.entityData.define(TYPE, "");
    }

    @Override
    public void tick() {
        super.tick();

        if(needsNewTrades){
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        }

        if(level().isClientSide()) return;

        if(tickCount % 20 == 0){
            if(this.getTarget() == null){
                this.switchMainHandItem(ItemStack::isEmpty);
                this.switchOffHandItem(ItemStack::isEmpty);
            }
        }

        if(restoreTimer > 0) restoreTimer--;

        if(needsNewTrades && !isTrading){
            this.updateUsesOfTrades();
            this.setupTrades();
            needsNewTrades = false;
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
        //TODO Move to village center
        //TODO buy from villager / swords, food, armor
        //TODO breed with villager?
        //TODO Supply Bread to villagers
        //TODO gossip?
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Trades", RecruitsHireTrade.listToNbt(getTrades()));
        nbt.putInt("TraderProgress", this.getTraderProgress());
        nbt.putInt("TraderLevel", this.getTraderLevel());
        nbt.putString("Type", this.getTraderType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTrades(RecruitsHireTrade.listFromNbt(nbt.getCompound("Trades")));
        this.setTraderProgress(nbt.getInt("TraderProgress"));
        this.setTraderLevel(nbt.getInt("TraderLevel"));
        this.setTraderType(nbt.getString("Type"));
    }

        //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);

    }
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((AsyncGroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public boolean canBeHired() {
        return false;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Noble Villager"));
        this.setCost(RecruitsServerConfig.ShieldmanCost.get());

        this.setEquipment();

        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        AbstractRecruitEntity.applySpawnValues(this);

        this.needsNewTrades = true;

        this.setupTraderType();
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
          (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);

        else return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem);
    }

    public List<List<String>> getEquipment(){
        return RecruitsServerConfig.ShieldmanStartEquipments.get();
    }

    public void openTradeGUI(Player player){
        this.isTrading(true);
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientOpenNobleTradeScreen(this.getUUID()));
    }
    public void addXpLevel(int level){
        super.addXpLevel(level);

        this.addTraderProgress(20);
    }
    public void setupTrades() {
        if(this.getCommandSenderWorld().isClientSide()) return;
        List<RecruitsHireTrade> possibleTrades = RecruitsHireTradesRegistry.getTrades(this.getTraderType(), this.getTraderLevel());
        if (possibleTrades == null || possibleTrades.isEmpty()) return;

        List<RecruitsHireTrade> current = this.getTrades();
        if (current == null) current = new ArrayList<>();

        Random rnd = new Random();

        for (RecruitsHireTrade trade : possibleTrades) {
            // Kopie erstellen, falls Trades als Templates dienen
            RecruitsHireTrade copy = trade.copy();
            copy.maxUses = 2 + rnd.nextInt(3);
            copy.uses = copy.maxUses;
            current.add(copy);
        }

        this.setTrades(current);
    }

    public int getTraderLevel(){
        return entityData.get(TRADER_LEVEL);
    }
    public int getTraderProgress() {
        return entityData.get(TRADER_PROGRESS);
    }

    public void setTraderProgress(int x) {
        this.entityData.set(TRADER_PROGRESS, x);
    }
    public void setTraderLevel(int x) {
        this.entityData.set(TRADER_LEVEL, x);
    }

    public String getTraderType() {
        return entityData.get(TYPE);
    }

    public void setupTraderType(){
        int i = this.random.nextInt(RecruitsHireTradesRegistry.getAllTraderTypes().size() - 1);
        String type = RecruitsHireTradesRegistry.getAllTraderTypes().get(i);
        this.setTraderType(type);
    }

    public void setTraderType(String x) {
        this.entityData.set(TYPE, x);
    }
    public void startSleeping(BlockPos pos) {
        super.startSleeping(pos);

        if(restoreTimer <= 0 ) this.restoreTrades();
    }
    private void restoreTrades() {
        List<RecruitsHireTrade> list = this.getTrades();

        for (RecruitsHireTrade trade : list) {
            trade.uses += random.nextInt(1,4);
            if(trade.uses > trade.maxUses) trade.uses = trade.maxUses;
        }

        this.setTrades(list);
        this.restoreTimer = 1200;
    }

    public void addTraderProgress(int x) {
        int current = this.getTraderProgress();
        if (x == 0) return;
        if(getTraderLevel() == 5){
            this.setTraderProgress(100);
            return;
        }


        int newProgress = current + x;
        //LevelUp
        if(newProgress >= 100){
            newProgress -= 100;

            this.setTraderLevel(getTraderLevel() + 1);
            this.needsNewTrades = true;
        }

        this.setTraderProgress(newProgress);
    }

    public void updateUsesOfTrades() {
        Random random = new Random();
        List<RecruitsHireTrade> list = this.getTrades();

        for (RecruitsHireTrade trade : list) {
            trade.maxUses += Math.min(7 , 2 + random.nextInt(3));
        }
        setTrades(list);
    }

    public List<RecruitsHireTrade> getTrades() {
        return RecruitsHireTrade.listFromNbt(this.entityData.get(TRADES));
    }

    public void setTrades(List<RecruitsHireTrade> list) {
        this.entityData.set(TRADES, RecruitsHireTrade.listToNbt(list));
    }

    public void doTrade(ResourceLocation resourceLocation){
        RecruitsHireTrade trade = null;
        List<RecruitsHireTrade> list = this.getTrades();
        for(RecruitsHireTrade canditade : list){
            if(canditade.resourceLocation.equals(resourceLocation)){
                trade = canditade;
                break;
            }
        }

        if(trade == null) return;

        trade.uses -= 1;

        addTraderProgress(15);

        this.setTrades(list);
    }

    public void addTrade(RecruitsHireTrade trade) {
        if (trade == null) return;
        List<RecruitsHireTrade> current = getTrades();
        boolean exists = current.stream().anyMatch(t -> Objects.equals(t.resourceLocation, trade.resourceLocation));
        if (exists) return;
        current.add(trade);
        setTrades(current);
    }

    public void removeTrade(ResourceLocation recruitType) {
        if (recruitType == null) return;
        List<RecruitsHireTrade> current = getTrades();
        boolean removed = current.removeIf(t -> Objects.equals(t.resourceLocation, recruitType));
        if (removed) setTrades(current);
    }
    public boolean hasTrade(ResourceLocation recruitType) {
        if (recruitType == null) return false;

        List<RecruitsHireTrade> trades = this.getTrades();
        if (trades == null || trades.isEmpty()) return false;

        for (RecruitsHireTrade trade : trades) {
            if (trade != null && recruitType.equals(trade.resourceLocation)) {
                return true;
            }
        }
        return false;
    }

    public void isTrading(boolean trading) {
        this.isTrading = trading;
    }

    protected void addParticlesAroundSelf(ParticleOptions p_35288_) {
        for(int i = 0; i < 5; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level().addParticle(p_35288_, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

}










