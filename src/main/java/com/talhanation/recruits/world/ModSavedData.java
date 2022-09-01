package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;

public class ModSavedData extends SavedData {
    private ServerLevel level;
    private static final Map<Level, ModSavedData> dataMap = new HashMap<>();
    private PlayerTeam team;
    private CompoundTag banner;

    public ModSavedData(){
        super();
    }

    @Override
    public CompoundTag save(CompoundTag p_77763_) {
        return null;
    }

    public static ModSavedData get(Level level){
        if(level instanceof ServerLevel){
            ServerLevel serverLevel = level.getServer().getLevel(Level.OVERWORLD);
            ModSavedData fromMap = dataMap.get(serverLevel);
            if(fromMap == null){
                DimensionDataStorage storage = serverLevel.getDataStorage();
                ModSavedData data = storage.computeIfAbsent(ModSavedData::load, ModSavedData::new, "recruits_level_data");

                if(data != null){
                    data.level = serverLevel;
                    data.setDirty();
                }

                dataMap.put(level, data);
                return data;
            }
            return fromMap;
        }
        return null;
    }

    public static ModSavedData load(CompoundTag nbt) {
        ModSavedData data = new ModSavedData();

        //data.banner = nbt.get(getTeam().getName() + "_TeamBanner")

        return data;
    }

    public CompoundTag getBannerNBT() {
        return this.banner;
    }
}
