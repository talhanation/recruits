package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Client → Server: assigns a group UUID to a leader entity so the
 * subsequent {@link MessageAssignGroupToCompanion} knows which group to use.
 */
public class MessageSetLeaderGroup implements Message<MessageSetLeaderGroup> {

    public static final CustomPacketPayload.Type<MessageSetLeaderGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesetleadergroup"));
    private UUID leaderUUID;
    @Nullable
    private UUID groupUUID;

    public MessageSetLeaderGroup() {}

    public MessageSetLeaderGroup(UUID leaderUUID, @Nullable UUID groupUUID) {
        this.leaderUUID = leaderUUID;
        this.groupUUID  = groupUUID;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.serverLevel().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100D),
                leader -> leader.getUUID().equals(this.leaderUUID) && leader.isAlive()
        ).forEach(leader -> {
            if (groupUUID == null) {
                leader.setGroupUUID(null);
                return;
            }
            RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
            if (group == null) return;
            leader.setGroupUUID(group.getUUID());
            RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        });
    }

    @Override
    public MessageSetLeaderGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.leaderUUID = buf.readUUID();
        boolean hasGroup = buf.readBoolean();
        this.groupUUID  = hasGroup ? buf.readUUID() : null;
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.leaderUUID);
        buf.writeBoolean(this.groupUUID != null);
        if (this.groupUUID != null) buf.writeUUID(this.groupUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageSetLeaderGroup> type() {
        return TYPE;
    }
}
