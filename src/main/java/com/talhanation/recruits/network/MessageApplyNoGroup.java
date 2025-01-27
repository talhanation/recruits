package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;

public class MessageApplyNoGroup implements Message<MessageApplyNoGroup> {

    private UUID owner;
    private int groupID;

    public MessageApplyNoGroup(){
    }

    public MessageApplyNoGroup(UUID owner, int groupID) {
        this.owner = owner;
        this.groupID = groupID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> recruitList = new ArrayList<>();
        if(player.getCommandSenderWorld() instanceof ServerLevel serverLevel){
            for(Entity entity : serverLevel.getEntities().getAll()){
                if(entity instanceof AbstractRecruitEntity recruit && recruit.isEffectedByCommand(owner, groupID))
                    recruitList.add(recruit);
            }
        }

        for(AbstractRecruitEntity recruit : recruitList){
            recruit.setGroup(0);
        }
    }
    public MessageApplyNoGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.groupID = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeInt(this.groupID);
    }

}