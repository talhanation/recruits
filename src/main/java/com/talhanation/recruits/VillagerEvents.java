package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;


public class VillagerEvents {
    protected final Random random = new Random();

    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (profession == Main.RECRUIT) {
                createRecruit(villager);
            }

            if (profession == Main.BOWMAN){
                createBowman(villager);
            }

            if (profession == Main.NOMAD){
                createNomad(villager);
            }

            if (profession == Main.RECRUIT_SHIELDMAN){
                createRecruitShieldman(villager);
            }
        }
        if (entity instanceof IronGolemEntity) {
            IronGolemEntity ironGolemEntity = (IronGolemEntity) entity;

            if (!ironGolemEntity.isPlayerCreated() && RecruitsModConfig.OverrideIronGolemSpawn.get()){
                List<AbstractRecruitEntity> list1 = entity.level.getEntitiesOfClass(AbstractRecruitEntity.class, ironGolemEntity.getBoundingBox().inflate(32));
                if (list1.size() > 1) {
                    ironGolemEntity.remove();
                    //System.out.println("golem was removed");
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

    }
    private static void createRecruit(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        recruit.copyPosition(villager);

        recruit.initSpawn();

        villager.remove();
        villager.level.addFreshEntity(recruit);
    }

    private static void createRecruitShieldman(LivingEntity entity){
        RecruitShieldmanEntity recruitShieldman = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        recruitShieldman.copyPosition(villager);

        recruitShieldman.initSpawn();

        villager.remove();
        villager.level.addFreshEntity(recruitShieldman);
    }

    private static void createBowman(LivingEntity entity){
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        bowman.copyPosition(villager);

        bowman.initSpawn();

        villager.remove();
        villager.level.addFreshEntity(bowman);
    }

    private static void createCrossBowman(LivingEntity entity){
        CrossBowmanEntity crossBowman = ModEntityTypes.CROSSBOWMAN.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        crossBowman.copyPosition(villager);
        crossBowman.setEquipment();
        crossBowman.setDropEquipment();
        crossBowman.setRandomSpawnBonus();
        crossBowman.setPersistenceRequired();
        crossBowman.setCanPickUpLoot(true);
        //crossBowman.reassessWeaponGoal();
        crossBowman.setGroup(2);
        villager.remove();
        villager.level.addFreshEntity(crossBowman);
    }

    private static void createNomad(LivingEntity entity){
        NomadEntity nomad = ModEntityTypes.NOMAD.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        nomad.copyPosition(villager);

        nomad.initSpawn();

        villager.remove();

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
            VillagerTrades.ITrade block_trade = new Trade(Items.EMERALD, 10, ModBlocks.RECRUIT_SHIELD_BLOCK.get(), 1, 4, 10);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            VillagerTrades.ITrade block_trade = new Trade(Items.EMERALD, 3, ModBlocks.RECRUIT_BLOCK.get(), 1, 4, 10);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FLETCHER) {
            VillagerTrades.ITrade block_trade = new Trade(Items.EMERALD, 4, ModBlocks.BOWMAN_BLOCK.get(), 1, 4, 10);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
    }

    static class EmeraldForItemsTrade extends Trade {
        public EmeraldForItemsTrade(IItemProvider buyingItem, int buyingAmount, int maxUses, int givenExp) {
            super(buyingItem, buyingAmount, Items.EMERALD, 1, maxUses, givenExp);
        }
    }

    static class MultiTrade implements VillagerTrades.ITrade {
        private final VillagerTrades.ITrade[] trades;

        public MultiTrade(VillagerTrades.ITrade... trades) {
            this.trades = trades;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity entity, Random random) {
            return trades[random.nextInt(trades.length)].getOffer(entity, random);
        }
    }

    static class Trade implements VillagerTrades.ITrade {
        private final Item buyingItem;
        private final Item sellingItem;
        private final int buyingAmount;
        private final int sellingAmount;
        private final int maxUses;
        private final int givenExp;
        private final float priceMultiplier;

        public Trade(IItemProvider buyingItem, int buyingAmount, IItemProvider sellingItem, int sellingAmount, int maxUses, int givenExp) {
            this.buyingItem = buyingItem.asItem();
            this.buyingAmount = buyingAmount;
            this.sellingItem = sellingItem.asItem();
            this.sellingAmount = sellingAmount;
            this.maxUses = maxUses;
            this.givenExp = givenExp;
            this.priceMultiplier = 0.05F;
        }

        public MerchantOffer getOffer(Entity entity, Random random) {
            return new MerchantOffer(new ItemStack(this.buyingItem, this.buyingAmount), new ItemStack(sellingItem, sellingAmount), maxUses, givenExp, priceMultiplier);
        }
    }

    static class ItemsForEmeraldsTrade implements VillagerTrades.ITrade {
        private final ItemStack itemStack;
        private final int emeraldCost;
        private final int numberOfItems;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public ItemsForEmeraldsTrade(Block p_i50528_1_, int p_i50528_2_, int p_i50528_3_, int p_i50528_4_, int p_i50528_5_) {
            this(new ItemStack(p_i50528_1_), p_i50528_2_, p_i50528_3_, p_i50528_4_, p_i50528_5_);
        }

        public ItemsForEmeraldsTrade(Item p_i50529_1_, int p_i50529_2_, int p_i50529_3_, int p_i50529_4_) {
            this(new ItemStack(p_i50529_1_), p_i50529_2_, p_i50529_3_, 12, p_i50529_4_);
        }

        public ItemsForEmeraldsTrade(Item p_i50530_1_, int p_i50530_2_, int p_i50530_3_, int p_i50530_4_, int p_i50530_5_) {
            this(new ItemStack(p_i50530_1_), p_i50530_2_, p_i50530_3_, p_i50530_4_, p_i50530_5_);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50531_1_, int p_i50531_2_, int p_i50531_3_, int p_i50531_4_, int p_i50531_5_) {
            this(p_i50531_1_, p_i50531_2_, p_i50531_3_, p_i50531_4_, p_i50531_5_, 0.05F);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50532_1_, int p_i50532_2_, int p_i50532_3_, int p_i50532_4_, int p_i50532_5_, float p_i50532_6_) {
            this.itemStack = p_i50532_1_;
            this.emeraldCost = p_i50532_2_;
            this.numberOfItems = p_i50532_3_;
            this.maxUses = p_i50532_4_;
            this.villagerXp = p_i50532_5_;
            this.priceMultiplier = p_i50532_6_;
        }

        public MerchantOffer getOffer(Entity p_221182_1_, Random p_221182_2_) {
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost), new ItemStack(this.itemStack.getItem(), this.numberOfItems), this.maxUses, this.villagerXp, this.priceMultiplier);
        }
    }


    private static void createRecruitIronGolem(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.level);
        IronGolemEntity villager = (IronGolemEntity) entity;
        recruit.copyPosition(villager);
        recruit.setEquipment();
        recruit.setDropEquipment();
        recruit.setRandomSpawnBonus();
        recruit.setPersistenceRequired();
        recruit.setCanPickUpLoot(true);
        recruit.setGroup(1);
        recruit.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove();
        villager.level.addFreshEntity(recruit);
    }

    private void createRecruitShieldmanIronGolem(LivingEntity entity){
        RecruitShieldmanEntity recruitShieldman = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(entity.level);
        IronGolemEntity villager = (IronGolemEntity) entity;
        recruitShieldman.copyPosition(villager);
        recruitShieldman.setEquipment();
        recruitShieldman.setDropEquipment();
        recruitShieldman.setRandomSpawnBonus();
        recruitShieldman.setPersistenceRequired();
        recruitShieldman.setCanPickUpLoot(true);
        recruitShieldman.setGroup(1);
        recruitShieldman.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove();
        villager.level.addFreshEntity(recruitShieldman);
    }

    private static void createBowmanIronGolem(LivingEntity entity){
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(entity.level);
        IronGolemEntity villager = (IronGolemEntity) entity;
        bowman.copyPosition(villager);
        bowman.setEquipment();
        bowman.setDropEquipment();
        bowman.setRandomSpawnBonus();
        bowman.setPersistenceRequired();
        bowman.setCanPickUpLoot(true);
        bowman.reassessWeaponGoal();
        bowman.setGroup(2);
        bowman.getInventory().setItem(5, Items.BREAD.getDefaultInstance());
        villager.remove();
        villager.level.addFreshEntity(bowman);
    }

}
