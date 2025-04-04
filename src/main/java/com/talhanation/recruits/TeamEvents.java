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
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TeamEvents {

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
    public void onWorldSave(LevelEvent.Save event){
        recruitsTeamManager.save(server.overworld());
        recruitsDiplomacyManager.save(server.overworld());
    }

    public static boolean isPlayerInATeam(Player player) {
        return player.getTeam() != null;
    }

    public static void openDisbandingScreen(Player player, UUID recruit) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("disband_screen");
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

    public static void openTeamEditScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            RecruitsTeam recruitsTeam = null;
            if(serverPlayer.getTeam() != null) {
                recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(player.getTeam().getName());
            }

            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new MessageToClientUpdateTeamEditScreen(TeamEvents.getCurrency(),
                            RecruitsServerConfig.FactionCreationCost.get(),
                            RecruitsServerConfig.MaxRecruitsForPlayer.get(),
                            recruitsTeam
                    ));

            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return Component.literal("team_edit_screen");
                }

                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new TeamEditMenu(i, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(player.getUUID());
            });
        }
        else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamEditScreen(player));
        }
    }
    public static boolean createTeam(boolean menu, ServerPlayer serverPlayer, @NotNull ServerLevel level, String teamName, String playerName, ItemStack banner, ChatFormatting color, byte colorByte) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        int cost = RecruitsServerConfig.FactionCreationCost.get();
        if(banner == null) banner = Items.BROWN_BANNER.getDefaultInstance();
        CompoundTag nbt = banner.serializeNBT();

        if (team != null) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
            return false;
        }
        else if (teamName.chars().count() > 32) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.teamname_to_long").withStyle(ChatFormatting.RED));
            return false;
        }
        else if (teamName.isBlank() || teamName.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.noname").withStyle(ChatFormatting.RED));
            return false;
        }
        else if(recruitsTeamManager.isNameInUse(teamName)) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
            return false;
        }
        else if(!playerHasEnoughEmeralds(serverPlayer, cost) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED));
            return false;
        }
        else if(recruitsTeamManager.isBannerBlank(banner) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.wrongbanner"));
            return false;
        }
        else if (recruitsTeamManager.isBannerInUse(nbt) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.banner_exists").withStyle(ChatFormatting.RED));
            return false;
        }
        else {
            Scoreboard scoreboard = server.getScoreboard();
            PlayerTeam newTeam = scoreboard.addPlayerTeam(teamName);
            newTeam.setDisplayName(Component.literal(teamName));

            newTeam.setColor(color);
            newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
            newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

            server.getScoreboard().addPlayerToTeam(playerName, newTeam);
            //TeamCommand
            if (menu) doPayment(serverPlayer, cost);

            recruitsTeamManager.addTeam(teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT(), colorByte, newTeam.getColor());
            addPlayerToData(level, teamName, 1, playerName);

            List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
            int recruitCount = recruits.size();
            addNPCToData(level, teamName, recruitCount);

            addRecruitToTeam(recruits, newTeam, level);

            Main.LOGGER.info("The new Team " + teamName + " has been created by " + playerName + ".");

            recruitsTeamManager.save(server.overworld());

            return true;
        }
    }

    public static void updateTeamInspectMenu(ServerPlayer player, ServerLevel level, String team){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(team);

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

            RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(teamName);

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
                    if(!fromLeader && leaderOfTeam != null) leaderOfTeam.sendSystemMessage(PLAYER_LEFT_TEAM_LEADER(playerName));

                    server.getScoreboard().removePlayerFromTeam(playerName, playerTeam);
                    addPlayerToData(level,teamName,-1, playerName);

                }
                removeRecruitFromTeam(teamName, player, level);
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
        recruitsTeamManager.save(server.overworld());
        return false;
    }

    public static void modifyTeam(ServerLevel level, String stringID, RecruitsTeam editedTeam, @Nullable ServerPlayer serverPlayer, int cost) {
        MinecraftServer server = level.getServer();
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(stringID);
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(stringID);

        if(serverPlayer != null){
            if(cost > 0 && !playerHasEnoughEmeralds(serverPlayer, cost)) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED));
                return;
            }
            else{
                doPayment(serverPlayer, cost);
            }

        }

        if(recruitsTeam != null && playerTeam != null){
            if(!recruitsTeam.getTeamLeaderUUID().equals(editedTeam.getTeamLeaderUUID())){
                notifyFactionMembers(level, recruitsTeam, 10, editedTeam.getTeamLeaderName());
                recruitsTeam.setTeamLeaderID(editedTeam.getTeamLeaderUUID());
                recruitsTeam.setTeamLeaderName(editedTeam.getTeamLeaderName());
            }

            if(!recruitsTeam.getTeamDisplayName().equals(editedTeam.getTeamDisplayName())){
                notifyFactionMembers(level, recruitsTeam, 11, editedTeam.getTeamDisplayName());
                recruitsTeam.setTeamDisplayName(editedTeam.getTeamDisplayName());
            }

            if(!recruitsTeam.getBanner().equals(editedTeam.getBanner())){
                notifyFactionMembers(level, recruitsTeam, 12, "");
                recruitsTeam.setBanner(editedTeam.getBanner());
            }

            recruitsTeam.setUnitColor(editedTeam.getUnitColor());
            recruitsTeam.setTeamColor(editedTeam.getTeamColor());
            recruitsTeam.setMaxNPCsPerPlayer(editedTeam.getMaxNPCsPerPlayer());

            playerTeam.setDisplayName(Component.literal(editedTeam.getTeamDisplayName()));
            playerTeam.setColor(ChatFormatting.getById(editedTeam.getTeamColor()));
        }
    }

    public static void notifyFactionMembers(ServerLevel level, RecruitsTeam recruitsTeam, int id, String notification){
        List<ServerPlayer> playersInTeam = TeamEvents.recruitsTeamManager.getPlayersInTeam(recruitsTeam.getStringID(), level);
        for (ServerPlayer teamPlayer : playersInTeam) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> teamPlayer), new MessageToClientSetDiplomaticToast(id, recruitsTeam, notification));
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
        recruitsTeamManager.save(server.overworld());
    }

    private static void removeRecruitsTeamData(String teamName) {
        recruitsTeamManager.removeTeam(teamName);
    }

    public static boolean addPlayerToTeam(@Nullable ServerPlayer player, ServerLevel level, String teamName, String namePlayerToAdd) {
        MinecraftServer server = level.getServer();
        ServerPlayer playerToAdd = server.getPlayerList().getPlayerByName(namePlayerToAdd);
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

        if(isPlayerAlreadyAFactionLeader(playerToAdd)){
            if(player != null) player.sendSystemMessage(CAN_NOT_ADD_OTHER_LEADER());
            return false;
        }

        if(playerTeam != null){
            server.getScoreboard().addPlayerToTeam(namePlayerToAdd, playerTeam);

            playerToAdd.sendSystemMessage(ADDED_PLAYER(teamName));
            if(player != null) player.sendSystemMessage(ADDED_PLAYER_LEADER(namePlayerToAdd));

            addPlayerToData(level,teamName,1, namePlayerToAdd);

            int recruits = getRecruitsOfPlayer(playerToAdd.getUUID(), level).size();
            addNPCToData(level, teamName, recruits);

            serverSideUpdateTeam(level);

            RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(teamName);
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> playerToAdd), new MessageToClientSetDiplomaticToast(8, recruitsTeam));

            notifyFactionMembers(level, recruitsTeam, 9, playerToAdd.getName().getString());

            recruitsTeamManager.save(server.overworld());
            return true;
        }
        else
            Main.LOGGER.error("Can not add " + playerToAdd + " to Team, because " + teamName + " does not exist!");
        return false;
    }

    public static boolean isPlayerAlreadyAFactionLeader(ServerPlayer playerToCheck){
        for(RecruitsTeam recruitsTeam : recruitsTeamManager.getTeams()) {
            if(recruitsTeam.getTeamLeaderUUID().equals(playerToCheck.getUUID())){
                return true;
            }
        }
        return false;
    }

    public static Component REMOVE_PLAYER_LEADER(String player){
        return Component.translatable("chat.recruits.team_creation.removedPlayerLeader", player);
    }

    public static final Component PLAYER_REMOVED = Component.translatable("chat.recruits.team_creation.removedPlayer");

    public static Component ADDED_PLAYER(String s){
        return Component.translatable("chat.recruits.team_creation.addedPlayer", s);
    }

    public static Component ADDED_PLAYER_LEADER(String s){
        return Component.translatable("chat.recruits.team_creation.addedPlayerLeader", s);
    }

    public static Component CAN_NOT_ADD_OTHER_LEADER(){
        return Component.translatable("chat.recruits.team_creation.canNotAddOtherLeader");
    }

    public static Component PLAYER_LEFT_TEAM_LEADER(String s){
        return Component.translatable("chat.recruits.team_creation.playerLeftTeamLeader", s);
    }

    public static void addPlayerToData(ServerLevel level, String teamName, int x, String namePlayerToAdd){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(teamName);;

        recruitsTeam.addPlayer(x);

        if(x > 0){ //actually adding the player therefor remove it from request list
            recruitsTeam.removeJoinRequest(namePlayerToAdd);
        }
    }
    public static void addNPCToData(ServerLevel level, String teamName, int x){
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(teamName);;

        if(recruitsTeam != null){
            recruitsTeam.addNPCs(x);
        }
        else Main.LOGGER.error("Could not modify recruits team: "+ teamName + ".Team does not exist.");
    }

    public static void sendJoinRequest(ServerLevel level, ServerPlayer player, String stringID) {
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(stringID);

        if(recruitsTeam != null){
            if(recruitsTeam.addPlayerAsJoinRequest(player.getName().getString())){
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> recruitsTeamManager.getTeamLeader(recruitsTeam, level)), new MessageToClientSetDiplomaticToast(7, recruitsTeam, player.getName().getString()));
            }
        }
        else Main.LOGGER.error("Could not add join request for "+ stringID + ".Team does not exist.");
    }

    public static void tryToRemoveFromTeam(Team team, ServerPlayer serverPlayer, ServerPlayer potentialRemovePlayer, ServerLevel level, String nameToRemove, boolean menu) {
        if (potentialRemovePlayer != null && team != null) {
            boolean isPlayerToRemove = potentialRemovePlayer.getName().getString().equals(nameToRemove);

            if (isPlayerToRemove) {
                TeamEvents.leaveTeam(false, potentialRemovePlayer, null, level, true);
                potentialRemovePlayer.sendSystemMessage(PLAYER_REMOVED);
                if(menu)serverPlayer.sendSystemMessage(REMOVE_PLAYER_LEADER(potentialRemovePlayer.getDisplayName().getString()));

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

        return playerEmeralds >= price || player.isCreative();
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
        ServerLevel level = (ServerLevel) oldOwner.getCommandSenderWorld();

        Team team = oldOwner.getTeam();

        if(team != null){
           Collection<String> list = team.getPlayers().stream().toList();
           List<ServerPlayer> playerList = level.players();

           boolean playerNotFound = false;
           ServerPlayer newOwner = playerList.stream().filter(player -> player.getUUID().equals(newOwnerUUID)).findFirst().orElse(null);

            if(newOwner != null){
                if(list.contains(newOwner.getName().getString())){

                    if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(team.getName(), newOwnerUUID)) {
                        oldOwner.sendSystemMessage(Component.translatable("chat.recruits.team.assignNewOwnerLimitReached"));
                        return;
                    }
                    recruit.disband(oldOwner, true, true);

                    Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> newOwner), new MessageToClientSetToast(0, oldOwner.getName().getString()));

                    recruit.hire(newOwner);
                }
                else
                    playerNotFound = true;
            }
            else
                playerNotFound = true;

            if(playerNotFound) oldOwner.sendSystemMessage(Component.translatable("chat.recruits.team.assignNewOwnerNotFound"));
        }
    }

    @SubscribeEvent
    public void onTypeCommandEvent(CommandEvent event){
        if (event.getParseResults() != null) {
            String command = event.getParseResults().getReader().getString();
            CommandSourceStack sourceStack = event.getParseResults().getContext().build(command).getSource();
            ServerPlayer sender = sourceStack.getPlayer();
            ServerLevel level = this.server.overworld();

            if(sender != null){
                if(command.contains("team")){
                    if(command.contains("add")) {
                        ItemStack mainhand = (sender).getMainHandItem();
                        String[] parts = command.split(" ");
                        String teamName = parts[2];

                        createTeam(false, sender, level, teamName, sender.getName().getString(), mainhand.getItem() instanceof BannerItem ? mainhand : null, ChatFormatting.WHITE, (byte) 0);
                        sourceStack.sendSuccess(() -> Component.translatable("commands.team.add.success", teamName), true);

                        event.setCanceled(true);
                        serverSideUpdateTeam(level);
                    }
                    else if(command.contains("remove")){
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        leaveTeam(true,sender, teamName, level, false);
                        sourceStack.sendSuccess(() -> Component.translatable("commands.team.remove.success", teamName), true);
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

                            sourceStack.sendSuccess(() -> Component.translatable("commands.team.remove.success", teamName), true);
                        } else {
                            sourceStack.sendFailure(Component.translatable("team.notFound", teamName));
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
                            sourceStack.sendSuccess(() -> Component.translatable("commands.team.join.success.single", playerName, teamName), true);
                            serverSideUpdateTeam(level);
                        } else {
                            sourceStack.sendFailure(Component.translatable("argument.player.unknown"));
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
                            sourceStack.sendSuccess(() -> Component.translatable("commands.team.leave.success.single", playerName), true);
                            serverSideUpdateTeam(level);
                        } else {
                            sourceStack.sendFailure(Component.translatable("argument.player.unknown"));
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
                        newTeam.setDisplayName(Component.literal(teamName));

                        newTeam.setColor(Objects.requireNonNull(ChatFormatting.getByName(color)));
                        newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
                        newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

                        recruitsTeamManager.addTeam(teamName,new UUID(0,0),"none", banner.serializeNBT(), colorByte, newTeam.getColor());

                        Main.LOGGER.info("The new Team " + teamName + " has been created by console.");

                        recruitsTeamManager.save(server.overworld());
                    }
                    else
                        sourceStack.sendFailure(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
                }
                else
                    sourceStack.sendFailure(Component.translatable("chat.recruits.team_creation.noname").withStyle(ChatFormatting.RED));
            }
            else
                sourceStack.sendFailure(Component.translatable("chat.recruits.team_creation.teamname_to_long").withStyle(ChatFormatting.RED));
        }
        else
            sourceStack.sendFailure(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
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

        recruitsTeamManager.save(level);
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
        RecruitsTeam recruitsTeam = recruitsTeamManager.getTeamByStringID(teamName);

        boolean flag = playerteam != null && level.getScoreboard().addPlayerToTeam(recruit.getStringUUID(), playerteam);
        if (!flag) {
            Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
        } else{
            recruit.setTarget(null);// fix "if owner was other team and now same team und was target"
            if(recruitsTeam != null) recruit.setColor(recruitsTeam.getUnitColor());
        }
    }

    public static void removeRecruitFromTeam(String teamName, ServerPlayer player, ServerLevel level){
        List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(player.getUUID(), level);
        Team team = level.getScoreboard().getPlayerTeam(teamName);
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
