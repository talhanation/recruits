package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class RecruitsGroupsSaveData extends SavedData {
    private static final String FILE_ID = "recruitsGroups";
    private List<RecruitsGroup> groups = new ArrayList<>();

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
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (RecruitsGroup group : this.groups) {
            list.add(group.toNBT());
        }
        nbt.put("groups", list);
        return nbt;
    }

    public List<RecruitsGroup> getAllGroups() {
        return this.groups;
    }

    public void setAllGroups(List<RecruitsGroup> groups) {
        this.groups = groups;
    }
}


