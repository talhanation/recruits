package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageToClientUpdateGroups;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class RecruitsGroupsManager {
    private final List<RecruitsGroup> groups = new ArrayList<>();
    private final Map<UUID, UUID> redirects = new HashMap<>();
    public void load(ServerLevel level) {
        RecruitsGroupsSaveData data = RecruitsGroupsSaveData.get(level);
        groups.clear();
        groups.addAll(data.getAllGroups());

        redirects.clear();
        redirects.putAll(data.getRedirects());
    }

    public void save(ServerLevel level) {
        RecruitsGroupsSaveData data = RecruitsGroupsSaveData.get(level);
        data.setAllGroups(groups);
        data.setRedirects(redirects);

        data.setDirty();
    }
    public void addOrUpdateGroup(ServerLevel level, ServerPlayer player, RecruitsGroup group) {
        if (group == null || level == null) return;

        removeGroup(group);
        if(!group.removed) groups.add(group);

        save(level);

        broadCastGroupsToPlayer(player);
    }

    public void removeGroup(RecruitsGroup group) {
        if (group == null) return;

        groups.removeIf(saved -> saved.getUUID().equals(group.getUUID()));

    }

    @Nullable
    public RecruitsGroup getGroup(UUID groupUUID) {
        for(RecruitsGroup group : groups){
            if(group.getUUID().equals(groupUUID)) return group;
        }
        return null;
    }

    @Nullable
    public void increaseSize(UUID groupUUID, ServerLevel serverLevel) {
        RecruitsGroup selected = null;
        for(RecruitsGroup group : groups){
            if(group.getUUID().equals(groupUUID)){
                group.increaseSize();
                selected = group;
                break;
            }
        }

        save(serverLevel);

        if(selected == null) return;
        broadCastGroupsToPlayer(serverLevel, selected.getPlayerUUID());
    }

    @Nullable
    public void decreaseSize(UUID groupUUID, ServerLevel serverLevel) {
        RecruitsGroup selected = null;
        for(RecruitsGroup group : groups){
            if(group.getUUID().equals(groupUUID)){
                group.decreaseSize();
                selected = group;
                break;
            }
        }

        save(serverLevel);

        if(selected == null) return;
        broadCastGroupsToPlayer(serverLevel, selected.getPlayerUUID());
    }

    @Nullable
    public List<RecruitsGroup> getPlayerGroups(Player player) {
        List<RecruitsGroup> list = new ArrayList<>();
        for(RecruitsGroup group : groups){
            if(group.getPlayerUUID().equals(player.getUUID())) list.add(group);
        }

        if(list.isEmpty()){
            list.addAll(getBaseGroups(player));
            groups.addAll(list);
        }

        return list;
    }

    public List<RecruitsGroup> getAllGroups() {
        return groups;
    }

    public void assignRecruitToGroup(AbstractRecruitEntity recruit) {
        recruit.getGroup();


        recruit.updateGroup();
    }

    public void broadCastGroupsToPlayer(ServerLevel level, UUID playerUUID) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
           if(player.getUUID().equals(playerUUID)){
               broadCastGroupsToPlayer(player);
               return;
           }
        }
    }
    public void broadCastGroupsToPlayer(Player player) {
        if (player == null) return;

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                new MessageToClientUpdateGroups(
                        RecruitsGroup.listToNbt(getPlayerGroups(player))
                ));
    }


    public static List<RecruitsGroup> getBaseGroups(Player player){
        return Arrays.asList(
                new RecruitsGroup("Infantry", player, 0),
                new RecruitsGroup("Ranged", player, 2),
                new RecruitsGroup("Cavalry", player, 5),
                new RecruitsGroup("Ranged Cavalry", player, 6)
        );
    }

    public void addRedirect(UUID oldId, UUID newId) {
        redirects.put(oldId, newId);
    }

    public UUID resolve(UUID uuid) {
        while (redirects.containsKey(uuid)) {
            uuid = redirects.get(uuid);
        }
        return uuid;
    }

    public void mergeGroups(RecruitsGroup groupToMerge, RecruitsGroup baseGroup) {
        int newSize = baseGroup.getSize() + groupToMerge.getSize();
        baseGroup.setSize(newSize);

        addRedirect(groupToMerge.getUUID(), baseGroup.getUUID());

        removeGroup(groupToMerge);
    }

    public void splitGroup(RecruitsGroup groupToSplit) {
        RecruitsGroup newGroup = groupToSplit.copy();
    }
}


