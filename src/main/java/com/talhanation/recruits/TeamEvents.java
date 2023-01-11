package com.talhanation.recruits;

import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
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
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.stringtemplate.v4.ST;

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
                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noenoughMoney").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
        }
        else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists"), serverPlayer.getUUID());


    }

    public static void saveDataToTeam(ServerLevel level, String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt) {
        DimensionDataStorage storage = level.getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" + teamName + "_data");

        RecruitsTeamSavedData.setTeam(teamName);
        RecruitsTeamSavedData.setTeamLeaderID(leaderUUID);
        RecruitsTeamSavedData.setTeamLeaderName(leaderName);
        RecruitsTeamSavedData.setBanner(bannerNbt);
        /*
        Main.LOGGER.debug("--------------");
        Main.LOGGER.debug("saveDataToTeam Team:" + data.getTeam());
        Main.LOGGER.debug("saveDataToTeam TeamLeader:" + data.getTeamLeaderID());
        Main.LOGGER.debug("saveDataToTeam Banner:" + data.getBanner());
        Main.LOGGER.debug("--------------");

         */

        data.setDirty();
    }

    public static boolean isBannerInUse(ServerLevel level, CompoundTag bannerNbt){
        List<PlayerTeam> teams = level.getScoreboard().getPlayerTeams().stream().toList();

        for (PlayerTeam team : teams){
            String teamName = team.getName();

            DimensionDataStorage storage = level.getDataStorage();
            RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  teamName + "_data");
            /*
            Main.LOGGER.debug("--------------");
            Main.LOGGER.debug("isBannerInUse Banner:" + bannerNbt);
            Main.LOGGER.debug("isBannerInUse team:" + teamName);
            Main.LOGGER.debug("--------------");
            Main.LOGGER.debug("isBannerInUse team:" + data.getTeam());
            Main.LOGGER.debug("isBannerInUse savedBanner:" + data.getBanner());
            Main.LOGGER.debug("--------------");

             */



            return bannerNbt.equals(data.getBanner());
        }
        return false;
    }

    public static boolean isBannerBlank(ItemStack itemStack){
        CompoundTag compoundtag = BlockItem.getBlockEntityData(itemStack);
        return compoundtag == null || !compoundtag.contains("Patterns");
    }

    public static void updateTeamInspectMenu(ServerPlayer player, ServerLevel level, String team){
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  team + "_data");
        ItemStack bannerStack = ItemStack.of(data.getBanner());
        List<String> joinRequests = data.getJoinRequests();
        int players = data.getPlayers();
        int npcs = data.getNpcs();


        /*
        Main.LOGGER.debug("-----------TeamEvents----------");
        Main.LOGGER.debug("teamName: " + team);
        Main.LOGGER.debug("leaderName: " + data.getTeamLeaderName());
        Main.LOGGER.debug("leaderUUID: " + data.getTeamLeaderID());
        Main.LOGGER.debug("Banner:" + data.getBanner());
        Main.LOGGER.debug("RequestList:" + data.getJoinRequests());
        Main.LOGGER.debug("npcs:" + npcs);
        Main.LOGGER.debug("players:" + players);
        Main.LOGGER.debug("-------------------------------");

         */

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateTeam(player.getUUID(), data.getTeamLeaderName(), data.getTeamLeaderID(), bannerStack, joinRequests, players, npcs));
    }

    public static void leaveTeam(ServerPlayer player, ServerLevel level) {
        MinecraftServer server = level.getServer();
        String playerName = player.getName().getString();
        String teamName = player.getTeam().getName();

        DimensionDataStorage storage = server.overworld().getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  teamName + "_data");
        boolean isLeader = data.getTeamLeaderID().equals(player.getUUID());

        String leaveTeamCommand = "/team leave " + playerName;
        String emptyTeam = "/team empty " + teamName;
        String removeTeam = "/team remove " + teamName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(player.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), player);

        if(isLeader){
            server.getCommands().performCommand(commandSourceStack, emptyTeam);
            server.getCommands().performCommand(commandSourceStack, removeTeam);
        }
        else {
            server.getCommands().performCommand(commandSourceStack, leaveTeamCommand);
        }
        addPlayerToData(level,teamName,-1, playerName);
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

    public static void addPlayerToData(ServerLevel level, String teamName, int x, String playerName){
        MinecraftServer server = level.getServer();
        DimensionDataStorage storage = server.overworld().getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  teamName + "_data");

        RecruitsTeamSavedData.addPlayer(x);


        if(x > 0){ //actually adding the player therefor remove it from request list
            RecruitsTeamSavedData.removeJoinRequest(playerName);
        }

        data.setDirty();
    }
    public static void addNPCToData(ServerLevel level, String teamName, int x){
        MinecraftServer server = level.getServer();
        DimensionDataStorage storage = server.overworld().getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  teamName + "_data");

        RecruitsTeamSavedData.addNpcs(x);
        data.setDirty();
    }

    public static void sendJoinRequest(ServerLevel level, Player player, String teamName) {
        MinecraftServer server = level.getServer();
        DimensionDataStorage storage = server.overworld().getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_" +  teamName + "_data");

        RecruitsTeamSavedData.addPlayerAsJoinRequest(player.getName().getString());
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
