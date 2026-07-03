package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.DebugEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageDebugGui implements Message<MessageDebugGui> {

    public static final CustomPacketPayload.Type<MessageDebugGui> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagedebuggui"));
    private int id;
    private UUID uuid;
    private String name;

    public MessageDebugGui() {
    }

    public MessageDebugGui(int id, UUID uuid, String name) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(16.0D),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> {
            DebugEvents.handleMessage(id, recruit, ((ServerPlayer) context.player()));
            recruit.setCustomName(Component.literal(name));
        });
    }

    public MessageDebugGui fromBytes(RegistryFriendlyByteBuf buf) {
        this.id = buf.readInt();
        this.uuid = buf.readUUID();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUUID(uuid);
        buf.writeUtf(name);
    }

    @Override
    public CustomPacketPayload.Type<MessageDebugGui> type() {
        return TYPE;
    }
}
