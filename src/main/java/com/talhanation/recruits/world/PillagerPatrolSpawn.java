package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.NaturalSpawner;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Random;

public class PillagerPatrolSpawn {
    private static final Random random = new Random();
    private final ServerLevel world;
    private int timer;
    private final double chance;


    public PillagerPatrolSpawn(ServerLevel level) {
        this.world = level;
        this.timer = getSpawnInterval();
        this.chance = RecruitsServerConfig.PillagerPatrolsSpawnChance.get();
    }

    public void tick() {
        if(timer > 0) --this.timer;

        if(this.timer <= 0){
            if (this.world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                double rnd = random.nextInt(100);

                if (rnd <= this.chance && this.attemptSpawnPatrol()){}//To avoid multiple method call
            }
            this.timer = getSpawnInterval();
        }
    }

    private boolean attemptSpawnPatrol() {
        Player player = this.world.getRandomPlayer();
        if (player == null) {
            return true;
        }
        else{
            if(!player.getCommandSenderWorld().dimensionType().hasRaids()){
                player = this.world.getRandomPlayer();
            }
            BlockPos blockpos = player.getOnPos();
            BlockPos blockpos2 = this.func_221244_a(blockpos, 90);
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.distSqr(blockpos) > 200) {
                BlockPos upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());

                int i = random.nextInt(10);
                switch (i) {
                    default -> spawnPillagerPatrol(upPos, blockpos, world);
                    case 8, 9 -> spawnSmallPillagerPatrol(upPos, blockpos, world);
                    case 1, 2 -> spawnMediumPillagerPatrol(upPos, blockpos, world);
                    case 3, 4 -> spawnLargePillagerPatrol(upPos, blockpos, world);
                }
                this.world.playSound(null, upPos.above(2), SoundEvents.RAID_HORN.get(), SoundSource.BLOCKS, 15F, 2F);

                Main.LOGGER.info("New Pillager Patrol Spawned at "+ upPos);
                player.sendSystemMessage(Component.literal("A Pillager Patrol Spawned next to you!").withStyle(ChatFormatting.GRAY));
                return true;
            }
        }
        return false;
    }

    private int getSpawnInterval(){
        //1200 == 1 min
        int minutes = RecruitsServerConfig.PillagerPatrolSpawnInterval.get(); //minutes

        return 1200 * minutes;
    }

    public static void spawnPillagerPatrol(BlockPos upPos, BlockPos targetPos, ServerLevel world) {
        Pillager pillagerLeader = createPillager(upPos, targetPos, world);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);

        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);

        createWitch(upPos, targetPos, world);

        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);

    }

    public static Pillager createPillager(BlockPos upPos, BlockPos targetPos, ServerLevel world){
        Pillager pillager = EntityType.PILLAGER.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);

        world.addFreshEntity(pillager);
        return pillager;
    }

    public static Witch createWitch(BlockPos upPos, BlockPos targetPos, ServerLevel world) {
        Witch pillager = EntityType.WITCH.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);
        world.addFreshEntity(pillager);
        return pillager;
    }

    public static Vindicator createVindicator(BlockPos upPos, BlockPos targetPos, ServerLevel world){
        Vindicator pillager = EntityType.VINDICATOR.create(world);
        pillager.moveTo(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
        pillager.finalizeSpawn(world, world.getCurrentDifficultyAt(upPos), MobSpawnType.PATROL, null, null);
        pillager.setPersistenceRequired();
        pillager.setPatrolTarget(targetPos);
        world.addFreshEntity(pillager);
        return pillager;
    }
    public static void spawnLargePillagerPatrol(BlockPos upPos, BlockPos targetPos, ServerLevel world) {
        Pillager pillagerLeader = createPillager(upPos, targetPos, world);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);


        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);

        createWitch(upPos, targetPos, world);
        createWitch(upPos, targetPos, world);
        createWitch(upPos, targetPos, world);

        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);

    }
    public static void spawnMediumPillagerPatrol(BlockPos upPos, BlockPos targetPos, ServerLevel world) {
        Pillager pillagerLeader = createPillager(upPos, targetPos, world);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);

        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);

        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);
        createVindicator(upPos, targetPos, world);

    }

    public static void spawnSmallPillagerPatrol(BlockPos upPos, BlockPos targetPos, ServerLevel world) {
        Pillager pillagerLeader = createPillager(upPos, targetPos, world);
        pillagerLeader.setAggressive(true);
        pillagerLeader.setCustomName(Component.literal("Pillager Leader"));
        pillagerLeader.setPatrolLeader(true);
        pillagerLeader.setCanJoinRaid(true);
        pillagerLeader.setCanPickUpLoot(true);


        createVindicator(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createPillager(upPos, targetPos, world);
        createWitch(upPos, targetPos, world);

    }

    @Nullable
    private BlockPos func_221244_a(BlockPos p_221244_1_, int p_221244_2_) {
        BlockPos blockpos = null;

        for(int i = 0; i < 10; ++i) {
            int j = p_221244_1_.getX() + random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int k = p_221244_1_.getZ() + random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int l = this.world.getHeight(Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (!this.world.getLevel().isCloseToVillage(blockpos1, 2) && NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, this.world, blockpos1, EntityType.WANDERING_TRADER)) {
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