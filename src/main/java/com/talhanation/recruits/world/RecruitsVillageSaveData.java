package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Speichert alle Dörfer (aller Dimensionen) und die aktiven Aufträge in der Overworld – wie die Claims.
 */
public class RecruitsVillageSaveData extends SavedData {
    private static final String FILE_ID = "recruitsVillages";

    private List<RecruitsVillage> villages = new ArrayList<>();
    private Map<UUID, NobleQuestProgress> activeQuests = new HashMap<>();

    public static RecruitsVillageSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RecruitsVillageSaveData::load, RecruitsVillageSaveData::new, FILE_ID);
    }

    public static RecruitsVillageSaveData load(CompoundTag nbt) {
        RecruitsVillageSaveData data = new RecruitsVillageSaveData();

        ListTag villageList = nbt.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < villageList.size(); i++) {
            RecruitsVillage village = RecruitsVillage.fromNBT(villageList.getCompound(i));
            if (village != null) data.villages.add(village);
        }

        ListTag questList = nbt.getList("activeQuests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag entry = questList.getCompound(i);
            if (!entry.hasUUID("player")) continue;
            NobleQuestProgress progress = NobleQuestProgress.fromNBT(entry.getCompound("progress"));
            if (progress != null) data.activeQuests.put(entry.getUUID("player"), progress);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag villageList = new ListTag();
        for (RecruitsVillage village : villages) villageList.add(village.toNBT());
        nbt.put("villages", villageList);

        ListTag questList = new ListTag();
        activeQuests.forEach((player, progress) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.put("progress", progress.toNBT());
            questList.add(entry);
        });
        nbt.put("activeQuests", questList);
        return nbt;
    }

    public List<RecruitsVillage> getVillages() {
        return villages;
    }

    public void setVillages(List<RecruitsVillage> villages) {
        this.villages = villages;
    }

    public Map<UUID, NobleQuestProgress> getActiveQuests() {
        return activeQuests;
    }

    public void setActiveQuests(Map<UUID, NobleQuestProgress> activeQuests) {
        this.activeQuests = activeQuests;
    }
}
