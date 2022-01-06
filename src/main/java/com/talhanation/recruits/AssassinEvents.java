package com.talhanation.recruits;

import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.init.ModEntityTypes;
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
        double d0min =0;
        double d1min =0;
        double d2min =0;
        if (target != null) {
            do {
                BlockPos blockPos = new BlockPos(target.getX(), target.getY(), target.getZ());
                d0 = (double) blockPos.getX() + (world.random.nextInt(16) + 32);
                d1 = (blockPos.getY() + world.random.nextInt(10) - 1);
                d2 = (double) blockPos.getZ() + (world.random.nextInt(16) + 32);
                d0min = (double) blockPos.getX() + (world.random.nextInt(16) + 16);
                d1min = (blockPos.getY() + world.random.nextInt(3));
                d2min = (double) blockPos.getZ() + (world.random.nextInt(16) + 16);

                System.out.println("DEBUG: d0 = "+ d0);
            }
            while (world.noCollision(new AxisAlignedBB(d0min, d1min, d2min, d0, d1, d2)));{
                for (int i = 0; i < count; i++) {
                    AssassinEntity assassin = ModEntityTypes.ASSASSIN.get().create(target.level);
                    assassin.setPos(d0, d1, d2);
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
}
