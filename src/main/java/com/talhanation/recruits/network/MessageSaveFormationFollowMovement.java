package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;
import java.util.UUID;

public class MessageSaveFormationFollowMovement implements RecruitsMessage<MessageSaveFormationFollowMovement> {

    private UUID player_uuid;

    private CompoundTag groups;
    private int formation;

    public MessageSaveFormationFollowMovement(){
    }

    public MessageSaveFormationFollowMovement(UUID player_uuid, List<UUID> groups, int formation) {
        this.player_uuid = player_uuid;
        this.groups = RecruitsGroup.uuidListToNbt(groups);
        this.formation = formation;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = context.getSender();
        if (player == null || !player.getUUID().equals(this.player_uuid)) return;
        List<UUID> requestedGroups = RecruitsGroup.uuidListFromNbt(groups);
        requestedGroups.removeIf(groupUuid -> !RecruitCommandAuthority.ownsGroup(player, groupUuid));
        CommandEvents.saveFormation(player, formation);
        CommandEvents.saveUUIDList(player, "ActiveGroups", requestedGroups);
    }

    public MessageSaveFormationFollowMovement fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.groups = buf.readNbt();
        this.formation = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeNbt(this.groups);
        buf.writeInt(this.formation);
    }

}
