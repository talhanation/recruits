package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.entities.ScoutEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageScoutTask implements Message<MessageScoutTask> {

    public static final CustomPacketPayload.Type<MessageScoutTask> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagescouttask"));
    private UUID recruit;
    private int state;

    public MessageScoutTask() {
    }
    public MessageScoutTask(UUID recruit, int state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        List<ScoutEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(ScoutEntity.class, ((ServerPlayer) context.player()).getBoundingBox().inflate(16D));
        for (ScoutEntity scoutEntity : list){

            if (scoutEntity.getUUID().equals(this.recruit)){

                scoutEntity.startTask(ScoutEntity.State.fromIndex(state));
                break;
            }
        }
    }
    public MessageScoutTask fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }

    @Override
    public CustomPacketPayload.Type<MessageScoutTask> type() {
        return TYPE;
    }
}
