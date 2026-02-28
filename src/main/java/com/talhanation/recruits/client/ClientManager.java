package com.talhanation.recruits.client;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
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
    public static boolean configValueIsClaimingAllowed;

    public static Map<String, RecruitsRoute> routesMap = new HashMap<>();
    public static boolean canPlayerHire;

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

    @OnlyIn(Dist.CLIENT)
    public static RecruitsGroup getSelectedGroup(){
        if(groups != null && !groups.isEmpty()){
            try{
                return groups.get(groupSelection);
            }
            catch (Exception exception){
                groupSelection = 0;
                return groups.get(0);
            }
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

        for (RecruitsGroup group : ClientManager.groups) {
            int count = groupCounts.getOrDefault(group.getUUID(), 0);
            group.setCount(count);
        }

        ClientManager.groups.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
    }
}
