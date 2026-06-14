package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public class RecruitsWorldSaveData extends SavedData {
    private static final String FILE_ID = "recruitsWorldId";
    private UUID worldId;

    public RecruitsWorldSaveData() {
        this.worldId = UUID.randomUUID();
        this.setDirty();
    }

    private RecruitsWorldSaveData(UUID worldId) {
        this.worldId = worldId == null ? UUID.randomUUID() : worldId;
        if (worldId == null) this.setDirty();
    }

    public static RecruitsWorldSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RecruitsWorldSaveData::load,
                RecruitsWorldSaveData::new,
                FILE_ID);
    }

    public static RecruitsWorldSaveData load(CompoundTag nbt) {
        UUID worldId = nbt.hasUUID("WorldId") ? nbt.getUUID("WorldId") : null;
        return new RecruitsWorldSaveData(worldId);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putUUID("WorldId", worldId);
        return nbt;
    }

    public UUID getWorldId() {
        return worldId;
    }
}
