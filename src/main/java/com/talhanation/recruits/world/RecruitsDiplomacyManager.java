package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.network.MessageToClientSetDiplomaticToast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class RecruitsDiplomacyManager {
    public Map<String, Map<String, DiplomacyStatus>> diplomacyMap = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsDiplomacySaveData data = RecruitsDiplomacySaveData.get(level);
        diplomacyMap = data.getDiplomacyMap();
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
    }
    public void setRelation(String team, String otherTeam, DiplomacyStatus relation, ServerLevel level) {
        DiplomacyStatus currentRelation = this.getRelation(team, otherTeam);

        if (currentRelation == relation) {
            return;
        }
        
        diplomacyMap.computeIfAbsent(team, k -> new HashMap<>()).put(otherTeam, relation);
        this.notifyPlayersInTeam(team, otherTeam, relation, level);

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
        RecruitsTeam team = TeamEvents.recruitsTeamManager.getTeamByStringID(teamName);
        RecruitsTeam otherTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(otherTeamName);

        if(team != null && otherTeam != null){
            List<ServerPlayer> playersInTeam = TeamEvents.recruitsTeamManager.getPlayersInTeam(team.getStringID(), level);
            for (ServerPlayer player : playersInTeam) {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientSetDiplomaticToast(relation.getByteValue(), otherTeam));
            }

            List<ServerPlayer> playersInTeam2 = TeamEvents.recruitsTeamManager.getPlayersInTeam(otherTeam.getStringID(), level);
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
            Map<String, RecruitsDiplomacyManager.DiplomacyStatus> relations = new HashMap<>();

            for (String otherTeam : teamTag.getAllKeys()) {
                byte statusByte = teamTag.getByte(otherTeam);
                RecruitsDiplomacyManager.DiplomacyStatus status = RecruitsDiplomacyManager.DiplomacyStatus.fromByte(statusByte);
                relations.put(otherTeam, status);
            }

            diplomacyMap.put(team, relations);
        }

        return diplomacyMap;
    }


}

