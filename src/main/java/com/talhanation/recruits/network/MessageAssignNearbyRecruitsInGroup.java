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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageAssignNearbyRecruitsInGroup implements Message<MessageAssignNearbyRecruitsInGroup> {

    public static final CustomPacketPayload.Type<MessageAssignNearbyRecruitsInGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassignnearbyrecruitsingroup"));
    private UUID groupUUID;

    public MessageAssignNearbyRecruitsInGroup() {
    }

    public MessageAssignNearbyRecruitsInGroup(UUID group) {
        this.groupUUID = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        RecruitsGroup newGroup = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if(newGroup == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.isEffectedByCommand(player.getUUID())
        ).forEach((recruit) -> this.setGroup(recruit, newGroup));

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup(player.serverLevel(), player, newGroup);

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
    }

    public void setGroup(AbstractRecruitEntity recruit, RecruitsGroup group){
        if(recruit.getGroupUUID().isPresent() && recruit.getGroupUUID().get().equals(group)){
            return;
        }

        group.addMember(recruit.getUUID());
        RecruitsGroup oldGroup = RecruitEvents.recruitsGroupsManager.getGroup(recruit.getGroup());
        if(oldGroup != null) oldGroup.removeMember(recruit.getUUID());

        recruit.setGroupUUID(groupUUID);
    }

    public MessageAssignNearbyRecruitsInGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageAssignNearbyRecruitsInGroup> type() {
        return TYPE;
    }
}