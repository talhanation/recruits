package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.CompanionListScreen;
import com.talhanation.recruits.world.CompanionRecord;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** Server → Client: the player's companions and where they are, for the companion list of his Messenger. */
public class MessageToClientCompanionList implements Message<MessageToClientCompanionList> {
    private static final int MAX_COMPANIONS = 512;
    private List<CompanionRecord> companions;

    public MessageToClientCompanionList() {
    }

    public MessageToClientCompanionList(List<CompanionRecord> companions) {
        this.companions = companions.size() > MAX_COMPANIONS ? companions.subList(0, MAX_COMPANIONS) : companions;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        if (Minecraft.getInstance().screen instanceof CompanionListScreen screen) screen.setCompanions(this.companions);
    }

    @Override
    public MessageToClientCompanionList fromBytes(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_COMPANIONS) throw new IllegalArgumentException("Too many companions: " + size);
        this.companions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) this.companions.add(CompanionRecord.fromBytes(buf));
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.companions.size());
        for (CompanionRecord record : this.companions) record.toBytes(buf);
    }
}
