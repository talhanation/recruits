package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.pillager.PillagerMeleeAttackGoal;
import com.talhanation.recruits.entities.ai.pillager.PillagerUseShield;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.datafix.fixes.OminousBannerRenameFix;
import net.minecraft.util.datafix.fixes.OminousBannerTileEntityRenameFix;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.raid.Raid;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.village.VillageSiegeEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class PillagerEvents {
    protected final Random random = new Random();

    @SubscribeEvent
    public void attackRecruit(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof PillagerEntity) {
            PillagerEntity pillager = (PillagerEntity) entity;
            if(RecruitsModConfig.PillagerIncreasedCombatRange.get()) {
                pillager.goalSelector.addGoal(2, new FindTargetGoal(pillager, 24.0F));
                pillager.goalSelector.addGoal(2, new RangedCrossbowAttackGoal<>(pillager, 1.0D, 24.0F));
            }
        }

        if (entity instanceof AbstractIllagerEntity) {
            AbstractIllagerEntity illager = (AbstractIllagerEntity) entity;
            illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AbstractRecruitEntity.class, true));
            if (RecruitsModConfig.PillagerAttackMonsters.get()){
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, ZombieEntity.class, true));
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AbstractSkeletonEntity.class, true));
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, SpiderEntity.class, true));
            }
            if (RecruitsModConfig.ShouldPillagersRaidNaturally.get()){
                illager.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(illager, AnimalEntity.class, true));
            }
        }

        if (entity instanceof MonsterEntity) {
            MonsterEntity monster = (MonsterEntity) entity;
            if (!(monster instanceof CreeperEntity))
            monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractRecruitEntity.class, true));
        }

        if (entity instanceof ZombieEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                ZombieEntity monster = (ZombieEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }
        if (entity instanceof AbstractSkeletonEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                AbstractSkeletonEntity monster = (AbstractSkeletonEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }
        if (entity instanceof SpiderEntity){
            if (RecruitsModConfig.MonstersAttackPillagers.get()) {
                SpiderEntity monster = (SpiderEntity) entity;
                monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AbstractIllagerEntity.class, true));
            }
        }

        if (entity instanceof VindicatorEntity && RecruitsModConfig.VindicatorSpawnItems.get()) {
            VindicatorEntity vindicator = (VindicatorEntity) entity;
            vindicator.goalSelector.addGoal(0, new PillagerUseShield(vindicator));
            vindicator.setPersistenceRequired();

            int i = this.random.nextInt(3);
            if (i == 2) vindicator.setItemInHand(Hand.MAIN_HAND, Items.IRON_AXE.getDefaultInstance());
            else {
                vindicator.setItemInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                vindicator.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
            }
        }

        if (entity instanceof PillagerEntity && RecruitsModConfig.PillagerSpawnItems.get()) {
            PillagerEntity pillager = (PillagerEntity) entity;
            pillager.goalSelector.addGoal(0, new PillagerMeleeAttackGoal(pillager, 1.15D, true));
            pillager.goalSelector.addGoal(0, new PillagerUseShield(pillager));
            pillager.setPersistenceRequired();

            int i = this.random.nextInt(6);
            switch (i){
                case 1:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.CROSSBOW.getDefaultInstance());
                    break;
                case 2:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.CROSSBOW.getDefaultInstance());
                    break;
                case 3:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.CROSSBOW.getDefaultInstance());
                    break;
                case 4:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.IRON_AXE.getDefaultInstance());
                    pillager.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
                    break;
                case 5:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                    pillager.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
                    break;
                case 0:
                    pillager.setItemInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                    pillager.setItemInHand(Hand.OFF_HAND, Items.SHIELD.getDefaultInstance());
                    break;
            }
        }
        /*
        if (entity instanceof VindicatorEntity) {
            VindicatorEntity vindicator = (VindicatorEntity) entity;

            List<PillagerEntity> list1 = entity.level.getEntitiesOfClass(PillagerEntity.class, vindicator.getBoundingBox().inflate(64));
            int max = 2 + random.nextInt(10);
            if (list1.size() > 1) {
                vindicator.remove();
            }
            else {
                for (int k = 0; k < max; k++) createPillager(vindicator);
            }
        }
        */
    }

    @SubscribeEvent
    public void onBiomeLoadingPillager(BiomeLoadingEvent event) {
        Biome.Category category = event.getCategory();
        if (RecruitsModConfig.PillagerSpawn.get()) {
            if (category != Biome.Category.NETHER && category != Biome.Category.THEEND && category != Biome.Category.NONE && category != Biome.Category.OCEAN && category != Biome.Category.RIVER) {
                event.getSpawns().getSpawner(EntityClassification.MONSTER).add(new MobSpawnInfo.Spawners(EntityType.PILLAGER, 1, 1, 2));
            }
        }
    }

    private void createPillager(LivingEntity entity){
        PillagerEntity pillager = EntityType.PILLAGER.create(entity.level);
        pillager.copyPosition(entity);
        entity.level.addFreshEntity(pillager);
    }

    @SubscribeEvent
    public void raidStartOnBurningOminus(EntityEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity) event.getEntity();
            ItemStack itemStack = itemEntity.getItem();

            World level = entity.level;
            if (itemStack.getItem().equals(Items.WHITE_BANNER)) {

                if (entity.isOnFire() && ItemStack.matches(itemStack, Raid.getLeaderBannerInstance())) {
                    PlayerEntity player = level.getNearestPlayer(entity, 16D);
                    if (player != null) {
                        EffectInstance effectinstance1 = player.getEffect(Effects.BAD_OMEN);
                        int i = 1;
                        if (effectinstance1 != null) {
                            i += effectinstance1.getAmplifier();
                            player.removeEffectNoUpdate(Effects.BAD_OMEN);
                        } else {
                            --i;
                        }
                        i = MathHelper.clamp(i, 0, 4);
                        EffectInstance effectinstance = new EffectInstance(Effects.BAD_OMEN, 120000, i, false, false, true);
                        if (!player.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                            player.addEffect(effectinstance);
                        }
                        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 0.5F, Explosion.Mode.BREAK);
                        entity.remove();
                    }
                }
            }
        }
    }
}

class FindTargetGoal extends Goal {
    private final AbstractRaiderEntity mob;
    private final float hostileRadiusSqr;
    private final Random random = new Random();
    public final EntityPredicate shoutTargeting = (new EntityPredicate()).range(8.0D).allowNonAttackable().allowInvulnerable().allowSameTeam().allowUnseeable().ignoreInvisibilityTesting();

    public FindTargetGoal(AbstractIllagerEntity p_i50573_2_, float p_i50573_3_) {
        this.mob = p_i50573_2_;
        this.hostileRadiusSqr = p_i50573_3_ * p_i50573_3_;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mob.getLastHurtByMob();
        return this.mob.getCurrentRaid() == null && this.mob.getTarget() != null && !this.mob.isAggressive() && (livingentity == null || livingentity.getType() != EntityType.PLAYER);
    }

    public void start() {
        super.start();
        this.mob.getNavigation().stop();

        for(AbstractRaiderEntity abstractraiderentity : this.mob.level.getNearbyEntities(AbstractRaiderEntity.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0D, 8.0D, 8.0D))) {
            abstractraiderentity.setTarget(this.mob.getTarget());
        }

    }

    public void stop() {
        super.stop();
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            for(AbstractRaiderEntity abstractraiderentity : this.mob.level.getNearbyEntities(AbstractRaiderEntity.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0D, 8.0D, 8.0D))) {
                abstractraiderentity.setTarget(livingentity);
                abstractraiderentity.setAggressive(true);
            }

            this.mob.setAggressive(true);
        }

    }

    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            if (this.mob.distanceToSqr(livingentity) > (double)this.hostileRadiusSqr) {
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                if (this.random.nextInt(50) == 0) {
                    this.mob.playAmbientSound();
                }
            } else {
                this.mob.setAggressive(true);
            }

            super.tick();
        }
    }
}