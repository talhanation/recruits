package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageRangedFire implements Message<MessageRangedFire> {

    public static final CustomPacketPayload.Type<MessageRangedFire> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagerangedfire"));
    private UUID player;
    private UUID group;
    private boolean should;

    public MessageRangedFire(){
    }

    public MessageRangedFire(UUID player, UUID group, boolean shields) {
        this.player = player;
        this.group = group;
        this.should = shields;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = ((ServerPlayer) context.player());
        List<AbstractRecruitEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, ((ServerPlayer) context.player()).getBoundingBox().inflate(100));
        for (AbstractRecruitEntity recruits : list) {
                CommandEvents.onRangedFireCommand(serverPlayer, this.player, recruits, group, should);
        }
    }
    public MessageRangedFire fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.should = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.should);
    }


    @Override
    public CustomPacketPayload.Type<MessageRangedFire> type() {
        return TYPE;
    }
}