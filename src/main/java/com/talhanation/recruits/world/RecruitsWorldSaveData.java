package com.talhanation.recruits.world;
import net.minecraft.core.HolderLookup;

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
                new SavedData.Factory<>(() -> new RecruitsWorldSaveData(null), RecruitsWorldSaveData::load),
                FILE_ID);
    }

    public static RecruitsWorldSaveData load(CompoundTag nbt, HolderLookup.Provider provider) {
        UUID worldId = nbt.hasUUID("WorldId") ? nbt.getUUID("WorldId") : null;
        return new RecruitsWorldSaveData(worldId);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.putUUID("WorldId", worldId);
        return nbt;
    }

    public UUID getWorldId() {
        return worldId;
    }
}
