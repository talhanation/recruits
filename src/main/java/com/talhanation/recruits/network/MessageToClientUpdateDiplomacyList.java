package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.Map;


public class MessageToClientUpdateDiplomacyList implements Message<MessageToClientUpdateDiplomacyList> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateDiplomacyList> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdatediplomacylist"));
    private CompoundTag diplomacyNbt;
    public MessageToClientUpdateDiplomacyList() {
    }

    public MessageToClientUpdateDiplomacyList(Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyStatusMap) {
        this.diplomacyNbt = RecruitsDiplomacyManager.mapToNbt(diplomacyStatusMap);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(IPayloadContext context) {
        ClientManager.diplomacyMap = RecruitsDiplomacyManager.mapFromNbt(diplomacyNbt);
    }

    @Override
    public MessageToClientUpdateDiplomacyList fromBytes(RegistryFriendlyByteBuf buf) {
        this.diplomacyNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.diplomacyNbt);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateDiplomacyList> type() {
        return TYPE;
    }
}