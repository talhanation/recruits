package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageSplitGroup implements Message<MessageSplitGroup> {

    public static final CustomPacketPayload.Type<MessageSplitGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesplitgroup"));
    private UUID groupUUID;

    public MessageSplitGroup() {
    }

    public MessageSplitGroup(UUID groupUUID) {
        this.groupUUID = groupUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        RecruitsGroup groupToSplit = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToSplit == null) return;

        RecruitEvents.recruitsGroupsManager.splitGroup(groupToSplit, player.serverLevel());
    }

    public MessageSplitGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageSplitGroup> type() {
        return TYPE;
    }
}