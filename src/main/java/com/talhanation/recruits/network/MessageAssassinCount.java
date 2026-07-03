package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.AssassinLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssassinCount implements Message<MessageAssassinCount> {

    public static final CustomPacketPayload.Type<MessageAssassinCount> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassassincount"));
    private int count;
    private UUID uuid;

    public MessageAssassinCount(){
    }

    public MessageAssassinCount(int count, UUID uuid) {
        this.count = count;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AssassinLeaderEntity.class,
                player.getBoundingBox().inflate(16.0D),
                (leader) -> leader.getUUID().equals(this.uuid)
        ).forEach((leader) -> leader.setCount(this.count));
    }
    public MessageAssassinCount fromBytes(RegistryFriendlyByteBuf buf) {
        this.count = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(count);
        buf.writeUUID(uuid);
    }


    @Override
    public CustomPacketPayload.Type<MessageAssassinCount> type() {
        return TYPE;
    }
}