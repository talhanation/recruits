package com.talhanation.recruits;

import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsTeam;
import com.talhanation.recruits.world.RecruitsTeamSavedData;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TeamEvents {

    public static boolean isPlayerInATeam(Player player) {
        return player.getTeam() != null;
    }


    public static void openTeamListScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return new TextComponent("Team List") {
                    };
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamListContainer(i, playerEntity);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(player.getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamListScreen(player));
        }
    }

    public static void openTeamInspectionScreen(Player player, Team team) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return new TextComponent("Team Inspection") {
                    };
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamInspectionContainer(i, playerEntity);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(player.getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamInspectionScreen(player));
        }
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

    public static void openTeamMainScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return new TranslatableComponent("team_main_screen") {
                    };
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamMainContainer(i, playerEntity);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(player.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTeamMainScreen(player));
        }
    }

    public static void openTeamAddPlayerScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return new TranslatableComponent("team_add_player_screen") {
                    };
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamManagePlayerContainer(i, playerEntity);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(player.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamAddPlayerScreen(player));
        }
    }


    public static void createTeam(ServerPlayer serverPlayer, ServerLevel level, String teamName, String playerName, int cost, ItemStack banner) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        server.getScoreboard().getPlayerTeams();
        String createTeamCommand = "/team add " + teamName;
        String joinTeamCommand = "/team join " + teamName + " " + playerName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(serverPlayer.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), serverPlayer);
        //re add banner to player
        //serverPlayer.getInventory().add(banner.copy());

        if(team == null){
            if(AssassinEvents.playerHasEnoughEmeralds(serverPlayer, cost)){
                if (!isBannerBlank(banner)) {
                    if (!isBannerInUse(level, banner.serializeNBT())) {
                        server.getCommands().performCommand(commandSourceStack, createTeamCommand);
                        server.getCommands().performCommand(commandSourceStack, joinTeamCommand);
                        AssassinEvents.doPayment(serverPlayer, cost);

                        saveDataToTeam(level, teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT());
                        addPlayerToData(level, teamName, 1, playerName);
                        Main.LOGGER.debug("The Team "+ teamName + " has been created by " + playerName + ".");
                    } else
                        serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_exists"), serverPlayer.getUUID());
                }
                else
                    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.wrongbanner"), serverPlayer.getUUID());

            }
            else
                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
        }
        else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists"), serverPlayer.getUUID());
    }

    public static void saveDataToTeam(ServerLevel level, String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);

        Main.LOGGER.debug("Before Teams: " + data.getTeams());
        data.addTeam(teamName, leaderUUID, leaderName, bannerNbt);
        data.setDirty();

        Main.LOGGER.debug("After Teams: " + data.getTeams());
    }

    public static boolean isBannerInUse(ServerLevel level, CompoundTag bannerNbt){
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        for(RecruitsTeam recruitsTeam : data.getTeams()){
            return bannerNbt.equals(recruitsTeam.getBanner());
        }
        return false;
    }

    public static boolean isBannerBlank(ItemStack itemStack){
        CompoundTag compoundtag = BlockItem.getBlockEntityData(itemStack);
        return compoundtag == null || !compoundtag.contains("Patterns");
    }

    public static void updateTeamInspectMenu(ServerPlayer player, ServerLevel level, String team){
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(team);
        Main.LOGGER.debug("updateTeamInspectMenu: Team: " + recruitsTeam);

        if(recruitsTeam != null){
            ItemStack bannerStack = ItemStack.of(recruitsTeam.getBanner());
            List<String> joinRequests = recruitsTeam.getJoinRequests();
            int players = recruitsTeam.getPlayers();
            int npcs = recruitsTeam.getNPCs();
            UUID leaderUUID = recruitsTeam.getTeamLeaderUUID();
            String leaderName = recruitsTeam.getTeamLeaderName();

            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateTeam(player.getUUID(), leaderName, leaderUUID, bannerStack, joinRequests, players, npcs));
        }
        else
            Main.LOGGER.error("Could not load recruitsTeamData for Team " + team + " because Team == null");
    }

    public static void leaveTeam(ServerPlayer player, ServerLevel level) {
        MinecraftServer server = level.getServer();
        String playerName = player.getName().getString();
        String teamName = player.getTeam().getName();

        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        boolean isLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());

        String leaveTeamCommand = "/team leave " + playerName;
        String emptyTeam = "/team empty " + teamName;
        String removeTeam = "/team remove " + teamName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(player.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), player);

        if(isLeader){
            server.getCommands().performCommand(commandSourceStack, emptyTeam);
            server.getCommands().performCommand(commandSourceStack, removeTeam);
            removeRecruitsTeamData(level, teamName);
        }
        else {
            server.getCommands().performCommand(commandSourceStack, leaveTeamCommand);
        }
        addPlayerToData(level,teamName,-1, playerName);
    }

    private static void removeRecruitsTeamData(ServerLevel level, String teamName) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        data.getTeams().remove(recruitsTeam);
    }

    public static void addPlayerToTeam(ServerPlayer player, ServerLevel level, String teamName, String namePlayerToAdd) {
        MinecraftServer server = level.getServer();
        String playerName = player.getName().getString();
        String joinTeamCommand = "/team join " + teamName + " " + namePlayerToAdd;

        String string_addedPlayer = ADDED_PLAYER.getString() + teamName;
        String string_addedPlayerLeader = namePlayerToAdd + ADDED_PLAYER_LEADER.getString();


        ServerPlayer playerToAdd = server.getPlayerList().getPlayerByName(namePlayerToAdd);
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(player.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), player);


        server.getCommands().performCommand(commandSourceStack, joinTeamCommand);
        playerToAdd.sendMessage(new TextComponent(string_addedPlayer), playerToAdd.getUUID());
        player.sendMessage(new TextComponent(string_addedPlayerLeader), player.getUUID());

        addPlayerToData(level,teamName,1, namePlayerToAdd);
    }
    public static final TranslatableComponent NO_PLAYER = new TranslatableComponent("chat.recruits.team_creation.could_not_find");
    private static final TranslatableComponent ADDED_PLAYER = new TranslatableComponent("chat.recruits.team_creation.addedPlayer");
    private static final TranslatableComponent ADDED_PLAYER_LEADER = new TranslatableComponent("chat.recruits.team_creation.addedPlayerLeader");

    public static void addPlayerToData(ServerLevel level, String teamName, int x, String namePlayerToAdd){
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = RecruitsTeamSavedData.getTeamByName(teamName);

        recruitsTeam.addPlayer(x);

        if(x > 0){ //actually adding the player therefor remove it from request list
            recruitsTeam.removeJoinRequest(namePlayerToAdd);
        }

        data.setDirty();
    }
    public static void addNPCToData(ServerLevel level, String teamName, int x){
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        recruitsTeam.addNPCs(x);
        data.setDirty();
    }

    public static void sendJoinRequest(ServerLevel level, Player player, String teamName) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        recruitsTeam.addPlayerAsJoinRequest(player.getName().getString());
        data.setDirty();
    }

    public static void tryToRemoveFromTeam(ServerPlayer serverPlayer, ServerLevel level, String nameToRemove) {
        if (serverPlayer != null) {
            boolean isPlayerToRemove = serverPlayer.getName().getString().equals(nameToRemove);
            if (isPlayerToRemove) {
                TeamEvents.leaveTeam(serverPlayer, level);
                serverPlayer.sendMessage(PLAYER_REMOVED, serverPlayer.getUUID());
            }
        }
    }

    private static final TranslatableComponent PLAYER_REMOVED = new TranslatableComponent("chat.recruits.team_creation.removedPlayer");
}
