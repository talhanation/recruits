package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;


public class MessageUpdateGroup implements RecruitsMessage<MessageUpdateGroup> {

    private CompoundTag groupNBT;

    public MessageUpdateGroup(){

    }

    public MessageUpdateGroup(RecruitsGroup group) {
        this.groupNBT = group.toNBT();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        RecruitsGroup updatedGroup = RecruitsGroup.fromNBT(this.groupNBT);
        ServerPlayer serverPLayer = context.getSender();
        if (serverPLayer == null || updatedGroup == null) return;
        if (!serverPLayer.getUUID().equals(updatedGroup.getPlayerUUID())) return;
        RecruitsGroup existingGroup = RecruitEvents.recruitsGroupsManager.getGroup(updatedGroup.getUUID());
        if (existingGroup != null && !serverPLayer.getUUID().equals(existingGroup.getPlayerUUID())) return;

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup((ServerLevel) serverPLayer.getCommandSenderWorld(), serverPLayer, updatedGroup);
    }
    public MessageUpdateGroup fromBytes(FriendlyByteBuf buf) {
        this.groupNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(groupNBT);
    }
}
