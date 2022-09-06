package com.talhanation.recruits;

import com.talhanation.recruits.inventory.TeamCreationContainer;
import com.talhanation.recruits.network.MessageOpenTeamCreationScreen;
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
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public class TeamEvents {

    public static boolean isPlayerInTeam(Player player) {
        return player.getTeam() == null;
    }

    public static boolean isPlayerTeamLeader(Player player, PlayerTeam team) {
        RecruitsTeamSavedData data = new RecruitsTeamSavedData();

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
            if (!isBannerBlank(banner)) {
                if (!isBannerInUse(level, banner.serializeNBT())) {
                    server.getCommands().performCommand(commandSourceStack, createTeamCommand);
                    server.getCommands().performCommand(commandSourceStack, joinTeamCommand);
                    AssassinEvents.doPayment(serverPlayer, cost);

                    saveDataToTeam(level, teamName, serverPlayer.getUUID(), banner.serializeNBT());
                    Main.LOGGER.debug("A new Team has been created: " + teamName);
                } else
                    serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_exists"), serverPlayer.getUUID());
            }
            else
                serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.banner_is_blank"), serverPlayer.getUUID());

        }
        else
            serverPlayer.sendMessage(new TranslatableComponent("chat.recruits.team_creation.team_exists").withStyle(ChatFormatting.RED), serverPlayer.getUUID());
    }

    public static void saveDataToTeam(ServerLevel level, String teamName, UUID leaderUUID, CompoundTag bannerNbt) {
        DimensionDataStorage storage = level.getDataStorage();
        RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_"+ teamName + "_data");

        RecruitsTeamSavedData.setTeam(teamName);
        RecruitsTeamSavedData.setTeamLeader(leaderUUID);
        RecruitsTeamSavedData.setBanner(bannerNbt);

        /*
        Main.LOGGER.debug("--------------");
        Main.LOGGER.debug("saveDataToTeam Team:" + data.getTeam());
        Main.LOGGER.debug("saveDataToTeam TeamLeader:" + data.getTeamLeader());
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
            RecruitsTeamSavedData data = storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, "recruits_"+ teamName + "_data");
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
}
