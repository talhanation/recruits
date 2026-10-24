package com.talhanation.recruits.network;

import com.talhanation.recruits.client.governor.GovernorClientHandler;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

/** Snapshot of the Governor data of one claim; optionally opens the Governor screen. */
public class MessageToClientGovernorData implements Message<MessageToClientGovernorData> {

    private CompoundTag snapshot;
    private boolean openScreen;

    public MessageToClientGovernorData() {
    }

    public MessageToClientGovernorData(CompoundTag snapshot, boolean openScreen) {
        this.snapshot = snapshot;
        this.openScreen = openScreen;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        GovernorClientHandler.onData(snapshot, openScreen);
    }

    @Override
    public MessageToClientGovernorData fromBytes(FriendlyByteBuf buf) {
        this.snapshot = buf.readNbt();
        this.openScreen = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(snapshot);
        buf.writeBoolean(openScreen);
    }
}
