package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageAssignGroupToPlayer implements Message<MessageAssignGroupToPlayer> {

    public static final CustomPacketPayload.Type<MessageAssignGroupToPlayer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassigngrouptoplayer"));
    private UUID owner;
    private CompoundTag tag;
    private UUID groupUUID;
    private boolean keepTeam;

    public MessageAssignGroupToPlayer() {
    }

    public MessageAssignGroupToPlayer(UUID owner, RecruitsPlayerInfo newOwner, UUID groupUUID) {
        this.owner = owner;
        this.tag = newOwner.toNBT();
        this.groupUUID = groupUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        ServerLevel serverLevel = (ServerLevel) player.getCommandSenderWorld();
        if(group == null) return;

        RecruitsPlayerInfo newOwner = RecruitsPlayerInfo.getFromNBT(tag);
        group.setPlayer(newOwner);

        List<AbstractRecruitEntity> list = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100D)
        );

        for(AbstractRecruitEntity recruit : list){
            if(recruit.getGroup() != null && recruit.getGroup().equals(groupUUID)){
                recruit.setOwnerUUID(Optional.of(newOwner.getUUID()));
                recruit.needsGroupUpdate = true;
            }
        }

        RecruitEvents.recruitsGroupsManager.save(serverLevel);
        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer((ServerLevel) player.getCommandSenderWorld(), newOwner.getUUID());

        FactionEvents.notifyPlayer(serverLevel, newOwner, 2, group.getName());
    }

    public MessageAssignGroupToPlayer fromBytes(RegistryFriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.tag = buf.readNbt();
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeNbt(tag);
        buf.writeUUID(groupUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageAssignGroupToPlayer> type() {
        return TYPE;
    }
}