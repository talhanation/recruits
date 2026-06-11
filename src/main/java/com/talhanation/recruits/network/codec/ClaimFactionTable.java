package com.talhanation.recruits.network.codec;

import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ClaimFactionTable {
    private static final int MAX_FACTIONS_PER_PACKET = 4096;

    private final List<RecruitsFaction> factions = new ArrayList<>();
    private final Map<String, Integer> indexesByStringId = new HashMap<>();
    private final IdentityHashMap<RecruitsFaction, Integer> indexesByIdentity = new IdentityHashMap<>();

    private ClaimFactionTable() {}

    static ClaimFactionTable collect(List<RecruitsClaim> claims) {
        ClaimFactionTable table = new ClaimFactionTable();

        for (RecruitsClaim claim : claims) {
            table.add(claim.getOwnerFaction());
            RecruitsPlayerInfo playerInfo = claim.getPlayerInfo();
            if (playerInfo != null) {
                table.add(playerInfo.getFaction());
            }
            table.addAll(claim.defendingParties);
            table.addAll(claim.attackingParties);
        }

        return table;
    }

    static ClaimFactionTable read(FriendlyByteBuf buf) {
        ClaimFactionTable table = new ClaimFactionTable();
        int count = NetworkBufferHelper.readBoundedVarInt(buf, MAX_FACTIONS_PER_PACKET, "factions");

        for (int i = 0; i < count; i++) {
            CompoundTag tag = buf.readNbt();
            table.factions.add(RecruitsFaction.fromNBT(tag == null ? new CompoundTag() : tag));
        }

        return table;
    }

    void write(FriendlyByteBuf buf) {
        buf.writeVarInt(factions.size());
        for (RecruitsFaction faction : factions) {
            buf.writeNbt(faction.toNBT());
        }
    }

    int indexOf(@Nullable RecruitsFaction faction) {
        if (faction == null) return -1;

        String stringId = faction.getStringID();
        if (stringId != null && !stringId.isEmpty()) {
            Integer index = indexesByStringId.get(stringId);
            return index == null ? -1 : index;
        }

        Integer index = indexesByIdentity.get(faction);
        return index == null ? -1 : index;
    }

    int size() {
        return factions.size();
    }

    @Nullable
    RecruitsFaction faction(int index) {
        if (index < 0 || index >= factions.size()) {
            throw new IllegalArgumentException("Invalid faction reference: " + index);
        }
        return factions.get(index);
    }

    private void addAll(List<RecruitsFaction> factions) {
        if (factions == null) return;

        for (RecruitsFaction faction : factions) {
            add(faction);
        }
    }

    private void add(@Nullable RecruitsFaction faction) {
        if (faction == null || indexOf(faction) >= 0) return;

        int index = factions.size();
        factions.add(faction);
        String stringId = faction.getStringID();
        if (stringId != null && !stringId.isEmpty()) {
            indexesByStringId.put(stringId, index);
        } else {
            indexesByIdentity.put(faction, index);
        }
    }
}
