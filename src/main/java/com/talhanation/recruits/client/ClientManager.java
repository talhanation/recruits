package com.talhanation.recruits.client;

import com.talhanation.recruits.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

public class ClientManager {
    public static List<RecruitsClaim> recruitsClaims = new ArrayList<>();
    public static List<RecruitsFaction> factions = new ArrayList<>();
    public static List<RecruitsGroup> groups = new ArrayList<>();
    public static RecruitsFaction ownFaction;
    public static Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap = new HashMap<>();
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

    @OnlyIn(Dist.CLIENT)
    public static RecruitsDiplomacyManager.DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL);
    }
    @OnlyIn(Dist.CLIENT)
    public static RecruitsPlayerInfo getPlayerInfo(){
        Player player = Minecraft.getInstance().player;
        if(player != null){
            return new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownFaction);
        }
       return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static RecruitsGroup getGroup(UUID groupUUID){
        for(RecruitsGroup group : groups){
            if(group.getUUID().equals(groupUUID)) return group;
        }

        return null;
    }
}
