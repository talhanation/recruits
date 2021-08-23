package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.server.management.DemoPlayerInteractionManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent.PotentialSpawns;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class PlayerEvents {

    @SubscribeEvent
    public void onVillagerCloseToBlock(PlayerInteractEvent.EntityInteract event) {
        Minecraft minecraft = Minecraft.getInstance();

        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        Entity entity = event.getTarget();
        if (entity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (clientPlayerEntity == null) {
                return;
            }

            if (profession == VillagerProfession.NONE) {
                //RecruitEntity recruit = new RecruitEntity(clientPlayerEntity.level, villager.getX(), villager.getY(), villager.getZ());
                villager.remove(false);
                //clientPlayerEntity.level.addEnt(recruit);
            }
        }
    }
}















        /*
        World world = clientPlayerEntity.level;
        List<VillagerEntity> list = world.getEntitiesOfClass(VillagerEntity.class, clientPlayerEntity.getBoundingBox().inflate(4.0D));
        for (VillagerEntity villager : list){

        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            RecruitEntity recruit = new RecruitEntity(world, villager.getX(), villager.getY(), villager.getZ());
             level.addFreshEntity(recruit);
            villager.kill();
*/