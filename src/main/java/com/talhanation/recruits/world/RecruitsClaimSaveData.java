package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;
public class RecruitsClaimSaveData extends SavedData {
    private static final String FILE_ID = "recruitsClaims";
    private static final SavedData.Factory<RecruitsClaimSaveData> FACTORY = new SavedData.Factory<>(RecruitsClaimSaveData::new, RecruitsClaimSaveData::load);
    private List<RecruitsClaim> claimList = new ArrayList<>();

    public static RecruitsClaimSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static RecruitsClaimSaveData load(CompoundTag nbt, HolderLookup.Provider registries) {
        RecruitsClaimSaveData data = new RecruitsClaimSaveData();
        if (nbt.contains("claims", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("claims", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                data.claimList.add(RecruitsClaim.fromNBT((CompoundTag) t));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (RecruitsClaim claim : this.claimList) {
            list.add(claim.toNBT());
        }
        nbt.put("claims", list);
        return nbt;
    }

    public List<RecruitsClaim> getAllClaims() {
        return this.claimList;
    }

    public void setAllClaims(List<RecruitsClaim> claims) {
        this.claimList = claims;
    }
}

