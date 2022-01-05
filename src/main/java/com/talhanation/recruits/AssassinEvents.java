package com.talhanation.recruits;

import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

public class AssassinEvents {


    @SubscribeEvent
    public void onAssassinateLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity living = event.getEntityLiving();

        if(living instanceof PlayerEntity){
            PlayerEntity player = (PlayerEntity) living;
            //if (isPlayerHunted(player))

        }

    }

    public static void createAssassin(String playerName, int count, World world) {
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        PlayerEntity target = list.getPlayerByName(playerName);
        if (target != null) {

            BlockPos blockPos = new BlockPos(target.getX(), target.getY(), target.getZ());

            double d0 = (double) blockPos.getX() + (world.random.nextInt(16) + 32);
            double d1 = (blockPos.getY() + world.random.nextInt(3));
            double d2 = (double) blockPos.getZ() + (world.random.nextInt(16) + 32);

            double d0min = (double) blockPos.getX() + (world.random.nextInt(16) + 16);
            double d1min = (blockPos.getY() + world.random.nextInt(3));
            double d2min = (double) blockPos.getZ() + (world.random.nextInt(16) + 16);

            if (world.noCollision(new AxisAlignedBB(d0min, d1min, d2min, d0, d1, d2))) {
                for (int i = 0; i < count; i++) {
                    AssassinEntity assassin = ModEntityTypes.ASSASSIN.get().create(target.level);
                    assassin.setPos(d0, d1, d2);
                    assassin.setEquipment();
                    assassin.setTame(false);
                    assassin.setIsInOrder(true);
                    assassin.setDropEquipment();
                    //assassin.setRandomSpawnBonus();
                    assassin.setPersistenceRequired();
                    assassin.setCanPickUpLoot(true);
                    assassin.setTarget(target);
                    target.level.addFreshEntity(assassin);
                }
            }
        }
    }
}
