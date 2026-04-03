package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;

public class MessageToClientUpdateEmbargoes implements Message<MessageToClientUpdateEmbargoes> {

    private CompoundTag embargoNbt;

    public MessageToClientUpdateEmbargoes() {
    }

    public MessageToClientUpdateEmbargoes(Map<UUID, String> embargoMap) {
        this.embargoNbt = RecruitsDiplomacyManager.embargoMapToNbt(embargoMap);
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.embargoMap = RecruitsDiplomacyManager.embargoMapFromNbt(embargoNbt);
    }

    @Override
    public MessageToClientUpdateEmbargoes fromBytes(FriendlyByteBuf buf) {
        this.embargoNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(embargoNbt);
    }
}
