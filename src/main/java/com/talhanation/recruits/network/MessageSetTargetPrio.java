package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IHasTargetPriority;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageSetTargetPrio implements Message<MessageSetTargetPrio> {

    public static final CustomPacketPayload.Type<MessageSetTargetPrio> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesettargetprio"));
    private UUID recruit;
    private int state;

    public MessageSetTargetPrio() {
    }
    public MessageSetTargetPrio(UUID recruit, int state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, ((ServerPlayer) context.player()).getBoundingBox().inflate(16D));
        for (AbstractRecruitEntity recruitEntity : list){

            if (recruitEntity.getUUID().equals(this.recruit) && recruitEntity instanceof IHasTargetPriority specialRecruit){

                specialRecruit.setTargetPriority(IHasTargetPriority.TargetPriority.fromIndex(state));
                break;
            }
        }
    }
    public MessageSetTargetPrio fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }

    @Override
    public CustomPacketPayload.Type<MessageSetTargetPrio> type() {
        return TYPE;
    }
}
