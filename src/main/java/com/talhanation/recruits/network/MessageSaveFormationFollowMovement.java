package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.UUID;

public class MessageSaveFormationFollowMovement implements Message<MessageSaveFormationFollowMovement> {

    public static final CustomPacketPayload.Type<MessageSaveFormationFollowMovement> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesaveformationfollowmovement"));
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

    public void executeServerSide(IPayloadContext context){
        CommandEvents.saveFormation(((ServerPlayer) context.player()), formation);
        CommandEvents.saveUUIDList(((ServerPlayer) context.player()), "ActiveGroups", RecruitsGroup.uuidListFromNbt(groups));
    }

    public MessageSaveFormationFollowMovement fromBytes(RegistryFriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.groups = buf.readNbt();
        this.formation = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeNbt(this.groups);
        buf.writeInt(this.formation);
    }


    @Override
    public CustomPacketPayload.Type<MessageSaveFormationFollowMovement> type() {
        return TYPE;
    }
}