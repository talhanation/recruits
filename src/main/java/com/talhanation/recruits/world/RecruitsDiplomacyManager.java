package com.talhanation.recruits.world;

import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.MessageToClientSetDiplomaticToast;
import com.talhanation.recruits.network.MessageToClientUpdateDiplomacyList;
import com.talhanation.recruits.network.MessageToClientUpdateEmbargoes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class RecruitsDiplomacyManager {

    public Map<String, Map<String, DiplomacyStatus>> diplomacyMap = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsDiplomacySaveData data = RecruitsDiplomacySaveData.get(level);
        diplomacyMap = data.getDiplomacyMap();
        embargoMap = data.getEmbargoMap();
    }

    public void save(ServerLevel level) {
        RecruitsDiplomacySaveData data = RecruitsDiplomacySaveData.get(level);

        for (Map.Entry<String, Map<String, DiplomacyStatus>> entry : diplomacyMap.entrySet()) {
            String team = entry.getKey();
            Map<String, DiplomacyStatus> relations = entry.getValue();
            for (Map.Entry<String, DiplomacyStatus> relationEntry : relations.entrySet()) {
                data.setRelation(team, relationEntry.getKey(), relationEntry.getValue().getByteValue());
            }
        }

        data.setDirty();
        this.broadcastDiplomacyMapToAll(level);
    }

    public void setRelation(String team, String otherTeam, DiplomacyStatus relation, ServerLevel level){
        this.setRelation(team, otherTeam, relation, level, true);
    }

    public void setRelation(String team, String otherTeam, DiplomacyStatus relation, ServerLevel level, boolean notifyPlayers) {
        DiplomacyStatus currentRelation = this.getRelation(team, otherTeam);

        if (currentRelation == relation) return;

        DiplomacyEvent.RelationChanged relEvent =
                new DiplomacyEvent.RelationChanged(team, otherTeam, level, currentRelation, relation);
        if (MinecraftForge.EVENT_BUS.post(relEvent)) return;

        diplomacyMap.computeIfAbsent(team, k -> new HashMap<>()).put(otherTeam, relation);
        if(notifyPlayers) this.notifyPlayersInTeam(team, otherTeam, relation, level);

        this.save(level);
    }

    public DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, DiplomacyStatus.NEUTRAL);
    }

    public enum DiplomacyStatus {
        NEUTRAL((byte) 0),
        ALLY((byte) 1),
        ENEMY((byte) 2);

        private final byte byteValue;

        DiplomacyStatus(byte byteValue) {
            this.byteValue = byteValue;
        }

        public byte getByteValue() {
            return byteValue;
        }

        public static DiplomacyStatus fromByte(byte value) {
            return switch (value) {
                case 1 -> ALLY;
                case 2 -> ENEMY;
                default -> NEUTRAL;
            };
        }
    }

    public void notifyPlayersInTeam(String teamName, String otherTeamName, DiplomacyStatus relation, ServerLevel level) {
        RecruitsFaction team = FactionEvents.recruitsFactionManager.getFactionByStringID(teamName);
        RecruitsFaction otherTeam = FactionEvents.recruitsFactionManager.getFactionByStringID(otherTeamName);

        if(team != null && otherTeam != null){
            List<ServerPlayer> playersInTeam = FactionEvents.recruitsFactionManager.getPlayersInTeam(team.getStringID(), level);
            for (ServerPlayer player : playersInTeam) {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientSetDiplomaticToast(relation.getByteValue(), otherTeam));
            }
            List<ServerPlayer> playersInTeam2 = FactionEvents.recruitsFactionManager.getPlayersInTeam(otherTeam.getStringID(), level);
            for (ServerPlayer player : playersInTeam2) {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientSetDiplomaticToast(relation.getByteValue() + 4, team));
            }
        }
    }

    public static CompoundTag mapToNbt(Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap) {
        CompoundTag nbt = new CompoundTag();
        diplomacyMap.forEach((team, relations) -> {
            CompoundTag teamTag = new CompoundTag();
            relations.forEach((otherTeam, status) -> teamTag.putByte(otherTeam, status.getByteValue()));
            nbt.put(team, teamTag);
        });
        return nbt;
    }

    public static Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> mapFromNbt(CompoundTag nbt) {
        Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap = new HashMap<>();
        for (String team : nbt.getAllKeys()) {
            CompoundTag teamTag = nbt.getCompound(team);
            Map<String, DiplomacyStatus> relations = new HashMap<>();
            for (String otherTeam : teamTag.getAllKeys()) {
                byte statusByte = teamTag.getByte(otherTeam);
                relations.put(otherTeam, DiplomacyStatus.fromByte(statusByte));
            }
            diplomacyMap.put(team, relations);
        }
        return diplomacyMap;
    }

    public void broadcastDiplomacyMapToPlayer(Player player) {
        if (player == null) return;
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                new MessageToClientUpdateDiplomacyList(diplomacyMap));
    }

    public void broadcastDiplomacyMapToAll(ServerLevel serverLevel) {
        if (serverLevel == null) return;
        for(ServerPlayer serverPlayer : serverLevel.players()){
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> serverPlayer),
                    new MessageToClientUpdateDiplomacyList(diplomacyMap));
        }
    }

    // -------------------------------------------------------------------------
    // Embargo
    // Key:   embargoed player UUID
    // Value: comma-separated list of team stringIDs that declared the embargo
    // -------------------------------------------------------------------------

    public Map<UUID, String> embargoMap = new HashMap<>();

    public void addEmbargo(UUID embargoedPlayerUUID, String declaringTeamID, ServerLevel level) {
        String current = embargoMap.get(embargoedPlayerUUID);
        if (current == null || current.isEmpty()) {
            embargoMap.put(embargoedPlayerUUID, declaringTeamID);
        } else if (!current.contains(declaringTeamID)) {
            embargoMap.put(embargoedPlayerUUID, current + "," + declaringTeamID);
        }

        // notify the embargoed player via toast
        RecruitsFaction declaringFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(declaringTeamID);
        if (declaringFaction != null) {
            ServerPlayer embargoedPlayer = level.getServer().getPlayerList().getPlayer(embargoedPlayerUUID);
            if (embargoedPlayer != null) {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> embargoedPlayer),
                        new MessageToClientSetDiplomaticToast(30, declaringFaction));
            }
        }

        saveEmbargo(level);
    }

    public void removeEmbargo(UUID embargoedPlayerUUID, String declaringTeamID, ServerLevel level) {
        String current = embargoMap.get(embargoedPlayerUUID);
        if (current == null) return;

        List<String> teams = new ArrayList<>(Arrays.asList(current.split(",")));
        teams.remove(declaringTeamID);

        if (teams.isEmpty()) {
            embargoMap.remove(embargoedPlayerUUID);
        } else {
            embargoMap.put(embargoedPlayerUUID, String.join(",", teams));
        }

        // notify the embargoed player via toast
        RecruitsFaction declaringFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(declaringTeamID);
        if (declaringFaction != null) {
            ServerPlayer embargoedPlayer = level.getServer().getPlayerList().getPlayer(embargoedPlayerUUID);
            if (embargoedPlayer != null) {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> embargoedPlayer),
                        new MessageToClientSetDiplomaticToast(31, declaringFaction));
            }
        }

        saveEmbargo(level);
    }

    public boolean hasEmbargo(UUID embargoedPlayerUUID, String declaringTeamID) {
        String embargoes = embargoMap.get(embargoedPlayerUUID);
        return embargoes != null && Arrays.asList(embargoes.split(",")).contains(declaringTeamID);
    }

    public String getEmbargoedTeams(UUID embargoedPlayerUUID) {
        return embargoMap.getOrDefault(embargoedPlayerUUID, "");
    }

    public void saveEmbargo(ServerLevel level) {
        RecruitsDiplomacySaveData data = RecruitsDiplomacySaveData.get(level);
        data.setEmbargoMap(new HashMap<>(embargoMap));
        data.setDirty();
        broadcastEmbargoesToAll(level);
    }

    public void broadcastEmbargoesToPlayer(Player player) {
        if (player == null) return;
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                new MessageToClientUpdateEmbargoes(embargoMap));
    }

    public void broadcastEmbargoesToAll(ServerLevel serverLevel) {
        if (serverLevel == null) return;
        for(ServerPlayer serverPlayer : serverLevel.players()){
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> serverPlayer),
                    new MessageToClientUpdateEmbargoes(embargoMap));
        }
    }

    public static CompoundTag embargoMapToNbt(Map<UUID, String> map) {
        CompoundTag nbt = new CompoundTag();
        map.forEach((uuid, teamIDs) -> {
            if (!teamIDs.isEmpty()) {
                nbt.putString(uuid.toString(), teamIDs);
            }
        });
        return nbt;
    }

    public static Map<UUID, String> embargoMapFromNbt(CompoundTag nbt) {
        Map<UUID, String> map = new HashMap<>();
        for (String key : nbt.getAllKeys()) {
            String csv = nbt.getString(key);
            if (!csv.isEmpty()) {
                map.put(UUID.fromString(key), csv);
            }
        }
        return map;
    }
}
