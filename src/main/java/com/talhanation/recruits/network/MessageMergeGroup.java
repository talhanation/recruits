package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageMergeGroup implements Message<MessageMergeGroup> {

    public static final CustomPacketPayload.Type<MessageMergeGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagemergegroup"));
    private UUID groupUUID;
    private UUID mergeUUID;

    public MessageMergeGroup() {
    }

    public MessageMergeGroup(UUID mergeUUID, UUID groupUUID) {
        this.mergeUUID = mergeUUID;
        this.groupUUID = groupUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        RecruitsGroup groupToMerge = RecruitEvents.recruitsGroupsManager.getGroup(mergeUUID);
        RecruitsGroup baseGroup = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToMerge == null || baseGroup == null) return;

        RecruitEvents.recruitsGroupsManager.mergeGroups(groupToMerge, baseGroup, player.serverLevel());
    }

    public MessageMergeGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        this.mergeUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
        buf.writeUUID(mergeUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageMergeGroup> type() {
        return TYPE;
    }
}