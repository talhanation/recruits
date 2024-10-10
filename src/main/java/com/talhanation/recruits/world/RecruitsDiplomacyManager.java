package com.talhanation.recruits.world;

import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
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

    public void setRelation(String team, String otherTeam, DiplomacyStatus relation) {
        diplomacyMap.computeIfAbsent(team, k -> new HashMap<>()).put(otherTeam, relation);
    }

    public void setAlliance(String team, String otherTeam) {
        setRelation(team, otherTeam, DiplomacyStatus.ALLY);
        setRelation(otherTeam, team, DiplomacyStatus.ALLY);
    }

    public void setNeutral(String team, String otherTeam) {
        setRelation(team, otherTeam, DiplomacyStatus.NEUTRAL);
        setRelation(otherTeam, team, DiplomacyStatus.NEUTRAL);
    }

    public void setEnemy(String team, String otherTeam) {
        setRelation(team, otherTeam, DiplomacyStatus.ENEMY);
        setRelation(otherTeam, team, DiplomacyStatus.ENEMY);
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
}

