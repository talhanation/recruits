package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class RecruitsGroupsSaveData extends SavedData {
    private static final String FILE_ID = "recruitsGroups";
    private List<RecruitsGroup> groups = new ArrayList<>();
    private Map<UUID, UUID> redirects = new HashMap<>();
    private Map<UUID, UUID> recruitRedirects = new HashMap<>();
    public static RecruitsGroupsSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RecruitsGroupsSaveData::load, RecruitsGroupsSaveData::new, FILE_ID);
    }

    public static RecruitsGroupsSaveData load(CompoundTag nbt) {
        RecruitsGroupsSaveData data = new RecruitsGroupsSaveData();
        if (nbt.contains("groups", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("groups", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                data.groups.add(RecruitsGroup.fromNBT((CompoundTag) t));
            }
        }

        if (nbt.contains("redirects", Tag.TAG_LIST)) {
            ListTag redirectList = nbt.getList("redirects", Tag.TAG_COMPOUND);
            for (Tag t : redirectList) {
                CompoundTag tag = (CompoundTag) t;
                UUID oldId = tag.getUUID("old");
                UUID newId = tag.getUUID("new");
                data.redirects.put(oldId, newId);
            }
        }

        if (nbt.contains("recruitRedirects", Tag.TAG_LIST)) {
            ListTag recruitRedirectList = nbt.getList("recruitRedirects", Tag.TAG_COMPOUND);
            for (Tag t : recruitRedirectList) {
                CompoundTag tag = (CompoundTag) t;
                UUID oldId = tag.getUUID("recruit");
                UUID newId = tag.getUUID("group");
                data.recruitRedirects.put(oldId, newId);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (RecruitsGroup group : this.groups) {
            list.add(group.toNBT());
        }
        nbt.put("groups", list);

        ListTag redirectList = new ListTag();
        for (Map.Entry<UUID, UUID> e : redirects.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("old", e.getKey());
            tag.putUUID("new", e.getValue());
            redirectList.add(tag);
        }
        nbt.put("redirects", redirectList);

        ListTag recruitRedirectList = new ListTag();
        for (Map.Entry<UUID, UUID> e : recruitRedirects.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("recruit", e.getKey());
            tag.putUUID("group", e.getValue());
            recruitRedirectList.add(tag);
        }
        nbt.put("recruitRedirects", recruitRedirectList);

        return nbt;
    }

    public List<RecruitsGroup> getAllGroups() {
        return this.groups;
    }

    public void setAllGroups(List<RecruitsGroup> groups) {
        this.groups = groups;
    }

    public Map<UUID, UUID> getRedirects() {
        return redirects;
    }

    public Map<UUID, UUID> getRecruitRedirects() {
        return recruitRedirects;
    }

    public void setRedirects(Map<UUID, UUID> map){
        this.redirects = map;
    }

    public void setRecruitRedirects(Map<UUID, UUID> map){
        this.recruitRedirects = map;
    }
}


