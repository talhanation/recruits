package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
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
    private final Map<UUID, RecruitsGroup> groupMap = new HashMap<>();
    private final Map<UUID, UUID> redirects = new HashMap<>();
    private final Map<UUID, UUID> recruitRedirects = new HashMap<>();
    public void load(ServerLevel level) {
        RecruitsGroupsSaveData data = RecruitsGroupsSaveData.get(level);
        groups.clear();
        redirects.clear();

        for (RecruitsGroup group : data.getAllGroups()) {
            this.addGroup(group);
        }

        redirects.putAll(data.getRedirects());
    }

    public void save(ServerLevel level) {
        RecruitsGroupsSaveData data = RecruitsGroupsSaveData.get(level);

        data.setAllGroups(groups);
        data.setRedirects(redirects);

        data.setDirty();
    }
    public void addOrUpdateGroup(ServerLevel level, ServerPlayer player, RecruitsGroup incoming) {
        if (incoming == null || level == null) return;

        UUID id = resolveGroup(incoming.getUUID());
        RecruitsGroup existing = groupMap.get(id);

        if (existing != null) {
            incoming.members = existing.members;
            incoming.setUUID(existing.getUUID());
        }

        removeGroup(id);

        if(!incoming.removed){
            this.addGroup(incoming);
        }

        save(level);
        broadCastGroupsToPlayer(player);
    }

    private void addGroup(RecruitsGroup group){
        groups.add(group);
        groupMap.put(group.getUUID(), group);
    }

    public void removeGroup(UUID group) {
        if (group == null) return;
        groupMap.remove(group);
        groups.removeIf(saved -> saved.getUUID().equals(group));

    }

    @Nullable
    public RecruitsGroup getGroup(UUID groupUUID) {
        groupUUID = resolveGroup(groupUUID);

        return groupMap.get(groupUUID);
    }

    public void addMember(UUID groupUUID, UUID member, ServerLevel serverLevel) {
        groupUUID = resolveGroup(groupUUID);

        RecruitsGroup group = groupMap.get(groupUUID);
        if (group == null) return;

        group.addMember(member);

        save(serverLevel);
        broadCastGroupsToPlayer(serverLevel, group.getPlayerUUID());
    }

    public void removeMember(UUID groupUUID, UUID member, ServerLevel serverLevel) {
        groupUUID = resolveGroup(groupUUID);

        RecruitsGroup group = groupMap.get(groupUUID);
        if (group == null) return;

        group.removeMember(member);

        save(serverLevel);
        broadCastGroupsToPlayer(serverLevel, group.getPlayerUUID());
    }

    public List<RecruitsGroup> getPlayerGroups(Player player) {
        List<RecruitsGroup> list = new ArrayList<>();
        for(RecruitsGroup group : groups){
            if(group.getPlayerUUID().equals(player.getUUID())) list.add(group);
        }

        if(list.isEmpty()){
            list.addAll(getBaseGroups(player));
            for (RecruitsGroup group : list) {
                this.addGroup(group);
            }
        }

        return list;
    }

    public List<RecruitsGroup> getPlayerGroupsForClient(Player player){
        List<RecruitsGroup> list = new ArrayList<>();

        for(RecruitsGroup group : getPlayerGroups(player)){
            RecruitsGroup copy = group.copy();
            copy.setUUID(group.getUUID());

            list.add(copy);
        }

        return list;
    }

    @Nullable
    public RecruitsGroup getPlayersGroupByName(Player player, String name) {
        if (player == null || name == null) return null;
        List<RecruitsGroup> groups = getPlayerGroups(player);

        for (RecruitsGroup group : groups) {
            if (group.getName().equalsIgnoreCase(name)) {
                return group;
            }
        }
        return null;
    }

    public List<RecruitsGroup> getAllGroups() {
        return groups;
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
                        RecruitsGroup.listToNbt(getPlayerGroupsForClient(player))
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

    public UUID resolveGroup(UUID uuid) {
        while (redirects.containsKey(uuid)) {
            uuid = redirects.get(uuid);
        }
        return uuid;
    }

    public UUID resolveRecruit(UUID recruitUUID, UUID fallbackGroupUUID) {
        if (recruitRedirects.containsKey(recruitUUID)) {
            return recruitRedirects.remove(recruitUUID);
        }
        return fallbackGroupUUID;
    }

    public void mergeGroups(RecruitsGroup groupToMerge, RecruitsGroup baseGroup, ServerLevel serverLevel) {
        baseGroup.members.addAll(groupToMerge.members);

        baseGroup.setSize(baseGroup.members.size());

        addRedirect(groupToMerge.getUUID(), baseGroup.getUUID());

        updateRecruitEntities(serverLevel, baseGroup, groupToMerge.members);

        removeGroup(groupToMerge.getUUID());

        this.save(serverLevel);
        this.broadCastGroupsToPlayer(serverLevel, groupToMerge.getPlayerUUID());
    }

    public void splitGroup(RecruitsGroup original, ServerLevel serverLevel) {
        List<UUID> members = new ArrayList<>(original.members);
        if (members.size() < 2) return;

        RecruitsGroup newGroup = original.copy();
        newGroup.setUUID(UUID.randomUUID());
        newGroup.setName(getNextGroupName(original.getName()));
        newGroup.members.clear();

        List<UUID> remain = new ArrayList<>();
        List<UUID> moved  = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            if (i % 2 == 0) moved.add(members.get(i));
            else remain.add(members.get(i));
        }

        // Listen setzen
        original.members = remain;
        newGroup.members = moved;

        original.setSize(remain.size());
        newGroup.setSize(moved.size());

        addGroup(newGroup);

        updateRecruitEntities(serverLevel, original, remain);
        updateRecruitEntities(serverLevel, newGroup, moved);

        save(serverLevel);
        broadCastGroupsToPlayer(serverLevel, original.getPlayerUUID());
    }

    private void updateRecruitEntities(ServerLevel level, RecruitsGroup newGroup, List<UUID> memberList) {
        for (UUID uuid : memberList) {
            AbstractRecruitEntity recruit = level.getEntity(uuid) instanceof AbstractRecruitEntity r ? r : null;

            if (recruit != null) {
                recruit.setGroupUUID(newGroup.getUUID());
                recruit.needsGroupUpdate = true;
            }
            else {
                recruitRedirects.put(uuid, newGroup.getUUID());
            }
        }
    }

    private static final String[] ROMAN = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    };

    private String getNextGroupName(String baseName) {
        String[] parts = baseName.trim().split(" ");
        String last = parts[parts.length - 1];

        int current = 1;

        for (int i = 1; i < ROMAN.length; i++) {
            if (ROMAN[i].equals(last)) {
                current = i + 1;
                baseName = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
                break;
            }
        }

        if (current >= ROMAN.length)
            return baseName + " " + (current);

        return baseName + " " + ROMAN[current];
    }

}


