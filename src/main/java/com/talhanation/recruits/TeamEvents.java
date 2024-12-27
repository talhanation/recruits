package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import com.talhanation.recruits.world.RecruitsTeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class  TeamEvents {

    public MinecraftServer server;
    public static RecruitsTeamManager recruitsTeamManager;
    public static RecruitsDiplomacyManager recruitsDiplomacyManager;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        ServerLevel level = this.server.overworld();

        Collection<PlayerTeam> list =  level.getScoreboard().getPlayerTeams();
        for(PlayerTeam playerTeam : list){
            playerTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
            playerTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());
        }

        recruitsTeamManager = new RecruitsTeamManager();
        recruitsTeamManager.load(server.overworld());

        recruitsDiplomacyManager = new RecruitsDiplomacyManager();
        recruitsDiplomacyManager.load(server.overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitsTeamManager.save(server.overworld());
        recruitsDiplomacyManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event){
        recruitsTeamManager.save(server.overworld());
        recruitsDiplomacyManager.save(server.overworld());
    }

    public static boolean isPlayerInATeam(Player player) {
        return player.getTeam() != null;
    }

    public static void openDisbandingScreen(Player player, UUID recruit) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return new TextComponent("disband_screen");
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new DisbandContainer(i, playerEntity, recruit);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(recruit);
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenDisbandScreen(player, recruit));
        }
    }

    public static void openTeamCreationScreen(Player player) {
        if (player instanceof ServerPlayer) {
            //Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player), new MessageToClientUpdateTeamCreationScreen(TeamEvents.getCurrency(), RecruitsServerConfig.TeamCreationCost.get(), RecruitsServerConfig.MaxRecruitsForPlayer.get()));
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return new TextComponent("team_creation_screen");
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
    public static boolean createTeam(boolean menu, ServerPlayer serverPlayer, @NotNull ServerLevel level, String teamName, String playerName, ItemStack banner, ChatFormatting color, byte colorByte) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        int cost = RecruitsServerConfig.TeamCreationCost.get();
        if(banner == null) banner = Items.BROWN_BANNER.getDefaultInstance();
        CompoundTag nbt = banner.serializeNBT();

        if (team == null) {
            if (teamName.chars().count() <= 24) {
                if (!(teamName.isBlank() || teamName.isEmpty())) {
                    if (!recruitsTeamManager.isNameInUse(teamName)) {
                        if (playerHasEnoughEmeralds(serverPlayer, cost) || !menu) {
                            if (!recruitsTeamManager.isBannerBlank(banner) || !menu) {
                                if (!recruitsTeamManager.isBannerInUse(nbt) || !menu) {
                                    Scoreboard scoreboard = server.getScoreboard();
                                    PlayerTeam newTeam = scoreboard.addPlayerTeam(teamName);
                                    newTeam.setDisplayName(new TextComponent(teamName));

                                    newTeam.setColor(Objects.requireNonNull(color));
                                    newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
                                    newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

                                    server.getScoreboard().addPlayerToTeam(playerName, newTeam);
                                    //TeamCommand
                                    if(menu) doPayment(serverPlayer, cost);

                                    recruitsTeamManager.addTeam(teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT(), colorByte, newTeam.getColor());
                                    addPlayerToData(level, teamName, 1, playerName);

                                    List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
                                    int recruitCount = recruits.size();
                                    addNPCToData(level, teamName, recruitCount);

                                    addRecruitToTeam(recruits, newTeam, level);


                                    Main.LOGGER.info("The new Team " + teamName + " has been created by " + playerName + ".");
                                    return true;
                                }
                                else
                                    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_exists").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
                            }
                            else
                                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.wrongbanner"), serverPlayer.getUUID());
                        }
                        else
                            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
                    }
                    else
                        serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
                }
                else
                    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noname").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
            } 
            else
                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.teamname_to_long").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
        }
        else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED), serverPlayer.getUUID());

        return false;
    }

    public static void updateTeamInspectMenu(ServerPlayer player, ServerLevel level, String team){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(team);

        if(recruitsTeam != null){
            ItemStack bannerStack = ItemStack.of(recruitsTeam.getBanner());
            List<String> joinRequests = recruitsTeam.getJoinRequests();
            int players = recruitsTeam.getPlayers();
            int npcs = recruitsTeam.getNPCs();
            UUID leaderUUID = recruitsTeam.getTeamLeaderUUID();
            String leaderName = recruitsTeam.getTeamLeaderName();

            //Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateTeam(player.getUUID(), leaderName, leaderUUID, bannerStack, joinRequests, players, npcs));
        }
        else
            Main.LOGGER.error("Could not load recruitsTeamData for Team " + team + " because Team == null");
    }

    public static boolean leaveTeam(boolean command, ServerPlayer player, String teamName, ServerLevel level, boolean fromLeader) {
        MinecraftServer server = level.getServer();
        String playerName = player.getName().getString();
        Team team = player.getTeam();

        if(team != null){
            if(teamName == null) teamName = team.getName();

            PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

            RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);

            boolean isLeader;
            if(recruitsTeam != null) {
                isLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());
            }
            else
                isLeader = command;

            int recruits = getRecruitsOfPlayer(player.getUUID(), level).size();
            addNPCToData(level, teamName, -recruits);

            if(playerTeam != null){
                if(isLeader){
                    removeTeam(level, teamName);
                }
                else {
                    ServerPlayer leaderOfTeam = server.getPlayerList().getPlayerByName(recruitsTeam.getTeamLeaderName());
                    if(!fromLeader && leaderOfTeam != null) leaderOfTeam.sendMessage(PLAYER_LEFT_TEAM_LEADER(playerName), leaderOfTeam.getUUID());

                    server.getScoreboard().removePlayerFromTeam(playerName, playerTeam);
                    addPlayerToData(level,teamName,-1, playerName);

                }
                removeRecruitFromTeam(player, level);
                return true;
            }
            else
                Main.LOGGER.error("Can not remove " + playerName + " from Team, because " + teamName + " does not exist!");

            serverSideUpdateTeam(level);
        }

        else {
            PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

            if(playerTeam != null){
                recruitsTeamManager.removeTeam(teamName);
                return true;
            }

        }

        return false;
    }

    public static void modifyTeam(ServerLevel level, String name, RecruitsTeam editedTeam) {
        MinecraftServer server = level.getServer();
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(name);
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(name);

        if(recruitsTeam != null && playerTeam != null){
            recruitsTeam.setTeamLeaderID(editedTeam.getTeamLeaderUUID());
            recruitsTeam.setTeamName(editedTeam.getTeamName());
            recruitsTeam.setTeamLeaderID(editedTeam.getTeamLeaderUUID());
            recruitsTeam.setTeamLeaderName(editedTeam.getTeamLeaderName());
            recruitsTeam.setBanner(editedTeam.getBanner());
            recruitsTeam.setUnitColor(editedTeam.getUnitColor());
            recruitsTeam.setTeamColor(editedTeam.getTeamColor());

            playerTeam.setDisplayName(new TextComponent(editedTeam.getTeamName()));
            playerTeam.setColor(ChatFormatting.getById(editedTeam.getTeamColor()));
        }
    }

    public static void removeTeam(ServerLevel level, String teamName){
        MinecraftServer server = level.getServer();
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

        if(playerTeam != null){
            server.getScoreboard().removePlayerTeam(playerTeam);

            removeRecruitsTeamData(teamName);

            recruitsTeamManager.removeTeam(teamName);
        }
    }

    private static void removeRecruitsTeamData(String teamName) {
        recruitsTeamManager.removeTeam(teamName);
    }

    public static boolean addPlayerToTeam(ServerPlayer player, ServerLevel level, String teamName, String namePlayerToAdd) {
        MinecraftServer server = level.getServer();
        ServerPlayer playerToAdd = server.getPlayerList().getPlayerByName(namePlayerToAdd);
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

        for(RecruitsTeam recruitsTeam : recruitsTeamManager.getTeams()) {
            if(recruitsTeam.getTeamLeaderUUID().equals(playerToAdd.getUUID())){
                player.sendMessage(CAN_NOT_ADD_OTHER_LEADER(), player.getUUID());
                return false;
            }
        }

        if(playerTeam != null){
            server.getScoreboard().addPlayerToTeam(namePlayerToAdd, playerTeam);

            if(playerToAdd != null) playerToAdd.sendMessage(ADDED_PLAYER(teamName), playerToAdd.getUUID());
            if(player != null) player.sendMessage(ADDED_PLAYER_LEADER(namePlayerToAdd), player.getUUID());

            addPlayerToData(level,teamName,1, namePlayerToAdd);

            int recruits = getRecruitsOfPlayer(playerToAdd.getUUID(), level).size();
            addNPCToData(level, teamName, recruits);

            serverSideUpdateTeam(level);

            RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> playerToAdd), new MessageToClientSetDiplomaticToast(8, recruitsTeam));

            List<ServerPlayer> playersInTeam = TeamEvents.recruitsTeamManager.getPlayersInTeam(teamName, level);
            for (ServerPlayer teamPlayer : playersInTeam) {
                if(!teamPlayer.getUUID().equals(playerToAdd.getUUID()))
                    Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> teamPlayer), new MessageToClientSetDiplomaticToast(9, recruitsTeam, playerToAdd.getName().getString()));
            }
            return true;
        }
        else
            Main.LOGGER.error("Can not add " + playerToAdd + " to Team, because " + teamName + " does not exist!");
        return false;
    }
    public static Component REMOVE_PLAYER_LEADER(String player){
        return new TranslatableComponent("chat.recruits.team_creation.removedPlayerLeader", player);
    }

    public static final Component PLAYER_REMOVED = new TranslatableComponent("chat.recruits.team_creation.removedPlayer");

    public static Component ADDED_PLAYER(String s){
        return new TranslatableComponent("chat.recruits.team_creation.addedPlayer", s);
    }

    public static Component ADDED_PLAYER_LEADER(String s){
        return new TranslatableComponent("chat.recruits.team_creation.addedPlayerLeader", s);
    }

    public static Component CAN_NOT_ADD_OTHER_LEADER(){
        return new TranslatableComponent("chat.recruits.team_creation.canNotAddOtherLeader");
    }

    public static Component PLAYER_LEFT_TEAM_LEADER(String s){
        return new TranslatableComponent("chat.recruits.team_creation.playerLeftTeamLeader", s);
    }

    public static void addPlayerToData(ServerLevel level, String teamName, int x, String namePlayerToAdd){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);;

        recruitsTeam.addPlayer(x);

        if(x > 0){ //actually adding the player therefor remove it from request list
            recruitsTeam.removeJoinRequest(namePlayerToAdd);
        }
    }
    public static void addNPCToData(ServerLevel level, String teamName, int x){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);;

        if(recruitsTeam != null){
            recruitsTeam.addNPCs(x);
        }
        else Main.LOGGER.error("Could not modify recruits team: "+ teamName + ".Team does not exist.");
    }

    public static void sendJoinRequest(ServerLevel level, ServerPlayer player, String teamName) {
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);

        if(recruitsTeam != null){
            if(recruitsTeam.addPlayerAsJoinRequest(player.getName().getString())){
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> recruitsTeamManager.getTeamLeader(recruitsTeam, level)), new MessageToClientSetDiplomaticToast(7, recruitsTeam, player.getName().getString()));
            }
        }
        else Main.LOGGER.error("Could not add join request for "+ teamName + ".Team does not exist.");
    }

    public static void tryToRemoveFromTeam(Team team, ServerPlayer serverPlayer, ServerPlayer potentialRemovePlayer, ServerLevel level, String nameToRemove, boolean menu) {
        if (potentialRemovePlayer != null && team != null) {
            boolean isPlayerToRemove = potentialRemovePlayer.getName().getString().equals(nameToRemove);

            if (isPlayerToRemove) {
                TeamEvents.leaveTeam(false, potentialRemovePlayer, null, level, true);
                potentialRemovePlayer.sendMessage(PLAYER_REMOVED, serverPlayer.getUUID());
                if(menu)serverPlayer.sendMessage(REMOVE_PLAYER_LEADER(potentialRemovePlayer.getDisplayName().getString()), serverPlayer.getUUID());

                List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
                int recruitCount = recruits.size();

                addNPCToData(level, team.getName(), -recruitCount);
                removeRecruitFromTeam(recruits, team, level);
            }
        }
    }

    public static ItemStack getCurrency(){
        ItemStack currencyItemStack;
        String str = RecruitsServerConfig.RecruitCurrency.get();
        Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));

        currencyItemStack = holder.map(itemHolder -> itemHolder.value().getDefaultInstance()).orElseGet(Items.EMERALD::getDefaultInstance);

        return currencyItemStack;
    }
    public static boolean playerHasEnoughEmeralds(ServerPlayer player, int price){
        Inventory playerInv = player.getInventory();
        int playerEmeralds = 0;

        Item currency = getCurrency().getItem();

        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot.equals(currency)){
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        return playerEmeralds >= price;
    }

    public static void doPayment(Player player, int costs){
        Inventory playerInv = player.getInventory();
        int playerEmeralds = 0;

        ItemStack currencyItemStack = getCurrency();


        //checkPlayerMoney
        playerEmeralds = playerGetEmeraldsInInventory(player, currencyItemStack.getItem());
        playerEmeralds = playerEmeralds - costs;

        //remove Player Emeralds
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == currencyItemStack.getItem()){
                playerInv.removeItemNoUpdate(i);
            }
        }

        //add Player Emeralds what is left
        ItemStack emeraldsLeft = getCurrency();

        emeraldsLeft.setCount(playerEmeralds);
        playerInv.add(emeraldsLeft);
    }

    public static int playerGetEmeraldsInInventory(Player player, Item currency) {
        int emeralds = 0;
        Inventory playerInv = player.getInventory();
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == currency){
                emeralds = emeralds + itemStackInSlot.getCount();
            }
        }
        return emeralds;
    }

    public static void assignToTeamMate(ServerPlayer oldOwner, UUID newOwnerUUID, AbstractRecruitEntity recruit) {
        ServerLevel level = oldOwner.getLevel();
        Team team = oldOwner.getTeam();

        if(team != null){
           Collection<String> list = team.getPlayers().stream().toList();
           List<ServerPlayer> playerList = level.players();

           boolean playerNotFound = false;
           ServerPlayer newOwner = playerList.stream().filter(player -> player.getUUID().equals(newOwnerUUID)).findFirst().orElse(null);

            if(newOwner != null){
                if(list.contains(newOwner.getName().getString())){
                    if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(newOwnerUUID)) {
                        oldOwner.sendMessage(new TranslatableComponent("chat.recruits.team.assignNewOwnerLimitReached"), oldOwner.getUUID());
                        return;
                    }
                    recruit.disband(oldOwner, true, true);
                    recruit.hire(newOwner);

                    Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> newOwner), new MessageToClientSetToast(0, oldOwner.getName().getString()));
                }
                else
                    playerNotFound = true;
            }
            else
                playerNotFound = true;

            if(playerNotFound) oldOwner.sendMessage(new TranslatableComponent("chat.recruits.team.assignNewOwnerNotFound"), oldOwner.getUUID());
        }
    }

    @SubscribeEvent
    public void onTypeCommandEvent(CommandEvent event){
        if (event.getParseResults() != null) {
            String command = event.getParseResults().getReader().getString();
            CommandSourceStack sourceStack = event.getParseResults().getContext().build(command).getSource();
            Entity entity = sourceStack.getEntity();
            ServerLevel level = this.server.overworld();

            if(entity instanceof ServerPlayer sender){
                if(command.contains("team")){
                    if(command.contains("add")) {
                        ItemStack mainhand = (sender).getMainHandItem();
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        createTeam(false, sender, level, teamName, sender.getName().getString(), mainhand.getItem() instanceof BannerItem ? mainhand : null, ChatFormatting.WHITE, (byte) 0);
                        sourceStack.sendSuccess(new TranslatableComponent("commands.team.add.success", teamName), true);
                        event.setCanceled(true);
                        serverSideUpdateTeam(level);
                    }
                    else if(command.contains("remove")){
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        leaveTeam(true,sender, teamName, level, false);
                        sourceStack.sendSuccess(new TranslatableComponent("commands.team.remove.success", teamName), true);
                        event.setCanceled(true);
                        serverSideUpdateTeam(level);
                    }
                    else if(command.contains("join") || command.contains("leave")){
                        serverSideUpdateTeam(level);
                    }
                }
            }
            else {
                if (command.contains("team")) {
                    if (command.contains("add")) {
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        createTeamConsole(sourceStack, level, teamName, "white", (byte) 0);
                        event.setCanceled(true);
                    }
                    else if (command.contains("remove")) {
                        String[] parts = command.split(" ");
                        String teamName = parts[2];

                        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

                        if (playerTeam != null) {

                            server.getScoreboard().removePlayerTeam(playerTeam);
                            recruitsTeamManager.removeTeam(teamName);

                            sourceStack.sendSuccess(new TranslatableComponent("commands.team.remove.success", teamName), true);
                        } else {
                            sourceStack.sendFailure(new TranslatableComponent("team.notFound", teamName));
                        }
                        event.setCanceled(true);
                    }
                    else if (command.contains("join")) {
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        String playerName = parts[3];

                        ServerPlayer player = this.server.getPlayerList().getPlayerByName(playerName);
                        if (player != null) {
                            addPlayerToTeam(player, this.server.overworld(), teamName, playerName);
                            sourceStack.sendSuccess(new TranslatableComponent("commands.team.join.success.single", playerName, teamName), true);
                            serverSideUpdateTeam(level);
                        } else {
                            sourceStack.sendFailure(new TranslatableComponent("argument.player.unknown"));
                        }
                        event.setCanceled(true);
                    }
                    else if (command.contains("leave")) {
                        String[] parts = command.split(" ");
                        String playerName = parts[2];

                        ServerPlayer player = this.server.getPlayerList().getPlayerByName(playerName);
                        if (player != null) {
                            Team team = player.getTeam();
                            tryToRemoveFromTeam(team, player, player,this.server.overworld(), playerName, false);
                            sourceStack.sendSuccess(new TranslatableComponent("commands.team.leave.success.single", playerName), true);
                            serverSideUpdateTeam(level);
                        } else {
                            sourceStack.sendFailure(new TranslatableComponent("argument.player.unknown"));
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    private void createTeamConsole(CommandSourceStack sourceStack, ServerLevel  level, String teamName, String color, byte colorByte) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);

        ItemStack banner = Items.BROWN_BANNER.getDefaultInstance();
        if (team == null) {
            if (teamName.chars().count() <= 13) {
                if (!(teamName.isBlank() || teamName.isEmpty())) {
                    if (!recruitsTeamManager.isNameInUse(teamName)) {
                            Scoreboard scoreboard = server.getScoreboard();
                            PlayerTeam newTeam = scoreboard.addPlayerTeam(teamName);
                            newTeam.setDisplayName(new TextComponent(teamName));

                            newTeam.setColor(Objects.requireNonNull(ChatFormatting.getByName(color)));
                            newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
                            newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

                            recruitsTeamManager.addTeam(teamName,new UUID(0,0),"none", banner.serializeNBT(), colorByte, newTeam.getColor());

                            Main.LOGGER.info("The new Team " + teamName + " has been created by console.");
                    }
                    else
                        sourceStack.sendFailure(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
                }
                else
                    sourceStack.sendFailure(new TranslatableComponent("chat.recruits.team_creation.noname").withStyle(ChatFormatting.RED));
            }
            else
                sourceStack.sendFailure(new TranslatableComponent("chat.recruits.team_creation.teamname_to_long").withStyle(ChatFormatting.RED));
        }
        else
            sourceStack.sendFailure(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
    }


    public static void serverSideUpdateTeam(ServerLevel level){
        List<AbstractRecruitEntity> recruitList = new ArrayList<>();
        for(Entity entity : level.getEntities().getAll()){
            if(entity instanceof AbstractRecruitEntity recruit)
                recruitList.add(recruit);
        }
        for(AbstractRecruitEntity recruit : recruitList){
            recruit.needsTeamUpdate = true;
        }
    }

    ////////////////////////////////////Recruit TEAM JOIN AND REMOVE////////////////////////////

    private static List<AbstractRecruitEntity> getRecruitsOfPlayer(UUID player_uuid, ServerLevel level) {
        List<AbstractRecruitEntity> list = new ArrayList<>();

        for(Entity entity : level.getEntities().getAll()){
            if(entity instanceof AbstractRecruitEntity recruit && recruit.getOwner() != null && recruit.getOwnerUUID().equals(player_uuid))
                list.add(recruit);
        }
        return list;
    }

    public static void addRecruitToTeam(List<AbstractRecruitEntity> recruits, Team team, ServerLevel level){
        for(AbstractRecruitEntity recruit : recruits){
            addRecruitToTeam(recruit, team, level);
        }
    }

    public static void addRecruitToTeam(AbstractRecruitEntity recruit, Team team, ServerLevel level){
        String teamName = team.getName();
        PlayerTeam playerteam = level.getScoreboard().getPlayerTeam(teamName);
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByName(teamName);

        boolean flag = playerteam != null && level.getScoreboard().addPlayerToTeam(recruit.getStringUUID(), playerteam);
        if (!flag) {
            Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
        } else{
            recruit.setTarget(null);// fix "if owner was other team and now same team und was target"
            if(recruitsTeam != null) recruit.setColor(recruitsTeam.getUnitColor());
        }

    }

    public static void removeRecruitFromTeam(ServerPlayer player, ServerLevel level){
        List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(player.getUUID(), level);
        Team team = player.getTeam();
        if(team  != null){
            removeRecruitFromTeam(recruits, team, level);
        }
    }
    public static void removeRecruitFromTeam(List<AbstractRecruitEntity> recruits, Team team, ServerLevel level){
        for(AbstractRecruitEntity recruit : recruits){
            removeRecruitFromTeam(recruit, team, level);
        }
    }
    public static void removeRecruitFromTeam(AbstractRecruitEntity recruit, Team team, ServerLevel level){
        Team recruitsTeam = recruit.getTeam();

        if(recruitsTeam != null && recruitsTeam.equals(team)){
            PlayerTeam recruitTeam = level.getScoreboard().getPlayerTeam(team.getName());
            if(recruitTeam != null) level.getScoreboard().removePlayerFromTeam(recruit.getStringUUID(), recruitTeam);
        }
    }
}
