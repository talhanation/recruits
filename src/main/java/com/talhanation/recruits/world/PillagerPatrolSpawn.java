package com.talhanation.recruits.world;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.RecruitShieldmanEntity;
import com.talhanation.recruits.entities.ai.PatrolLeaderTargetAttackers;
import com.talhanation.recruits.entities.ai.villager.FollowCaravanOwner;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

public class PillagerPatrolSpawn {
    private final Random random = new Random();
    private final ServerLevel world;
    private int timer;
    private int delay;
    private double chance;

    public PillagerPatrolSpawn(ServerLevel level) {
        this.world = level;
        this.timer = 18000;//12000 == 10 min
        this.delay = 3000;
        this.chance =  RecruitsModConfig.PillagerPatrolsSpawnChance.get();
    }

    public void tick() {
        //Main.LOGGER.debug("Timer: " + timer);

        if (RecruitsModConfig.ShouldPillagerPatrolsSpawn.get() && --this.timer <= 0) {
            this.timer = 12000;
            this.delay -= 12000;
            if(delay < 0){
                delay = 0;
            }
            if (this.delay <= 0) {
                this.delay = 12000;
                if (this.world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                    double i = this.chance;
                    this.chance = Mth.clamp(this.chance, 5, 100);
                    if (this.random.nextInt(100) <= i && this.attemptSpawnPatrol()) {
                        this.chance = RecruitsModConfig.PillagerPatrolsSpawnChance.get();
                    }
                }
            }
        }

    }

    private boolean attemptSpawnPatrol() {
        Player player = this.world.getRandomPlayer();
        if (player == null) {
            return true;
        } else if (this.random.nextInt(5) != 0) {
            return false;
        } else {
            BlockPos blockpos = new BlockPos(player.position());
            BlockPos blockpos2 = this.func_221244_a(blockpos, 90);
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.distSqr(blockpos) > 200) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());

                int i = random.nextInt(10);
                switch(i) {
                    default -> spawnPillagerPatrol(upPos, blockpos);
                    case 8,9 -> spawnSmallPillagerPatrol(upPos, blockpos);
                    case 1,2 -> spawnMediumPillagerPatrol(upPos, blockpos);
                    case 3,4 -> spawnLargePillagerPatrol(upPos, blockpos);
                }
                return true;
            }
            return false;
        }
    }

    private void spawnPillagerPatrol(BlockPos upPos, BlockPos targetPos) {
        Pillager pillagerLeader = createPillager(upPos, targetPos);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);

        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);

        this.createWitch(upPos, targetPos);

        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
    }

    private Pillager createPillager(BlockPos upPos, BlockPos targetPos){
        Pillager pillager = EntityType.PILLAGER.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);

        world.addFreshEntity(pillager);
        return pillager;
    }

    private Witch createWitch(BlockPos upPos, BlockPos targetPos) {
        Witch pillager = EntityType.WITCH.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);
        world.addFreshEntity(pillager);
        return pillager;
    }

    private Vindicator createVindicator(BlockPos upPos, BlockPos targetPos){
        Vindicator pillager = EntityType.VINDICATOR.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);
        world.addFreshEntity(pillager);
        return pillager;
    }
    private void spawnLargePillagerPatrol(BlockPos upPos, BlockPos targetPos) {
        Pillager pillagerLeader = createPillager(upPos, targetPos);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);


        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);

        this.createWitch(upPos, targetPos);
        this.createWitch(upPos, targetPos);
        this.createWitch(upPos, targetPos);

        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
    }
    private void spawnMediumPillagerPatrol(BlockPos upPos, BlockPos targetPos) {
        Pillager pillagerLeader = createPillager(upPos, targetPos);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);

        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);

        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
        this.createVindicator(upPos, targetPos);
    }

    private void spawnSmallPillagerPatrol(BlockPos upPos, BlockPos targetPos) {
        Pillager pillagerLeader = createPillager(upPos, targetPos);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);


        this.createVindicator(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createPillager(upPos, targetPos);
        this.createWitch(upPos, targetPos);
    }

    @Nullable
    private BlockPos func_221244_a(BlockPos p_221244_1_, int p_221244_2_) {
        BlockPos blockpos = null;

        for(int i = 0; i < 10; ++i) {
            int j = p_221244_1_.getX() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int k = p_221244_1_.getZ() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int l = this.world.getHeight(Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, this.world, blockpos1, EntityType.WANDERING_TRADER)) {
                blockpos = blockpos1;
                break;
            }
        }

        return blockpos;
    }

    private boolean func_226559_a_(BlockPos p_226559_1_) {
        Iterator var2 = BlockPos.betweenClosed(p_226559_1_, p_226559_1_.offset(1, 2, 1)).iterator();

        BlockPos blockpos;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            blockpos = (BlockPos)var2.next();
        } while(this.world.getBlockState(blockpos).getBlockSupportShape(this.world, blockpos).isEmpty() && world.getFluidState(blockpos).isEmpty());

        return false;
    }
}