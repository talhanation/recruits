package com.talhanation.recruits.world;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Was der Client über einen Noble, sein Dorf und den eigenen Ruf wissen muss.
 * Wird beim Öffnen des Hauptscreens und nach jeder Auftragsaktion geschickt.
 */
public class NobleVillageInfo {

    public UUID nobleId;
    public boolean hasVillage;
    public int inhabitants;
    /** Jobless villagers of the village (without village: around the noble) that can be hired. */
    public int joblessVillagers;

    /** Ruf ist aktiv: Config an und der Noble gehört zu einem Dorf. */
    public boolean reputationActive;
    public int playerReputation;
    public int factionReputation;
    public boolean hasFaction;
    public boolean isLeader;
    public int effectiveReputation;

    public boolean questsActive;
    public final List<NobleQuest> offers = new ArrayList<>();
    public final List<Boolean> offerTaken = new ArrayList<>();

    @Nullable
    public NobleQuest activeQuest;
    public int activeProgress;
    public boolean activeCompleted;
    /** Der aktive Auftrag gehört zum Dorf dieses Nobles. */
    public boolean activeQuestHere;
    public long activeTicksLeft;

    public NobleReputation.Tier getTier() {
        return reputationActive ? NobleReputation.getTier(effectiveReputation) : NobleReputation.Tier.NEUTRAL;
    }

    public boolean isHostile() {
        return reputationActive && getTier() == NobleReputation.Tier.HOSTILE;
    }

    public int getPrice(int basePrice) {
        return reputationActive ? NobleReputation.getPrice(basePrice, effectiveReputation) : basePrice;
    }

    public boolean isTradeLevelUnlocked(int tradeLevel) {
        return !reputationActive || NobleReputation.isTradeLevelUnlocked(effectiveReputation, tradeLevel);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(nobleId);
        buf.writeBoolean(hasVillage);
        buf.writeVarInt(inhabitants);
        buf.writeVarInt(joblessVillagers);

        buf.writeBoolean(reputationActive);
        buf.writeInt(playerReputation);
        buf.writeInt(factionReputation);
        buf.writeBoolean(hasFaction);
        buf.writeBoolean(isLeader);
        buf.writeInt(effectiveReputation);

        buf.writeBoolean(questsActive);
        buf.writeVarInt(offers.size());
        for (int i = 0; i < offers.size(); i++) {
            offers.get(i).toBytes(buf);
            buf.writeBoolean(i < offerTaken.size() && offerTaken.get(i));
        }

        buf.writeBoolean(activeQuest != null);
        if (activeQuest != null) {
            activeQuest.toBytes(buf);
            buf.writeVarInt(activeProgress);
            buf.writeBoolean(activeCompleted);
            buf.writeBoolean(activeQuestHere);
            buf.writeLong(activeTicksLeft);
        }
    }

    public static NobleVillageInfo fromBytes(FriendlyByteBuf buf) {
        NobleVillageInfo info = new NobleVillageInfo();
        info.nobleId = buf.readUUID();
        info.hasVillage = buf.readBoolean();
        info.inhabitants = buf.readVarInt();
        info.joblessVillagers = buf.readVarInt();

        info.reputationActive = buf.readBoolean();
        info.playerReputation = buf.readInt();
        info.factionReputation = buf.readInt();
        info.hasFaction = buf.readBoolean();
        info.isLeader = buf.readBoolean();
        info.effectiveReputation = buf.readInt();

        info.questsActive = buf.readBoolean();
        int offerCount = buf.readVarInt();
        for (int i = 0; i < offerCount; i++) {
            info.offers.add(NobleQuest.fromBytes(buf));
            info.offerTaken.add(buf.readBoolean());
        }

        if (buf.readBoolean()) {
            info.activeQuest = NobleQuest.fromBytes(buf);
            info.activeProgress = buf.readVarInt();
            info.activeCompleted = buf.readBoolean();
            info.activeQuestHere = buf.readBoolean();
            info.activeTicksLeft = buf.readLong();
        }
        return info;
    }
}
