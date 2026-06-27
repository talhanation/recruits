package com.talhanation.recruits.client;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClientManager {
    public static List<RecruitsClaim> recruitsClaims = new ArrayList<>();
    public static Map<UUID, RecruitsClaim> activeSiegeClaims = new HashMap<>();
    public static List<RecruitsFaction> factions = new ArrayList<>();
    public static List<RecruitsGroup> groups = new ArrayList<>();
    public static RecruitsFaction ownFaction;
    public static Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap = new HashMap<>();
    public static Map<String, Long> treaties = new HashMap<>();
    // Key: embargoed player UUID, Value: comma-separated declaring team stringIDs
    public static Map<UUID, String> embargoMap = new HashMap<>();
    public static int configValueClaimCost;
    public static int configValueChunkCost;
    public static int configValueMaxClaimChunks = RecruitsClaim.DEFAULT_MAX_SIZE;
    public static boolean configValueCascadeClaimCost;
    public static ItemStack currencyItemStack;
    public static boolean isFactionEditingAllowed;
    public static boolean isFactionManagingAllowed;
    public static List<RecruitsPlayerInfo> onlinePlayers = new ArrayList<>();
    public static ItemStack currency;
    public static int factionCreationPrice;
    public static int factionMaxRecruitsPerPlayerConfigSetting;
    public static boolean configValueNobleNeedsVillagers;
    public static int availableRecruitsToHire;
    public static int formationSelection;
    public static int groupSelection;
    @Nullable
    public static RecruitsClaim currentClaim;
    public static boolean configValueIsClaimingAllowed;

    public static final Map<UUID, Component> groupAggroState = new HashMap<>();
    public static final Map<UUID, Component> groupMoveState = new HashMap<>();

    public static final Map<UUID, Set<String>> groupSpecialStates = new HashMap<>();

    public static void setGroupAggroState(UUID groupUUID, Component state) {
        if (groupUUID != null) groupAggroState.put(groupUUID, state);
    }

    public static void setGroupMoveState(UUID groupUUID, Component state) {
        if (groupUUID != null) groupMoveState.put(groupUUID, state);
    }

    public static Component getGroupAggroState(UUID groupUUID) {
        return groupUUID == null ? null : groupAggroState.get(groupUUID);
    }

    public static Component getGroupMoveState(UUID groupUUID) {
        return groupUUID == null ? null : groupMoveState.get(groupUUID);
    }

    public static void addGroupSpecialState(UUID groupUUID, String key) {
        if (groupUUID == null || key == null) return;
        groupSpecialStates.computeIfAbsent(groupUUID, k -> new LinkedHashSet<>()).add(key);
        saveSpecialStates();
    }

    public static void removeGroupSpecialState(UUID groupUUID, String key) {
        if (groupUUID == null || key == null) return;
        Set<String> set = groupSpecialStates.get(groupUUID);
        if (set != null) {
            set.remove(key);
            if (set.isEmpty()) groupSpecialStates.remove(groupUUID);
            saveSpecialStates();
        }
    }

    public static Set<String> getGroupSpecialStates(UUID groupUUID) {
        if (groupUUID == null) return java.util.Collections.emptySet();
        return groupSpecialStates.getOrDefault(groupUUID, java.util.Collections.emptySet());
    }
    public static boolean configFogOfWarEnabled;

    public static Map<String, RecruitsRoute> routesMap = new HashMap<>();
    public static boolean canPlayerHire;

    public static void rebuildActiveSieges() {
        activeSiegeClaims.clear();
        for (RecruitsClaim claim : recruitsClaims) {
            if (claim.isUnderSiege) {
                activeSiegeClaims.put(claim.getUUID(), claim);
            }
        }
    }

    public static void updateActiveSiege(RecruitsClaim claim) {
        if (claim == null) return;
        if (claim.isUnderSiege) {
            activeSiegeClaims.put(claim.getUUID(), claim);
        } else {
            activeSiegeClaims.remove(claim.getUUID());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static RecruitsDiplomacyManager.DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasTreaty(String factionA, String factionB) {
        String[] sorted = new String[]{factionA, factionB};
        java.util.Arrays.sort(sorted);
        String key = sorted[0] + "|" + sorted[1];
        Long expiry = treaties.get(key);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    @OnlyIn(Dist.CLIENT)
    public static long getTreatyRemainingMillis(String factionA, String factionB) {
        String[] sorted = new String[]{factionA, factionB};
        java.util.Arrays.sort(sorted);
        String key = sorted[0] + "|" + sorted[1];
        long expiry = treaties.getOrDefault(key, 0L);
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    @OnlyIn(Dist.CLIENT)
    public static RecruitsPlayerInfo getPlayerInfo() {
        Player player = Minecraft.getInstance().player;
        if (player != null) return new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownFaction);
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static RecruitsGroup getGroup(UUID groupUUID) {
        for (RecruitsGroup group : groups) {
            if (group.getUUID().equals(groupUUID)) return group;
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static RecruitsGroup getSelectedGroup() {
        if (groups != null && !groups.isEmpty()) {
            try { return groups.get(groupSelection); }
            catch (Exception e) { groupSelection = 0; return groups.get(0); }
        }
        return null;
    }

    public static void updateGroups() {
        Player player = Minecraft.getInstance().player;
        if (player == null || groups == null || groups.isEmpty()) return;

        List<AbstractRecruitEntity> recruits = player.level()
                .getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(100));
        recruits.removeIf(r -> !r.isEffectedByCommand(player.getUUID()));

        Map<UUID, Integer> groupCounts = new HashMap<>();
        for (AbstractRecruitEntity recruit : recruits) {
            UUID groupId = recruit.getGroup();
            if (groupId == null) continue;
            groupCounts.put(groupId, groupCounts.getOrDefault(groupId, 0) + 1);
        }

        for (RecruitsGroup group : ClientManager.groups) group.setCount(groupCounts.getOrDefault(group.getUUID(), 0));
        ClientManager.groups.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
    }

    @OnlyIn(Dist.CLIENT)
    public static void loadRoutes() {
        routesMap.clear();
        try {
            List<RecruitsRoute> loaded = RecruitsRoute.loadAllRoutes(getRoutesDirectory());
            for (RecruitsRoute route : loaded) routesMap.put(route.getId().toString(), route);
        } catch (IOException e) {
            // start with empty map
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void saveRoute(RecruitsRoute route) {
        try {
            route.saveToFile(getRoutesDirectory());
            routesMap.put(route.getId().toString(), route);
        } catch (IOException e) {
            // could not save
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void renameRoute(RecruitsRoute route, String newName) {
        route.deleteFile(getRoutesDirectory());
        route.setName(newName);
        saveRoute(route);
    }

    @OnlyIn(Dist.CLIENT)
    public static void deleteRoute(RecruitsRoute route) {
        routesMap.remove(route.getId().toString());
        route.deleteFile(getRoutesDirectory());
    }

    @OnlyIn(Dist.CLIENT)
    public static List<RecruitsRoute> getRoutesList() {
        return new ArrayList<>(routesMap.values());
    }

    private static File getRoutesDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "recruits/routes/" + WorldMapStorageId.detectCurrent());
    }

    private static File getSpecialStatesFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "recruits/groupstates/" + WorldMapStorageId.detectCurrent());
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "special_states.txt");
    }

    @OnlyIn(Dist.CLIENT)
    public static void loadSpecialStates() {
        groupSpecialStates.clear();
        File file = getSpecialStatesFile();
        if (!file.exists()) return;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                try {
                    UUID uuid = UUID.fromString(line.substring(0, eq).trim());
                    String[] keys = line.substring(eq + 1).split(",");
                    Set<String> set = new LinkedHashSet<>();
                    for (String k : keys) {
                        String key = k.trim();
                        if (!key.isEmpty()) set.add(key);
                    }
                    if (!set.isEmpty()) groupSpecialStates.put(uuid, set);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed line
                }
            }
        } catch (IOException e) {
            // start with empty map
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void saveSpecialStates() {
        File file = getSpecialStatesFile();
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file))) {
            for (Map.Entry<UUID, Set<String>> entry : groupSpecialStates.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                writer.write(entry.getKey().toString() + "=" + String.join(",", entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            // could not save
        }
    }
}