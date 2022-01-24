package com.talhanation.recruits;

import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AssassinEvents {


    public static void createAssassin(String playerName, int count, World world) {
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        PlayerEntity target = list.getPlayerByName(playerName);
        double d0 = 0;
        double d1 = 0;
        double d2 = 0;
        BlockPos blockPos;

        if (target != null) {
            blockPos = calculateSpawnPos(target);

            while (!isFreeToSpawn(blockPos, target)){
                blockPos = calculateSpawnPos(target);
            }

            if (isFreeToSpawn(blockPos, target)){
                for (int i = 0; i < count; i++) {
                    AssassinEntity assassin = ModEntityTypes.ASSASSIN.get().create(target.level);
                    assassin.setPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
                    assassin.setEquipment();
                    assassin.setTame(false);
                    assassin.setIsInOrder(true);
                    assassin.setDropEquipment();
                    assassin.setPersistenceRequired();
                    assassin.setCanPickUpLoot(true);
                    assassin.setTarget(target);
                    target.level.addFreshEntity(assassin);
                }
            }
        }

    }

    private static BlockPos calculateSpawnPos(PlayerEntity target){
        BlockPos blockPos = new BlockPos(target.getX(), target.getY(), target.getZ());
        double d0 = (double) blockPos.getX() + (target.level.random.nextInt(16) + 32);
        double d1 = (blockPos.getY() + target.level.random.nextInt(10) - 1);
        double d2 = (double) blockPos.getZ() + (target.level.random.nextInt(16) + 32);

        System.out.println("DEBUG: d0 = " + d0);
        System.out.println("DEBUG: d1 = " + d1);
        System.out.println("DEBUG: d2 = " + d2);
        return new BlockPos(d0, d1, d2);
    }

    private static boolean isFreeToSpawn(BlockPos blockPos, PlayerEntity targetPlayer){
        for(int i = 0; i < 10; i++){
            BlockPos pos = new BlockPos(blockPos.getX(), blockPos.getY() + i, blockPos.getZ());
            BlockState blockstate = targetPlayer.level.getBlockState(pos);
            Block block = blockstate.getBlock();

            if(block.equals(Blocks.AIR)){
                targetPlayer.level.setBlock(pos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                return true;
            }
        }
        return false;
    }
}
