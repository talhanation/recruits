package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageGroup implements Message<MessageGroup> {

    public static final CustomPacketPayload.Type<MessageGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagegroup"));
    private UUID groupUUID;
    private UUID recruitUUID;

    public MessageGroup() {
    }

    public MessageGroup(UUID groupUUID, UUID recruitUUID) {
        this.groupUUID = groupUUID;
        this.recruitUUID = recruitUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.getUUID().equals(this.recruitUUID)
        ).forEach((recruit) -> this.setGroup(recruit, player, groupUUID));
    }

    public void setGroup(AbstractRecruitEntity recruit, ServerPlayer player , UUID groupUUID){
        RecruitsGroup oldGroup = RecruitEvents.recruitsGroupsManager.getGroup(recruit.getGroup());
        RecruitsGroup newGroup = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if(oldGroup != null && newGroup != null && oldGroup.getUUID().equals(newGroup.getUUID())) return;

        if(oldGroup != null) RecruitEvents.recruitsGroupsManager.removeMember(oldGroup.getUUID(), recruit.getUUID(), player.serverLevel());
        if(newGroup != null) RecruitEvents.recruitsGroupsManager.addMember(newGroup.getUUID(), recruit.getUUID(), player.serverLevel());

        recruit.setGroupUUID(newGroup.getUUID());
    }

    public MessageGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        this.recruitUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
        buf.writeUUID(recruitUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageGroup> type() {
        return TYPE;
    }
}