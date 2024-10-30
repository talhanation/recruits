package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.MessageToClientSetToast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class RecruitsDiplomacyManager {
    private Map<String, Map<String, DiplomacyStatus>> diplomacyMap = new HashMap<>();

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

    public DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, DiplomacyStatus.NEUTRAL);
    }

    public void setRelation(String team, String otherTeam, DiplomacyStatus relation, ServerLevel level) {
        diplomacyMap.computeIfAbsent(team, k -> new HashMap<>()).put(otherTeam, relation);
        this.notifyPlayersInTeam(team, otherTeam, relation, level);
    }

    public Map<String, DiplomacyStatus> getAllRelations(String team) {
        return diplomacyMap.getOrDefault(team, new HashMap<>());
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

    private void notifyPlayersInTeam(String team, String otherTeam, DiplomacyStatus relation, ServerLevel level) {
        List<ServerPlayer> playersInTeam = getPlayersInTeam(team, level);
        for (ServerPlayer player : playersInTeam) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientSetToast(relation.getByteValue(), team));
        }

        List<ServerPlayer> playersInTeam2 = getPlayersInTeam(otherTeam, level);
        for (ServerPlayer player : playersInTeam2) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientSetToast(relation.getByteValue() + 4, otherTeam));
        }
    }

    private List<ServerPlayer> getPlayersInTeam(String team, ServerLevel level) {
        PlayerTeam playerTeam = level.getScoreboard().getPlayersTeam(team);
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

    public static CompoundTag mapToNbt(Map<String, DiplomacyStatus> map) {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();

        for (Map.Entry<String, DiplomacyStatus> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Team", entry.getKey());
            entryTag.putByte("Status", entry.getValue().getByteValue());
            list.add(entryTag);
        }
        nbt.put("DiplomacyMap", list);
        return nbt;
    }

    public static Map<String, DiplomacyStatus> mapFromNbt(CompoundTag nbt) {
        Map<String, DiplomacyStatus> map = new HashMap<>();
        ListTag list = nbt.getList("DiplomacyMap", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            String team = entryTag.getString("Team");
            DiplomacyStatus status = DiplomacyStatus.fromByte(entryTag.getByte("Status"));
            map.put(team, status);
        }
        return map;
    }
}

