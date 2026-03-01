package com.talhanation.recruits.world;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.MessageToClientSetDiplomaticToast;
import com.talhanation.recruits.network.MessageToClientUpdateTreaties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class RecruitsTreatyManager {

    // key: "factionA|factionB" (sorted) -> expiry time in milliseconds (real time)
    private final Map<String, Long> treaties = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsTreatySaveData data = RecruitsTreatySaveData.get(level);
        treaties.clear();
        treaties.putAll(data.getTreaties());
    }

    public void save(ServerLevel level) {
        RecruitsTreatySaveData data = RecruitsTreatySaveData.get(level);
        data.setTreaties(treaties);
        data.setDirty();
        broadcastTreatiesToAll(level);
    }

    private String makeKey(String factionA, String factionB) {
        String[] sorted = new String[]{factionA, factionB};
        Arrays.sort(sorted);
        return sorted[0] + "|" + sorted[1];
    }

    public boolean hasTreaty(String factionA, String factionB) {
        String key = makeKey(factionA, factionB);
        Long expiry = treaties.get(key);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    public long getTreatyExpiryMillis(String factionA, String factionB) {
        String key = makeKey(factionA, factionB);
        return treaties.getOrDefault(key, 0L);
    }

    public long getTreatyRemainingMillis(String factionA, String factionB) {
        long expiry = getTreatyExpiryMillis(factionA, factionB);
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public void addTreaty(String factionA, String factionB, int durationHours, ServerLevel level) {
        String key = makeKey(factionA, factionB);
        long expiry = System.currentTimeMillis() + (long) durationHours * 3600 * 1000;
        treaties.put(key, expiry);
        save(level);
        notifyTreatyPlayers(factionA, factionB, true, level);
    }

    /** Sets the treaty expiry to an exact millisecond timestamp (for admin commands with minute precision). */
    public void addTreatyRaw(String factionA, String factionB, long expiryMs, ServerLevel level) {
        String key = makeKey(factionA, factionB);
        treaties.put(key, expiryMs);
        save(level);
        notifyTreatyPlayers(factionA, factionB, true, level);
    }

    public void removeTreaty(String factionA, String factionB, ServerLevel level) {
        String key = makeKey(factionA, factionB);
        if (treaties.remove(key) != null) {
            save(level);
            notifyTreatyPlayers(factionA, factionB, false, level);
        }
    }

    public void tick(ServerLevel level) {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, Long> entry : treaties.entrySet()) {
            if (now >= entry.getValue()) {
                expired.add(entry.getKey());
            }
        }
        for (String key : expired) {
            treaties.remove(key);
            String[] parts = key.split("\\|", 2);
            if (parts.length == 2) {
                notifyTreatyPlayers(parts[0], parts[1], false, level);
            }
        }
        if (!expired.isEmpty()) {
            save(level);
        }
    }

    private void notifyTreatyPlayers(String factionAId, String factionBId, boolean established, ServerLevel level) {
        if (FactionEvents.recruitsFactionManager == null) return;
        RecruitsFaction factionA = FactionEvents.recruitsFactionManager.getFactionByStringID(factionAId);
        RecruitsFaction factionB = FactionEvents.recruitsFactionManager.getFactionByStringID(factionBId);
        if (factionA == null || factionB == null) return;

        int toastId = established ? 20 : 21;

        List<ServerPlayer> playersA = FactionEvents.recruitsFactionManager.getPlayersInTeam(factionAId, level);
        for (ServerPlayer player : playersA) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientSetDiplomaticToast(toastId, factionB));
        }

        List<ServerPlayer> playersB = FactionEvents.recruitsFactionManager.getPlayersInTeam(factionBId, level);
        for (ServerPlayer player : playersB) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientSetDiplomaticToast(toastId, factionA));
        }
    }

    public Map<String, Long> getTreaties() {
        return Collections.unmodifiableMap(treaties);
    }

    public void broadcastTreatiesToAll(ServerLevel level) {
        if (level == null) return;
        for (ServerPlayer player : level.players()) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientUpdateTreaties(treaties));
        }
    }

    public void broadcastTreatiesToPlayer(ServerPlayer player) {
        if (player == null) return;
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MessageToClientUpdateTreaties(treaties));
    }

    public static CompoundTag mapToNbt(Map<String, Long> treaties) {
        CompoundTag nbt = new CompoundTag();
        treaties.forEach(nbt::putLong);
        return nbt;
    }

    public static Map<String, Long> mapFromNbt(CompoundTag nbt) {
        Map<String, Long> map = new HashMap<>();
        for (String key : nbt.getAllKeys()) {
            map.put(key, nbt.getLong(key));
        }
        return map;
    }
}
