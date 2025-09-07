package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.util.DelayedExecutor;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsFactionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
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

public class FactionEvents {

    public MinecraftServer server;
    public static RecruitsFactionManager recruitsFactionManager;
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

        recruitsFactionManager = new RecruitsFactionManager();
        recruitsFactionManager.load(server.overworld());

        recruitsDiplomacyManager = new RecruitsDiplomacyManager();
        recruitsDiplomacyManager.load(server.overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitsFactionManager.save(server.overworld());
        recruitsDiplomacyManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event){
        recruitsFactionManager.save(server.overworld());
        recruitsDiplomacyManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof Player player){
            recruitsFactionManager.broadcastOnlinePlayersToAll(server.overworld());
            recruitsFactionManager.broadcastFactionsToPlayer(player);

            recruitsDiplomacyManager.broadcastDiplomacyMapToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(EntityLeaveLevelEvent event){
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof Player){
            DelayedExecutor.runLater(()-> recruitsFactionManager.broadcastOnlinePlayersToAll(server.overworld()),1000L);
        }
    }


    public static void createTeam(boolean menu, ServerPlayer serverPlayer, @NotNull ServerLevel level, String teamName, String playerName, ItemStack banner, ChatFormatting color, byte colorByte) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        int cost = RecruitsServerConfig.FactionCreationCost.get();
        if(banner == null) banner = Items.BROWN_BANNER.getDefaultInstance();
        CompoundTag nbt = banner.serializeNBT();

        if (team != null) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
        }
        else if (teamName.chars().count() > 32) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.teamname_to_long").withStyle(ChatFormatting.RED));
        }
        else if (teamName.isBlank() || teamName.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.noname").withStyle(ChatFormatting.RED));
        }
        else if(recruitsFactionManager.isNameInUse(teamName)) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED));
        }
        else if(!playerHasEnoughEmeralds(serverPlayer, cost) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED));
        }
        else if(recruitsFactionManager.isBannerBlank(banner) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.wrongbanner"));
        }
        else if (recruitsFactionManager.isBannerInUse(nbt) && menu) {
            serverPlayer.sendSystemMessage(Component.translatable("chat.recruits.team_creation.banner_exists").withStyle(ChatFormatting.RED));
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

            recruitsFactionManager.addTeam(teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT(), colorByte, newTeam.getColor());
            addPlayerToData(level, teamName, 1, playerName);

            List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
            int recruitCount = recruits.size();
            addNPCToData(level, teamName, recruitCount);

            addRecruitToTeam(recruits, newTeam, level);

            Main.LOGGER.info("The new Team " + teamName + " has been created by " + playerName + ".");

            recruitsFactionManager.save(server.overworld());
        }
    }

    public static void leaveTeam(boolean command, ServerPlayer player, String teamName, ServerLevel level, boolean fromLeader) {
        MinecraftServer server = level.getServer();
        String playerName = player.getName().getString();
        Team team = player.getTeam();

        if(team != null){
            if(teamName == null) teamName = team.getName();

            PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

            RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(teamName);

            boolean isLeader;
            if(recruitsFaction != null) {
                isLeader = recruitsFaction.getTeamLeaderUUID().equals(player.getUUID());
            }
            else
                isLeader = command;

            int recruits = getRecruitsOfPlayer(player.getUUID(), level).size();
            addNPCToData(level, teamName, -recruits);

            if(playerTeam != null){
                if(isLeader){
                    removeTeam(level, teamName);
                    return;
                }
                else {
                    ServerPlayer leaderOfTeam = server.getPlayerList().getPlayerByName(recruitsFaction.getTeamLeaderName());
                    if(!fromLeader && leaderOfTeam != null) leaderOfTeam.sendSystemMessage(PLAYER_LEFT_TEAM_LEADER(playerName));

                    server.getScoreboard().removePlayerFromTeam(playerName, playerTeam);
                    addPlayerToData(level,teamName,-1, playerName);

                }
                removeRecruitFromTeam(teamName, player, level);
                return;
            }
            else
                Main.LOGGER.error("Can not remove " + playerName + " from Team, because " + teamName + " does not exist!");

            serverSideUpdateTeam(level);
        }

        else {
            PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

            if(playerTeam != null){
                recruitsFactionManager.removeTeam(teamName);
            }

        }
        recruitsFactionManager.save(server.overworld());
    }

    public static void modifyTeam(ServerLevel level, String stringID, RecruitsFaction editedTeam, @Nullable ServerPlayer serverPlayer, int cost) {
        MinecraftServer server = level.getServer();
        RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(stringID);
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

        if(recruitsFaction != null && playerTeam != null){
            if(!recruitsFaction.getTeamLeaderUUID().equals(editedTeam.getTeamLeaderUUID())){
                notifyFactionMembers(level, recruitsFaction, 10, editedTeam.getTeamLeaderName());
                recruitsFaction.setTeamLeaderID(editedTeam.getTeamLeaderUUID());
                recruitsFaction.setTeamLeaderName(editedTeam.getTeamLeaderName());
            }

            if(!recruitsFaction.getTeamDisplayName().equals(editedTeam.getTeamDisplayName())){
                notifyFactionMembers(level, recruitsFaction, 11, editedTeam.getTeamDisplayName());
                recruitsFaction.setTeamDisplayName(editedTeam.getTeamDisplayName());
            }

            if(!recruitsFaction.getBanner().equals(editedTeam.getBanner())){
                notifyFactionMembers(level, recruitsFaction, 12, "");
                recruitsFaction.setBanner(editedTeam.getBanner());
            }

            recruitsFaction.setUnitColor(editedTeam.getUnitColor());
            recruitsFaction.setTeamColor(editedTeam.getTeamColor());
            recruitsFaction.setMaxNPCsPerPlayer(editedTeam.getMaxNPCsPerPlayer());

            playerTeam.setDisplayName(Component.literal(editedTeam.getTeamDisplayName()));
            playerTeam.setColor(ChatFormatting.getById(editedTeam.getTeamColor()));


            for(RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()){
                if(claim.getOwnerFaction().getStringID().equals(editedTeam.getStringID())){
                    claim.setOwnerFaction(editedTeam);
                }
            }

            ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(level);

            recruitsFactionManager.save(level);
        }
    }

    public static void notifyFactionMembers(ServerLevel level, RecruitsFaction recruitsFaction, int id, String notification){
        List<ServerPlayer> playersInTeam = FactionEvents.recruitsFactionManager.getPlayersInTeam(recruitsFaction.getStringID(), level);
        for (ServerPlayer teamPlayer : playersInTeam) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> teamPlayer), new MessageToClientSetDiplomaticToast(id, recruitsFaction, notification));
        }
    }

    public static void removeTeam(ServerLevel level, String teamName){
        MinecraftServer server = level.getServer();
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

        if(playerTeam != null){
            server.getScoreboard().removePlayerTeam(playerTeam);

            removeRecruitsFactionData(teamName);

            for(RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()){
                if(claim.getOwnerFaction().getStringID().equals(teamName)){
                    ClaimEvents.recruitsClaimManager.removeClaim(claim);
                }
            }

            ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(level);

            recruitsFactionManager.removeTeam(teamName);
        }
        recruitsFactionManager.save(server.overworld());
    }

    private static void removeRecruitsFactionData(String teamName) {
        recruitsFactionManager.removeTeam(teamName);
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

            RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(teamName);
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> playerToAdd), new MessageToClientSetDiplomaticToast(8, recruitsFaction));

            notifyFactionMembers(level, recruitsFaction, 9, playerToAdd.getName().getString());

            recruitsFactionManager.save(server.overworld());
            return true;
        }
        else
            Main.LOGGER.error("Can not add " + playerToAdd + " to Team, because " + teamName + " does not exist!");
        return false;
    }

    public static boolean isPlayerAlreadyAFactionLeader(ServerPlayer playerToCheck){
        for(RecruitsFaction recruitsFaction : recruitsFactionManager.getFactions()) {
            if(recruitsFaction.getTeamLeaderUUID().equals(playerToCheck.getUUID())){
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
        RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(teamName);;

        recruitsFaction.addPlayer(x);

        if(x > 0){ //actually adding the player therefor remove it from request list
            recruitsFaction.removeJoinRequest(namePlayerToAdd);
        }

        recruitsFactionManager.save(level);
    }
    public static void addNPCToData(ServerLevel level, String teamName, int x){
        RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(teamName);;

        if(recruitsFaction != null){
            recruitsFaction.addNPCs(x);
        }
        else Main.LOGGER.error("Could not modify recruits team: "+ teamName + ".Team does not exist.");
    }

    public static void sendJoinRequest(ServerLevel level, ServerPlayer player, String stringID) {
        RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(stringID);

        if(recruitsFaction != null){
            if(recruitsFaction.addPlayerAsJoinRequest(player.getName().getString())){
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> recruitsFactionManager.getTeamLeader(recruitsFaction, level)), new MessageToClientSetDiplomaticToast(7, recruitsFaction, player.getName().getString()));
                recruitsFactionManager.broadcastFactionsToAll(level);
            }
        }
        else Main.LOGGER.error("Could not add join request for "+ stringID + ".Team does not exist.");
    }

    public static void tryToRemoveFromTeam(Team team, ServerPlayer serverPlayer, ServerPlayer potentialRemovePlayer, ServerLevel level, String nameToRemove, boolean menu) {
        if (potentialRemovePlayer != null && team != null) {
            boolean isPlayerToRemove = potentialRemovePlayer.getName().getString().equals(nameToRemove);

            if (isPlayerToRemove) {
                FactionEvents.leaveTeam(false, potentialRemovePlayer, null, level, true);
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
                        delayedServerSideUpdate(level);
                    }
                    else if(command.contains("remove")){
                        String[] parts = command.split(" ");
                        String teamName = parts[2];
                        leaveTeam(true,sender, teamName, level, false);
                        sourceStack.sendSuccess(() -> Component.translatable("commands.team.remove.success", teamName), true);
                        event.setCanceled(true);
                        delayedServerSideUpdate(level);
                    }
                    else if(command.contains("join") || command.contains("leave")){
                        delayedServerSideUpdate(level);
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
                            recruitsFactionManager.removeTeam(teamName);

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
                            delayedServerSideUpdate(level);
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
                            delayedServerSideUpdate(level);
                        } else {
                            sourceStack.sendFailure(Component.translatable("argument.player.unknown"));
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    public void delayedServerSideUpdate(ServerLevel serverLevel){
        DelayedExecutor.runLater(()-> serverSideUpdateTeam(serverLevel), 500L);
    }

    private void createTeamConsole(CommandSourceStack sourceStack, ServerLevel  level, String teamName, String color, byte colorByte) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);

        ItemStack banner = Items.BROWN_BANNER.getDefaultInstance();
        if (team == null) {
            if (teamName.chars().count() <= 13) {
                if (!(teamName.isBlank() || teamName.isEmpty())) {
                    if (!recruitsFactionManager.isNameInUse(teamName)) {
                        Scoreboard scoreboard = server.getScoreboard();
                        PlayerTeam newTeam = scoreboard.addPlayerTeam(teamName);
                        newTeam.setDisplayName(Component.literal(teamName));

                        newTeam.setColor(Objects.requireNonNull(ChatFormatting.getByName(color)));
                        newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
                        newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

                        recruitsFactionManager.addTeam(teamName,new UUID(0,0),"none", banner.serializeNBT(), colorByte, newTeam.getColor());

                        Main.LOGGER.info("The new Team " + teamName + " has been created by console.");

                        recruitsFactionManager.save(server.overworld());
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

        recruitsFactionManager.save(level);
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
        RecruitsFaction recruitsFaction = recruitsFactionManager.getTeamByStringID(teamName);

        boolean flag = playerteam != null && level.getScoreboard().addPlayerToTeam(recruit.getStringUUID(), playerteam);
        if (!flag) {
            Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
        } else{
            recruit.setTarget(null);// fix "if owner was other team and now same team und was target"
            if(recruitsFaction != null) recruit.setColor(recruitsFaction.getUnitColor());
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
        Team recruitsFaction = recruit.getTeam();

        if(recruitsFaction != null && recruitsFaction.equals(team)){
            PlayerTeam recruitTeam = level.getScoreboard().getPlayerTeam(team.getName());
            if(recruitTeam != null) level.getScoreboard().removePlayerFromTeam(recruit.getStringUUID(), recruitTeam);
        }
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
}
