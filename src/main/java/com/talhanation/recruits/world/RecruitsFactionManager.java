package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.MessageToClientUpdateFactions;
import com.talhanation.recruits.network.MessageToClientUpdateOnlinePlayers;
import com.talhanation.recruits.network.MessageToClientUpdateOwnFaction;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class RecruitsFactionManager {
    private final Map<String, RecruitsFaction> teams = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsTeamSaveData data = RecruitsTeamSaveData.get(level);
        teams.clear();
        teams.putAll(data.getTeams());

        teams.values().forEach(this::loadConfig);
    }

    public void loadConfig(RecruitsFaction team) {
        team.maxPlayers = RecruitsServerConfig.MaxPlayersInFaction.get();
        team.maxNPCs = RecruitsServerConfig.MaxNPCsInFaction.get();
    }

    public void save(ServerLevel level) {
        RecruitsTeamSaveData data = RecruitsTeamSaveData.get(level);
        data.setTeams(teams);
        data.setDirty();

        broadcastOwnFactionToAll(level);
        broadcastFactionsToAll(level);
        broadcastOnlinePlayersToAll(level);
    }

    public Collection<RecruitsFaction> getFactions() {
        return teams.values();
    }

    @Nullable
    public RecruitsFaction getFactionByStringID(String stringID) {
        return teams.get(stringID);
    }

    public List<ServerPlayer> getPlayersInTeam(String stringID, ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam playerTeam = scoreboard.getPlayerTeam(stringID);

        List<ServerPlayer> list = new ArrayList<>();

        if(playerTeam != null){
            for(ServerPlayer p : level.players()){
                if(playerTeam.getPlayers().contains(p.getName().getString())){
                    list.add(p);
                }
            }
        }

        return list;
    }
    @Nullable
    public ServerPlayer getTeamLeader(RecruitsFaction recruitsFaction, ServerLevel level) {
        for(ServerPlayer p : level.players()){
            if(p.getUUID().equals(recruitsFaction.getTeamLeaderUUID())){
                return p;
            }
        }
        return null;
    }

    public void addTeam(String teamName, String teamDisplayName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt, byte color, ChatFormatting teamColor) {
        RecruitsFaction recruitsFaction = new RecruitsFaction();
        recruitsFaction.setStringID(teamName);
        recruitsFaction.setTeamDisplayName(teamDisplayName);
        recruitsFaction.setTeamLeaderID(leaderUUID);
        recruitsFaction.setTeamLeaderName(leaderName);
        recruitsFaction.setBanner(bannerNbt);
        recruitsFaction.setUnitColor(color);
        recruitsFaction.setTeamColor(teamColor.getId());
        recruitsFaction.setMaxNPCsPerPlayer(RecruitsServerConfig.MaxRecruitsForPlayer.get());
        recruitsFaction.setMaxPlayers(RecruitsServerConfig.MaxPlayersInFaction.get());
        recruitsFaction.setMaxNPCs(RecruitsServerConfig.MaxNPCsInFaction.get());
        teams.put(teamName, recruitsFaction);
    }
    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }

    public static boolean isNameInUse(String teamName, List<RecruitsFaction> factions) {
        boolean equ = false;
        for(RecruitsFaction recruitsFaction : factions){
            equ = recruitsFaction.getStringID().toLowerCase().strip().equals(teamName.toLowerCase());
        }
        return equ;
    }

    public static boolean isDisplayNameInUse(String displayName, List<RecruitsFaction> factions) {
        boolean equ = false;
        for(RecruitsFaction recruitsFaction : factions){
            equ = recruitsFaction.getTeamDisplayName().toLowerCase().strip().equals(displayName.toLowerCase());
        }
        return equ;
    }

    public static boolean isBannerInUse(CompoundTag bannerNbt, List<RecruitsFaction> factions){
        boolean inUse = false;
        if(bannerNbt != null){
            for(RecruitsFaction recruitsFaction : factions){
                inUse = bannerNbt.equals(recruitsFaction.getBanner());
            }
        }
        return inUse;
    }
    public static boolean isBannerBlank(ItemStack itemStack){
        CompoundTag compoundtag = BlockItem.getBlockEntityData(itemStack);
        return compoundtag == null || !compoundtag.contains("Patterns");
    }
    public boolean isDisplayNameInUse(String displayName){
        return isDisplayNameInUse(displayName, getFactions().stream().toList());
    }
    public boolean isNameInUse(String factionName){
        return isNameInUse(factionName, getFactions().stream().toList());
    }

    public boolean isBannerInUse(CompoundTag bannerNbt){
        return isBannerInUse(bannerNbt, getFactions().stream().toList());
    }

    public boolean canPlayerJoin(RecruitsFaction recruitsFaction){
        int config = RecruitsServerConfig.MaxPlayersInFaction.get();
        if(config == 0) {
            return true;
        }
        else
            return config <= recruitsFaction.getPlayers();
    }

    public boolean canRecruitJoin(RecruitsFaction recruitsFaction){
        int config = RecruitsServerConfig.MaxPlayersInFaction.get();
        if(config == 0){
            return true;
        }
        else
            return RecruitsServerConfig.MaxNPCsInFaction.get() < recruitsFaction.getNPCs();
    }

    public void broadcastOnlinePlayersToPlayer(ServerPlayer serverPlayer , ServerLevel serverLevel) {
        if (serverPlayer == null) return;

        List<RecruitsPlayerInfo> playerInfoList = new ArrayList<>();

        for(ServerPlayer onlinePlayer : serverLevel.players()){
            if(onlinePlayer.getTeam() != null){
                RecruitsFaction faction = this.getFactionByStringID(onlinePlayer.getTeam().getName());
                playerInfoList.add(new RecruitsPlayerInfo(onlinePlayer.getUUID(), onlinePlayer.getScoreboardName(), faction));
            }
            else
                playerInfoList.add(new RecruitsPlayerInfo(onlinePlayer.getUUID(), onlinePlayer.getScoreboardName()));
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> serverPlayer),
                new MessageToClientUpdateOnlinePlayers(playerInfoList));
    }

    public void broadcastFactionsToAll(ServerLevel serverLevel) {
        if (serverLevel == null) return;

        for(ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()){
            broadcastFactionsToPlayer(serverPlayer);
        }
    }

    public void broadcastToFactionPlayers(String factionID, ServerLevel serverLevel) {
        if (serverLevel == null) return;

        RecruitsFaction faction = this.getFactionByStringID(factionID);
        if (faction == null) return;

        PlayerTeam team = serverLevel.getScoreboard().getPlayerTeam(factionID);
        if (team == null) return;

        for (String playerName : team.getPlayers()) {
            ServerPlayer player = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayerByName(playerName);

            if (player != null) {
                this.broadcastOwnFactionToPlayer(player);
            }
        }
    }

    private void broadcastOwnFactionToPlayer(Player player) {
        String teamName = "";
        if(player.getTeam() != null){
            teamName = player.getTeam().getName();
        }

        RecruitsFaction faction = this.getFactionByStringID(teamName);

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                new MessageToClientUpdateOwnFaction(faction));
    }
    public void broadcastFactionsToPlayer(Player player) {
        if (player == null) return;

        String factionID = null;
        if(player.getTeam() != null){
            factionID = player.getTeam().getName();
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                new MessageToClientUpdateFactions(this.getFactions().stream().toList(),
                        factionID,
                        RecruitsServerConfig.ShouldFactionEditingBeAllowed.get(),
                        RecruitsServerConfig.ShouldFactionManagingBeAllowed.get(),
                        RecruitsServerConfig.FactionCreationCost.get(),
                        RecruitsServerConfig.MaxRecruitsForPlayer.get(),
                        FactionEvents.getCurrency()
                        ));

        this.broadcastOwnFactionToPlayer(player);
    }

    public void broadcastOnlinePlayersToAll(ServerLevel serverLevel) {
        if (serverLevel == null) return;

        List<RecruitsPlayerInfo> playerInfoList = new ArrayList<>();

        for(ServerPlayer onlinePlayer : serverLevel.getServer().getPlayerList().getPlayers()){
            if(onlinePlayer.getTeam() != null){
                RecruitsFaction faction = this.getFactionByStringID(onlinePlayer.getTeam().getName());
                playerInfoList.add(new RecruitsPlayerInfo(onlinePlayer.getUUID(), onlinePlayer.getScoreboardName(), faction));
            }
            else
                playerInfoList.add(new RecruitsPlayerInfo(onlinePlayer.getUUID(), onlinePlayer.getScoreboardName()));
        }

        for(ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()){
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> serverPlayer),
                    new MessageToClientUpdateOnlinePlayers(playerInfoList));
        }
    }

    public void broadcastOwnFactionToAll(ServerLevel serverLevel) {
        if (serverLevel == null) return;

        for(ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()){
            broadcastOwnFactionToPlayer(serverPlayer);
        }
    }
}

