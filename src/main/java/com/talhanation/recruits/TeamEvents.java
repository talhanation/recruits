package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsTeam;
import com.talhanation.recruits.world.RecruitsTeamSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TeamEvents {

    public static boolean isPlayerInATeam(Player player) {
        return player.getTeam() != null;
    }

    /*
    @SubscribeEvent
    public void onCommandEvent(CommandEvent event){
        String command = event.getParseResults().getReader().getString();
        Main.LOGGER.debug("Command: " + command);
    }
    */

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
    public static void createTeam(ServerPlayer serverPlayer, ServerLevel level, String teamName, String playerName, ItemStack banner) {
        MinecraftServer server = level.getServer();
        PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
        server.getScoreboard().getPlayerTeams();
        String createTeamCommand = "/team add " + teamName;
        String joinTeamCommand = "/team join " + teamName + " " + playerName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(serverPlayer.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), serverPlayer);
        int cost = 10;
        //Commands

        if (team == null) {
            if (!(teamName.isBlank() || teamName.isEmpty())) {
                if (!isNameInUse(level, teamName)) {
                    if (playerHasEnoughEmeralds(serverPlayer, cost)) {
                        if (!isBannerBlank(banner)) {
                            if (!isBannerInUse(level, banner.serializeNBT())) {
                                server.getCommands().performCommand(commandSourceStack, createTeamCommand);
                                server.getCommands().performCommand(commandSourceStack, joinTeamCommand);
                                doPayment(serverPlayer, cost);

                                saveDataToTeam(level, teamName, serverPlayer.getUUID(), serverPlayer.getScoreboardName(), banner.serializeNBT());
                                addPlayerToData(level, teamName, 1, playerName);
                                Main.LOGGER.debug("The Team " + teamName + " has been created by " + playerName + ".");
                            } else
                                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_exists"), serverPlayer.getUUID());
                        } else
                            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.wrongbanner"), serverPlayer.getUUID());
                    } else
                        serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noenough_money").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
                } else
                    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists"), serverPlayer.getUUID());
            } else
                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noname"), serverPlayer.getUUID());
        }else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists"), serverPlayer.getUUID());
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

        RecruitsTeamSavedData data = RecruitsTeamSavedData.get(level);
        RecruitsTeam recruitsTeam = data.getTeamByName(teamName);

        boolean isLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());

        String leaveTeamCommand = "/team leave " + playerName;
        String emptyTeam = "/team empty " + teamName;
        String removeTeam = "/team remove " + teamName;
        CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(player.getOnPos()), Vec2.ZERO, level, 2, playerName, new TextComponent(playerName), level.getServer(), player);

        if(isLeader){
            removeRecruitsTeamData(level, teamName);
            server.getCommands().performCommand(commandSourceStack, emptyTeam);
            server.getCommands().performCommand(commandSourceStack, removeTeam);

            data.getTeams().removeIf(team -> team.getTeamName().equals(teamName));
            data.setDirty();
        }
        else {
            server.getCommands().performCommand(commandSourceStack, leaveTeamCommand);
            addPlayerToData(level,teamName,-1, playerName);
        }
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

    public static ItemStack getCurrency(){
        ItemStack currencyItemStack;
        String str = RecruitsModConfig.RecruitCurrency.get();
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
    private static final TranslatableComponent PLAYER_REMOVED = new TranslatableComponent("chat.recruits.team_creation.removedPlayer");
}
