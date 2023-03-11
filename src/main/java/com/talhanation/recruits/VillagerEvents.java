package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModProfessions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class VillagerEvents {
    protected final Random random = new Random();

    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingTickEvent event) {
        HashMap<VillagerProfession, EntityType<? extends  AbstractRecruitEntity>> entitiesByProfession = new HashMap<>(){{
            put(ModProfessions.RECRUIT.get(), ModEntityTypes.RECRUIT.get());
            put(ModProfessions.BOWMAN.get(), ModEntityTypes.BOWMAN.get());
            put(ModProfessions.SHIELDMAN.get(), ModEntityTypes.RECRUIT_SHIELDMAN.get());
            }
        };

        Entity entity = event.getEntity();
        if (entity instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if(entitiesByProfession.containsKey(profession)) {
                EntityType<? extends AbstractRecruitEntity> recruitType = entitiesByProfession.get(profession);
                createRecruit(villager, recruitType);
            }
        }


        if (entity instanceof IronGolem ironGolemEntity) {

            if (!ironGolemEntity.isPlayerCreated() && RecruitsModConfig.OverrideIronGolemSpawn.get()){
                List<AbstractRecruitEntity> list1 = entity.level.getEntitiesOfClass(AbstractRecruitEntity.class, ironGolemEntity.getBoundingBox().inflate(32));
                if (list1.size() > 1) {
                    ironGolemEntity.remove(Entity.RemovalReason.KILLED);
                    //System.out.println(olem was removed");
                }
                else {
                    int i = this.random.nextInt(5);
                    if (i == 1) createBowmanIronGolem(ironGolemEntity);
                    else if (i == 0) createRecruitShieldmanIronGolem(ironGolemEntity);
                    else createRecruitIronGolem(ironGolemEntity);
                    //System.out.println("Spawned new Recruit");
                }
            }
        }


        if (entity instanceof ZombieVillager zombie){
            VillagerProfession profession = zombie.getVillagerData().getProfession();
            /*
            if (profession.equals(ModProfessions.RECRUIT.get())) {
                zombie.remove(Entity.RemovalReason.DISCARDED);
            }

            if (profession.equals(ModProfessions.BOWMAN.get())){
                zombie.remove(Entity.RemovalReason.DISCARDED);
            }
            /*
            if (profession.equals(ModProfessions.NOMAD.get())){
                zombie.remove(Entity.RemovalReason.DISCARDED);
            }

            if (profession.equals(ModProfessions.SHIELDMAN.get())){
                zombie.remove(Entity.RemovalReason.DISCARDED);
            }
             */
        }
    }
    private static void createRecruit(Villager villager, EntityType<? extends AbstractRecruitEntity> recruitType){
        AbstractRecruitEntity abstractRecruit = recruitType.create(villager.level);
        if (abstractRecruit != null) {
            abstractRecruit.copyPosition(villager);
            abstractRecruit.initSpawn();
            villager.remove(Entity.RemovalReason.DISCARDED);
            villager.level.addFreshEntity(abstractRecruit);
        }
    }

    private static void createNomad(LivingEntity entity){
        NomadEntity nomad = ModEntityTypes.NOMAD.get().create(entity.level);
        Villager villager = (Villager) entity;
        nomad.copyPosition(villager);

        nomad.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);

        RecruitHorseEntity horse = createHorse(nomad);
        nomad.startRiding(horse);

        villager.level.addFreshEntity(nomad);
    }

    private static RecruitHorseEntity createHorse(LivingEntity entity) {
        RecruitHorseEntity horse = ModEntityTypes.RECRUIT_HORSE.get().create(entity.level);
        horse.setPos(entity.getX(), entity.getY(), entity.getZ());
        horse.invulnerableTime = 60;
        horse.setPersistenceRequired();
        return horse;
    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {

        if (event.getType() == VillagerProfession.ARMORER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 10, ModBlocks.RECRUIT_SHIELD_BLOCK.get(), 1, 4, 10);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 3, ModBlocks.RECRUIT_BLOCK.get(), 1, 4, 10);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FLETCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 4, ModBlocks.BOWMAN_BLOCK.get(), 1, 4, 10);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
    }

    private static void createRecruitIronGolem(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.level);
        IronGolem villager = (IronGolem) entity;
        recruit.copyPosition(villager);

        recruit.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        recruit.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(recruit);
    }

    private void createRecruitShieldmanIronGolem(LivingEntity entity){
        RecruitShieldmanEntity recruitShieldman = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(entity.level);
        IronGolem villager = (IronGolem) entity;
        recruitShieldman.copyPosition(villager);

        recruitShieldman.initSpawn();

        recruitShieldman.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(recruitShieldman);
    }

    private static void createBowmanIronGolem(LivingEntity entity){
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(entity.level);
        IronGolem villager = (IronGolem) entity;
        bowman.copyPosition(villager);

        bowman.initSpawn();

        bowman.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(bowman);
    }

    static class EmeraldForItemsTrade extends Trade {
        public EmeraldForItemsTrade(ItemLike buyingItem, int buyingAmount, int maxUses, int givenExp) {
            super(buyingItem, buyingAmount, Items.EMERALD, 1, maxUses, givenExp);
        }
    }

    static class Trade implements VillagerTrades.ItemListing {
        private final Item buyingItem;
        private final Item sellingItem;
        private final int buyingAmount;
        private final int sellingAmount;
        private final int maxUses;
        private final int givenExp;
        private final float priceMultiplier;

        public Trade(ItemLike buyingItem, int buyingAmount, ItemLike sellingItem, int sellingAmount, int maxUses,
                     int givenExp) {
            this.buyingItem = buyingItem.asItem();
            this.buyingAmount = buyingAmount;
            this.sellingItem = sellingItem.asItem();
            this.sellingAmount = sellingAmount;
            this.maxUses = maxUses;
            this.givenExp = givenExp;
            this.priceMultiplier = 0.05F;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            return new MerchantOffer(new ItemStack(this.buyingItem, this.buyingAmount),
                    new ItemStack(sellingItem, sellingAmount), maxUses, givenExp, priceMultiplier);
        }
    }

    static class ItemsForEmeraldsTrade implements VillagerTrades.ItemListing {
        private final ItemStack itemStack;
        private final int emeraldCost;
        private final int numberOfItems;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public ItemsForEmeraldsTrade(Block p_i50528_1_, int p_i50528_2_, int p_i50528_3_, int p_i50528_4_,
                                     int p_i50528_5_) {
            this(new ItemStack(p_i50528_1_), p_i50528_2_, p_i50528_3_, p_i50528_4_, p_i50528_5_);
        }

        public ItemsForEmeraldsTrade(Item p_i50529_1_, int p_i50529_2_, int p_i50529_3_, int p_i50529_4_) {
            this(new ItemStack(p_i50529_1_), p_i50529_2_, p_i50529_3_, 12, p_i50529_4_);
        }

        public ItemsForEmeraldsTrade(Item p_i50530_1_, int p_i50530_2_, int p_i50530_3_, int p_i50530_4_,
                                     int p_i50530_5_) {
            this(new ItemStack(p_i50530_1_), p_i50530_2_, p_i50530_3_, p_i50530_4_, p_i50530_5_);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50531_1_, int p_i50531_2_, int p_i50531_3_, int p_i50531_4_,
                                     int p_i50531_5_) {
            this(p_i50531_1_, p_i50531_2_, p_i50531_3_, p_i50531_4_, p_i50531_5_, 0.05F);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50532_1_, int p_i50532_2_, int p_i50532_3_, int p_i50532_4_,
                                     int p_i50532_5_, float p_i50532_6_) {
            this.itemStack = p_i50532_1_;
            this.emeraldCost = p_i50532_2_;
            this.numberOfItems = p_i50532_3_;
            this.maxUses = p_i50532_4_;
            this.villagerXp = p_i50532_5_;
            this.priceMultiplier = p_i50532_6_;
        }

        public MerchantOffer getOffer(Entity p_221182_1_, RandomSource p_221182_2_) {
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost),
                    new ItemStack(this.itemStack.getItem(), this.numberOfItems), this.maxUses, this.villagerXp,
                    this.priceMultiplier);
        }
    }
}
