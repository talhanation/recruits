package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageClearUpkeepGui implements Message<MessageClearUpkeepGui> {

    public static final CustomPacketPayload.Type<MessageClearUpkeepGui> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageclearupkeepgui"));
    private UUID uuid;

    public MessageClearUpkeepGui(){
    }

    public MessageClearUpkeepGui(UUID uuid) {
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16.0D),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> {
            recruit.clearUpkeepPos();
            recruit.clearUpkeepEntity();
        });
    }

    public MessageClearUpkeepGui fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }


    @Override
    public CustomPacketPayload.Type<MessageClearUpkeepGui> type() {
        return TYPE;
    }
}