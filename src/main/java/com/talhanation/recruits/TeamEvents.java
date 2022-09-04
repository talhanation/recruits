package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.inventory.TeamCreationContainer;
import com.talhanation.recruits.network.MessageOpenTeamCreationScreen;
import com.talhanation.recruits.world.ModSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class TeamEvents {

    public static boolean isPlayerInTeam(Player player) {
        return player.getTeam() == null;
    }

    public static boolean isPlayerTeamLeader(Player player, PlayerTeam team) {
        ModSavedData data = new ModSavedData();

        return false;
    }

    public static void openTeamCreationScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return new TextComponent("Team Creation") {
                    };
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamCreationContainer(i, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(player.getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamCreationScreen(player));
        }
    }


    public static void createTeam(ServerPlayer serverPlayer, ServerLevel level, String teamName, String playerName, int cost, ItemStack banner) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        String createTeamCommand = "/team add " + teamName;
        String joinTeamCommand = "/team join " + teamName + " " + playerName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(serverPlayer.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), serverPlayer);


        if(team == null){
            //CompoundTag saved_nbt = null;
            //if (getSavedBannerNBTFromTeam(level, teamName) != null){
            // saved_nbt = getSavedBannerNBTFromTeam(level, teamName);
            //}

            //if(!saved_nbt.equals(banner.serializeNBT())){
                server.getCommands().performCommand(commandSourceStack, createTeamCommand);
                server.getCommands().performCommand(commandSourceStack, joinTeamCommand);
                AssassinEvents.doPayment(serverPlayer, cost);
                saveBannerToTeam(level, teamName, banner);
                Main.LOGGER.debug("A new Team has been created: " + teamName);
            //}
            //else
            //    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_exists"), serverPlayer.getUUID());

        }
        else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED), serverPlayer.getUUID());

        getSavedBannerNBTFromTeam(level, teamName);

    }

    public static int getTeamCreationCost() {
        return RecruitsModConfig.TeamCreationCost.get();
    }

    public static CompoundTag getSavedBannerNBTFromTeam(ServerLevel level, String team) {
        ModSavedData data = ModSavedData.get(level, team, null);


        Main.LOGGER.debug("--------------");
        Main.LOGGER.debug("Team: " + team);
        Main.LOGGER.debug("getSavedBanner: " + data.getBannerNBT());
        Main.LOGGER.debug("--------------");



        return data.getBannerNBT();
    }

    public static void saveBannerToTeam(ServerLevel level, String team, ItemStack banner) {
        ModSavedData data = ModSavedData.get(level, team, banner.serializeNBT());


        Main.LOGGER.debug("--------------");
        Main.LOGGER.debug("saveBannerToTeam Team:" + data.getTeam());
        Main.LOGGER.debug("saveBannerToTeam Banner:" + data.getBannerNBT());
        Main.LOGGER.debug("--------------");

        data.setDirty();
    }

}
