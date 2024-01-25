package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsTeam;
import com.talhanation.recruits.world.RecruitsTeamSavedData;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class TeamEvents {

    public MinecraftServer server;

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
    public static void openTeamListScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return new TextComponent("team_list_screen");
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
                    return new TextComponent("team_inspection_screen");
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

    public static void openTeamMainScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return new TextComponent("team_main_screen");
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
                    return new TextComponent("team_add_player_screen");
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
    public static void createTeam(ServerPlayer serverPlayer, @NotNull ServerLevel level, String teamName, String playerName, ItemStack banner, String color) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        int cost = 10;

        if (team == null) {
            if (teamName.chars().count() <= 13) {
                if (!(teamName.isBlank() || teamName.isEmpty())) {
                    if (!isNameInUse(level, teamName)) {
                        if (playerHasEnoughEmeralds(serverPlayer, cost)) {
                            if (!isBannerBlank(banner)) {
                                if (!isBannerInUse(level, banner.serializeNBT())) {
                                    Scoreboard scoreboard = server.getScoreboard();
                                    PlayerTeam newTeam = scoreboard.addPlayerTeam(teamName);
                                    newTeam.setDisplayName(new TextComponent(teamName));

                                    newTeam.setColor(Objects.requireNonNull(ChatFormatting.getByName(color)));
                                    newTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
                                    newTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());

                                    server.getScoreboard().addPlayerToTeam(playerName, newTeam);
                                    //TeamCommand
                                    doPayment(serverPlayer, cost);

                                    saveDataToTeam(level, teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT());
                                    addPlayerToData(level, teamName, 1, playerName);

                                    List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
                                    int recruitCount = recruits.size();
                                    addNPCToData(level, teamName, recruitCount);

                                    addRecruitToTeam(recruits, newTeam, level);


                                    Main.LOGGER.info("The new Team " + teamName + " has been created by " + playerName + ".");
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
    }

    private static boolean isNameInUse(ServerLevel level, String teamName) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        List<RecruitsTeam> list = data.getTeams().stream().toList();
        boolean equ = false;
        for(RecruitsTeam recruitsTeam : list){
            equ = recruitsTeam.getTeamName().toLowerCase().strip().equals(teamName.toLowerCase());
        }
        return equ;
    }

    public static void saveDataToTeam(ServerLevel level, String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);

        data.addTeam(teamName, leaderUUID, leaderName, bannerNbt);
        data.setDirty();
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

        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);

        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        boolean isLeader;
        if(recruitsTeam != null) {
            isLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());
        }
        else
            isLeader = false;

        int recruits = getRecruitsOfPlayer(player.getUUID(), level).size();
        addNPCToData(level, teamName, -recruits);

        if(playerTeam != null){
            if(isLeader){
                server.getScoreboard().removePlayerTeam(playerTeam);
                removeRecruitsTeamData(data, teamName);
            }
            else {
                server.getScoreboard().removePlayerFromTeam(playerName, playerTeam);
                addPlayerToData(level,teamName,-1, playerName);
            }

            removeRecruitFromTeam(player, level);
        }
        else
            Main.LOGGER.error("Can not remove " + playerName + " from Team, because " + teamName + " does not exist!");

        serverSideUpdateTeam(level);
    }

    private static void removeRecruitsTeamData(RecruitsTeamSavedData data,  String teamName) {
        data.getTeams().removeIf(team -> team.getTeamName().equals(teamName));
        data.setDirty();
    }

    public static void addPlayerToTeam(ServerPlayer player, ServerLevel level, String teamName, String namePlayerToAdd) {
        MinecraftServer server = level.getServer();
        ServerPlayer playerToAdd = server.getPlayerList().getPlayerByName(namePlayerToAdd);
        PlayerTeam playerTeam = server.getScoreboard().getPlayerTeam(teamName);
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);

        for(RecruitsTeam recruitsTeam : data.getTeams()) {
            if(recruitsTeam.getTeamLeaderUUID().equals(player.getUUID())){
                player.sendMessage(CAN_NOT_ADD_OTHER_LEADER(), player.getUUID());
                return;
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
        }
        else
            Main.LOGGER.error("Can not add " + playerToAdd + " to Team, because " + teamName + " does not exist!");
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

        if(recruitsTeam != null){
            recruitsTeam.addNPCs(x);
            data.setDirty();
        }
        else Main.LOGGER.error("Could not modify recruits team: "+ teamName + ".Team does not exist.");
    }

    public static void sendJoinRequest(ServerLevel level, ServerPlayer player, String teamName) {
        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        if(recruitsTeam != null){
            recruitsTeam.addPlayerAsJoinRequest(player.getName().getString());
            data.setDirty();
        }
        else Main.LOGGER.error("Could not add join request for "+ teamName + ".Team does not exist.");
    }

    public static void tryToRemoveFromTeam(ServerPlayer serverPlayer, ServerPlayer potentialRemovePlayer, ServerLevel level, String nameToRemove) {
        if (potentialRemovePlayer != null) {
            boolean isPlayerToRemove = potentialRemovePlayer.getName().getString().equals(nameToRemove);

            if (isPlayerToRemove) {
                TeamEvents.leaveTeam(potentialRemovePlayer, level);

                potentialRemovePlayer.sendMessage(PLAYER_REMOVED, potentialRemovePlayer.getUUID());
                serverPlayer.sendMessage(REMOVE_PLAYER_LEADER(potentialRemovePlayer.getDisplayName().getString()), serverPlayer.getUUID());

                List<AbstractRecruitEntity> recruits = getRecruitsOfPlayer(serverPlayer.getUUID(), level);
                int recruitCount = recruits.size();

                addNPCToData(level, serverPlayer.getTeam().getName(), -recruitCount);
                removeRecruitFromTeam(recruits, serverPlayer.getTeam(), level);
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

    public static void assignToTeamMate(ServerPlayer oldOwner, AbstractRecruitEntity recruit) {
        ServerLevel level = oldOwner.getLevel();
        Team team = oldOwner.getTeam();

        if(team != null){
            Collection<String> list = team.getPlayers().stream().toList();
            List<ServerPlayer> playerList = level.getEntitiesOfClass(ServerPlayer.class, oldOwner.getBoundingBox().inflate(32D));

            playerList.sort(Comparator.comparing(serverPlayer -> serverPlayer.distanceTo(oldOwner)));
            playerList.remove(0);// 0 is oldOwner

            boolean playerNotFound = false;
            ServerPlayer newOwner = null;
            if(!playerList.isEmpty()) newOwner = playerList.get(0);


            if(newOwner != null){
                if(list.contains(newOwner.getName().getString())){
                    recruit.disband(oldOwner, true);
                    if(!recruit.hire(newOwner)){

                    };
                }
                else
                    playerNotFound = true;
            }
            else
                playerNotFound = true;

            if(playerNotFound) oldOwner.sendMessage(new TranslatableComponent("chat.recruits.team.assignNewOwnerNotFound"), oldOwner.getUUID());
        }
        else
            oldOwner.sendMessage(new TranslatableComponent("chat.recruits.team.assignNewOwnerNoTeam"), oldOwner.getUUID());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        ServerLevel level = this.server.overworld();

        Collection<PlayerTeam> list =  level.getScoreboard().getPlayerTeams();
        for(PlayerTeam playerTeam : list){
            playerTeam.setAllowFriendlyFire(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamFriendlyFireSetting.get());
            playerTeam.setSeeFriendlyInvisibles(RecruitsServerConfig.GlobalTeamSetting.get() && RecruitsServerConfig.GlobalTeamSeeFriendlyInvisibleSetting.get());
        }
    }
    @SubscribeEvent
    public void onPlayerTypeCommandEvent(CommandEvent event){
        if (event.getParseResults() != null) {
            String command = event.getParseResults().getReader().getString();
            Entity sender = event.getParseResults().getContext().build(command).getSource().getEntity();

            if(command.contains("team") && (command.contains("join") || command.contains("add") || command.contains("remove") || command.contains("leave"))){
                if(RecruitsServerConfig.DisableVanillaTeamCommands.get()) {
                    event.setCanceled(true);

                    if(sender instanceof ServerPlayer serverPlayer){
                        serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.isDisabledInConfig").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
                        serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.canEnableInConfig"), serverPlayer.getUUID());
                    }
                    return;
                }
                else
                if(sender instanceof ServerPlayer serverPlayer) serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.warnVanillaCommand").withStyle(ChatFormatting.RED), serverPlayer.getUUID());


                ServerLevel level = this.server.overworld();
                if(command.contains("join") || command.contains("leave") || command.contains("remove")){
                    serverSideUpdateTeam(level);
                }
            }
        }
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

        boolean flag = playerteam != null && level.getScoreboard().addPlayerToTeam(recruit.getStringUUID(), playerteam);
        if (!flag) {
            Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
        } else
            recruit.setTarget(null);// fix "if owner was other team and now same team und was target"
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
