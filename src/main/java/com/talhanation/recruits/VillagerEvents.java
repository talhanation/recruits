package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModProfessions;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


import java.util.*;

public class VillagerEvents {
    protected final Random random = new Random();

    private static void spawnRecruits(ServerLevel level, BlockPos villagePos) {

        Random random = new Random();
        BlockPos spawnPos = RecruitsPatrolSpawn.func_221244_a(villagePos, 20, random, level);

        if (spawnPos != null && RecruitsPatrolSpawn.func_226559_a_(spawnPos, level) && spawnPos.distSqr(villagePos) > 10) {
            BlockPos upPos = new BlockPos(spawnPos.getX(), spawnPos.getY() + 2, spawnPos.getZ());

            int i = random.nextInt(4);
            switch (i) {
                default -> spawnSmallGuardRecruits(upPos, level, random);
                case 1 -> spawnMediumGuardRecruits(upPos, level, random);
                case 2 -> spawnLargeGuardRecruits(upPos, level, random);
            }
        }
    }

    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingTickEvent event) {
        HashMap<VillagerProfession, EntityType<? extends  AbstractRecruitEntity>> entitiesByProfession = new HashMap<>(){{
            put(ModProfessions.RECRUIT.get(), ModEntityTypes.RECRUIT.get());
            put(ModProfessions.BOWMAN.get(), ModEntityTypes.BOWMAN.get());
            put(ModProfessions.SHIELDMAN.get(), ModEntityTypes.RECRUIT_SHIELDMAN.get());
            put(ModProfessions.HORSEMAN.get(), ModEntityTypes.HORSEMAN.get());
            put(ModProfessions.NOMAD.get(), ModEntityTypes.NOMAD.get());
            put(ModProfessions.CROSSBOWMAN.get(), ModEntityTypes.CROSSBOWMAN.get());
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

            if (!ironGolemEntity.isPlayerCreated() && RecruitsServerConfig.OverrideIronGolemSpawn.get()){
                List<AbstractRecruitEntity> list1 = entity.getCommandSenderWorld().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        ironGolemEntity.getBoundingBox().inflate(32)
                );
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
    }
    private static void createRecruit(Villager villager, EntityType<? extends AbstractRecruitEntity> recruitType){
        AbstractRecruitEntity abstractRecruit = recruitType.create(villager.getCommandSenderWorld());
        if (abstractRecruit != null) {
            abstractRecruit.copyPosition(villager);

            abstractRecruit.initSpawn();

            for(ItemStack itemStack : villager.getInventory().items){
                abstractRecruit.getInventory().addItem(itemStack);
            }

            Component name = villager.getCustomName();
            if(name  != null) abstractRecruit.setCustomName(name);

            villager.getCommandSenderWorld().addFreshEntity(abstractRecruit);
            if(RecruitsServerConfig.RecruitTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
            villager.discard();
        }
    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {

        if (event.getType() == VillagerProfession.ARMORER) {
            Trade block_trade = new Trade(Items.EMERALD, 15, ModBlocks.RECRUIT_SHIELD_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            Trade block_trade = new Trade(Items.EMERALD, 8, ModBlocks.RECRUIT_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FLETCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 10, ModBlocks.BOWMAN_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FLETCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 20, ModBlocks.CROSSBOWMAN_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.CARTOGRAPHER) {
            Trade block_trade = new Trade(Items.EMERALD, 30, ModBlocks.NOMAD_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 30, ModBlocks.HORSEMAN_BLOCK.get(), 1, 2, 20);
            List<VillagerTrades.ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
    }

    private static void createRecruitIronGolem(LivingEntity entity){
        RecruitEntity recruit = ModEntityTypes.RECRUIT.get().create(entity.getCommandSenderWorld());
        IronGolem villager = (IronGolem) entity;
        recruit.copyPosition(villager);

        recruit.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        recruit.getInventory().setItem(8, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.getCommandSenderWorld().addFreshEntity(recruit);
    }

    private void createRecruitShieldmanIronGolem(LivingEntity entity){
        RecruitShieldmanEntity recruitShieldman = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(entity.getCommandSenderWorld());
        IronGolem villager = (IronGolem) entity;
        recruitShieldman.copyPosition(villager);

        recruitShieldman.initSpawn();

        recruitShieldman.getInventory().setItem(8, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.getCommandSenderWorld().addFreshEntity(recruitShieldman);
    }

    private static void createBowmanIronGolem(LivingEntity entity){
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(entity.getCommandSenderWorld());
        IronGolem villager = (IronGolem) entity;
        bowman.copyPosition(villager);

        bowman.initSpawn();

        bowman.getInventory().setItem(8, Items.BREAD.getDefaultInstance());
        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.getCommandSenderWorld().addFreshEntity(bowman);
    }

    private static void spawnSmallGuardRecruits(BlockPos upPos, ServerLevel world, Random random) {
        RecruitEntity guardLeader = createGuardLeader(upPos, "Village Guard Leader", world, random);

        createGuardRecruit(upPos, guardLeader, "Village Guard", world, random);
        createGuardRecruit(upPos, guardLeader, "Village Guard", world, random);
        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardBowman(upPos, guardLeader, world, random);
    }

    private static void spawnMediumGuardRecruits(BlockPos upPos, ServerLevel world, Random random) {
        RecruitEntity guardLeader = createGuardLeader(upPos, "Village Guard Leader", world, random);

        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardBowman(upPos, guardLeader, world, random);
        createGuardBowman(upPos, guardLeader, world, random);
        createPatrolCrossbowman(upPos, guardLeader, world, random);
        createPatrolCrossbowman(upPos, guardLeader, world, random);

        createGuardHorseman(upPos, guardLeader, "Village Guard", world, random);
        createGuardNomad(upPos, guardLeader, "Village Guard", world, random);
    }

    private static void spawnLargeGuardRecruits(BlockPos upPos, ServerLevel world, Random random) {
        RecruitEntity guardLeader = createGuardLeader(upPos, "Village Guard Leader", world, random);

        createGuardRecruit(upPos, guardLeader, "Village Guard", world, random);
        createGuardRecruit(upPos, guardLeader, "Village Guard", world, random);
        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardShieldman(upPos, guardLeader, "Village Guard", world, random);
        createGuardBowman(upPos, guardLeader, world, random);
        createGuardBowman(upPos, guardLeader, world, random);
        createGuardBowman(upPos, guardLeader, world, random);
        createPatrolCrossbowman(upPos, guardLeader, world, random);
        createPatrolCrossbowman(upPos, guardLeader, world, random);
        createPatrolCrossbowman(upPos, guardLeader, world, random);

        createGuardHorseman(upPos, guardLeader, "Village Guard", world, random);
        createGuardNomad(upPos, guardLeader, "Village Guard", world, random);
        createGuardHorseman(upPos, guardLeader, "Village Guard", world, random);
        createGuardNomad(upPos, guardLeader, "Village Guard", world, random);

    }

    public static RecruitEntity createGuardLeader(BlockPos upPos, String name, ServerLevel world, Random random){
        RecruitEntity patrolLeader = ModEntityTypes.RECRUIT.get().create(world);
        patrolLeader.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        patrolLeader.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        setGuardLeaderEquipment(patrolLeader);
        patrolLeader.setPersistenceRequired();

        patrolLeader.setXpLevel(1 + random.nextInt(2));
        patrolLeader.addLevelBuffsForLevel(patrolLeader.getXpLevel());
        patrolLeader.setHunger(100);
        patrolLeader.setMoral(100);
        patrolLeader.setCost(45);
        patrolLeader.setXp(random.nextInt(200));
        patrolLeader.setCustomName(Component.literal(name));

        patrolLeader.setProtectUUID(Optional.of(patrolLeader.getUUID()));

        world.addFreshEntity(patrolLeader);
        return patrolLeader;
    }


    public static void setGuardLeaderEquipment(RecruitEntity recruit) {
        Random random = new Random();
        recruit.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        recruit.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        recruit.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        recruit.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        int j = random.nextInt(10);
        ItemStack item = new ItemStack(Items.EMERALD);
        item.setCount(8 + j);
        recruit.inventory.setItem(8, item);

        recruit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        recruit.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        int k = random.nextInt(8);
        ItemStack food;
        switch(k) {
            default -> food = new ItemStack(Items.BREAD);
            case 1 -> food = new ItemStack(Items.COOKED_BEEF);
            case 2 -> food = new ItemStack(Items.COOKED_CHICKEN);
            case 3 -> food = new ItemStack(Items.COOKED_MUTTON);
        }
        food.setCount(32 + k);
        recruit.inventory.setItem(7, food);
    }

    private static void createGuardRecruit(BlockPos upPos, RecruitEntity patrolLeader, String name, ServerLevel world, Random random) {
        RecruitEntity recruitEntity = ModEntityTypes.RECRUIT.get().create(world);
        recruitEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        recruitEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        recruitEntity.setPersistenceRequired();
        recruitEntity.setEquipment();
        recruitEntity.setXpLevel(1 + random.nextInt(2));
        recruitEntity.addLevelBuffsForLevel(recruitEntity.getXpLevel());
        recruitEntity.setHunger(80);
        recruitEntity.setMoral(65);
        recruitEntity.setCost(10);
        recruitEntity.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        recruitEntity.setShouldProtect(true);
        recruitEntity.setXp(random.nextInt(80));

        recruitEntity.setCustomName(Component.literal(name));


        world.addFreshEntity(recruitEntity);
        //Main.LOGGER.debug("SpawnPatrol: patrol spawned");
    }

    private static void createGuardBowman(BlockPos upPos, RecruitEntity patrolLeader, ServerLevel world, Random random) {
        BowmanEntity bowman = ModEntityTypes.BOWMAN.get().create(world);
        bowman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        bowman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        bowman.setEquipment();
        bowman.setPersistenceRequired();

        bowman.setXpLevel(1 + random.nextInt(2));
        bowman.addLevelBuffsForLevel(bowman.getXpLevel());
        bowman.setHunger(80);
        bowman.setMoral(65);
        bowman.setCost(16);
        bowman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        bowman.setShouldProtect(true);
        bowman.setXp(random.nextInt(120));

        bowman.setCustomName(Component.literal("Village Guard"));

        world.addFreshEntity(bowman);
    }

    private static void createGuardShieldman(BlockPos upPos, RecruitEntity patrolLeader, String name, ServerLevel world, Random random) {
        RecruitShieldmanEntity shieldmanEntity = ModEntityTypes.RECRUIT_SHIELDMAN.get().create(world);
        shieldmanEntity.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        shieldmanEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        shieldmanEntity.setEquipment();
        shieldmanEntity.setPersistenceRequired();

        shieldmanEntity.setXpLevel(1 + random.nextInt(2));
        shieldmanEntity.addLevelBuffsForLevel(shieldmanEntity.getXpLevel());
        shieldmanEntity.setHunger(80);
        shieldmanEntity.setMoral(65);
        shieldmanEntity.setCost(24);
        shieldmanEntity.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        shieldmanEntity.setShouldProtect(true);
        shieldmanEntity.setXp(random.nextInt(120));

        shieldmanEntity.setCustomName(Component.literal(name));

        world.addFreshEntity(shieldmanEntity);
    }

    private static void createGuardHorseman(BlockPos upPos, RecruitEntity patrolLeader, String name, ServerLevel world, Random random) {
        HorsemanEntity horseman = ModEntityTypes.HORSEMAN.get().create(world);
        horseman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        horseman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        horseman.setEquipment();
        horseman.setPersistenceRequired();

        horseman.setXpLevel(1 + random.nextInt(2));
        horseman.addLevelBuffsForLevel(horseman.getXpLevel());
        horseman.setHunger(80);
        horseman.setMoral(75);
        horseman.setCost(20);
        horseman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        horseman.setShouldProtect(true);
        horseman.setXp(random.nextInt(120));

        horseman.setCustomName(Component.literal(name));

        world.addFreshEntity(horseman);
    }

    private static void createGuardNomad(BlockPos upPos, RecruitEntity patrolLeader, String name, ServerLevel world, Random random) {
        NomadEntity nomad = ModEntityTypes.NOMAD.get().create(world);
        nomad.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        nomad.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        nomad.setEquipment();
        nomad.setPersistenceRequired();

        nomad.setXpLevel(1 + random.nextInt(2));
        nomad.addLevelBuffsForLevel(nomad.getXpLevel());
        nomad.setHunger(80);
        nomad.setMoral(75);
        nomad.setCost(25);
        nomad.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        nomad.setShouldProtect(true);
        nomad.setXp(random.nextInt(120));

        nomad.setCustomName(Component.literal(name));

        world.addFreshEntity(nomad);
    }

    private static void createPatrolCrossbowman(BlockPos upPos, RecruitEntity patrolLeader, ServerLevel world, Random random) {
        CrossBowmanEntity crossBowman = ModEntityTypes.CROSSBOWMAN.get().create(world);
        crossBowman.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        crossBowman.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        crossBowman.setEquipment();
        crossBowman.setPersistenceRequired();

        crossBowman.setXpLevel(1 + random.nextInt(3));
        crossBowman.addLevelBuffsForLevel(crossBowman.getXpLevel());
        crossBowman.setHunger(80);
        crossBowman.setMoral(65);
        crossBowman.setCost(32);
        crossBowman.setProtectUUID(Optional.of(patrolLeader.getUUID()));
        crossBowman.setShouldProtect(true);
        crossBowman.setXp(random.nextInt(120));

        crossBowman.setCustomName(Component.literal("Village Guard"));

        world.addFreshEntity(crossBowman);
    }

    static class Trade implements VillagerTrades.ItemListing {
        private final Item buyingItem;
        private final Item sellingItem;
        private final int buyingAmount;
        private final int sellingAmount;
        private final int maxUses;
        private final int givenExp;
        private final float priceMultiplier;

        public Trade(ItemLike buyingItem, int buyingAmount, ItemLike sellingItem, int sellingAmount, int maxUses, int givenExp) {
            this.buyingItem = buyingItem.asItem();
            this.buyingAmount = buyingAmount;
            this.sellingItem = sellingItem.asItem();
            this.sellingAmount = sellingAmount;
            this.maxUses = maxUses;
            this.givenExp = givenExp;
            this.priceMultiplier = 0.05F;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            return new MerchantOffer(new ItemStack(this.buyingItem, this.buyingAmount), new ItemStack(sellingItem, sellingAmount), maxUses, givenExp, priceMultiplier);
        }
    }
}
