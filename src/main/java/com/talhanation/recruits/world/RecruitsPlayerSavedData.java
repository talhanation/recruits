package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;


public class RecruitsPlayerSavedData extends SavedData {
    public static int recruits;
    public RecruitsPlayerSavedData(){
        super();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putInt("recruits", recruits);

        return nbt;
    }
    public static RecruitsPlayerSavedData load(CompoundTag nbt) {
        RecruitsPlayerSavedData data = new RecruitsPlayerSavedData();
        if (nbt.contains("recruits")) {
            recruits = nbt.getInt("recruits");
        }

        return data;
    }

    public static void setRecruits(int x) {
        recruits = x;
    }
}

