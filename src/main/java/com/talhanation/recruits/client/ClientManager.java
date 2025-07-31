package com.talhanation.recruits.client;

import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientManager {
    public static List<RecruitsClaim> recruitsClaims = new ArrayList<>();
    public static List<RecruitsTeam> teams = new ArrayList<>();
    public static Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap = new HashMap<>();
    public static int configValueClaimCost;
    public static int configValueChunkCost;
    public static boolean configValueCascadeClaimCost;
    public static ItemStack currencyItemStack;
    public static RecruitsDiplomacyManager.DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL);
    }
}
