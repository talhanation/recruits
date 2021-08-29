package com.talhanation.recruits.client.events;


import com.google.common.collect.ImmutableList;
import com.talhanation.recruits.Main;

import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Random;

public class VillagerEvents {

    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        Entity entity = event.getEntityLiving();
        if (entity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (clientPlayerEntity == null) {
                return;
            }

            if (profession == Main.RECRUIT) {
                createRecruit(villager);
            }

            if (profession == Main.BOWMAN){
                createBowman(villager);
            }
        }

    }
    private static void createRecruit(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        recruit.copyPosition(villager);
        recruit.setEquipment();
        recruit.setDropEquipment();
        recruit.setRandomSpawnBonus();
        recruit.setPersistenceRequired();
        recruit.setCanPickUpLoot(true);
        recruit.setGroup(1);
        villager.remove();
        villager.level.addFreshEntity(recruit);

    }

    private static void createBowman(LivingEntity entity){
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(entity.level);
        VillagerEntity villager = (VillagerEntity) entity;
        bowman.copyPosition(villager);
        bowman.setEquipment();
        bowman.setDropEquipment();
        bowman.setRandomSpawnBonus();
        bowman.setPersistenceRequired();
        bowman.setCanPickUpLoot(true);
        bowman.reassessWeaponGoal();
        bowman.setGroup(2);
        villager.remove();
        villager.level.addFreshEntity(bowman);
    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.WEAPONSMITH || event.getType() == VillagerProfession.ARMORER) {
            event.getTrades().put(2, ImmutableList.of(
                    new Trade(Items.EMERALD, 20, ModBlocks.RECRUIT_BLOCK.get(), 4, 16, 2)
            ));
            event.getTrades().put(2, ImmutableList.of(
                    new Trade(Items.EMERALD, 24, ModBlocks.BOWMAN_BLOCK.get(), 4, 16, 2)
            ));
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

}
