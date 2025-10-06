package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitsHireTradesRegistry;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.network.MessageToClientOpenNobleTradeScreen;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import java.util.stream.Collectors;

public class VillagerNobleEntity extends AbstractRecruitEntity {
    private static final EntityDataAccessor<CompoundTag> TRADES = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> XP_PROGRESS = SynchedEntityData.defineId(VillagerNobleEntity.class, EntityDataSerializers.INT);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public VillagerNobleEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRADES, new CompoundTag());
        this.entityData.define(XP_PROGRESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if(level().isClientSide()) return;

        if(tickCount % 20 == 0){
            if(this.getTarget() == null){
                this.switchMainHandItem(ItemStack::isEmpty);
                this.switchOffHandItem(ItemStack::isEmpty);
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
        //Move to village center
        //buy from villager / swords, food, armor
        //breed with villager?
        //Supply Bread to villagers
        //gossip?
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Trades", RecruitsHireTrade.listToNbt(getTrades()));
        nbt.putInt("XpProgress", this.getXpProgress());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTrades(RecruitsHireTrade.listFromNbt(nbt.getCompound("Trades")));
        this.setXpProgress(nbt.getInt("XpProgress"));
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
        this.setCost(RecruitsServerConfig.RecruitCost.get());

        this.setEquipment();

        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        this.setGroup(1);

        AbstractRecruitEntity.applySpawnValues(this);

        this.setupTrades();
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
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientOpenNobleTradeScreen(this.getUUID()));
    }
    public void addXpLevel(int level){
        super.addXpLevel(level);

        this.addXpProgress(50);
    }
    public void setupTrades() {
        List<RecruitsHireTrade> possibleTrades = RecruitsHireTradesRegistry.getAll();

        List<RecruitsHireTrade> current = this.getTrades();
        if (current == null) current = new ArrayList<>();

        int xpLevel = getTraderLevel();

        List<RecruitsHireTrade> finalCurrent = current;
        List<RecruitsHireTrade> candidates = possibleTrades.stream()
                .filter(t -> t.minLevel <= xpLevel)
                .filter(t -> finalCurrent.stream().noneMatch(c -> Objects.equals(c.recruitType, t.recruitType)))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return;

        Random rnd = new Random();
        Collections.shuffle(candidates, rnd);

        int added = 0;
        for (RecruitsHireTrade candidate : candidates) {
            if (added >= 2) break;

            if (candidate.chance <= 0) continue;
            if (rnd.nextInt(100) < candidate.chance) {

                candidate.uses = rnd.nextInt(1,4);
                this.addTrade(candidate);
                added++;
            }
        }
    }
    public int getTraderLevel(){
        return Math.max(1, this.getXpProgress() / 100);
    }
    public int getXpProgress() {
        return entityData.get(XP_PROGRESS);
    }

    public void setXpProgress(int x) {
        this.entityData.set(XP_PROGRESS, x);
    }

    public void addXpProgress(int x){
        int current =  this.getXpProgress();
        int newXp = current + x;
        if(newXp < 1000 && current % 100 == 0){
            updateUsesOfTrades();
        }

        this.setXpProgress(Mth.clamp(newXp, 0, 1000));
    }

    public void updateUsesOfTrades() {
        Random random = new Random();
        List<RecruitsHireTrade> list = this.getTrades();
        for (RecruitsHireTrade trade : list) {
            int minLevel = trade.minLevel;

            if (this.getTraderLevel() >= minLevel) {
                float baseChance = Math.max(0.2F, 1.0F - (minLevel * 0.15F));

                if (random.nextFloat() < baseChance) {
                    int additionalUses = 1 + random.nextInt(3);
                    trade.uses += additionalUses;
                }
            }
        }
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
            if(canditade.recruitType.equals(resourceLocation)){
                trade = canditade;
                break;
            }
        }

        if(trade == null) return;

        trade.uses -= 1;

        addXpProgress(trade.minLevel * 5);

        this.setTrades(list);
    }

    public void addTrade(RecruitsHireTrade trade) {
        if (trade == null) return;
        List<RecruitsHireTrade> current = getTrades();
        boolean exists = current.stream().anyMatch(t -> Objects.equals(t.recruitType, trade.recruitType));
        if (exists) return;
        current.add(trade);
        setTrades(current);
    }

    public void removeTrade(RecruitsHireTrade trade) {
        if (trade == null) return;
        List<RecruitsHireTrade> current = getTrades();
        boolean removed = current.removeIf(t -> Objects.equals(t.recruitType, trade.recruitType));
        if (removed) setTrades(current);
    }
}










