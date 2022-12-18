package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.DebugInvContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageCommandScreen;
import com.talhanation.recruits.network.MessageDebugScreen;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class DebugEvents {


    public static void handleMessage(int id, AbstractRecruitEntity recruits) {
        switch (id){
            case 0 -> {recruits.addXp(1); recruits.checkLevel();}
            case 1 -> {recruits.addXp(RecruitsModConfig.RecruitsMaxXpForLevelUp.get()); recruits.checkLevel();}
            case 2 -> {recruits.setCost(recruits.getCost() + 1);}
            case 3 -> {recruits.setCost(recruits.getCost() - 1);}

            case 4 -> {recruits.setHunger(recruits.getHunger() + 1);}
            case 5 -> {recruits.setHunger(recruits.getHunger() - 1);}

            case 6 -> {recruits.setMoral(recruits.getMoral() + 1);}
            case 7 -> {recruits.setMoral(recruits.getMoral() - 1);}

            case 8 -> {recruits.setHealth(recruits.getHealth() + 1);}
            case 9 -> {recruits.setHealth(recruits.getHealth() - 1);}
        }
    }
}
