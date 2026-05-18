package com.talhanation.recruits.network;

import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.*;

public class MessageApplyNoGroup implements RecruitsMessage<MessageApplyNoGroup> {

    private UUID owner;
    private UUID groupID;

    public MessageApplyNoGroup(){
    }

    public MessageApplyNoGroup(UUID owner, UUID groupID) {
        this.owner = owner;
        this.groupID = groupID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!player.getUUID().equals(this.owner) || !RecruitCommandAuthority.ownsGroup(player, this.groupID)) return;
        List<AbstractRecruitEntity> recruitList = new ArrayList<>();

        for(Entity entity : player.serverLevel().getEntities().getAll()){
            if(entity instanceof AbstractRecruitEntity recruit && recruit.getGroup() != null && recruit.getGroup().equals(groupID) && RecruitCommandAuthority.ownsRecruit(player, recruit))
                recruitList.add(recruit);
        }

        for(AbstractRecruitEntity recruit : recruitList){
            recruit.setGroupUUID(null);
        }
    }
    public MessageApplyNoGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.groupID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.groupID);
    }

}
