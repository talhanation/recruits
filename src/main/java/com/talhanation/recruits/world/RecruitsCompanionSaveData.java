package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves the companion register in the overworld. The map is the live map of the
 * {@link RecruitsCompanionManager}, so a world save always writes the current state.
 */
public class RecruitsCompanionSaveData extends SavedData {
    private static final String DATA_NAME = "recruits_companions";

    final Map<UUID, CompanionRecord> records = new HashMap<>();

    public static RecruitsCompanionSaveData load(CompoundTag nbt) {
        RecruitsCompanionSaveData data = new RecruitsCompanionSaveData();
        ListTag list = nbt.getList("companions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompanionRecord record = CompanionRecord.load(list.getCompound(i));
            if (record != null) data.records.put(record.uuid, record);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (CompanionRecord record : records.values()) {
            list.add(record.save());
        }
        nbt.put("companions", list);
        return nbt;
    }

    public static RecruitsCompanionSaveData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(RecruitsCompanionSaveData::load, RecruitsCompanionSaveData::new, DATA_NAME);
    }
}
