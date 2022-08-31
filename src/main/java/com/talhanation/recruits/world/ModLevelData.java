package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;

public class ModLevelData extends SavedData {
    private ServerLevel level;
    private static final Map<Level, ModLevelData> dataMap = new HashMap<>();
    private PlayerTeam team;
    private BannerItem banner;

    public ModLevelData(){
        super();
    }

    public static ModLevelData get(Level level){
        if(level instanceof ServerLevel){
            ServerLevel serverLevel = level.getServer().getLevel(Level.OVERWORLD);
            ModLevelData fromMap = dataMap.get(serverLevel);
            if(fromMap == null){
                DimensionDataStorage storage = serverLevel.getDataStorage();
                ModLevelData data = storage.computeIfAbsent(ModLevelData::load, ModLevelData::new, "recruits_level_data");

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

    public static ModLevelData load(CompoundTag nbt) {
        ModLevelData data = new ModLevelData();

        //data.banner = nbt.get(getTeam().getName() + "_TeamBanner")

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        if(banner != null && team != null){
            nbt.put(getTeam().getName() + "_TeamBanner", getBanner().getShareTag(getBanner().getDefaultInstance()));
            Main.LOGGER.debug("Saved: "+nbt);
        }
        return nbt;
    }

    public void setBannerAndTeam(BannerItem bannerItem, PlayerTeam team){
        this.banner = bannerItem;
        this.team = team;
    }

    public BannerItem getBanner(){
        return banner;
    }

    public PlayerTeam getTeam(){
        return team;
    }
}
