package com.talhanation.recruits.client;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
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
    public static int configValueClaimCost;
    public static int configValueChunkCost;
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

    // -------------------------------------------------------------------------
    // Route helpers
    // -------------------------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public static void loadRoutes() {
        routesMap.clear();
        try {
            List<RecruitsRoute> loaded = RecruitsRoute.loadAllRoutes(RecruitsRoute.getRoutesDirectory());
            for (RecruitsRoute route : loaded) routesMap.put(route.getId().toString(), route);
        } catch (IOException e) {
            // start with empty map
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void saveRoute(RecruitsRoute route) {
        try {
            route.saveToFile(RecruitsRoute.getRoutesDirectory());
            routesMap.put(route.getId().toString(), route);
        } catch (IOException e) {
            // could not save
        }
    }

    /**
     * Renames a route: deletes the old name-based file, then saves under the new name.
     */
    @OnlyIn(Dist.CLIENT)
    public static void renameRoute(RecruitsRoute route, String newName) {
        route.deleteFile(RecruitsRoute.getRoutesDirectory());
        route.setName(newName);
        saveRoute(route);
    }

    @OnlyIn(Dist.CLIENT)
    public static void deleteRoute(RecruitsRoute route) {
        routesMap.remove(route.getId().toString());
        route.deleteFile(RecruitsRoute.getRoutesDirectory());
    }

    @OnlyIn(Dist.CLIENT)
    public static List<RecruitsRoute> getRoutesList() {
        return new ArrayList<>(routesMap.values());
    }
}
