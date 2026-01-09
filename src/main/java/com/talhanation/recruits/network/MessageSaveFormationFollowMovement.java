package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageSaveFormationFollowMovement implements Message<MessageSaveFormationFollowMovement> {

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

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        CommandEvents.saveFormation(context.getSender(), formation);
        CommandEvents.saveUUIDList(context.getSender(), "ActiveGroups", RecruitsGroup.uuidListFromNbt(groups));
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